<template>
  <section ref="root" class="auth-page auth-page--material">
    <form class="auth-card auth-card--material" data-reveal @submit.prevent="submitRegister">
      <div class="auth-card__visual auth-card__visual--material auth-card__visual--warm">
        <span class="material-icon-badge">
          <UserPlus :size="24" />
        </span>
        <p class="page-kicker">Join as Reader</p>
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
          <input v-model="form.username" autocomplete="username" name="username" />
        </label>
        <label class="md-field">
          <span>密码</span>
          <input v-model="form.password" autocomplete="new-password" name="password" type="password" />
        </label>
        <button class="button button-filled" :disabled="isSubmitting" type="submit">
          <LoaderCircle v-if="isSubmitting" class="spin" :size="16" />
          {{ isSubmitting ? '创建中...' : '创建账号' }}
        </button>
        <RouterLink class="auth-switch" to="/login">已有管理员账号，去登录</RouterLink>
        <p v-if="message" class="form-message">{{ message }}</p>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { LoaderCircle, UserPlus } from '@lucide/vue'

import { registerUser } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'

const root = ref<HTMLElement | null>(null)
const form = reactive({
  username: '',
  password: '',
})
const message = ref('')
const isSubmitting = ref(false)

usePageReveal(root)

async function submitRegister() {
  if (!form.username.trim() || form.password.length < 6) {
    message.value = '请输入用户名，并保证密码至少 6 位'
    return
  }

  isSubmitting.value = true
  message.value = ''
  try {
    const user = await registerUser({
      username: form.username.trim(),
      password: form.password,
    })
    message.value = `账号 ${user.username} 创建成功，可以等待后续普通登录入口接入。`
    form.password = ''
  } catch (error) {
    message.value = error instanceof Error ? error.message : '注册失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
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
