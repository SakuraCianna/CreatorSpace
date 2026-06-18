<template>
  <section class="auth-page">
    <form class="auth-card" @submit.prevent="submitRegister">
      <h1>注册 CreatorSpace</h1>
      <label>
        用户名
        <input v-model="form.username" autocomplete="username" name="username" />
      </label>
      <label>
        密码
        <input v-model="form.password" autocomplete="new-password" name="password" type="password" />
      </label>
      <button class="button button-filled" :disabled="isSubmitting" type="submit">
        {{ isSubmitting ? '创建中...' : '创建账号' }}
      </button>
      <RouterLink class="auth-switch" to="/login">已有账号，去登录</RouterLink>
      <p v-if="message" class="form-message">{{ message }}</p>
    </form>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { registerUser } from '@/services/content'

const form = reactive({
  username: '',
  password: '',
})
const message = ref('')
const isSubmitting = ref(false)

// 提交注册表单。
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
    message.value = `账号 ${user.username} 创建成功`
    form.password = ''
  } catch (error) {
    message.value = error instanceof Error ? error.message : '注册失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}
</script>
