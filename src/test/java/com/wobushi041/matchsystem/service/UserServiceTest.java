package com.wobushi041.matchsystem.service;

import com.wobushi041.matchsystem.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class UserServiceTest {

    @Resource
    private RedisTemplate redisTemplate;
    @Test
    void test() {
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        // 插
//        valueOperations.set("hsuString", "dog");
//        valueOperations.set("hsuInt", 1);
//        valueOperations.set("hsuDouble", 2.0);
//        User user = new User();
//        user.setId(1L);
//        user.setUsername("hsu");
//        valueOperations.set("hsuUser", user);
        // 查
//        Object hsu = valueOperations.get("hsuString");
//        Assertions.assertEquals("dog", (String) hsu);
//        hsu = valueOperations.get("hsuInt");
//        Assertions.assertEquals(1, (int) (Integer) hsu);
//        hsu=valueOperations.get("hsuDouble");
//        Assertions.assertEquals(2.0, (Double) hsu);
//        System.out.println(valueOperations.get("hsuUser"));
//        // 删
//        redisTemplate.delete("hsuString");
//        redisTemplate.delete("hsuInt");
//        redisTemplate.delete("hsuDouble");
//        redisTemplate.delete("hsuUser");
    }



}