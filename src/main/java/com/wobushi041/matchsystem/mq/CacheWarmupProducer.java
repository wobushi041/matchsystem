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

/**
 * 把缓存预热任务包装成message，并投递消息
 * 1.生成任务消息对象RecommendCacheWarmupMessage
 * 2.给消息设置延迟时间
 * 3.吧消息发送给对应交换机
 */
@Component
@Slf4j
public class CacheWarmupProducer {

    private static final String TASK_ID_FORMAT = "recommend:%d:%d:%d";

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${match.cache.warmup.delay-millis}")
    private long delayMillis;

    /**
     * 按默认延迟时间发送任务
     * @param message
     */
    public void scheduleWarmupTask(RecommendCacheWarmupMessage message) {
        sendDelayMessage(message, delayMillis);
    }

    /**
     * 按指定延迟时间发送任务
     * @param message
     * @param delayMillis
     */
    public void scheduleWarmupTask(RecommendCacheWarmupMessage message, long delayMillis) {
        sendDelayMessage(message, delayMillis);
    }

    /**
     * 发送延时消息方法
     * 真正执行 MQ 投递
     * 发到 cache.warmup.delay.exchange
     * 路由到延时队列
     * 给消息设置 expiration
     */
    public void sendDelayMessage(RecommendCacheWarmupMessage message, long delayMillis) {
        rabbitTemplate.convertAndSend(
                //目标交换机
                RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE,
                //路由键
                RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY,
                message,
                msg -> {
                    //设置过期时间，这条消息先放进延时队列，
                    //等 delayMillis 毫秒后过期，过期后由 RabbitMQ 自动转发到执行队列
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    //设置消息持久化
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                });
        log.info("schedule warmup message, taskId={}, runId={}, delayMillis={}",
                message.getTaskId(), message.getRunId(), delayMillis);

    }

    /**
     * 生成任务消息对象RecommendCacheWarmupMessage并封装
     */
    public RecommendCacheWarmupMessage newWarmupMessage(long userId, long pageNum, long pageSize) {
        RecommendCacheWarmupMessage message = new RecommendCacheWarmupMessage();
        //标识“这是谁的哪一页推荐缓存任务”
        message.setTaskId(String.format(TASK_ID_FORMAT, userId, pageNum, pageSize));
        //标识“这一次具体投递的消息实例”
        message.setRunId(UUID.randomUUID().toString());
        message.setUserId(userId);
        message.setPageNum(pageNum);
        message.setPageSize(pageSize);
        message.setCreateTime(System.currentTimeMillis());
        return message;
    }

}
