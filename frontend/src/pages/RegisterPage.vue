<template>
  <section ref="root" class="premium-auth-page">
    <div class="glass-card glass-card--register" data-reveal>
      <div class="card-header">
        <div class="logo-box">
          <UserPlus :size="28" :stroke-width="2" />
        </div>
        <h1>加入 {{ siteName }}</h1>
        <p class="subtitle">创建您的读者账号</p>
      </div>

      <form class="auth-form" @submit.prevent="submitRegister">
        <div class="input-row">
          <div class="input-group">
            <label>用户名</label>
            <input v-model="form.username" autocomplete="username" maxlength="64" minlength="3" placeholder="起个名字" />
          </div>
          <div class="input-group">
            <label>密码</label>
            <input v-model="form.password" type="password" autocomplete="new-password" minlength="6" placeholder="设置密码" />
          </div>
        </div>

        <div class="input-group">
          <label>电子邮箱</label>
          <input v-model="form.email" type="email" autocomplete="email" placeholder="输入邮箱地址" />
        </div>

        <div class="input-group input-group--with-btn">
          <label>验证码</label>
          <div class="code-input-row">
            <input v-model="form.verificationCode" autocomplete="one-time-code" maxlength="6" inputmode="numeric" placeholder="6位验证码" />
            <button class="send-code-btn" type="button" :disabled="codeCountdown > 0 || !isValidEmail" @click="sendCode">
              {{ codeCountdown > 0 ? `${codeCountdown}s 后重试` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div class="hcaptcha-wrapper">
          <VueHcaptcha ref="hcaptchaRef" :sitekey="hcaptchaSiteKey" @verify="onVerify" @expired="onExpired" @error="onError" />
        </div>

        <button class="submit-btn" :disabled="isSubmitting || isRedirecting || !hcaptchaToken" type="submit">
          <LoaderCircle v-if="isSubmitting || isRedirecting" class="spin" :size="18" />
          <span v-else>立即注册</span>
        </button>

        <div class="card-footer">
          <span class="text-muted">已有账号？</span>
          <RouterLink class="link" :to="loginRoute">直接登录</RouterLink>
        </div>

        <div v-if="message" class="error-msg" :class="{'success-msg': messageType === 'success'}">
          {{ message }}
        </div>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, UserPlus } from '@lucide/vue'
import VueHcaptcha from '@hcaptcha/vue3-hcaptcha'
import { registerUser, sendRegisterCode } from '@/services/content'
import { HttpError, toUserMessage } from '@/services/http'
import { normalizeAuthRedirect } from '@/shared/authRedirect'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { useSiteIdentity } from '@/shared/siteIdentity'

const root = ref<HTMLElement | null>(null)
const route = useRoute()
const router = useRouter()
const { siteName } = useSiteIdentity({ load: false })
const form = reactive({
  username: '',
  email: '',
  verificationCode: '',
  password: '',
})
const codeCountdown = ref(0)
let codeTimer: number | undefined
const message = ref('')
const messageType = ref<'idle' | 'success' | 'error'>('idle')
const isSubmitting = ref(false)
const isRedirecting = ref(false)
const registerAttemptId = ref(0)
let redirectTimer: number | undefined
const USERNAME_MIN_LENGTH = 3
const USERNAME_MAX_LENGTH = 64
const PASSWORD_MIN_LENGTH = 6
const PASSWORD_MAX_LENGTH = 72

// Allow basic email check for UX, backend restricts to QQ email if needed
const isValidEmail = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))

function sendCode() {
  sendRegisterCode(form.email)
  codeCountdown.value = 60
  codeTimer = window.setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(codeTimer)
      codeTimer = undefined
    }
  }, 1000)
}
const loginRoute = computed(() => ({
  name: 'login',
  query: {
    redirect: readRegisterRedirectPath(),
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
usePageReveal(root)
onBeforeUnmount(() => {
  clearRedirectTimer()
  if (codeTimer !== undefined) {
    clearInterval(codeTimer)
    codeTimer = undefined
  }
})
watch([() => form.username, () => form.password], () => {
  if (isRedirecting.value) {
    return
  }
  registerAttemptId.value += 1
  if (!isSubmitting.value) {
    clearMessage()
  }
})
async function submitRegister() {
  clearRedirectTimer()
  isRedirecting.value = false
  const username = form.username.trim()
  const validationMessage = validateRegisterForm(username, form.password)
  if (validationMessage) {
    registerAttemptId.value += 1
    setMessage(validationMessage, 'error')
    return
  }
  if (!hcaptchaToken.value) {
    registerAttemptId.value += 1
    setMessage('请完成人机验证', 'error')
    return
  }
  isSubmitting.value = true
  clearMessage()
  const attemptId = ++registerAttemptId.value
  try {
    const user = await registerUser({
      username,
      email: form.email.trim(),
      verificationCode: form.verificationCode,
      password: form.password,
      hcaptchaToken: hcaptchaToken.value,
    })
    if (!isCurrentRegisterAttempt(attemptId, username)) {
      return
    }
    isRedirecting.value = true
    form.password = ''
    setMessage(`账号 ${user.username} 创建成功，正在前往登录页。`, 'success')
    redirectTimer = window.setTimeout(() => {
      router.push(loginRoute.value)
    }, 900)
  } catch (error) {
    if (hcaptchaRef.value) hcaptchaRef.value.reset()
    hcaptchaToken.value = ''
    if (isCurrentRegisterAttempt(attemptId, username)) {
      const msg = error instanceof HttpError
        ? (error.backendMessage || toUserMessage(error, '注册失败，请稍后重试'))
        : '注册失败，请稍后重试'
      setMessage(msg, 'error')
    }
  } finally {
    isSubmitting.value = false
  }
}
function validateRegisterForm(username: string, password: string): string {
  if (!username) {
    return '请输入用户名'
  }
  if (username.length < USERNAME_MIN_LENGTH || username.length > USERNAME_MAX_LENGTH) {
    return `用户名长度必须是 ${USERNAME_MIN_LENGTH} 到 ${USERNAME_MAX_LENGTH} 个字符`
  }
  if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    return '请输入有效的邮箱'
  }
  if (!form.verificationCode || form.verificationCode.length !== 6) {
    return '请输入 6 位验证码'
  }
  if (!password) {
    return '请输入密码'
  }
  if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
    return `密码长度必须是 ${PASSWORD_MIN_LENGTH} 到 ${PASSWORD_MAX_LENGTH} 个字符`
  }
  return ''
}
function setMessage(value: string, type: 'success' | 'error') {
  message.value = value
  messageType.value = type
}
function clearMessage() {
  message.value = ''
  messageType.value = 'idle'
}
function isCurrentRegisterAttempt(attemptId: number, username: string): boolean {
  return attemptId === registerAttemptId.value && username === form.username.trim()
}
function clearRedirectTimer() {
  if (redirectTimer === undefined) {
    return
  }
  window.clearTimeout(redirectTimer)
  redirectTimer = undefined
}
function readRegisterRedirectPath() {
  const redirect = normalizeAuthRedirect(route.query.redirect, '/creator/articles')
  return redirect.startsWith('/admin') ? '/creator/articles' : redirect
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
  max-width: 440px;
  padding: 36px;
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
  margin-bottom: 24px;
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

.input-row {
  display: flex;
  gap: 12px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
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

.code-input-row {
  display: flex;
  gap: 8px;
}

.send-code-btn {
  height: 48px;
  padding: 0 16px;
  border: none;
  background: rgba(17, 24, 39, 0.06);
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  border-radius: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.send-code-btn:hover:not(:disabled) {
  background: rgba(17, 24, 39, 0.12);
}

.send-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: #6b7280;
  background: rgba(0, 0, 0, 0.04);
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
  gap: 8px;
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

.text-muted {
  color: #6b7280;
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

@media (max-width: 480px) {
  .glass-card {
    padding: 32px 24px;
    border-radius: 24px;
  }
  .input-row {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
