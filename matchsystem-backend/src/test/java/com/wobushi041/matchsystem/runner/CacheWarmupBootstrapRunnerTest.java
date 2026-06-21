package com.wobushi041.matchsystem.runner;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.dto.RecommendCacheSnapshot;
import com.wobushi041.matchsystem.model.dto.RecommendCacheValue;
import com.wobushi041.matchsystem.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.matchsystem.model.enums.RecommendCacheStatus;
import com.wobushi041.matchsystem.mq.CacheWarmupProducer;
import com.wobushi041.matchsystem.service.RecommendCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheWarmupBootstrapRunnerTest {

    @Mock
    private CacheWarmupProducer cacheWarmupProducer;

    @Mock
    private RecommendCacheService recommendCacheService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void run_shouldRefreshMissingCacheAndScheduleNextRound() throws InterruptedException {
        CacheWarmupBootstrapRunner runner = new CacheWarmupBootstrapRunner();
        RLock lock = mock(RLock.class);
        RecommendCacheWarmupMessage bootstrapMessage = new RecommendCacheWarmupMessage();
        bootstrapMessage.setTaskId("recommend:1:1:10");
        bootstrapMessage.setRunId("bootstrap-run");
        bootstrapMessage.setUserId(1L);
        bootstrapMessage.setPageNum(1L);
        bootstrapMessage.setPageSize(10L);

        RecommendCacheWarmupMessage nextMessage = new RecommendCacheWarmupMessage();
        nextMessage.setTaskId("recommend:1:1:10");
        nextMessage.setRunId("next-run");
        nextMessage.setUserId(1L);
        nextMessage.setPageNum(1L);
        nextMessage.setPageSize(10L);

        Page<User> userPage = new Page<>(1L, 10L);
        userPage.setTotal(10L);
        RecommendCacheSnapshot missingSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1:1:10",
                RecommendCacheStatus.ABSENT,
                null
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1:1:10",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );

        ReflectionTestUtils.setField(runner, "cacheWarmupProducer", cacheWarmupProducer);
        ReflectionTestUtils.setField(runner, "recommendCacheService", recommendCacheService);
        ReflectionTestUtils.setField(runner, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(runner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(runner, "seedUserIds", "1");
        ReflectionTestUtils.setField(runner, "defaultPageNum", 1L);
        ReflectionTestUtils.setField(runner, "defaultPageSize", 10L);
        ReflectionTestUtils.setField(runner, "lockLeaseSeconds", 5L);

        when(cacheWarmupProducer.newWarmupMessage(1L, 1L, 10L)).thenReturn(bootstrapMessage, nextMessage);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recommendCacheService.getRecommendCacheSnapshot(1L, 1L, 10L)).thenReturn(missingSnapshot);
        when(recommendCacheService.refreshRecommendCache(1L, 1L, 10L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        runner.run(applicationArguments);

        verify(recommendCacheService).refreshRecommendCache(1L, 1L, 10L);
        verify(cacheWarmupProducer).scheduleWarmupTask(nextMessage, 29000L);
        verify(lock).unlock();
    }

    @Test
    void run_shouldReuseValidCacheWithoutRefreshing() throws InterruptedException {
        CacheWarmupBootstrapRunner runner = new CacheWarmupBootstrapRunner();
        RLock lock = mock(RLock.class);
        RecommendCacheWarmupMessage bootstrapMessage = new RecommendCacheWarmupMessage();
        bootstrapMessage.setTaskId("recommend:2:1:10");
        bootstrapMessage.setRunId("bootstrap-run");
        bootstrapMessage.setUserId(2L);
        bootstrapMessage.setPageNum(1L);
        bootstrapMessage.setPageSize(10L);

        RecommendCacheWarmupMessage nextMessage = new RecommendCacheWarmupMessage();
        nextMessage.setTaskId("recommend:2:1:10");
        nextMessage.setRunId("next-run");
        nextMessage.setUserId(2L);
        nextMessage.setPageNum(1L);
        nextMessage.setPageSize(10L);

        Page<User> userPage = new Page<>(1L, 10L);
        userPage.setTotal(6L);
        RecommendCacheSnapshot validSnapshot = new RecommendCacheSnapshot(
                "user:recommend:2:1:10",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );

        ReflectionTestUtils.setField(runner, "cacheWarmupProducer", cacheWarmupProducer);
        ReflectionTestUtils.setField(runner, "recommendCacheService", recommendCacheService);
        ReflectionTestUtils.setField(runner, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(runner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(runner, "seedUserIds", "2");
        ReflectionTestUtils.setField(runner, "defaultPageNum", 1L);
        ReflectionTestUtils.setField(runner, "defaultPageSize", 10L);
        ReflectionTestUtils.setField(runner, "lockLeaseSeconds", 5L);

        when(cacheWarmupProducer.newWarmupMessage(2L, 1L, 10L)).thenReturn(bootstrapMessage, nextMessage);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recommendCacheService.getRecommendCacheSnapshot(2L, 1L, 10L)).thenReturn(validSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(validSnapshot)).thenReturn(26000L);

        runner.run(applicationArguments);

        verify(recommendCacheService, never()).refreshRecommendCache(2L, 1L, 10L);
        verify(cacheWarmupProducer).scheduleWarmupTask(nextMessage, 26000L);
        verify(lock).unlock();
    }

    @Test
    void run_shouldRefreshWhenCacheEntersRefreshAheadWindow() throws InterruptedException {
        CacheWarmupBootstrapRunner runner = new CacheWarmupBootstrapRunner();
        RLock lock = mock(RLock.class);
        RecommendCacheWarmupMessage bootstrapMessage = new RecommendCacheWarmupMessage();
        bootstrapMessage.setTaskId("recommend:3:1:10");
        bootstrapMessage.setRunId("bootstrap-run");
        bootstrapMessage.setUserId(3L);
        bootstrapMessage.setPageNum(1L);
        bootstrapMessage.setPageSize(10L);

        RecommendCacheWarmupMessage nextMessage = new RecommendCacheWarmupMessage();
        nextMessage.setTaskId("recommend:3:1:10");
        nextMessage.setRunId("next-run");
        nextMessage.setUserId(3L);
        nextMessage.setPageNum(1L);
        nextMessage.setPageSize(10L);

        Page<User> userPage = new Page<>(1L, 10L);
        userPage.setTotal(7L);
        RecommendCacheSnapshot refreshAheadSnapshot = new RecommendCacheSnapshot(
                "user:recommend:3:1:10",
                RecommendCacheStatus.REFRESH_AHEAD,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 500)
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:3:1:10",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );

        ReflectionTestUtils.setField(runner, "cacheWarmupProducer", cacheWarmupProducer);
        ReflectionTestUtils.setField(runner, "recommendCacheService", recommendCacheService);
        ReflectionTestUtils.setField(runner, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(runner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(runner, "seedUserIds", "3");
        ReflectionTestUtils.setField(runner, "defaultPageNum", 1L);
        ReflectionTestUtils.setField(runner, "defaultPageSize", 10L);
        ReflectionTestUtils.setField(runner, "lockLeaseSeconds", 5L);

        when(cacheWarmupProducer.newWarmupMessage(3L, 1L, 10L)).thenReturn(bootstrapMessage, nextMessage);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recommendCacheService.getRecommendCacheSnapshot(3L, 1L, 10L)).thenReturn(refreshAheadSnapshot);
        when(recommendCacheService.refreshRecommendCache(3L, 1L, 10L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        runner.run(applicationArguments);

        verify(recommendCacheService).refreshRecommendCache(3L, 1L, 10L);
        verify(cacheWarmupProducer).scheduleWarmupTask(nextMessage, 29000L);
        verify(lock).unlock();
    }
}
