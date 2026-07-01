<template>
  <section ref="root" class="premium-auth-page">
    <div class="glass-card" data-reveal>
      <div class="card-header">
        <div class="logo-box">
          <ShieldCheck :size="28" :stroke-width="2" />
        </div>
        <h1>{{ loginMode === 'ADMIN' ? '工作台登录' : '欢迎回来' }}</h1>
        <p class="subtitle">{{ loginMode === 'ADMIN' ? '使用管理员身份管理各项服务' : '登录您的账号以继续探索' }}</p>
      </div>
      
      <form class="auth-form" @submit.prevent="submitLogin">
        <div class="mode-toggle">
          <button type="button" :class="{ 'active': loginMode === 'USER' }" @click="loginMode = 'USER'">普通用户</button>
          <button type="button" :class="{ 'active': loginMode === 'ADMIN' }" @click="loginMode = 'ADMIN'">管理员</button>
        </div>

        <div class="input-group">
          <label>用户名 / 邮箱</label>
          <input v-model="form.username" autocomplete="username" placeholder="请输入用户名或邮箱" />
        </div>

        <div class="input-group">
          <label>密码</label>
          <input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" />
        </div>

        <div class="hcaptcha-wrapper">
          <VueHcaptcha ref="hcaptchaRef" :sitekey="hcaptchaSiteKey" @verify="onVerify" @expired="onExpired" @error="onError" />
        </div>

        <button class="submit-btn" :disabled="isSubmitting || !hcaptchaToken" type="submit">
          <LoaderCircle v-if="isSubmitting" class="spin" :size="18" />
          <span v-else>{{ loginMode === 'ADMIN' ? '登录后台' : '登录账号' }}</span>
        </button>

        <div class="card-footer">
          <RouterLink class="link" :to="{ name: 'forgot-password' }">忘记密码？</RouterLink>
          <span class="divider"></span>
          <RouterLink class="link" :to="registerRoute">注册新账号</RouterLink>
        </div>

        <div v-if="message" class="error-msg">
          {{ message }}
        </div>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, ShieldCheck } from '@lucide/vue'
import VueHcaptcha from '@hcaptcha/vue3-hcaptcha'
import { loginAdmin, loginUser } from '@/services/content'
import { HttpError, toUserMessage } from '@/services/http'
import { isAdminRedirect, normalizeAuthRedirect } from '@/shared/authRedirect'
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
const loginMode = ref<'ADMIN' | 'USER'>(route.query.mode === 'admin' || isAdminRedirect(route.query.redirect) ? 'ADMIN' : 'USER')
const loginAttemptId = ref(0)

const registerRoute = computed(() => ({
  name: 'register',
  query: {
    redirect: readPublicRedirectPath(),
  },
}))
usePageReveal(root)

const hcaptchaToken = ref('')
const hcaptchaRef = ref<any>(null)
const hcaptchaSiteKey = import.meta.env.VITE_HCAPTCHA_SITE_KEY

function onVerify(token: string) {
  hcaptchaToken.value = token
}
function onExpired() {
  hcaptchaToken.value = ''
}
function onError() {
  hcaptchaToken.value = ''
}

watch([loginMode, () => form.username, () => form.password], () => {
  loginAttemptId.value += 1
  message.value = ''
})

async function submitLogin() {
  if (!form.username.trim() || !form.password) {
    message.value = '请输入账号和密码'
    return
  }
  if (!hcaptchaToken.value) {
    message.value = '请完成人机验证'
    return
  }
  isSubmitting.value = true
  message.value = ''
  const attemptId = loginAttemptId.value
  const modeSnapshot = loginMode.value
  const usernameSnapshot = form.username.trim()
  try {
    const payload = {
      username: usernameSnapshot,
      password: form.password,
      hcaptchaToken: hcaptchaToken.value,
    }
    const token = modeSnapshot === 'ADMIN' ? await loginAdmin(payload) : await loginUser(payload)
    session.setSession(token.accessToken, token.user, token.refreshToken)
    form.password = ''
    router.push(modeSnapshot === 'ADMIN' ? readRedirectPath() : readPublicRedirectPath())
  } catch (error) {
    if (hcaptchaRef.value) hcaptchaRef.value.reset()
    hcaptchaToken.value = ''
    if (attemptId === loginAttemptId.value && modeSnapshot === loginMode.value && usernameSnapshot === form.username.trim()) {
      message.value = loginErrorMessage(error)
    }
  } finally {
    isSubmitting.value = false
  }
}

function loginErrorMessage(error: unknown) {
  if (!(error instanceof HttpError)) {
    return '登录失败，请稍后重试'
  }
  if (error.status === 0) {
    return '无法连接到服务器，请检查网络'
  }
  return error.backendMessage || toUserMessage(error, '登录失败')
}

function readPublicRedirectPath() {
  const redirect = normalizeAuthRedirect(route.query.redirect, '/articles')
  return redirect.startsWith('/admin') ? '/articles' : redirect
}

function readRedirectPath() {
  return normalizeAuthRedirect(route.query.redirect, '/admin')
}
</script>

<style scoped>
.premium-auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 72px);
  padding: 20px;
  background-image: url('@/assets/images/auth_bg.jpg');
  background-size: cover;
  background-position: center;
  position: relative;
}

.premium-auth-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(17, 24, 39, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.glass-card {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 400px;
  padding: 40px 36px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 28px;
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  animation: floatUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes floatUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-box {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #1f2937, #111827);
  color: white;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 24px rgba(17, 24, 39, 0.25);
}

.card-header h1 {
  font-size: 26px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 6px 0;
  letter-spacing: -0.5px;
}

.card-header .subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  font-weight: 500;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mode-toggle {
  display: flex;
  background: rgba(17, 24, 39, 0.04);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 4px;
}

.mode-toggle button {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-toggle button.active {
  background: white;
  color: #111827;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-group label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-left: 4px;
}

.input-group input {
  height: 48px;
  padding: 0 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  color: #111827;
  transition: all 0.2s;
  font-weight: 500;
}

.input-group input:focus {
  outline: none;
  border-color: #315bff;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(49, 91, 255, 0.1);
}

.hcaptcha-wrapper {
  display: flex;
  justify-content: center;
  transform: scale(0.95);
  transform-origin: center;
  margin: -4px 0;
}

.submit-btn {
  height: 52px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #111827, #374151);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 8px 20px rgba(17, 24, 39, 0.2);
  margin-top: 6px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(17, 24, 39, 0.3);
  background: linear-gradient(135deg, #000000, #1f2937);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 12px;
  font-size: 14px;
}

.link {
  color: #315bff;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s;
}

.link:hover {
  color: #174ea6;
  text-decoration: underline;
}

.divider {
  width: 4px;
  height: 4px;
  background: #d1d5db;
  border-radius: 50%;
}

.error-msg {
  text-align: center;
  font-size: 13px;
  color: #dc2626;
  background: #fee2e2;
  padding: 10px;
  border-radius: 10px;
  margin-top: 4px;
  font-weight: 500;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 480px) {
  .glass-card {
    padding: 32px 24px;
    border-radius: 24px;
  }
}
</style>
