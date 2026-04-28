package com.wobushi041.matchsystem.mq;

import com.wobushi041.matchsystem.config.RabbitMqConfig;
import com.wobushi041.matchsystem.model.dto.RecommendCacheWarmupMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Component
@Slf4j
public class CacheWarmupProducer {

    private static final String TASK_ID_FORMAT = "recommend:%d:%d:%d";

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${match.cache.warmup.delay-millis}")
    private long delayMillis;

    public void scheduleWarmupTask(RecommendCacheWarmupMessage message) {
        sendDelayMessage(message, delayMillis);
    }

    public void scheduleWarmupTask(RecommendCacheWarmupMessage message, long delayMillis) {
        sendDelayMessage(message, delayMillis);
    }

    /**
     * 启动时立即触发一次预热，避免首轮等待 delay-millis。
     */
    public void scheduleWarmupTaskNow(RecommendCacheWarmupMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CACHE_WARMUP_EXECUTE_EXCHANGE,
                RabbitMqConfig.CACHE_WARMUP_EXECUTE_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                });
        log.info("dispatch immediate warmup message, taskId={}, runId={}",
                message.getTaskId(), message.getRunId());
    }

    //发送延时消息方法
    public void sendDelayMessage(RecommendCacheWarmupMessage message, long delayMillis) {
        rabbitTemplate.convertAndSend(
                //目标交换机
                RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE,
                //路由键
                RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY,
                message,
                msg -> {
                    //设置过期时间
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    //设置消息持久化
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                });
        log.info("schedule warmup message, taskId={}, runId={}, delayMillis={}",
                message.getTaskId(), message.getRunId(), delayMillis);

    }


    public RecommendCacheWarmupMessage newWarmupMessage(long userId, long pageNum, long pageSize) {
        RecommendCacheWarmupMessage message = new RecommendCacheWarmupMessage();
        message.setTaskId(String.format(TASK_ID_FORMAT, userId, pageNum, pageSize));
        message.setRunId(UUID.randomUUID().toString());
        message.setUserId(userId);
        message.setPageNum(pageNum);
        message.setPageSize(pageSize);
        message.setCreateTime(System.currentTimeMillis());
        return message;
    }

}
