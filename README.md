# matchsystem-backend

用户匹配系统后端，基于 Spring Boot + MyBatis-Plus 实现，提供用户注册、登录、队伍管理、标签匹配、推荐缓存预热等业务能力，并集成 Knife4j 接口文档。

## 1. 技术栈

### 核心框架

- Java 8
- Spring Boot 2.6.13
- MyBatis-Plus 3.5.2
- Maven

### 数据与缓存

- MySQL 5.7+（建议 8.x）
- Redis 6.x+
- Redisson 3.17.7
- RabbitMQ 3.x+

### 工具与文档

- Knife4j 4.4.0
- EasyExcel 3.1.0
- Gson 2.9.0
- Apache Commons Lang3 / BeanUtils
- Lombok
- Spring Boot Validation

## 2. 项目结构

```text
matchsystem-backend
├─ src/main/java/com/wobushi041/matchsystem
│  ├─ MatchsystemApplication.java       # 启动类
│  ├─ common                            # 通用返回对象、错误码、工具方法
│  ├─ config                            # 配置类
│  ├─ contant                           # 常量定义
│  ├─ controller                        # REST 接口
│  ├─ exception                         # 业务异常与全局异常处理
│  ├─ job                               # 定时任务相关
│  ├─ mapper                            # MyBatis-Plus Mapper 接口
│  ├─ model
│  │  ├─ domain                         # 实体类
│  │  ├─ dto                            # 数据传输对象
│  │  ├─ enums                          # 枚举
│  │  ├─ request                        # 请求参数对象
│  │  └─ vo                             # 视图返回对象
│  ├─ mq                                # RabbitMQ 
│  ├─ once                              # 一次性脚本
│  ├─ runner                            # ApplicationRunner
│  ├─ service                           # 业务接口与实现
│  └─ utils                             # 工具类
├─ src/main/resources
│  ├─ application-template.yml          # 开发环境配置模板
│  ├─ application-prod-template.yml     # 生产环境配置模板（环境变量注入）
│  ├─ mapper/                           # MyBatis XML（UserMapper、TeamMapper、UserTeamMapper）
│  ├─ sql/create_table.sql              # 建表脚本
│  └─ banner.txt                        # 启动 Banner
├─ src/test/java                        # 单元测试
└─ pom.xml
```

> **注意：** 本仓库不包含任何真实配置文件。`application.yml` 和 `application-prod.yml` 已被 `.gitignore` 忽略，仅提交模板文件。敏感信息通过模板复制 + 环境变量注入管理。

## 3. 架构概览

```text
                          ┌─────────────┐
                          │   Client    │
                          └──────┬──────┘
                                 │ HTTP
                          ┌──────▼──────┐
                          │ Controller  │  (UserController / TeamController)
                          └──────┬──────┘
                                 │
                          ┌──────▼──────┐
                          │   Service   │  (业务逻辑层)
                          └──┬───┬───┬──┘
                             │   │   │
               ┌─────────────┘   │   └─────────────┐
               │                  │                  │
        ┌──────▼──────┐   ┌──────▼──────┐   ┌──────▼──────┐
        │   MySQL     │   │    Redis    │   │  RabbitMQ   │
        │  (持久化)    │   │ (缓存+Session)│  │ (延时消息)   │
        └─────────────┘   └──────┬──────┘   └─────────────┘
                                 │
                          ┌──────▼──────┐
                          │  Redisson   │
                          │ (分布式锁)   │
                          └─────────────┘
```

**推荐缓存预热流程：**

1. 应用启动时 `CacheWarmupBootstrapRunner` 发送延时消息到 RabbitMQ
2. `CacheWarmupProducer` 将消息投递到延时队列，到期后自动转发到执行队列
3. `CacheWarmupConsumer` 消费消息，刷新 Redis 缓存
4. `RecommendCacheService` 使用 Redisson 分布式锁保证缓存重建的并发安全
5. 逻辑过期机制：缓存到期后不删除，后台异步刷新，用户请求始终命中缓存

## 4. 环境要求

| 组件     | 最低版本 | 说明                   |
|----------|----------|------------------------|
| JDK      | 1.8      | 项目编译目标为 Java 8  |
| Maven    | 3.6+     | 需要支持 Java 8 编译   |
| MySQL    | 5.7+     | 建议 8.x               |
| Redis    | 6.x+     | 用于缓存和分布式会话   |
| RabbitMQ | 3.x+     | 需启用延迟消息插件     |

## 5. 快速启动

### 5.1 克隆并进入项目

```powershell
git clone https://github.com/wobushi041/matchsystem.git
cd matchsystem\matchsystem-backend
```

### 5.2 初始化数据库

执行建表脚本：

```text
src/main/resources/sql/create_table.sql
```

默认数据库名：`matchsystem`，包含三张表：`user`、`team`、`user_team`。

### 5.3 配置应用

从模板复制配置文件，按本地环境修改连接信息：

```powershell
Copy-Item src/main/resources/application-template.yml src/main/resources/application.yml
Copy-Item src/main/resources/application-prod-template.yml src/main/resources/application-prod.yml
```

`application-template.yml` 中需要关注的配置块：

| 配置块 | 说明 |
|--------|------|
| `spring.datasource.*` | MySQL 连接信息 |
| `spring.redis.*` | Redis 连接信息（同时被 Spring Session 和 Redisson 使用） |
| `spring.rabbitmq.*` | RabbitMQ 连接信息 |
| `spring.session.*` | 分布式会话配置（默认使用 Redis 存储） |
| `match.cache.warmup.*` | 推荐缓存预热参数（详见模板文件注释） |
| `mybatis-plus.*` | ORM 配置（驼峰映射、逻辑删除等） |
| `knife4j.*` / `springdoc.*` | 接口文档开关 |

完整配置请参考模板文件，此处仅列出关键连接信息示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/matchsystem?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_mysql_username
    password: your_mysql_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

> **Redisson 说明：** `RedissonConfig` 读取 `spring.redis.*` 配置并固定使用 Redis DB 3。如需修改 Redisson 使用的数据库编号，请编辑 `RedissonConfig.java` 中的 `setDatabase(3)`。

### 5.4 启动服务

```powershell
mvn spring-boot:run
```

启动成功后默认访问地址：

| 地址 | 说明 |
|------|------|
| `http://localhost:8080/api` | 服务根路径 |
| `http://localhost:8080/api/doc.html` | Knife4j 接口文档 |
| `http://localhost:8080/api/v3/api-docs` | OpenAPI JSON 文档 |

## 6. 核心功能

### 用户管理

- 用户注册、登录、注销、查询、删除、更新
- Spring Session + Redis 实现分布式登录态
- 管理员权限校验（搜索用户、删除用户等接口仅管理员可用）

### 标签匹配与用户推荐

- 用户标签以 JSON 列表存储在 `user.tags` 字段
- 按标签列表搜索用户（`/user/search/tags`）
- 基于标签编辑距离算法的用户匹配推荐（`/user/match`）
- 推荐用户列表接口（`/user/recommend`），带分页与缓存

### 队伍管理

- 队伍创建、更新、删除、查询（支持分页）
- 加入队伍（公开 / 加密需密码）、退出队伍
- 查询我创建的队伍、我加入的队伍
- 队伍状态：公开（0）、私有（1）、加密（2），到期自动过期

### 推荐缓存预热

- 启动时自动触发缓存预热（可通过 `match.cache.warmup.bootstrap-enabled` 开关控制）
- RabbitMQ 延时消息驱动：消息在延时队列中等待，到期后自动转发到执行队列消费
- 逻辑过期机制：缓存带逻辑过期时间，到期后不删除，后台异步刷新，用户请求始终命中旧缓存
- Redisson 分布式锁保证缓存重建时只有一个线程执行，避免缓存击穿
- 刷新提前量控制（`refresh-ahead-millis`）：在逻辑过期前主动触发刷新

### 数据导入

- 支持通过 EasyExcel 批量导入用户数据（`once/importuser` 包下脚本）

## 7. 接口概览

### 用户接口 (`/user`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/user/register` | 用户注册 | 公开 |
| POST | `/user/login` | 用户登录 | 公开 |
| POST | `/user/logout` | 用户注销 | 登录 |
| GET | `/user/current` | 获取当前登录用户 | 登录 |
| GET | `/user/search` | 搜索用户（按用户名） | 管理员 |
| GET | `/user/search/tags` | 按标签搜索用户 | 登录 |
| POST | `/user/delete` | 删除用户 | 管理员 |
| POST | `/user/update` | 更新用户信息 | 登录 |
| GET | `/user/recommend` | 推荐用户列表（分页） | 登录 |
| GET | `/user/match` | 标签匹配推荐（最多 20 人） | 登录 |

### 队伍接口 (`/team`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/team/add` | 创建队伍 | 登录 |
| POST | `/team/delete` | 删除队伍（仅队长） | 登录 |
| POST | `/team/update` | 更新队伍信息（仅队长） | 登录 |
| GET | `/team/get` | 获取队伍详情 | 公开 |
| GET | `/team/list` | 查询队伍列表 | 公开 |
| GET | `/team/list/page` | 分页查询队伍 | 公开 |
| POST | `/team/join` | 加入队伍 | 登录 |
| POST | `/team/quit` | 退出队伍 | 登录 |
| GET | `/team/list/my/create` | 查询我创建的队伍 | 登录 |
| GET | `/team/list/my/join` | 查询我加入的队伍 | 登录 |

> 以上接口均带上下文路径前缀 `/api`，完整文档请访问 Knife4j：`http://localhost:8080/api/doc.html`

## 8. 打包与运行

### 8.1 打包

```powershell
mvn clean package
```

### 8.2 运行 jar

```powershell
java -jar target/matchsystem-0.0.1-SNAPSHOT.jar
```

> 打包时默认跳过测试（`pom.xml` 中 `skipTests=true`），如需执行测试：`mvn clean package -DskipTests=false`

## 9. 生产配置建议

### 配置文件管理

- 本地开发使用 `application.yml`
- 生产环境使用 `application-prod.yml`
- 敏感配置优先通过环境变量注入（生产模板已预留 `${VAR:default}` 占位符）
- Redis、RabbitMQ、MySQL 的密码不建议直接写死到仓库

### 环境变量一览

**基础连接配置：**

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `MYSQL_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/matchsystem` |
| `MYSQL_USERNAME` | MySQL 用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | `replace_me` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | `replace_me` |
| `REDIS_DATABASE` | Redis 数据库编号 | `0` |
| `RABBITMQ_HOST` | RabbitMQ 地址 | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ 端口 | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | `guest` |

**缓存预热配置：**

| 环境变量 | 说明 | 生产默认值 |
|----------|------|------------|
| `MATCH_CACHE_WARMUP_BOOTSTRAP_ENABLED` | 启动时是否触发预热 | `true` |
| `MATCH_CACHE_WARMUP_SEED_USER_IDS` | 预热种子用户 ID | `1` |
| `MATCH_CACHE_WARMUP_DELAY_MILLIS` | 延时消息延迟（毫秒） | `43200000`（12h） |
| `MATCH_CACHE_WARMUP_LOGIC_EXPIRE_MILLIS` | 逻辑过期时间（毫秒） | `43200000`（12h） |
| `MATCH_CACHE_WARMUP_REFRESH_AHEAD_MILLIS` | 刷新提前量（毫秒） | `1800000`（30min） |
| `MATCH_CACHE_WARMUP_REDIS_TTL_MINUTES` | Redis 物理 TTL（分钟） | `1440`（24h） |
| `MATCH_CACHE_WARMUP_LOCK_LEASE_SECONDS` | 分布式锁持有时间（秒） | `30` |

> 完整配置项请参考 `application-prod-template.yml`。

## 10. 常见问题与排查

### 启动阶段

| 问题 | 排查方向 |
|------|----------|
| 数据库连接失败 | 检查 MySQL 是否启动、账号密码是否正确、数据库 `matchsystem` 是否已创建 |
| Redis 连接失败 | 检查 Redis 是否启动、密码是否正确、端口是否被占用 |
| RabbitMQ 连接失败 | 检查 RabbitMQ 是否启动，确认延迟消息插件已启用（`rabbitmq-plugins enable rabbitmq_delayed_message_exchange`） |
| Redisson 初始化失败 | Redisson 读取 `spring.redis.*` 配置并固定使用 DB 3，确认 Redis 允许访问该数据库；如有密码需确保 `spring.redis.password` 已配置 |

### 运行阶段

| 问题 | 排查方向 |
|------|----------|
| 登录态无效 | 检查 Redis 是否正常运行，确认前端请求正确携带 Cookie；部署到线上时需修改 `session.cookie.domain` 为实际域名 |
| 推荐缓存未预热 | 检查 RabbitMQ 是否启动，确认 `match.cache.warmup.bootstrap-enabled=true` |
| 文档页面无法访问 | 确认服务已启动，地址需带上下文路径 `/api`（如 `http://localhost:8080/api/doc.html`） |

### 开发阶段

| 问题 | 排查方向 |
|------|----------|
| IDEA 提示 `get/set` 不存在 | 检查 Lombok 插件是否安装、Annotation Processing 是否开启、项目 JDK 是否为 1.8 |
| Knife4j 页面 404 | Spring Boot 2.6 + Knife4j 4.x 存在已知路径兼容问题，检查 `spring.mvc.pathmatch.matching-strategy` 配置 |
| `application.yml` 找不到 | 该文件已被 gitignore，需从模板复制（见 5.3 节） |

