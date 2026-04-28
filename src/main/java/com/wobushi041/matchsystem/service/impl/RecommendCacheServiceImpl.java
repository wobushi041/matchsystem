package com.wobushi041.matchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.dto.RecommendCacheSnapshot;
import com.wobushi041.matchsystem.model.dto.RecommendCacheValue;
import com.wobushi041.matchsystem.model.enums.RecommendCacheStatus;
import com.wobushi041.matchsystem.service.RecommendCacheService;
import com.wobushi041.matchsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RecommendCacheServiceImpl implements RecommendCacheService {

    private static final long MIN_DELAY_MILLIS = 1L;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Value("${match.cache.warmup.redis-ttl-minutes}")
    private long redisTtlMinutes;

    @Value("${match.cache.warmup.logic-expire-millis}")
    private long logicExpireMillis;

    @Value("${match.cache.warmup.refresh-ahead-millis}")
    private long refreshAheadMillis;

    @Value("${match.cache.warmup.delay-millis}")
    private long fallbackDelayMillis;

    @Override
    public Page<User> getRecommendUsers(long pageNum, long pageSize, long userId) {
        RecommendCacheSnapshot cacheSnapshot = getRecommendCacheSnapshot(userId, pageNum, pageSize);
        if (cacheSnapshot.getStatus() != RecommendCacheStatus.ABSENT
                && cacheSnapshot.getCacheValue() != null
                && cacheSnapshot.getCacheValue().getUserPage() != null) {
            return cacheSnapshot.getCacheValue().getUserPage();
        }
        return queryAndCacheUsers(userId, pageNum, pageSize, false).getCacheValue().getUserPage();
    }

    @Override
    public Page<User> refreshRecommendUsers(long userId, long pageNum, long pageSize) {
        return refreshRecommendCache(userId, pageNum, pageSize).getCacheValue().getUserPage();
    }

    @Override
    public RecommendCacheSnapshot refreshRecommendCache(long userId, long pageNum, long pageSize) {
        return queryAndCacheUsers(userId, pageNum, pageSize, true);
    }

    @Override
    public RecommendCacheSnapshot getRecommendCacheSnapshot(long userId, long pageNum, long pageSize) {
        String redisKey = buildRecommendCacheKey(userId, pageNum, pageSize);
        RBucket<Object> bucket = redissonClient.getBucket(redisKey);
        Object cachedValue = bucket.get();
        return buildSnapshot(redisKey, cachedValue);
    }

    @Override
    public long calculateNextDelayMillis(RecommendCacheSnapshot cacheSnapshot) {
        if (cacheSnapshot == null
                || cacheSnapshot.getCacheValue() == null
                || cacheSnapshot.getCacheValue().getLogicExpireTime() == null) {
            return Math.max(MIN_DELAY_MILLIS, fallbackDelayMillis);
        }
        long nextDelayMillis = cacheSnapshot.getCacheValue().getLogicExpireTime()
                - System.currentTimeMillis()
                - refreshAheadMillis;
        return Math.max(MIN_DELAY_MILLIS, nextDelayMillis);
    }

    @Override
    public String buildRecommendCacheKey(long userId, long pageNum, long pageSize) {
        return String.format("user:recommend:%d:%d:%d", userId, pageNum, pageSize);
    }

    @SuppressWarnings("unchecked")
    private RecommendCacheSnapshot buildSnapshot(String redisKey, Object cachedValue) {
        if (cachedValue == null) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.ABSENT, null);
        }
        RecommendCacheValue cacheValue;
        if (cachedValue instanceof RecommendCacheValue) {
            cacheValue = (RecommendCacheValue) cachedValue;
        } else if (cachedValue instanceof Page) {
            // 兼容旧格式缓存，视为已逻辑过期，触发一次刷新即可迁移到新结构。
            cacheValue = new RecommendCacheValue((Page<User>) cachedValue, 0L);
        } else {
            log.warn("unexpected recommend cache value type, redisKey={}, valueType={}",
                    redisKey, cachedValue.getClass().getName());
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.ABSENT, null);
        }
        if (cacheValue.getUserPage() == null) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.ABSENT, null);
        }
        if (cacheValue.getLogicExpireTime() == null
                || cacheValue.getLogicExpireTime() <= System.currentTimeMillis()) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.LOGIC_EXPIRED, cacheValue);
        }
        if (refreshAheadMillis > 0
                && cacheValue.getLogicExpireTime() - System.currentTimeMillis() <= refreshAheadMillis) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.REFRESH_AHEAD, cacheValue);
        }
        return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.VALID, cacheValue);
    }

    private RecommendCacheSnapshot queryAndCacheUsers(long userId, long pageNum, long pageSize, boolean failOnCacheError) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        String redisKey = buildRecommendCacheKey(userId, pageNum, pageSize);
        RBucket<Object> bucket = redissonClient.getBucket(redisKey);
        RecommendCacheValue cacheValue = new RecommendCacheValue(
                userPage,
                System.currentTimeMillis() + logicExpireMillis
        );
        try {
            bucket.set(cacheValue, redisTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("refresh recommend cache error, redisKey={}", redisKey, e);
            if (failOnCacheError) {
                throw new IllegalStateException("refresh recommend cache error");
            }
        }
        return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.VALID, cacheValue);
    }
}
