<template>
<!-- 读者注册表单页面 -->
<!-- 读者注册表单页面 -->
<!-- 读者用户账号注册页面 -->
  <section ref="root" class="auth-page auth-page--material">
    <form class="auth-card auth-card--material" data-reveal @submit.prevent="submitRegister">
      <div class="auth-card__visual auth-card__visual--material auth-card__visual--warm">
        <span class="material-icon-badge">
          <UserPlus :size="24" />
        </span>
        <p class="page-kicker">Join {{ siteName }}</p>
        <h1>注册读者身份</h1>
        <p>普通用户可以为后续评论、点赞、收藏和好友可见内容预留身份。第一阶段只需要用户名和密码。</p>
        <div class="material-benefits" aria-label="注册能力">
          <span>Reader</span>
          <span>Comment</span>
          <span>Favorite</span>
        </div>
      </div>
      <div class="auth-card__form">
        <div>
          <p class="page-kicker">Create account</p>
          <h2>普通用户注册</h2>
        </div>
        <label class="md-field">
          <span>用户名</span>
          <input
            v-model="form.username"
            autocomplete="username"
            maxlength="64"
            minlength="3"
            name="username"
          />
        </label>
        <label class="md-field">
          <span>密码</span>
          <input
            v-model="form.password"
            autocomplete="new-password"
            maxlength="72"
            minlength="6"
            name="password"
            type="password"
          />
        </label>
        <button class="button button-filled" :disabled="isSubmitting || isRedirecting" type="submit">
          <LoaderCircle v-if="isSubmitting" class="spin" :size="16" />
          {{ isSubmitting ? '创建中...' : isRedirecting ? '正在前往登录页' : '创建账号' }}
        </button>
        <RouterLink class="auth-switch" :to="loginRoute">已有账号，去登录</RouterLink>
        <p v-if="message" class="form-message" :class="`form-message--${messageType}`">{{ message }}</p>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
// 导入所需的 Composition API 和 Vue 依赖
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, UserPlus } from '@lucide/vue'

import { registerUser } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { normalizeAuthRedirect } from '@/shared/authRedirect'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { useSiteIdentity } from '@/shared/siteIdentity'

// 声明读者账号注册表单和处理状态变量
const root = ref<HTMLElement | null>(null)
const route = useRoute()
const router = useRouter()
const { siteName } = useSiteIdentity({ load: false })
const form = reactive({
  username: '',
  password: '',
})
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
const loginRoute = computed(() => ({
  name: 'login',
  query: {
    redirect: readRegisterRedirectPath(),
  },
}))

usePageReveal(root)
onBeforeUnmount(clearRedirectTimer)
watch([() => form.username, () => form.password], () => {
  if (isRedirecting.value) {
    return
  }
  registerAttemptId.value += 1
  if (!isSubmitting.value) {
    clearMessage()
  }
})

// 提交读者注册表单, 校验通过后向后端创建新用户, 注册成功后自动延时跳转登录页
// 提交读者注册表单, 校验通过后向后端创建新用户, 注册成功后自动延时跳转登录页
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

  isSubmitting.value = true
  clearMessage()
  const attemptId = ++registerAttemptId.value
  try {
    const user = await registerUser({
      username,
      password: form.password,
    })
    if (!isCurrentRegisterAttempt(attemptId, username)) {
      return
    }
    isRedirecting.value = true
    form.password = ''
    setMessage(`账号 ${user.username} 创建成功，正在前往登录页完成登录。`, 'success')
    redirectTimer = window.setTimeout(() => {
      router.push(loginRoute.value)
    }, 900)
  } catch (error) {
    if (isCurrentRegisterAttempt(attemptId, username)) {
      setMessage(toUserMessage(error, '注册失败，请稍后重试'), 'error')
    }
  } finally {
    isSubmitting.value = false
  }
}

// 对注册表单输入进行前台基础长度与合法性限制校验
// 对注册表单输入进行前台基础长度与合法性限制校验
function validateRegisterForm(username: string, password: string): string {
  if (!username) {
    return '请输入用户名'
  }
  if (username.length < USERNAME_MIN_LENGTH || username.length > USERNAME_MAX_LENGTH) {
    return `用户名长度必须是 ${USERNAME_MIN_LENGTH} 到 ${USERNAME_MAX_LENGTH} 个字符`
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

.auth-card--wide {
  display: grid;
  grid-template-columns: 1fr 0.9fr;
  overflow: hidden;
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

.auth-mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 48px;
  overflow: hidden;
  border: 1px solid var(--md-sys-color-outline);
  border-radius: 999px;
  background: var(--md-sys-color-surface-container-lowest);
}

.auth-mode-switch button {
  min-height: 46px;
  border: 0;
  background: transparent;
  color: var(--md-sys-color-on-surface-variant);
  font: inherit;
  font-weight: 760;
  cursor: pointer;
}

.auth-mode-switch button.is-active {
  background: var(--md-sys-color-secondary-container);
  color: #00201c;
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
    linear-gradient(160deg, rgba(216, 226, 255, 0.9), rgba(156, 242, 228, 0.62)),
    var(--md-sys-color-primary-container);
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

.auth-card__visual--warm {
  background:
    linear-gradient(160deg, rgba(255, 221, 176, 0.96), rgba(216, 226, 255, 0.72)),
    var(--md-sys-color-tertiary-container);
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

.material-benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.material-benefits span {
  min-height: 32px;
  padding: 7px 12px;
  border: 1px solid color-mix(in srgb, currentColor 22%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--md-sys-color-surface) 54%, transparent);
  font-size: 12px;
  font-weight: 780;
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
  .auth-card--wide,
  .auth-card--material {
    grid-template-columns: 1fr;
  }

  .auth-card--material {
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
