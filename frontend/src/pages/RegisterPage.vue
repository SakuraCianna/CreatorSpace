<template>
  <section ref="root" class="auth-page">
    <form class="auth-card auth-card--wide" data-reveal @submit.prevent="submitRegister">
      <div class="auth-card__visual auth-card__visual--warm">
        <UserPlus :size="28" />
        <p class="page-kicker">Join as Reader</p>
        <h1>注册读者身份</h1>
        <p>普通用户可以为后续评论、点赞、收藏和好友可见内容预留身份。第一阶段只需要用户名和密码。</p>
      </div>
      <div class="auth-card__form">
        <label>
          用户名
          <input v-model="form.username" autocomplete="username" name="username" />
        </label>
        <label>
          密码
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
