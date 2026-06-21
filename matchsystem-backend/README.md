# AI 编程匹配助手 - 后端

AI 编程匹配助手后端，基于 Spring Boot 3.5.3 + Java 21 实现。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 核心框架 | Spring Boot | 3.5.3 |
| JDK | OpenJDK | 21 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis + Redisson | 6.x+ / 3.17.7 |
| 消息队列 | RabbitMQ | 3.x+ |
| AI 集成 | LangChain4j + DeepSeek | 1.1.0 |
| API 文档 | Knife4j | 4.6.0 |
| 构建工具 | Maven | 3.6+ |

## 项目结构

```plaintext
src/main/java/com/wobushi041/matchsystem/
├── MatchsystemApplication.java    # 启动类
├── ai/                            # AI 相关
│   ├── AiChatService.java         # AI 对话服务
│   ├── AiChatServiceFactory.java  # 服务工厂
│   ├── guardrail/                 # 输入护栏
│   ├── listener/                  # 模型监听器
│   ├── model/                     # 模型配置
│   └── rag/                       # RAG 配置
├── common/                        # 通用组件
│   ├── BaseResponse.java          # 统一响应结构
│   ├── ErrorCode.java             # 错误码定义
│   └── ResultUtils.java           # 响应工具类
├── config/                        # 配置类
│   ├── Knife4jConfig.java         # API 文档配置
│   ├── MyBatisPlusConfig.java     # ORM 配置
│   ├── RabbitMqConfig.java        # 消息队列配置
│   ├── RedissonConfig.java        # 分布式锁配置
│   ├── RedisTemplateConfig.java   # Redis 配置
│   └── WebMvcConfig.java          # Web 配置
├── controller/                    # REST 接口
│   ├── AiController.java          # AI 对话接口
│   ├── TeamController.java        # 队伍管理接口
│   └── UserController.java        # 用户管理接口
├── exception/                     # 异常处理
│   ├── BusinessException.java     # 业务异常
│   └── GlobalExceptionHandler.java # 全局异常处理
├── mapper/                        # MyBatis Mapper
│   ├── TeamMapper.java
│   ├── UserMapper.java
│   └── UserTeamMapper.java
├── model/                         # 数据模型
│   ├── domain/                    # 实体类
│   ├── dto/                       # 数据传输对象
│   ├── enums/                     # 枚举
│   ├── request/                   # 请求对象
│   └── vo/                        # 视图对象
├── mq/                            # 消息队列
│   ├── CacheWarmupConsumer.java   # 缓存预热消费者
│   └── CacheWarmupProducer.java   # 缓存预热生产者
├── runner/                        # 启动任务
│   └── CacheWarmupBootstrapRunner.java
├── service/                       # 业务服务
│   ├── impl/                      # 服务实现
│   ├── RecommendCacheService.java # 推荐缓存服务
│   ├── TeamService.java
│   ├── UserService.java
│   └── UserTeamService.java
└── utils/                         # 工具类
    └── AlgorithmUtils.java        # 算法工具（编辑距离）
```

## 核心功能

### 用户管理

- 注册/登录/注销（Spring Session + Redis 分布式会话）
- 用户信息 CRUD
- 管理员权限校验

### 标签匹配

- 用户标签 JSON 存储
- 基于编辑距离的标签相似度计算
- 按标签搜索用户

### 队伍管理

- 队伍 CRUD
- 公开/加密/私有三种状态
- 加入/退出队伍

### 推荐缓存预热

- RabbitMQ 延时消息驱动
- 逻辑过期 + 分布式锁
- 启动时自动预热

### AI 编程助手

- DeepSeek 模型集成
- SSE 流式对话
- 用户标签 RAG 增强

## 快速启动

### 1. 初始化数据库

执行建表脚本：

```sql
-- 文件位置：src/main/resources/sql/create_table.sql
-- 默认数据库名：matchsystem
```

### 2. 配置应用

```bash
# 复制配置模板
cp src/main/resources/application-template.yml src/main/resources/application.yml

# 编辑配置文件，填写以下信息：
# - MySQL 连接信息
# - Redis 连接信息
# - RabbitMQ 连接信息
# - DeepSeek API Key（可选，AI 功能需要）
```

### 3. 启动服务

```bash
mvn spring-boot:run
```

### 4. 访问接口文档

启动成功后访问：http://localhost:8080/api/doc.html

## 接口概览

### 用户接口 `/api/user`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/register` | 注册 | 公开 |
| POST | `/login` | 登录 | 公开 |
| POST | `/logout` | 注销 | 登录 |
| GET | `/current` | 获取当前用户 | 登录 |
| GET | `/search` | 搜索用户 | 管理员 |
| GET | `/search/tags` | 按标签搜索 | 登录 |
| POST | `/delete` | 删除用户 | 管理员 |
| POST | `/update` | 更新用户 | 登录 |
| GET | `/recommend` | 推荐列表 | 登录 |
| GET | `/match` | 标签匹配 | 登录 |

### 队伍接口 `/api/team`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/add` | 创建队伍 | 登录 |
| POST | `/delete` | 删除队伍 | 登录 |
| POST | `/update` | 更新队伍 | 登录 |
| GET | `/get` | 获取详情 | 公开 |
| GET | `/list` | 队伍列表 | 公开 |
| POST | `/join` | 加入队伍 | 登录 |
| POST | `/quit` | 退出队伍 | 登录 |
| GET | `/list/my/create` | 我创建的 | 登录 |
| GET | `/list/my/join` | 我加入的 | 登录 |

### AI 接口 `/api/ai`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/chat` | SSE 流式对话 | 登录 |
| POST | `/chat` | 普通对话 | 登录 |

## 打包部署

```bash
# 打包（跳过测试）
mvn clean package

# 运行
java -jar target/matchsystem-0.0.1-SNAPSHOT.jar
```

生产环境建议通过环境变量注入敏感配置，参考 `application-prod-template.yml`。
