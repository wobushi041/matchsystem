package com.wobushi041.matchsystem.job;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class PrecaheJobTest {
    @Resource
    private RedissonClient redissonClient;
    @Test
    void testWatchDog() {
        RLock lock = redissonClient.getLock("041:precachejob:docache:lock");
        try {
            // 只有一个线程能获取到锁
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                Thread.sleep(300000);//todo 实际要执行的代码
                System.out.println("getLock: " + Thread.currentThread().getId()+"现在时间"+new Date());
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
    @Test
    void testWatchDog2() {
        RLock lock = redissonClient.getLock("041:precachejob:docache:lock");
        try {
            // 尝试获取锁，不等待，启用看门狗机制（leaseTime=-1）
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("getLock: " + Thread.currentThread().getId() + " at " + new Date());

                // 模拟3分钟的业务执行时间
                long startTime = System.currentTimeMillis();
                long duration = 3 * 60 * 1000; // 3分钟，单位毫秒

                while (System.currentTimeMillis() - startTime < duration) {
                    // 每隔30秒打印一次日志，证明任务还在执行
                    Thread.sleep(30000);
                    System.out.println("Task is still running... " +
                            ((System.currentTimeMillis() - startTime) / 1000) + " seconds elapsed");
                }

                System.out.println("Task completed after 3 minutes");
            } else {
                System.out.println("Failed to acquire lock");
            }
        } catch (InterruptedException e) {
            System.out.println("Task was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId() + " at " + new Date());
                lock.unlock();
            }
        }

        /**
         * 日志：
         * getLock: 1 at Wed Apr 15 09:13:21 CST 2026
         * Task is still running... 30 seconds elapsed
         * Task is still running... 60 seconds elapsed
         * Task is still running... 90 seconds elapsed
         * Task is still running... 120 seconds elapsed
         * Task is still running... 150 seconds elapsed
         * Task is still running... 180 seconds elapsed
         * Task completed after 3 minutes
         * unLock: 1 at Wed Apr 15 09:16:22 CST 2026
         */
    }


}
