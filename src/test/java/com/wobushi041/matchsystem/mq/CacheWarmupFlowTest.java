package com.wobushi041.matchsystem.mq;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.config.RabbitMqConfig;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.dto.RecommendCacheSnapshot;
import com.wobushi041.matchsystem.model.dto.RecommendCacheValue;
import com.wobushi041.matchsystem.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.matchsystem.model.enums.RecommendCacheStatus;
import com.wobushi041.matchsystem.service.RecommendCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "match.cache.warmup.bootstrap-enabled=false",
        "match.cache.warmup.delay-millis=30000",
        "match.cache.warmup.logic-expire-millis=30000",
        "match.cache.warmup.refresh-ahead-millis=1000",
        "match.cache.warmup.lock-lease-seconds=5",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class CacheWarmupFlowTest {

    @Autowired
    private CacheWarmupProducer cacheWarmupProducer;

    @Autowired
    private CacheWarmupConsumer cacheWarmupConsumer;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private RecommendCacheService recommendCacheService;

    @MockBean
    private RedissonClient redissonClient;

    private RLock lock;

    @BeforeEach
    void setUp() throws InterruptedException {
        lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void scheduleWarmupTask_shouldSetSpecifiedDelay() {
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1001L, 1L, 10L);

        cacheWarmupProducer.scheduleWarmupTask(message, 30000L);

        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                eq(message),
                postProcessorCaptor.capture());

        Message processedMessage = postProcessorCaptor.getValue().postProcessMessage(
                new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));

        assertEquals("30000", processedMessage.getMessageProperties().getExpiration());
        assertEquals(MessageDeliveryMode.PERSISTENT, processedMessage.getMessageProperties().getDeliveryMode());
    }

    @Test
    void deliverCallback_shouldRefreshExpiredCacheAndScheduleNextRound() {
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1003L, 2L, 5L);
        Page<User> userPage = new Page<>(2L, 5L);
        userPage.setTotal(3L);
        RecommendCacheSnapshot expiredSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1003:2:5",
                RecommendCacheStatus.LOGIC_EXPIRED,
                new RecommendCacheValue(userPage, System.currentTimeMillis() - 1000)
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1003:2:5",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );
        when(recommendCacheService.getRecommendCacheSnapshot(1003L, 2L, 5L)).thenReturn(expiredSnapshot);
        when(recommendCacheService.refreshRecommendCache(1003L, 2L, 5L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        cacheWarmupConsumer.deliverCallback(message);

        verify(recommendCacheService).refreshRecommendCache(1003L, 2L, 5L);

        ArgumentCaptor<RecommendCacheWarmupMessage> nextMessageCaptor =
                ArgumentCaptor.forClass(RecommendCacheWarmupMessage.class);
        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                nextMessageCaptor.capture(),
                postProcessorCaptor.capture());

        RecommendCacheWarmupMessage nextMessage = nextMessageCaptor.getValue();
        assertNotNull(nextMessage);
        assertEquals(message.getTaskId(), nextMessage.getTaskId());
        assertEquals(message.getUserId(), nextMessage.getUserId());
        assertEquals(message.getPageNum(), nextMessage.getPageNum());
        assertEquals(message.getPageSize(), nextMessage.getPageSize());
        assertNotEquals(message.getRunId(), nextMessage.getRunId());
        Message processedMessage = postProcessorCaptor.getValue().postProcessMessage(
                new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));
        assertEquals("29000", processedMessage.getMessageProperties().getExpiration());
        verify(lock).unlock();
    }

    @Test
    void deliverCallback_shouldRefreshWhenCacheEntersRefreshAheadWindow() {
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1005L, 1L, 20L);
        Page<User> userPage = new Page<>(1L, 20L);
        userPage.setTotal(12L);
        RecommendCacheSnapshot refreshAheadSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1005:1:20",
                RecommendCacheStatus.REFRESH_AHEAD,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 800)
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1005:1:20",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );
        when(recommendCacheService.getRecommendCacheSnapshot(1005L, 1L, 20L)).thenReturn(refreshAheadSnapshot);
        when(recommendCacheService.refreshRecommendCache(1005L, 1L, 20L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        cacheWarmupConsumer.deliverCallback(message);

        verify(recommendCacheService).refreshRecommendCache(1005L, 1L, 20L);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                any(RecommendCacheWarmupMessage.class),
                any(MessagePostProcessor.class));
        verify(lock).unlock();
    }

    @Test
    void deliverCallback_shouldReuseValidCacheAndStillScheduleNextRound() {
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1004L, 1L, 20L);
        Page<User> userPage = new Page<>(1L, 20L);
        userPage.setTotal(8L);
        RecommendCacheSnapshot validSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1004:1:20",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 15000)
        );
        when(recommendCacheService.getRecommendCacheSnapshot(1004L, 1L, 20L)).thenReturn(validSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(validSnapshot)).thenReturn(12000L);

        cacheWarmupConsumer.deliverCallback(message);

        verify(recommendCacheService, never()).refreshRecommendCache(1004L, 1L, 20L);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                any(RecommendCacheWarmupMessage.class),
                any(MessagePostProcessor.class));
        verify(lock).unlock();
    }
}
