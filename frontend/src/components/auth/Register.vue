<template>
  <div class="register-container">
    <a-card class="register-card" title="注册 - 导师推荐系统">
      <a-form
        :model="formState"
        :rules="rules"
        @finish="handleRegister"
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

        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱"
            size="large"
          >
            <template #prefix>
              <MailOutlined />
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

        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password
            v-model:value="formState.confirmPassword"
            placeholder="请再次输入密码"
            size="large"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item label="用户类型" name="userType">
          <a-radio-group v-model:value="formState.userType" size="large">
            <a-radio value="STUDENT">学生</a-radio>
            <a-radio value="MENTOR">导师</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="身份码（可选）" name="accessCode">
          <a-input
            v-model:value="formState.accessCode"
            placeholder="输入身份码可获得特殊权限（管理员码：123456）"
            size="large"
          >
            <template #prefix>
              <KeyOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            :loading="loading"
            size="large"
            block
          >
            注册
          </a-button>
        </a-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
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
import { UserOutlined, LockOutlined, MailOutlined, KeyOutlined } from '@ant-design/icons-vue'

export default defineComponent({
  name: 'Register',
  components: {
    UserOutlined,
    LockOutlined,
    MailOutlined,
    KeyOutlined
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)

    const formState = reactive({
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      userType: 'STUDENT',
      accessCode: ''
    })

    const validatePassword = async (rule, value) => {
      if (value !== formState.password) {
        return Promise.reject('两次输入的密码不一致')
      }
      return Promise.resolve()
    }

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码至少6个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { validator: validatePassword, trigger: 'blur' }
      ],
      userType: [
        { required: true, message: '请选择用户类型', trigger: 'change' }
      ]
    }

    const handleRegister = async () => {
      loading.value = true
      try {
        await store.dispatch('auth/register', {
          username: formState.username,
          email: formState.email,
          password: formState.password,
          userType: formState.userType,
          accessCode: formState.accessCode
        })
        message.success('注册成功')
        router.push('/mentors')
      } catch (error) {
        message.error(error.message || '注册失败')
      } finally {
        loading.value = false
      }
    }

    return {
      formState,
      rules,
      loading,
      handleRegister
    }
  }
})
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 450px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.register-footer {
  text-align: center;
  margin-top: 16px;
}

.register-footer a {
  margin-left: 8px;
  color: #1890ff;
}
</style>
