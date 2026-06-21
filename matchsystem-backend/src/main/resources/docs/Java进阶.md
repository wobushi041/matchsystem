# Java 进阶学习指南

## 一、Java 核心进阶

### 1.1 JVM 深入理解
- JVM 内存模型：堆、栈、方法区、程序计数器
- 垃圾回收算法：标记-清除、复制、标记-整理、分代收集
- 常见垃圾收集器：CMS、G1、ZGC 的区别和适用场景
- JVM 调优实战：-Xms、-Xmx、-XX:+UseG1GC 等参数配置
- 类加载机制：双亲委派模型、自定义类加载器

### 1.2 并发编程
- 线程基础：Thread、Runnable、Callable 的区别
- 线程池：ThreadPoolExecutor 七大参数、四种拒绝策略
- 锁机制：synchronized 原理、ReentrantLock、ReadWriteLock
- 并发工具类：CountDownLatch、CyclicBarrier、Semaphore
- volatile 关键字：内存可见性、禁止指令重排
- CAS 与 AQS：原子操作、队列同步器原理
- ThreadLocal：原理、内存泄漏问题及解决方案

### 1.3 集合框架深入
- HashMap 底层原理：数组+链表+红黑树、扩容机制
- ConcurrentHashMap：JDK7 分段锁 vs JDK8 CAS+synchronized
- ArrayList vs LinkedList：时间复杂度对比和使用场景
- TreeMap 和 TreeSet：红黑树的应用

## 二、Spring 生态

### 2.1 Spring Framework
- IOC 容器：BeanFactory vs ApplicationContext
- AOP 原理：JDK 动态代理 vs CGLIB
- 事务管理：@Transactional 失效的常见场景
- 循环依赖：三级缓存解决方案

### 2.2 Spring Boot
- 自动配置原理：@EnableAutoConfiguration、spring.factories
- Starter 机制：如何自定义 Starter
- 配置加载：application.yml 优先级、Profile 多环境配置
- 常用注解：@ConditionalOnClass、@ConditionalOnProperty

### 2.3 Spring Cloud（微服务）
- 服务注册与发现：Nacos、Eureka
- 负载均衡：Ribbon、LoadBalancer
- 服务调用：Feign、RestTemplate
- 熔断降级：Sentinel、Hystrix
- 网关：Spring Cloud Gateway
- 配置中心：Nacos Config

## 三、数据库进阶

### 3.1 MySQL 深入
- 索引原理：B+ 树、聚簇索引、非聚簇索引
- 索引优化：联合索引最左前缀、覆盖索引、索引下推
- 事务隔离级别：读未提交、读已提交、可重复读、串行化
- MVCC 原理：ReadView、undo log、版本链
- 锁机制：行锁、表锁、间隙锁、临键锁
- SQL 优化：explain 执行计划分析

### 3.2 Redis
- 数据结构：String、Hash、List、Set、ZSet 的底层实现
- 持久化：RDB vs AOF
- 缓存问题：缓存穿透、缓存击穿、缓存雪崩及解决方案
- 分布式锁：SETNX、Redisson、RedLock
- 集群模式：主从、哨兵、Cluster

## 四、设计模式与架构

### 4.1 常用设计模式
- 单例模式：懒汉、饿汉、双重检查锁、枚举实现
- 工厂模式：简单工厂、工厂方法、抽象工厂
- 策略模式：消除 if-else 的利器
- 观察者模式：事件驱动架构基础
- 代理模式：静态代理、动态代理

### 4.2 架构思想
- DDD 领域驱动设计基础
- 微服务拆分原则
- RESTful API 设计规范
- 消息队列应用：解耦、削峰、异步
