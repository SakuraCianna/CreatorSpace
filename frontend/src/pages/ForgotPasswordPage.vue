<template>
  <section ref="root" class="auth-page auth-page--material">
    <form class="auth-card auth-card--material" data-reveal @submit.prevent="step === 'email' ? sendCode() : submitReset()">
      <div class="auth-card__visual auth-card__visual--material auth-card__visual--warm">
        <span class="material-icon-badge">
          <ShieldCheck :size="24" />
        </span>
        <p class="page-kicker">Reset Password</p>
        <h1>找回密码</h1>
        <p>使用注册时的 QQ 邮箱接收验证码重置密码。</p>
      </div>
      <div class="auth-card__form">
        <template v-if="step === 'email'">
          <div>
            <p class="page-kicker">Step 1</p>
            <h2>输入注册邮箱</h2>
          </div>
          <label class="md-field">
            <span>QQ 邮箱</span>
            <input
              v-model="email"
              autocomplete="email"
              name="email"
              type="email"
              placeholder="yourname@qq.com"
            />
          </label>
          <button class="button button-filled" :disabled="sending || !isValidEmail" type="submit">
            <LoaderCircle v-if="sending" class="spin" :size="16" />
            发送验证码
          </button>
        </template>
        <template v-else>
          <div>
            <p class="page-kicker">Step 2</p>
            <h2>重置密码</h2>
          </div>
          <label class="md-field">
            <span>验证码</span>
            <input
              v-model="verificationCode"
              autocomplete="one-time-code"
              maxlength="6"
              name="code"
              inputmode="numeric"
              placeholder="输入 6 位验证码"
            />
            <button
              class="button button-tonal code-send-btn"
              type="button"
              :disabled="codeCountdown > 0"
              @click="resendCode"
            >
              {{ codeCountdown > 0 ? `重新发送(${codeCountdown}s)` : '重新发送验证码' }}
            </button>
          </label>
          <label class="md-field">
            <span>新密码</span>
            <input
              v-model="newPassword"
              autocomplete="new-password"
              maxlength="72"
              minlength="6"
              name="newPassword"
              type="password"
            />
          </label>
          <button class="button button-filled" :disabled="resetting" type="submit">
            <LoaderCircle v-if="resetting" class="spin" :size="16" />
            重置密码
          </button>
        </template>
        <RouterLink class="auth-switch" :to="{ name: 'login' }">返回登录</RouterLink>
        <p v-if="message" class="form-message" :class="`form-message--${messageType}`">{{ message }}</p>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { LoaderCircle, ShieldCheck } from '@lucide/vue'

import { sendForgotPasswordCode, resetPassword } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'

const root = ref<HTMLElement | null>(null)
const router = useRouter()

const step = ref<'email' | 'reset'>('email')
const email = ref('')
const verificationCode = ref('')
const newPassword = ref('')
const message = ref('')
const messageType = ref<'idle' | 'success' | 'error'>('idle')
const sending = ref(false)
const resetting = ref(false)
const codeCountdown = ref(0)
let codeTimer: number | undefined

const isValidEmail = computed(() => /^[^\s@]+@qq\.com$/.test(email.value))

usePageReveal(root)

onBeforeUnmount(() => {
  if (codeTimer !== undefined) {
    clearInterval(codeTimer)
    codeTimer = undefined
  }
})

async function sendCode() {
  if (!isValidEmail.value) {
    setMessage('请输入有效的 QQ 邮箱', 'error')
    return
  }
  sending.value = true
  clearMessage()
  try {
    await sendForgotPasswordCode(email.value)
    startCountdown()
    step.value = 'reset'
    setMessage('验证码已发送，请查收邮件', 'success')
  } catch (e) {
    setMessage(toUserMessage(e, '发送失败'), 'error')
  } finally {
    sending.value = false
  }
}

async function resendCode() {
  try {
    await sendForgotPasswordCode(email.value)
    startCountdown()
    setMessage('验证码已重新发送', 'success')
  } catch (e) {
    setMessage(toUserMessage(e, '发送失败'), 'error')
  }
}

function startCountdown() {
  codeCountdown.value = 60
  codeTimer = window.setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(codeTimer)
      codeTimer = undefined
    }
  }, 1000)
}

async function submitReset() {
  if (!verificationCode.value || verificationCode.value.length !== 6) {
    setMessage('请输入 6 位验证码', 'error')
    return
  }
  if (!newPassword.value || newPassword.value.length < 6) {
    setMessage('密码长度不能少于 6 位', 'error')
    return
  }
  resetting.value = true
  clearMessage()
  try {
    await resetPassword({
      email: email.value,
      verificationCode: verificationCode.value,
      newPassword: newPassword.value,
    })
    setMessage('密码重置成功，即将跳转登录页', 'success')
    setTimeout(() => router.push({ name: 'login' }), 1500)
  } catch (e) {
    setMessage(toUserMessage(e, '重置失败'), 'error')
  } finally {
    resetting.value = false
  }
}

function setMessage(value: string, type: 'success' | 'error') {
  message.value = value
  messageType.value = type
}

function clearMessage() {
  message.value = ''
  messageType.value = 'idle'
}
</script>

<style scoped>
.auth-page {
  display: grid;
  min-height: calc(100vh - 72px);
  place-items: center;
  padding: clamp(28px, 5vw, 72px) 0;
}

.auth-card {
  width: min(960px, 100%);
  border: 1px solid var(--md-sys-color-outline-variant);
  border-radius: 28px;
  background: var(--md-sys-color-surface-container-lowest);
  box-shadow: var(--md-sys-elevation-2);
}

.auth-card--material {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(360px, 0.82fr);
  gap: 0;
  min-height: 560px;
  overflow: hidden;
}

.auth-card__visual,
.auth-card__form {
  display: grid;
  gap: 18px;
  padding: 32px;
}

.auth-card__form {
  align-content: center;
  gap: 18px;
  padding: clamp(28px, 4vw, 48px);
}

.auth-card__visual {
  align-content: end;
  min-height: 420px;
  background:
    linear-gradient(145deg, rgba(16, 19, 31, 0.88), rgba(49, 91, 255, 0.66)),
    var(--tone-night);
  color: #fff;
}

.auth-card__visual--material {
  position: relative;
  align-content: end;
  min-height: 100%;
  padding: clamp(32px, 5vw, 56px);
  overflow: hidden;
  background:
    linear-gradient(160deg, rgba(255, 221, 176, 0.96), rgba(216, 226, 255, 0.72)),
    var(--md-sys-color-tertiary-container);
  color: var(--md-sys-color-on-primary-container);
}

.auth-card__visual--material::before {
  content: "";
  position: absolute;
  right: -90px;
  bottom: -120px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--md-sys-color-primary) 28%, transparent);
}

.auth-card__visual--material > * {
  position: relative;
  z-index: 1;
}

.auth-card__visual--material h1,
.auth-card__visual--material p,
.auth-card__visual--material .page-kicker,
.auth-card__visual--material svg {
  color: inherit;
}

.auth-card h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.08;
}

.auth-card h2 {
  margin: 6px 0 0;
  color: var(--md-sys-color-on-surface);
  font-size: 26px;
  line-height: 1.16;
}

.auth-card label {
  display: grid;
  gap: 8px;
  color: var(--md-sys-color-on-surface-variant);
  font-size: 13px;
  font-weight: 750;
}

.auth-card input {
  width: 100%;
  min-height: 56px;
  border: 1px solid transparent;
  border-bottom: 1px solid var(--md-sys-color-outline);
  border-radius: 12px 12px 0 0;
  padding: 0 14px;
  background: var(--md-sys-color-surface-container);
  color: var(--md-sys-color-on-surface);
  outline: 0;
  transition: background 180ms ease, border-color 180ms ease;
}

.auth-card input:focus {
  border-bottom-color: var(--md-sys-color-primary);
  background: var(--md-sys-color-surface-container-high);
}

.auth-card button {
  width: 100%;
}

.auth-switch {
  display: inline-flex;
  justify-content: center;
  color: var(--md-sys-color-primary);
  font-size: 14px;
  font-weight: 760;
}

.material-icon-badge {
  display: inline-grid;
  width: 56px;
  height: 56px;
  place-items: center;
  border-radius: 18px;
  background: color-mix(in srgb, var(--md-sys-color-surface) 72%, transparent);
  box-shadow: var(--md-sys-elevation-1);
}

.code-send-btn {
  margin-top: 8px;
  width: 100%;
}

.form-message {
  margin: 0;
  padding: 10px 12px;
  border-left: 3px solid transparent;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.55;
}

.form-message--error {
  border-left-color: var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
}

.form-message--success {
  border-left-color: var(--tone-teal);
  background: rgba(0, 124, 114, 0.08);
  color: #055f57;
}

@media (max-width: 1020px) {
  .auth-card--material {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-card__visual--material {
    min-height: 280px;
  }
}

@media (max-width: 760px) {
  .auth-page {
    padding-top: 28px;
  }

  .auth-card {
    border-radius: 24px;
  }

  .auth-card__visual--material,
  .auth-card__form {
    padding: 24px;
  }

  .auth-card__visual--material {
    min-height: 240px;
  }

  .auth-card h1 {
    font-size: 34px;
  }
}
</style>
