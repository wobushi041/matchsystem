# 前端 Vue.js 开发指南

## 一、Vue 3 核心

### 1.1 组合式 API（Composition API）
- setup 语法糖：<script setup>
- ref 与 reactive：响应式数据
- computed：计算属性
- watch 与 watchEffect：侦听器
- 生命周期钩子：onMounted、onUpdated、onUnmounted
- provide/inject：跨层级通信

### 1.2 模板语法
- 插值：{{ }}、v-text、v-html
- 条件渲染：v-if、v-else-if、v-else、v-show
- 列表渲染：v-for、key 的重要性
- 事件处理：v-on、事件修饰符、按键修饰符
- 表单绑定：v-model 及其原理
- 插槽：默认插槽、具名插槽、作用域插槽

### 1.3 组件化开发
- Props 定义与类型校验
- Emits 事件声明
- defineProps、defineEmits、defineExpose
- 组件注册：全局 vs 局部
- 动态组件：component :is
- keep-alive：组件缓存

## 二、Vue 生态

### 2.1 Vue Router
- 路由配置：createRouter、history 模式
- 动态路由：params 与 query
- 嵌套路由：children
- 路由守卫：全局、路由独享、组件内
- 路由懒加载：动态 import
- 导航：router-link、programmatic navigation

### 2.2 Pinia 状态管理
- Store 定义：defineStore
- State、Getters、Actions
- 持久化：pinia-plugin-persistedstate
- Store 之间的互相调用
- 与 Vuex 的对比

### 2.3 网络请求
- Axios 封装：拦截器、请求/响应处理
- 请求取消：AbortController
- 文件上传与下载
- 错误处理与重试机制

## 三、UI 框架与工具

### 3.1 Element Plus
- 常用组件：ElButton、ElTable、ElForm、ElDialog
- 表单验证：el-form rules
- 表格分页：ElPagination 配合后端接口
- 自定义主题与按需引入

### 3.2 开发工具链
- Vite：开发服务器、构建优化、插件系统
- TypeScript 集成：类型定义、泛型组件
- ESLint + Prettier：代码规范
- UnoCSS / Tailwind CSS：原子化 CSS

## 四、项目实战要点

### 4.1 性能优化
- 路由懒加载与组件异步加载
- 图片懒加载：v-lazy
- 虚拟滚动：大数据列表
- 代码分割与 Tree Shaking
- keep-alive 缓存策略

### 4.2 工程化
- 环境变量：.env 文件
- 代理配置：vite.config.ts proxy
- 构建优化：gzip、CDN 引入
- Git 规范：commit message、分支策略

### 4.3 常见面试题
- Vue 2 vs Vue 3 的区别
- 响应式原理：Object.defineProperty vs Proxy
- nextTick 的作用和原理
- 组件通信方式汇总
- Vue 的 diff 算法
