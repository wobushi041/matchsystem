package com.wobushi041.matchsystem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mybatis plus分页插件配置类
 */
@Configuration
@MapperScan("com.wobushi041.matchsystem.mapper")
public class MyBatisPlusConfig {
    /**
     * 使用 MyBatis-Plus 提供的分页查询（如 Page 对象）时，框架会自动在 SQL 语句末尾拼接 LIMIT 语句，
     * 从而实现物理分页，而不是在内存中分页，大大提高了大数据量查询的性能
     * MybatisPlusInterceptor 拦截 SQL，判断是否为分页查询
     */
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

}
