<template>
  <section class="auth-page">
    <form class="auth-card" @submit.prevent="submitLogin">
      <h1>登录 CreatorSpace</h1>
      <label>
        用户名
        <input v-model="form.username" autocomplete="username" name="username" />
      </label>
      <label>
        密码
        <input v-model="form.password" autocomplete="current-password" name="password" type="password" />
      </label>
      <button class="button button-filled" :disabled="isSubmitting" type="submit">
        {{ isSubmitting ? '登录中...' : '登录' }}
      </button>
      <RouterLink class="auth-switch" to="/register">没有账号，去注册</RouterLink>
      <p v-if="message" class="form-message">{{ message }}</p>
    </form>
  </section>
</template>

<script setup lang="ts">
/**
 * 登录页保留普通用户入口，等待普通用户登录接口接入。
 */
import { onBeforeUnmount, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

const form = reactive({
  username: '',
  password: '',
})
const message = ref('')
const isSubmitting = ref(false)
let loginTimer: number | undefined

// 提交登录表单。
function submitLogin() {
  if (!form.username.trim() || !form.password) {
    message.value = '请输入用户名和密码'
    return
  }

  isSubmitting.value = true
  loginTimer = window.setTimeout(() => {
    message.value = '普通用户登录将在下一阶段接入'
    form.password = ''
    isSubmitting.value = false
    loginTimer = undefined
  }, 240)
}

onBeforeUnmount(() => {
  if (loginTimer !== undefined) {
    window.clearTimeout(loginTimer)
  }
})
</script>
