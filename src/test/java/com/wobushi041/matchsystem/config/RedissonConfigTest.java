package com.wobushi041.matchsystem.config;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedissonConfigTest {


    @Resource
    private RedissonClient redissonClient;
    @Test
    void redissonClient() {
        ArrayList<String> list = new ArrayList<>();
        list.add("041");
        System.out.println(list.get(0));
        list.remove(0);
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("hsu");
        System.out.println("rlist:" + rList.get(0));



    }
}