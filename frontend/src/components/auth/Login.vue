<template>
  <div class="login-container">
    <a-card class="login-card" title="登录 - 导师推荐系统">
      <a-form
        :model="formState"
        :rules="rules"
        @finish="handleLogin"
        layout="vertical"
      >
        <a-form-item label="用户名" name="username">
          <a-input
            v-model:value="formState.username"
            placeholder="请输入用户名"
            size="large"
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="formState.password"
            placeholder="请输入密码"
            size="large"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            :loading="loading"
            size="large"
            block
          >
            登录
          </a-button>
        </a-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, reactive, ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'

export default defineComponent({
  name: 'Login',
  components: {
    UserOutlined,
    LockOutlined
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)

    const formState = reactive({
      username: '',
      password: ''
    })

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码至少6个字符', trigger: 'blur' }
      ]
    }

    const handleLogin = async () => {
      loading.value = true
      try {
        await store.dispatch('auth/login', {
          username: formState.username,
          password: formState.password
        })
        message.success('登录成功')
        router.push('/mentors')
      } catch (error) {
        message.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }

    return {
      formState,
      rules,
      loading,
      handleLogin
    }
  }
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.login-footer {
  text-align: center;
  margin-top: 16px;
}

.login-footer a {
  margin-left: 8px;
  color: #1890ff;
}
</style>
