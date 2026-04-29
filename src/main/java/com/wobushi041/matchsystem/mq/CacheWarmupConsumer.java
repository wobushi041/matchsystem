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


/**
 * 监听执行队列
 * 收到消息后加分布式锁
 * 查询当前缓存状态
 * 判断是否需要刷新
 * 计算下一次调度时间
 * 再投递下一轮任务，形成闭环
 */
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
        String redisKey = "cache:warmup:lock:" + message.getTaskId();
        RLock lock = redissonClient.getLock(redisKey);
        boolean locked = false;
        try {
            //得到锁，改true
            locked = lock.tryLock(0, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                log.info("繁忙未得到锁, taskId={}, runId={}", message.getTaskId(), message.getRunId());
                //也直接放弃本次执行，不重试，不重复消费
                return;
            }
            //读当前缓存快照
            //getRecommendCacheSnapshot调用buildSnapshot方法，将redis中的值反序列化成RecommendCacheSnapshot对象
            RecommendCacheSnapshot cacheSnapshot = recommendCacheService.getRecommendCacheSnapshot(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize());
            Page<User> userPage = null;
            boolean refreshed = false;
            //如果状态为无效，刷新缓存快照并得到
            if (cacheSnapshot.getStatus() != RecommendCacheStatus.VALID) {
                cacheSnapshot = recommendCacheService.refreshRecommendCache(
                        message.getUserId(),
                        message.getPageNum(),
                        message.getPageSize());
                userPage = cacheSnapshot.getCacheValue().getUserPage();
                refreshed = true;
            } else {
                //直接得到缓存快照
                userPage = cacheSnapshot.getCacheValue().getUserPage();
            }
            //计算下次调度时间，逻辑封装在recommendCacheService中
            long nextDelayMillis = recommendCacheService.calculateNextDelayMillis(cacheSnapshot);
            //创建下一轮消息并调用cacheWarmupProducer重新投递
            RecommendCacheWarmupMessage nextMessage = cacheWarmupProducer.newWarmupMessage(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize()
            );
            //投递下一轮任务，参数为nextMessage, nextDelayMillis
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
