package com.wobushi041.matchsystem.mq;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.config.RabbitMqConfig;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.dto.RecommendCacheSnapshot;
import com.wobushi041.matchsystem.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.matchsystem.model.enums.RecommendCacheStatus;
import com.wobushi041.matchsystem.service.RecommendCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CacheWarmupConsumer {
    //缓存预热消费者
    @Resource
    private RecommendCacheService recommendCacheService;

    @Resource
    //用于消费之后在投递新任务
    private CacheWarmupProducer cacheWarmupProducer;

    @Resource
    private RedissonClient redissonClient;

    @Value("${match.cache.warmup.lock-lease-seconds}")
    private long lockLeaseSeconds;
    //监听执行队列
    @RabbitListener(queues = RabbitMqConfig.CACHE_WARMUP_EXECUTE_QUEUE)
    public void deliverCallback(RecommendCacheWarmupMessage message) {
        String lockKey = "cache:warmup:lock:" + message.getTaskId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            //得到锁，改true
            locked = lock.tryLock(0, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                log.info("繁忙未得到锁, taskId={}, runId={}", message.getTaskId(), message.getRunId());
                //该消息被正常消费，不重排队
                return;
            }
            RecommendCacheSnapshot cacheSnapshot = recommendCacheService.getRecommendCacheSnapshot(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize());
            Page<User> userPage = null;
            boolean refreshed = false;
            if (cacheSnapshot.getStatus() != RecommendCacheStatus.VALID) {
                cacheSnapshot = recommendCacheService.refreshRecommendCache(
                        message.getUserId(),
                        message.getPageNum(),
                        message.getPageSize());
                userPage = cacheSnapshot.getCacheValue().getUserPage();
                refreshed = true;
            } else {
                userPage = cacheSnapshot.getCacheValue().getUserPage();
            }
            long nextDelayMillis = recommendCacheService.calculateNextDelayMillis(cacheSnapshot);
            RecommendCacheWarmupMessage nextMessage = cacheWarmupProducer.newWarmupMessage(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize()
            );
            cacheWarmupProducer.scheduleWarmupTask(nextMessage, nextDelayMillis);
            log.info("{} recommend cache, taskId={}, runId={}, status={}, total={}, nextDelayMillis={}",
                    refreshed ? "refresh" : "skip refresh",
                    message.getTaskId(),
                    message.getRunId(),
                    cacheSnapshot.getStatus(),
                    userPage == null ? 0 : userPage.getTotal(),
                    nextDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpRejectAndDontRequeueException("cache warmup interrupted", e);
        } catch (Exception e) {
            log.error("refresh recommend cache failed, taskId={}, runId={}", message.getTaskId(), message.getRunId(), e);
            throw new AmqpRejectAndDontRequeueException("cache warmup failed", e);
        } finally {
            //判断当前线程是否持有锁，是则unlock
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
