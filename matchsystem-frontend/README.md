# AI 编程匹配助手 - 前端

AI 编程匹配助手前端，基于 Vue 3 + Vant UI 构建。

## 技术栈

- **框架**：Vue 3 + TypeScript
- **构建**：Vite 2.9
- **UI 组件**：Vant 3.4
- **路由**：Vue Router 4
- **HTTP 客户端**：Axios

## 项目结构

```plaintext
src/
├── components/          # 公共组件
│   ├── TeamCardList.vue # 队伍卡片列表
│   └── UserCardList.vue # 用户卡片列表
├── pages/               # 页面组件
│   ├── Index.vue        # 首页（推荐/匹配）
│   ├── SearchPage.vue   # 搜索页
│   ├── SearchResultPage.vue # 搜索结果页
│   ├── TeamPage.vue     # 队伍列表页
│   ├── TeamAddPage.vue  # 创建队伍页
│   ├── TeamUpdatePage.vue # 更新队伍页
│   ├── UserPage.vue     # 个人信息页
│   ├── UserLoginPage.vue # 登录页
│   ├── UserEditPage.vue # 编辑信息页
│   ├── UserUpdatePage.vue # 更新信息页
│   ├── UserTeamJoinPage.vue # 加入队伍页
│   ├── UserTeamCreatePage.vue # 创建队伍页
│   └── AiChatPage.vue   # AI 编程助手页
├── services/            # API 服务层
│   └── user.ts          # 用户相关接口
├── models/              # 类型定义
│   ├── user.d.ts        # 用户类型
│   └── team.d.ts        # 队伍类型
├── states/              # 状态管理
│   └── user.ts          # 用户状态
├── plugins/             # 插件配置
│   └── myAxios.ts       # Axios 实例配置
├── config/              # 配置文件
│   └── route.ts         # 路由配置
├── constants/           # 常量定义
│   └── team.ts          # 队伍相关常量
├── layouts/             # 布局组件
│   └── BasicLayout.vue  # 基础布局
└── assets/              # 静态资源
```

## 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | Index | 首页，支持推荐模式和心动模式 |
| `/team` | TeamPage | 队伍列表 |
| `/team/add` | TeamAddPage | 创建队伍 |
| `/team/update` | TeamUpdatePage | 更新队伍 |
| `/search` | SearchPage | 搜索用户 |
| `/user/list` | SearchResultPage | 搜索结果 |
| `/user` | UserPage | 个人信息 |
| `/user/edit` | UserEditPage | 编辑信息 |
| `/user/login` | UserLoginPage | 登录 |
| `/user/team/join` | UserTeamJoinPage | 加入队伍 |
| `/user/team/create` | UserTeamCreatePage | 创建队伍 |
| `/ai/chat` | AiChatPage | AI 编程助手 |

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览构建结果
npm run preview
```

## 配置说明

后端 API 地址在 `src/plugins/myAxios.ts` 中配置，默认连接 `http://localhost:8080/api`。

## 组件说明

### UserCardList

用户卡片列表组件，展示用户头像、用户名、标签等信息。

```vue
<user-card-list :user-list="userList" :loading="loading" />
```

### TeamCardList

队伍卡片列表组件，展示队伍名称、描述、成员数、状态等信息。

```vue
<team-card-list :team-list="teamList" :loading="loading" />
```
