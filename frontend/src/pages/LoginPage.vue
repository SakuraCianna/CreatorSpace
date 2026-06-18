<template>
  <section ref="root" class="auth-page auth-page--material">
    <form class="auth-card auth-card--material" data-reveal @submit.prevent="submitLogin">
      <div class="auth-card__visual auth-card__visual--material">
        <span class="material-icon-badge">
          <ShieldCheck :size="24" />
        </span>
        <p class="page-kicker">CreatorSpace Account</p>
        <h1>{{ loginMode === 'ADMIN' ? '进入内容工作台' : '进入创作社区' }}</h1>
        <p>{{ loginMode === 'ADMIN' ? '使用管理员身份管理文章、作品、灵感墙、评论审核与主题配置。' : '登录后参与评论、回复、收藏，并为后续好友可见内容保留身份。' }}</p>
        <div class="material-benefits" aria-label="登录能力">
          <span>CMS</span>
          <span>Gallery</span>
          <span>Comments</span>
        </div>
      </div>
      <div class="auth-card__form">
        <div>
          <p class="page-kicker">Sign in</p>
          <h2>{{ loginMode === 'ADMIN' ? '管理员登录' : '访客登录' }}</h2>
        </div>
        <div class="auth-mode-switch">
          <button type="button" :class="{ 'is-active': loginMode === 'USER' }" @click="loginMode = 'USER'">访客登录</button>
          <button type="button" :class="{ 'is-active': loginMode === 'ADMIN' }" @click="loginMode = 'ADMIN'">管理员登录</button>
        </div>
        <label class="md-field">
          <span>用户名</span>
          <input v-model="form.username" autocomplete="username" name="username" />
        </label>
        <label class="md-field">
          <span>密码</span>
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
