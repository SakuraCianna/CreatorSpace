<template>
  <!-- 读者与管理员安全登录页面 -->
  <!-- 用户与管理员登录页 -->
  <section ref="root" class="auth-page auth-page--material">
    <form class="auth-card auth-card--material" data-reveal @submit.prevent="submitLogin">
      <div class="auth-card__visual auth-card__visual--material">
        <span class="material-icon-badge">
          <ShieldCheck :size="24" />
        </span>
        <p class="page-kicker">{{ accountKicker }}</p>
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
          <h2>{{ loginMode === 'ADMIN' ? '管理员登录' : '普通用户登录' }}</h2>
        </div>
        <div class="auth-mode-switch">
          <button type="button" :class="{ 'is-active': loginMode === 'USER' }"
            @click="loginMode = 'USER'">普通用户登录</button>
          <button type="button" :class="{ 'is-active': loginMode === 'ADMIN' }"
            @click="loginMode = 'ADMIN'">管理员登录</button>
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
        <RouterLink class="auth-switch" :to="registerRoute">没有账号，先注册普通用户</RouterLink>
        <p v-if="message" class="form-message">{{ message }}</p>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
// 导入所需的组件和 Vue 钩子
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, ShieldCheck } from '@lucide/vue'

import { loginAdmin, loginUser } from '@/services/content'
import { HttpError, toUserMessage } from '@/services/http'
import { isAdminRedirect, normalizeAuthRedirect } from '@/shared/authRedirect'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { useSessionStore } from '@/shared/sessionStore'
import { siteAccountLabel, useSiteIdentity } from '@/shared/siteIdentity'

// 声明登录页面的状态数据与控制器
const root = ref<HTMLElement | null>(null)
const router = useRouter()
const route = useRoute()
const session = useSessionStore()
const { identity } = useSiteIdentity({ load: false })
const form = reactive({
  username: '',
  password: '',
})
const message = ref('')
const isSubmitting = ref(false)
const loginMode = ref<'ADMIN' | 'USER'>(route.query.mode === 'admin' || isAdminRedirect(route.query.redirect) ? 'ADMIN' : 'USER')
const loginAttemptId = ref(0)
const accountKicker = computed(() => siteAccountLabel(identity.value))
const registerRoute = computed(() => ({
  name: 'register',
  query: {
    redirect: readPublicRedirectPath(),
  },
}))

usePageReveal(root)

watch([loginMode, () => form.username, () => form.password], () => {
  loginAttemptId.value += 1
  message.value = ''
})

// 提交用户登录表单, 支持普通用户和管理员身份切换, 登录成功后写入 Token 到本地并跳转回原路由
async function submitLogin() {
  if (!form.username.trim() || !form.password) {
    message.value = '请输入用户名和密码'
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
    }
    const token = modeSnapshot === 'ADMIN' ? await loginAdmin(payload) : await loginUser(payload)
    session.setSession(token.accessToken, token.user)
    form.password = ''
    router.push(modeSnapshot === 'ADMIN' ? readRedirectPath() : readPublicRedirectPath())
  } catch (error) {
    if (attemptId === loginAttemptId.value && modeSnapshot === loginMode.value && usernameSnapshot === form.username.trim()) {
      message.value = loginErrorMessage(error, modeSnapshot)
    }
  } finally {
    isSubmitting.value = false
  }
}

// 根据后端返回的 Http 状态码及错误信息生成友好的用户提示文案
function loginErrorMessage(error: unknown, mode: 'ADMIN' | 'USER') {
  if (!(error instanceof HttpError)) {
    return mode === 'ADMIN'
      ? '管理员登录失败：前端没有拿到明确错误，请检查浏览器控制台和后端日志。'
      : '登录失败：前端没有拿到明确错误，请检查浏览器控制台和后端日志。'
  }
  if (error.status === 0) {
    return `登录请求没有连上后端：${error.message}`
  }
  const backendText = describeBackendError(error)
  if (mode === 'ADMIN') {
    if (error.status === 401) {
      return `管理员登录失败：${backendText}。这是账号或密码校验失败，不是后台权限问题。`
    }
    if (error.status === 403) {
      return `管理员登录失败：${backendText}。这代表密码已通过，但账号被禁用、缺少 ADMIN 角色，或登录态和后台权限不匹配。`
    }
    return `管理员登录失败：${error.message}`
  }
  if (error.status === 401) {
    return `普通用户登录失败：${backendText}。请检查账号是否存在以及密码是否输入正确。`
  }
  if (error.status === 403) {
    return `普通用户登录失败：${backendText}。账号存在但当前不可用，可能已被禁用或锁定。`
  }
  return toUserMessage(error, '登录失败')
}

function describeBackendError(error: HttpError) {
  const backendMessage = error.backendMessage.trim()
  return backendMessage ? `后端返回：${backendMessage}` : `后端返回 ${error.status}`
}

// 读取前台路径重定向地址, 避免管理员重定向去前台时被拦截
function readPublicRedirectPath() {
  const redirect = normalizeAuthRedirect(route.query.redirect, '/articles')
  return redirect.startsWith('/admin') ? '/articles' : redirect
}

function readRedirectPath() {
  return normalizeAuthRedirect(route.query.redirect, '/admin')
}
</script>

<style scoped>
.auth-page {
  position: relative;
  display: grid;
  min-height: calc(100vh - 72px);
  place-items: center;
  padding: clamp(28px, 5vw, 72px) 0;
}

.auth-page::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 24%, rgba(49, 91, 255, 0.14), transparent 28vw),
    radial-gradient(circle at 82% 18%, rgba(0, 106, 96, 0.12), transparent 26vw),
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(247, 249, 255, 0.72));
}

.auth-card {
  position: relative;
  z-index: 1;
  width: min(960px, 100%);
  border: 1px solid rgba(116, 119, 127, 0.24);
  border-radius: 24px;
  background: var(--md-sys-color-surface-container-lowest);
  box-shadow: 0 28px 80px rgba(30, 38, 64, 0.18), 0 1px 0 rgba(255, 255, 255, 0.9) inset;
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
  min-height: 540px;
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
  gap: 20px;
  padding: clamp(30px, 4vw, 52px);
  background: rgba(255, 255, 255, 0.96);
}

.auth-mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  min-height: 48px;
  padding: 4px;
  border: 1px solid rgba(68, 71, 79, 0.22);
  border-radius: 999px;
  background: #eef2ff;
}

.auth-mode-switch button {
  min-height: 40px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #35415c;
  font: inherit;
  font-weight: 760;
  cursor: pointer;
  transition: background 180ms ease, color 180ms ease, box-shadow 180ms ease;
}

.auth-mode-switch button:hover {
  background: rgba(255, 255, 255, 0.78);
  color: #111827;
}

.auth-mode-switch button.is-active {
  background: #101827;
  color: #ffffff;
  box-shadow: 0 8px 24px rgba(16, 24, 39, 0.18);
}

.auth-mode-switch button:focus-visible {
  outline: 2px solid var(--md-sys-color-primary);
  outline-offset: 2px;
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
    radial-gradient(circle at 84% 82%, rgba(84, 230, 200, 0.26), transparent 34%),
    linear-gradient(150deg, #101827 0%, #173b87 52%, #087568 100%);
  color: #f8fbff;
}

.auth-card__visual--material::before {
  content: "";
  position: absolute;
  right: -90px;
  bottom: -120px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: rgba(147, 197, 253, 0.24);
  box-shadow: -80px -60px 120px rgba(84, 230, 200, 0.16);
}

.auth-card__visual--warm {
  background:
    linear-gradient(160deg, rgba(255, 221, 176, 0.96), rgba(216, 226, 255, 0.72)),
    var(--md-sys-color-tertiary-container);
}

.auth-card__visual--material>* {
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
  color: #151922;
  font-size: 26px;
  line-height: 1.16;
}

.auth-card label {
  display: grid;
  gap: 8px;
  color: #353d4f;
  font-size: 13px;
  font-weight: 750;
}

.auth-card input {
  width: 100%;
  min-height: 56px;
  border: 1px solid rgba(116, 119, 127, 0.24);
  border-bottom-color: rgba(68, 71, 79, 0.38);
  border-radius: 14px;
  padding: 0 14px;
  background: #f8fafc;
  color: #111827;
  caret-color: var(--md-sys-color-primary);
  outline: 0;
  transition: background 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.auth-card input:focus {
  border-color: color-mix(in srgb, var(--md-sys-color-primary) 56%, #ffffff);
  background: #ffffff;
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--md-sys-color-primary) 12%, transparent);
}

.auth-card button {
  width: 100%;
}

.auth-card .button-filled {
  min-height: 52px;
  color: #ffffff;
  background: linear-gradient(135deg, #315bff, #174ea6);
  box-shadow: 0 16px 32px rgba(49, 91, 255, 0.24);
}

.auth-card .button-filled:hover {
  color: #ffffff;
  background: linear-gradient(135deg, #254be8, #123f92);
}

.auth-switch {
  display: inline-flex;
  justify-content: center;
  color: #174ea6;
  font-size: 14px;
  font-weight: 760;
}

.auth-switch:hover {
  color: #0b57d0;
  text-decoration: underline;
  text-underline-offset: 4px;
}

.material-icon-badge {
  display: inline-grid;
  width: 56px;
  height: 56px;
  place-items: center;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
  color: #bff7ec;
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.auth-card__visual--material .page-kicker {
  color: #9ff5e5;
}

.auth-card__visual--material p:not(.page-kicker) {
  max-width: 34ch;
  color: rgba(248, 251, 255, 0.82);
  line-height: 1.72;
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
  background: rgba(255, 255, 255, 0.13);
  color: #eff6ff;
  font-size: 12px;
  font-weight: 780;
}

.form-message {
  margin: 0;
  color: var(--tone-coral);
  font-size: 14px;
  line-height: 1.55;
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
