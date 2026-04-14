package com.wobushi041.matchsystem.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.service.UserService;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PrecaheJob {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private List<Long> mainCommentUser = Arrays.asList(1L);

    @Resource
    RedissonClient redissonClient;

    @Scheduled(cron = "0 0 9 * * *")
    public void doCacheRecommentUser() {
        // 获取分布式锁，防止多个任务同时执行
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");
        // try尝试立即获取锁，如果成功获取到锁，则执行下面的代码块；如果获取失败，则不进行等待，直接执行后续的逻辑
        try {
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("getlock:"+Thread.currentThread().getId());
                for (Long userId : mainCommentUser) {
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userCommentPage = userService.page(new Page<>(1, 10), queryWrapper);
                    String redisKey = String.format("yupao:user:recommend:%s", userId);
                    try {
                        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                        valueOperations.set(redisKey, userCommentPage, 1, TimeUnit.DAYS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("lock error", e);
        } finally {
             //首先检查当前线程是否持有该锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unlock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}

