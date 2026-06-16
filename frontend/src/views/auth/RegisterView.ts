import { defineComponent, h, reactive, ref } from 'vue'

export default defineComponent({
  name: 'RegisterView',
  setup() {
    const form = reactive({
      username: '',
      password: '',
    })
    const message = ref('')

    function submitRegister() {
      if (!form.username.trim() || form.password.length < 6) {
        message.value = '请输入用户名，并保证密码至少 6 位'
        return
      }

      message.value = '注册接口待接入：当前表单只收集用户名和密码'
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
            h('button', { class: 'button button-filled', type: 'submit' }, '创建账号'),
            message.value ? h('p', { class: 'form-message' }, message.value) : null,
          ],
        ),
      ])
  },
})
