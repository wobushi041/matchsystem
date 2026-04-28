package com.wobushi041.matchsystem.runner;

import com.wobushi041.matchsystem.mq.CacheWarmupProducer;
import com.wobushi041.matchsystem.model.dto.RecommendCacheSnapshot;
import com.wobushi041.matchsystem.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.matchsystem.model.enums.RecommendCacheStatus;
import com.wobushi041.matchsystem.service.RecommendCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CacheWarmupBootstrapRunner implements ApplicationRunner {

    private static final String CACHE_WARMUP_BOOTSTRAP_LOCK_KEY_PREFIX = "cache:warmup:bootstrap:lock:";

    @Resource
    private CacheWarmupProducer cacheWarmupProducer;

    @Resource
    private RecommendCacheService recommendCacheService;

    @Resource
    private RedissonClient redissonClient;

    @Value("${match.cache.warmup.bootstrap-enabled}")
    private boolean bootstrapEnabled;

    @Value("${match.cache.warmup.seed-user-ids}")
    private String seedUserIds;

    @Value("${match.cache.warmup.default-page-num}")
    private long defaultPageNum;

    @Value("${match.cache.warmup.default-page-size}")
    private long defaultPageSize;

    @Value("${match.cache.warmup.lock-lease-seconds}")
    private long lockLeaseSeconds;

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("cache warmup bootstrap is disabled");
            return;
        }
        try {
            List<Long> userIds = Arrays.stream(seedUserIds.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            for (Long userId : userIds) {
                RecommendCacheWarmupMessage message =
                        cacheWarmupProducer.newWarmupMessage(userId, defaultPageNum, defaultPageSize);
                bootstrapWarmupTask(message);
            }
            log.info("cache warmup bootstrap finished, userIds={}", userIds);
        } catch (Exception e) {
            throw new IllegalStateException("cache warmup bootstrap failed", e);
        }
    }

    private void bootstrapWarmupTask(RecommendCacheWarmupMessage message) throws InterruptedException {
        String lockKey = CACHE_WARMUP_BOOTSTRAP_LOCK_KEY_PREFIX + message.getTaskId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                log.info("cache warmup bootstrap lock busy, taskId={}, runId={}", message.getTaskId(), message.getRunId());
                return;
            }
            RecommendCacheSnapshot cacheSnapshot = recommendCacheService.getRecommendCacheSnapshot(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize());
            boolean refreshed = false;
            if (cacheSnapshot.getStatus() != RecommendCacheStatus.VALID) {
                cacheSnapshot = recommendCacheService.refreshRecommendCache(
                        message.getUserId(),
                        message.getPageNum(),
                        message.getPageSize());
                refreshed = true;
            }
            long nextDelayMillis = recommendCacheService.calculateNextDelayMillis(cacheSnapshot);
            RecommendCacheWarmupMessage nextMessage = cacheWarmupProducer.newWarmupMessage(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize());
            cacheWarmupProducer.scheduleWarmupTask(nextMessage, nextDelayMillis);
            log.info("{} bootstrap warmup, taskId={}, runId={}, status={}, nextDelayMillis={}",
                    refreshed ? "refresh" : "reuse",
                    message.getTaskId(),
                    message.getRunId(),
                    cacheSnapshot.getStatus(),
                    nextDelayMillis);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
