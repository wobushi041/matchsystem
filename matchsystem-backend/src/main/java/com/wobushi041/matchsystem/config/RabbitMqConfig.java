package com.wobushi041.matchsystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {
    //定义延时交换机名，队列名，路由键
    public static final String CACHE_WARMUP_DELAY_EXCHANGE = "cache.warmup.delay.exchange";
    public static final String CACHE_WARMUP_DELAY_QUEUE = "cache.warmup.delay.queue";
    public static final String CACHE_WARMUP_DELAY_ROUTING_KEY = "cache.warmup.delay";
    //定义执行交换机名，队列名，路由键
    public static final String CACHE_WARMUP_EXECUTE_EXCHANGE = "cache.warmup.execute.exchange";
    public static final String CACHE_WARMUP_EXECUTE_QUEUE = "cache.warmup.execute.queue";
    public static final String CACHE_WARMUP_EXECUTE_ROUTING_KEY = "cache.warmup.execute";
    //定义失败交换机名，队列名，路由键
    public static final String CACHE_WARMUP_FAIL_EXCHANGE = "cache.warmup.fail.exchange";
    public static final String CACHE_WARMUP_FAIL_QUEUE = "cache.warmup.fail.queue";
    public static final String CACHE_WARMUP_FAIL_ROUTING_KEY = "cache.warmup.fail";

    @Bean
    //声明 Bean：延时交换机
    public DirectExchange cacheWarmupDelayExchange() {
        //直接交换机类型
        return new DirectExchange(CACHE_WARMUP_DELAY_EXCHANGE, true, false);
    }

    @Bean
    //声明 Bean：延时队列
    public Queue cacheWarmupDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", CACHE_WARMUP_EXECUTE_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", CACHE_WARMUP_EXECUTE_ROUTING_KEY);
        // 创建延时队列（持久化、非独占、非自动删除、带 DLX 参数）
        //durable持久化，exclusive独占机制
        return new Queue(CACHE_WARMUP_DELAY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding cacheWarmupDelayBinding() {
        return BindingBuilder.bind(cacheWarmupDelayQueue())
                .to(cacheWarmupDelayExchange())
                .with(CACHE_WARMUP_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange cacheWarmupExecuteExchange() {
        return new DirectExchange(CACHE_WARMUP_EXECUTE_EXCHANGE, true, false);
    }

    @Bean
    public Queue cacheWarmupExecuteQueue() {
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("x-dead-letter-exchange", CACHE_WARMUP_FAIL_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", CACHE_WARMUP_FAIL_ROUTING_KEY);
        return new Queue(CACHE_WARMUP_EXECUTE_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding cacheWarmupExecuteBinding() {
        return BindingBuilder.bind(cacheWarmupExecuteQueue())
                .to(cacheWarmupExecuteExchange())
                .with(CACHE_WARMUP_EXECUTE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange cacheWarmupFailExchange() {
        return new DirectExchange(CACHE_WARMUP_FAIL_EXCHANGE, true, false);
    }

    @Bean
    public Queue cacheWarmupFailQueue() {
        return new Queue(CACHE_WARMUP_FAIL_QUEUE, true);
    }

    @Bean
    public Binding cacheWarmupFailBinding() {
        return BindingBuilder.bind(cacheWarmupFailQueue())
                .to(cacheWarmupFailExchange())
                .with(CACHE_WARMUP_FAIL_ROUTING_KEY);
    }







    @Bean
    //消息转换器
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        //设置 JSON 转换器
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    //声明 Bean：监听容器工厂
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        //显式设置 JSON 消息转换器
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
