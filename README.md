# AI 编程匹配助手

基于标签的智能编程伙伴匹配平台，支持用户推荐、队伍管理和 AI 编程助手。

## 简介

AI 编程匹配助手是一个前后端分离的编程伙伴匹配系统，通过标签编辑距离算法实现用户智能匹配。系统采用 Redis 缓存预热 + RabbitMQ 延时消息的架构保证推荐接口的高性能，并集成 LangChain4j + DeepSeek 提供 AI 编程助手功能。

## 快速开始

### 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21+ | 后端编译目标 |
| Maven | 3.6+ | 后端构建工具 |
| Node.js | 18+ | 前端运行环境 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.x+ | 缓存 + Session 存储 |
| RabbitMQ | 3.x+ | 消息队列（需启用延迟消息插件） |

### 克隆项目

```bash
git clone --recurse-submodules https://github.com/wobushi041/matchsystem.git
cd matchsystem
```

### 后端启动

```bash
cd matchsystem-backend

# 1. 初始化数据库（执行 sql/create_table.sql）

# 2. 复制配置文件并修改
cp src/main/resources/application-template.yml src/main/resources/application.yml
# 编辑 application.yml，填写 MySQL、Redis、RabbitMQ 连接信息

# 3. 启动服务
mvn spring-boot:run
```

### 前端启动

```bash
cd matchsystem-frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:5173 | Vue 开发服务器 |
| 后端 API | http://localhost:8080/api | Spring Boot 服务 |
| API 文档 | http://localhost:8080/api/doc.html | Knife4j 接口文档 |

## 功能特性

- **用户管理**：注册、登录、个人信息编辑、标签管理
- **智能匹配**：基于标签编辑距离算法的用户推荐
- **队伍系统**：创建/加入/退出队伍，支持公开/加密/私有状态
- **推荐缓存**：Redis 缓存预热 + 逻辑过期 + 分布式锁，保证高并发性能
- **AI 助手**：集成 DeepSeek 的 SSE 流式编程助手，支持 RAG 增强

## 技术栈

### 前端

- Vue 3 + TypeScript
- Vite 构建工具
- Vant UI 组件库
- Vue Router 路由管理
- Axios HTTP 客户端

### 后端

- Spring Boot 3.5.3 + Java 21
- MyBatis-Plus ORM
- MySQL 数据库
- Redis + Redisson（缓存/分布式锁）
- RabbitMQ（延时消息队列）
- LangChain4j + DeepSeek（AI 对话）
- Knife4j（API 文档）

## 项目结构

```plaintext
matchsystem/
├── matchsystem-frontend/    # 前端项目（Vue 3）
│   ├── src/
│   │   ├── components/      # 公共组件
│   │   ├── pages/           # 页面组件
│   │   ├── services/        # API 服务
│   │   ├── models/          # 类型定义
│   │   └── plugins/         # 插件配置
│   └── package.json
│
├── matchsystem-backend/     # 后端项目（Spring Boot）
│   ├── src/main/java/       # Java 源码
│   ├── src/main/resources/  # 配置文件、Mapper XML
│   └── pom.xml
│
└── .gitignore               # 统一忽略规则
```

## 部署

### 前端部署

```bash
cd matchsystem-frontend
npm run build
# 将 dist/ 目录部署到 Nginx 或其他静态服务器
```

### 后端部署

```bash
cd matchsystem-backend
mvn clean package
java -jar target/matchsystem-0.0.1-SNAPSHOT.jar
```

生产环境建议通过环境变量注入敏感配置，参考 `application-prod-template.yml`。

## 常见问题

### Q: 子模块克隆失败？

A: 确保使用 `--recurse-submodules` 参数，或执行 `git submodule init && git submodule update`。

### Q: RabbitMQ 连接失败？

A: 需要启用延迟消息插件：`rabbitmq-plugins enable rabbitmq_delayed_message_exchange`。

### Q: Redisson 初始化失败？

A: Redisson 固定使用 Redis DB 3，确保 Redis 允许访问该数据库。

## 许可证

MIT License
