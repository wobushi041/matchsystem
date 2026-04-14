package com.wobushi041.matchsystem.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类，用于配置Redisson客户端连接
 * Redisson是一个基于Redis的Java驻留式内存数据网格（In-Memory Data Grid），用于分布式应用程序，提供分布式Java对象和服务支持。
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")// 用于将配置文件中以特定前缀开头的属性值映射到一个Java Bean 中。
@Data
public class RedissonConfig {

    private String host;// Redis主机地址
    private String port;// Redis端口号
    private String password;// Redis密码

    /**
     * 配置Redisson客户端连接
     * @return 返回RedissonClient对象，用于与Redis交互
     */
    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        // 写到3库
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);

        if (password != null && !password.trim().isEmpty()) {
            config.useSingleServer().setPassword(password);
        }
        // 2. 创建实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}