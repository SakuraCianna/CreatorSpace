// 导入并注册 Pinia 状态管理库以及 Vue 应用实例创建函数
import { createPinia } from 'pinia'
import { createApp } from 'vue'

// 导入主根组件与全局路由规则配置
import App from './App.vue'
import router from './router'
// 导入 markdown 代码高亮样式与应用全局基础样式
import 'highlight.js/styles/github-dark.css'
import '../styles/base.css'

// 初始化 Vue 应用并挂载 Pinia 状态管理与路由管理器
// 初始化 Vue 应用实例并依次挂载 Pinia 状态管理与前端路由规则
createApp(App).use(createPinia()).use(router).mount('#app')
