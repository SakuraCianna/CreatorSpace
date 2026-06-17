import { defineComponent, h, reactive, ref } from 'vue'

import { registerUser } from '@/api/content'

export default defineComponent({
  name: 'RegisterView',
  // 准备注册页表单状态和提交交互。
  setup() {
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

    return () =>
      h('section', { class: 'auth-page' }, [
        h(
          'form',
          {
            class: 'auth-card',
            onSubmit: (event: Event) => {
              event.preventDefault()
              submitRegister()
            },
          },
          [
            h('h1', '注册 CreatorSpace'),
            h('p', '注册默认开放，只需要用户名和密码。邮箱字段不会进入第一阶段。'),
            h('label', [
              '用户名',
              h('input', {
                autocomplete: 'username',
                name: 'username',
                value: form.username,
                onInput: (event: Event) => {
                  form.username = (event.target as HTMLInputElement).value
                },
              }),
            ]),
            h('label', [
              '密码',
              h('input', {
                autocomplete: 'new-password',
                name: 'password',
                type: 'password',
                value: form.password,
                onInput: (event: Event) => {
                  form.password = (event.target as HTMLInputElement).value
                },
              }),
            ]),
            h(
              'button',
              {
                class: 'button button-filled',
                disabled: isSubmitting.value,
                type: 'submit',
              },
              isSubmitting.value ? '创建中...' : '创建账号',
            ),
            message.value ? h('p', { class: 'form-message' }, message.value) : null,
          ],
        ),
      ])
  },
})
