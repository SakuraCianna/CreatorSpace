<template>
  <div class="auth-page-wrapper">
    <div class="auth-card">
      <div class="auth-card-left">
        <div class="brand-logo">
          <ShieldCheck :size="24" :stroke-width="1.5" />
          <span>CreatorSpace</span>
        </div>
        <div class="left-content">
          <h2>"记录创意，<br/>搭建数字花园。"</h2>
          <p>Join our community of builders and creators.</p>
        </div>
      </div>
      
      <div class="auth-card-right">
        <div class="form-wrapper">
          <div class="form-header">
            <h1>创建账号</h1>
            <p>只需几秒，开启您的专属创作空间</p>
          </div>
          
          <form class="auth-form" @submit.prevent="submitRegister">
            <div class="input-group">
              <label>用户名</label>
              <div class="input-inner">
                <input v-model="form.username" autocomplete="username" placeholder="请输入字母和数字" />
              </div>
            </div>

            <div class="input-group">
              <label>邮箱</label>
              <div class="input-inner">
                <input v-model="form.email" type="email" autocomplete="email" placeholder="请输入常用邮箱" />
              </div>
            </div>

            <div class="input-group">
              <label>验证码</label>
              <div class="input-with-button">
                <input v-model="form.verificationCode" type="text" placeholder="6位验证码" maxlength="6" />
                <button type="button" class="send-code-btn" :disabled="isSendingCode || countdown > 0 || !hcaptchaToken || !form.email" @click="sendCode">
                  <LoaderCircle v-if="isSendingCode" class="spin" :size="16" />
                  <span v-else-if="countdown > 0">{{ countdown }}s 后重试</span>
                  <span v-else>发送验证码</span>
                </button>
              </div>
            </div>

            <div class="input-group">
              <label>密码</label>
              <div class="input-inner">
                <input v-model="form.password" type="password" autocomplete="new-password" placeholder="至少 8 位，包含字母和数字" />
              </div>
            </div>

            <div class="hcaptcha-wrapper">
              <VueHcaptcha ref="hcaptchaRef" :sitekey="hcaptchaSiteKey" @verify="onVerify" @expired="onExpired" @error="onError" />
            </div>

            <button class="submit-btn" :disabled="isSubmitting" type="submit">
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
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, onUnmounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, ShieldCheck } from '@lucide/vue'
import VueHcaptcha from '@hcaptcha/vue3-hcaptcha'
import { registerUser, sendRegisterCode } from '../services/content'
import { HttpError, toUserMessage } from '../services/http'
import { normalizeAuthRedirect } from '../shared/authRedirect'

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: '',
  email: '',
  verificationCode: '',
  password: '',
})
const message = ref('')
const isSuccess = ref(false)
const isSubmitting = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)
let timer: number | undefined

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

watch([() => form.username, () => form.email, () => form.password, () => form.verificationCode], () => {
  message.value = ''
  isSuccess.value = false
})

async function sendCode() {
  if (!form.email.trim()) {
    message.value = '请先输入邮箱'
    isSuccess.value = false
    return
  }
  if (!hcaptchaToken.value) {
    message.value = '请先完成人机验证'
    isSuccess.value = false
    return
  }
  isSendingCode.value = true
  message.value = ''
  isSuccess.value = false
  try {
    await sendRegisterCode(form.email.trim(), hcaptchaToken.value)
    message.value = '验证码已发送，请查收'
    isSuccess.value = true
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
    // Optional: reset hcaptcha so they have to verify again to resend
    if (hcaptchaRef.value) hcaptchaRef.value.reset()
    hcaptchaToken.value = ''
  } catch (error) {
    isSuccess.value = false
    message.value = registerErrorMessage(error)
    if (hcaptchaRef.value) hcaptchaRef.value.reset()
    hcaptchaToken.value = ''
  } finally {
    isSendingCode.value = false
  }
}

async function submitRegister() {
  if (!form.username.trim() || !form.email.trim() || !form.password || !form.verificationCode.trim()) {
    message.value = '请填写完整的注册信息'
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
      verificationCode: form.verificationCode.trim(),
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
    isSuccess.value = false
    message.value = registerErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function registerErrorMessage(error: unknown) {
  if (!(error instanceof HttpError)) {
    return '请求失败，请稍后重试'
  }
  if (error.status === 0) {
    return '无法连接到服务器，请检查网络'
  }
  return error.backendMessage || toUserMessage(error, '请求失败')
}

function readPublicRedirectPath() {
  const redirect = normalizeAuthRedirect(route.query.redirect, '/articles')
  return redirect.startsWith('/admin') ? '/articles' : redirect
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.auth-page-wrapper {
  display: flex;
  height: calc(100vh - 64px);
  width: 100%;
  align-items: center;
  justify-content: center;
  background: transparent;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  padding: 12px 24px;
  box-sizing: border-box;
}

.auth-card {
  display: flex;
  width: 100%;
  max-width: 1000px;
  height: auto;
  min-height: 600px;
  max-height: 100%;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.auth-card-left {
  flex: 1;
  position: relative;
  background-image: url('../assets/images/auth_illustration.jpg');
  background-size: cover;
  background-position: center;
  color: white;
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.auth-card-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.8) 0%, rgba(124, 58, 237, 0.8) 100%);
  mix-blend-mode: multiply;
}

.auth-card-left::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 60%);
}

.brand-logo {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.left-content {
  position: relative;
  z-index: 10;
}

.left-content h2 {
  font-size: 36px;
  line-height: 1.25;
  font-weight: 700;
  margin: 0 0 16px 0;
  letter-spacing: -1px;
  color: #ffffff !important;
}

.left-content p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9) !important;
  margin: 0;
}

.auth-card-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #ffffff;
}

.form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 28px;
}

.form-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.form-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
}

.input-inner {
  position: relative;
}

.input-group input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  color: #111827;
  background: #f9fafb;
  transition: all 0.2s;
  box-sizing: border-box;
}

.input-group input:focus {
  outline: none;
  border-color: #6366f1;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.input-with-button {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-button input {
  padding-right: 120px;
}

.send-code-btn {
  position: absolute;
  right: 6px;
  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 6px;
  background: #4f46e5;
  color: white;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.2s;
}

.send-code-btn:hover:not(:disabled) {
  background: #4338ca;
}

.send-code-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.hcaptcha-wrapper {
  display: flex;
  justify-content: center;
  transform: scale(0.92);
  transform-origin: center center;
}

.submit-btn {
  height: 44px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: opacity 0.2s, transform 0.1s;
  margin-top: 4px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.form-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 13px;
}

.text-muted {
  color: #6b7280;
  margin-right: 6px;
}

.link-primary {
  color: #4f46e5;
  font-weight: 600;
  text-decoration: none;
}

.link-primary:hover {
  text-decoration: underline;
}

.error-msg {
  font-size: 13px;
  color: #b91c1c;
  background: #fef2f2;
  padding: 10px;
  border-radius: 8px;
  text-align: center;
  font-weight: 500;
  border: 1px solid #fecaca;
}

.success-msg {
  color: #059669;
  background: #d1fae5;
  border-color: #a7f3d0;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 860px) {
  .auth-card {
    height: auto;
    max-width: 420px;
    flex-direction: column;
  }
  .auth-card-left {
    padding: 32px 24px;
    min-height: 200px;
  }
  .left-content h2 {
    font-size: 28px;
  }
  .auth-card-right {
    padding: 32px 24px;
  }
}
</style>
