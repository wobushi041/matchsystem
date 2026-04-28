package com.wobushi041.matchsystem;

import com.sun.org.apache.xpath.internal.operations.String;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class MatchsystemApplicationTests {

    @Resource
    private RedissonClient redissonClient;


    @Test
    void redissonTest() {
        RBucket<Object> test = redissonClient.getBucket("test");
        test.set("123", 1, TimeUnit.DAYS);
        System.out.println(test.get());
        Assertions.assertEquals("123", test.get());
        RLock lock = redissonClient.getLock("041:lock");
        try {
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("获取到锁"+Thread.currentThread().getId());
                Thread.sleep(30000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
        void assertionTest() {
            Assertions.assertEquals(1, 1 + 0);
            Assertions.assertTrue(true);
            Assertions.assertFalse(false);
            Assertions.assertNotEquals(2, 3);
            java.lang.String string1 = "hello";
            java.lang.String string2 = "hello";
            //判断两个引用是不是指向同一个对象
            Assertions.assertSame(string1, string2);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, 1 + 0),
                () -> Assertions.assertTrue(true),
                () -> Assertions.assertFalse(false)
                );

        }
}


