import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
import 'highlight.js/styles/github-dark.css'
import '../styles/base.css'

createApp(App).use(createPinia()).use(router).mount('#app')
