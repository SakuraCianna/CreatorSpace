<template>
  <div class="saas-auth-layout">
    <div class="auth-split auth-visual">
      <div class="visual-content">
        <div class="brand-logo">
          <ShieldCheck :size="32" :stroke-width="1.5" />
          <span>CreatorSpace</span>
        </div>
        <div class="visual-quote">
          <h2>"记录创意，<br/>开始搭建您的数字花园。"</h2>
          <p>Join our community of builders and creators.</p>
        </div>
      </div>
    </div>
    
    <div class="auth-split auth-form-container">
      <div class="form-wrapper">
        <div class="form-header">
          <h1>创建账号</h1>
          <p>只需几秒，开启您的专属创作空间</p>
        </div>
        
        <form class="auth-form" @submit.prevent="submitRegister">
          <div class="input-group">
            <label>用户名</label>
            <input v-model="form.username" autocomplete="username" placeholder="请输入字母和数字" />
          </div>

          <div class="input-group">
            <label>邮箱</label>
            <input v-model="form.email" type="email" autocomplete="email" placeholder="请输入常用邮箱" />
          </div>

          <div class="input-group">
            <label>密码</label>
            <input v-model="form.password" type="password" autocomplete="new-password" placeholder="至少 8 位，包含字母和数字" />
          </div>

          <div class="hcaptcha-wrapper">
            <VueHcaptcha ref="hcaptchaRef" :sitekey="hcaptchaSiteKey" @verify="onVerify" @expired="onExpired" @error="onError" />
          </div>

          <button class="submit-btn" :disabled="isSubmitting || !hcaptchaToken" type="submit">
            <LoaderCircle v-if="isSubmitting" class="spin" :size="18" />
            <span v-else>注册账号</span>
          </button>

          <div class="form-footer">
            <span class="text-muted">已有账号？</span>
            <RouterLink class="link-primary" :to="loginRoute">直接登录</RouterLink>
          </div>

          <div v-if="message" class="error-msg" :class="{ 'success-msg': isSuccess }">
            {{ message }}
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, ShieldCheck } from '@lucide/vue'
import VueHcaptcha from '@hcaptcha/vue3-hcaptcha'
import { registerUser } from '@/services/content'
import { HttpError, toUserMessage } from '@/services/http'
import { normalizeAuthRedirect } from '@/shared/authRedirect'

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: '',
  email: '',
  password: '',
})
const message = ref('')
const isSuccess = ref(false)
const isSubmitting = ref(false)

const loginRoute = computed(() => ({
  name: 'login',
  query: {
    redirect: readPublicRedirectPath(),
  },
}))

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

watch([() => form.username, () => form.email, () => form.password], () => {
  message.value = ''
  isSuccess.value = false
})

async function submitRegister() {
  if (!form.username.trim() || !form.email.trim() || !form.password) {
    message.value = '请填写完整的注册信息'
    isSuccess.value = false
    return
  }
  if (!hcaptchaToken.value) {
    message.value = '请完成人机验证'
    isSuccess.value = false
    return
  }
  isSubmitting.value = true
  message.value = ''
  isSuccess.value = false
  try {
    await registerUser({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      hcaptchaToken: hcaptchaToken.value,
    })
    isSuccess.value = true
    message.value = '注册成功！正在跳转至登录页面...'
    setTimeout(() => {
      router.push({
        name: 'login',
        query: {
          redirect: readPublicRedirectPath(),
          username: form.username.trim(),
        },
      })
    }, 1200)
  } catch (error) {
    if (hcaptchaRef.value) hcaptchaRef.value.reset()
    hcaptchaToken.value = ''
    isSuccess.value = false
    message.value = registerErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function registerErrorMessage(error: unknown) {
  if (!(error instanceof HttpError)) {
    return '注册失败，请稍后重试'
  }
  if (error.status === 0) {
    return '无法连接到服务器，请检查网络'
  }
  return error.backendMessage || toUserMessage(error, '注册失败')
}

function readPublicRedirectPath() {
  const redirect = normalizeAuthRedirect(route.query.redirect, '/articles')
  return redirect.startsWith('/admin') ? '/articles' : redirect
}
</script>

<style scoped>
.saas-auth-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
  background-color: #ffffff;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.auth-split {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.auth-visual {
  background-image: url('@/assets/images/auth_split_bg.jpg');
  background-size: cover;
  background-position: center;
  position: relative;
  color: white;
  padding: 48px;
  justify-content: space-between;
}

.auth-visual::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.6) 100%);
  z-index: 1;
}

.visual-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: space-between;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.visual-quote h2 {
  font-size: 40px;
  line-height: 1.2;
  font-weight: 600;
  margin: 0 0 16px 0;
  letter-spacing: -1px;
}

.visual-quote p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.auth-form-container {
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  padding: 24px;
}

.form-wrapper {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.form-header p {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.input-group input {
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 15px;
  color: #111827;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-group input:focus {
  outline: none;
  border-color: #000000;
  box-shadow: 0 0 0 1px #000000;
}

.hcaptcha-wrapper {
  display: flex;
  justify-content: center;
  transform: scale(0.95);
  transform-origin: left center;
  margin-top: 4px;
}

.submit-btn {
  height: 44px;
  border: none;
  border-radius: 8px;
  background: #111827;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.2s;
  margin-top: 8px;
}

.submit-btn:hover:not(:disabled) {
  background: #374151;
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.form-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
}

.text-muted {
  color: #6b7280;
  margin-right: 6px;
}

.link-primary {
  color: #111827;
  font-weight: 600;
  text-decoration: none;
}

.link-primary:hover {
  text-decoration: underline;
}

.error-msg {
  font-size: 14px;
  color: #dc2626;
  background: #fee2e2;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
  font-weight: 500;
}

.success-msg {
  color: #059669;
  background: #d1fae5;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 860px) {
  .auth-visual {
    display: none;
  }
  .form-wrapper {
    max-width: 100%;
    padding: 0 24px;
  }
}
</style>
