package com.wobushi041.matchsystem.once.importuser;

import com.wobushi041.matchsystem.mapper.UserMapper;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.service.UserService;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 导入用户任务
 */
@Data
@Component
public class InsertUsers {

  public   final int INSERT_COUNT = 5000;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;

    /**
     * 批量插入用户
     */
//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < INSERT_COUNT; i++) {
            User user = new User();
            user.setUsername("041041");
            user.setUserAccount("10086");
            user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/PiajxSqBRaELkfM4IsxxWrB70flGuaDcq55mDxh8r4DuwOJLuluSmRCH9Pk1MFibry5icVgHtfwMmnYGqT49svVKV3X1wMer2OCC3ob5leZX5lF8HMbPo1Qww/132");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("1343");
            user.setEmail("133435");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("12");
            user.setTags("");
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println("循环插入执行时间（毫秒）：" + stopWatch.getLastTaskTimeMillis());
    }



    // 线程池的设置ExecutorService 接口:Java 中用于执行异步任务的接口。
    private ExecutorService executorService = new ThreadPoolExecutor(
            16, // corePoolSize: 核心线程数。线程池中始终保持活跃的线程数量，即使它们处于空闲状态。
            1000, // maximumPoolSize: 最大线程数。线程池中允许的最大线程数量。
            10000, // keepAliveTime: 当线程数超过核心线程数时，这是非核心线程空闲前的最大存活时间。
            TimeUnit.MINUTES, // 时间单位。上面的 keepAliveTime 的单位。
            new ArrayBlockingQueue<>(10000) // 工作队列。存放待执行任务的阻塞队列，具有先进先出等特性的阵列支持的有界队列。
    );

    /**
     * 使用并发方法批量插入用户数据。
     * 通过分割任务并利用CompletableFuture与线程池，实现高效的并行数据插入。
     * 此方法尤其适用于处理大量数据插入操作，能显著提高性能。
     */
    public void doConcurrencyInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start(); // 开始计时
        final int INSERT_NUM = 100000; // 总插入数据量
        final int batchSize = 5000; // 每批次处理的数据量
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        // 根据批次大小分割任务
        for (int i = 0; i < Math.ceil((double) INSERT_NUM / batchSize); i++) {
            List<User> userList = new ArrayList<>();
            // 创建每批次的用户数据
            for (int j = 0; j < batchSize; j++) {
                User user = new User();
                user.setUsername("041041");
                user.setUserAccount("10086");
                user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/PiajxSqBRaELkfM4IsxxWrB70flGuaDcq55mDxh8r4DuwOJLuluSmRCH9Pk1MFibry5icVgHtfwMmnYGqT49svVKV3X1wMer2OCC3ob5leZX5lF8HMbPo1Qww/132");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("1343");
                user.setEmail("133435");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("12");
                user.setTags("");
                userMapper.insert(user);
            }
            // 异步执行数据库插入操作
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("ThreadName：" + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureList.add(future);
        }
        // 等待所有异步任务完成
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        stopWatch.stop(); // 停止计时
        System.out.println("并发批量插入执行时间（毫秒）：" + stopWatch.getLastTaskTimeMillis());
    }
}
