# Go 语言编程指南

## 一、Go 语言基础

### 1.1 基本语法
- 变量声明：var、短变量声明 :=
- 数据类型：int、float64、string、bool、byte、rune
- 常量与 iota：枚举实现
- 数组与切片：slice 底层原理、扩容机制
- Map：使用方式、并发安全问题
- 字符串处理：strings 包、rune 与字节

### 1.2 流程控制
- if-else、for（Go 没有 while）
- switch：类型 switch、fallthrough
- defer：执行顺序（LIFO）、与 panic/recover 配合
- goto 的使用场景

### 1.3 函数与方法
- 多返回值：error 处理模式
- 可变参数：... 语法
- 函数作为一等公民：回调、闭包
- 方法：值接收者 vs 指针接收者
- init 函数：包初始化顺序

## 二、Go 核心特性

### 2.1 接口与结构体
- 结构体：嵌套组合、匿名字段
- 接口：隐式实现、接口组合
- 空接口 interface{}：任意类型
- 类型断言与类型选择
- 鸭子类型哲学

### 2.2 Goroutine 与 Channel
- Goroutine：轻量级协程、调度模型 GMP
- Channel：有缓冲 vs 无缓冲、方向限制
- select 多路复用：超时控制、非阻塞通信
- sync 包：Mutex、RWMutex、WaitGroup、Once
- Context：取消传播、超时控制、值传递

### 2.3 错误处理
- error 接口：errors.New、fmt.Errorf
- 错误包装：errors.Is、errors.As
- 自定义错误类型
- panic/recover：不可滥用

## 三、Go 工程实践

### 3.1 项目结构
- Go Modules：go.mod、go.sum
- 包组织：cmd/、internal/、pkg/ 分层
- 编译与交叉编译
- 常用构建标签：go build -tags

### 3.2 常用标准库
- net/http：HTTP 服务端与客户端
- encoding/json：序列化与反序列化
- database/sql：数据库操作
- context：请求上下文管理
- log/slog：结构化日志（Go 1.21+）

### 3.3 Web 开发框架
- Gin：路由、中间件、参数绑定、分组路由
- Echo：高性能 Web 框架
- gRPC：Protocol Buffers、服务定义、流式通信

## 四、Go 进阶

### 4.1 内存管理
- 堆与栈：逃逸分析
- GC 机制：三色标记法、写屏障
- 内存对齐与 struct 字段排列优化
- sync.Pool：对象复用

### 4.2 性能优化
- pprof：CPU、内存、goroutine 分析
- benchmark：基准测试编写
- 内联优化与编译器提示
- 常见性能陷阱：string 拼接、map 预分配

### 4.3 并发模式
- Fan-in / Fan-out：扇入扇出模式
- Pipeline：流水线模式
- Worker Pool：工作池模式
- Rate Limiting：限流实现
