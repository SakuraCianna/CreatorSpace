<template>
  <section ref="root" class="auth-page">
    <form class="auth-card auth-card--wide" data-reveal @submit.prevent="submitLogin">
      <div class="auth-card__visual">
        <ShieldCheck :size="28" />
        <p class="page-kicker">Creator Login</p>
        <h1>{{ loginMode === 'ADMIN' ? '进入内容工作台' : '登录后参与创作互动' }}</h1>
        <p>{{ loginMode === 'ADMIN' ? '管理员登录后可以进入 CMS，管理文章、作品、灵感、评论和主题配置。' : '普通用户登录后可以评论、回复和收藏公开内容。' }}</p>
      </div>
      <div class="auth-card__form">
        <div class="auth-mode-switch">
          <button type="button" :class="{ 'is-active': loginMode === 'USER' }" @click="loginMode = 'USER'">访客登录</button>
          <button type="button" :class="{ 'is-active': loginMode === 'ADMIN' }" @click="loginMode = 'ADMIN'">管理员登录</button>
        </div>
        <label>
          用户名
          <input v-model="form.username" autocomplete="username" name="username" />
        </label>
        <label>
          密码
          <input v-model="form.password" autocomplete="current-password" name="password" type="password" />
        </label>
        <button class="button button-filled" :disabled="isSubmitting" type="submit">
          <LoaderCircle v-if="isSubmitting" class="spin" :size="16" />
          {{ isSubmitting ? '登录中...' : loginMode === 'ADMIN' ? '登录后台' : '登录账号' }}
        </button>
        <RouterLink class="auth-switch" to="/register">没有账号，先注册普通用户</RouterLink>
        <p v-if="message" class="form-message">{{ message }}</p>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, ShieldCheck } from '@lucide/vue'

import { loginAdmin, loginUser } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { useSessionStore } from '@/shared/sessionStore'

const root = ref<HTMLElement | null>(null)
const router = useRouter()
const route = useRoute()
const session = useSessionStore()
const form = reactive({
  username: '',
  password: '',
})
const message = ref('')
const isSubmitting = ref(false)
const loginMode = ref(route.query.mode === 'admin' || readRedirectPath().startsWith('/admin') ? 'ADMIN' : 'USER')

usePageReveal(root)

async function submitLogin() {
  if (!form.username.trim() || !form.password) {
    message.value = '请输入用户名和密码'
    return
  }

  isSubmitting.value = true
  message.value = ''
  try {
    const payload = {
      username: form.username.trim(),
      password: form.password,
    }
    const token = loginMode.value === 'ADMIN' ? await loginAdmin(payload) : await loginUser(payload)
    session.setSession(token.accessToken, token.user)
    form.password = ''
    router.push(loginMode.value === 'ADMIN' ? readRedirectPath() : readPublicRedirectPath())
  } catch (error) {
    message.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}

function readPublicRedirectPath() {
  const redirect = readRedirectPath()
  return redirect.startsWith('/admin') ? '/articles' : redirect
}

function readRedirectPath() {
  const redirect = route.query.redirect
  if (Array.isArray(redirect)) {
    return redirect[0] || '/admin'
  }
  return redirect || '/admin'
}
</script>
