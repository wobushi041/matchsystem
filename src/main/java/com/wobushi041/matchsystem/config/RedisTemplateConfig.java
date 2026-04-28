package com.wobushi041.matchsystem.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;


@Configuration
//自定义 RedisTemplate Bean ，覆盖 Spring Boot 默认配置的实例。项目中注入并使用的是自定义封装了序列化方式的实例，而非默认的
//@EnableRedisHttpSession,yml配置了timeout，注释掉
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
        // 配置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 配置推荐的序列化器

        // key和Hash类型的field（HashKey）的序列化器用String序列化器
        RedisSerializer<String> string = RedisSerializer.string();
        redisTemplate.setKeySerializer(string);
        redisTemplate.setHashKeySerializer(string);
        // value和Hash类型的value的序列化器则用JSON序列化器
        GenericJackson2JsonRedisSerializer json = getJsonRedisSerializer();
        redisTemplate.setHashValueSerializer(json);
        redisTemplate.setValueSerializer(json);
        return redisTemplate;
    }

    /**
     * 显式设置 Spring Session 使用你的序列化器
     * Session 默认使用的是 JDK 序列化(JdkSerializationRedisSerializer)
     * 这里我们使用 JSON 序列化(GenericJackson2JsonRedisSerializer)解决乱码
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return getJsonRedisSerializer();
    }


    private GenericJackson2JsonRedisSerializer getJsonRedisSerializer(){
        // Jackson的序列化器默认不支持Java8的时间API，但是可以加个模块来让它支持
        // 创建并配置 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 激活默认类型信息(遇到过Jackson转换后的json没有类型信息这个坑)
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        // 关键：支持 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用时间戳格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 使用 GenericJackson2JsonRedisSerializer
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}
