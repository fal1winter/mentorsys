import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true
})

// Request interceptor
instance.interceptors.request.use(
  config => {
    // Cookie-based authentication - no need to add Authorization header
    // Cookies are automatically sent with withCredentials: true
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
instance.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message.error('未授权，请重新登录')
          // Clear user data from store
          localStorage.removeItem('user')
          router.push('/login')
          break
        case 403:
          message.error('拒绝访问')
          break
        case 404:
          message.error('请求的资源不存在')
          break
        case 500:
          message.error('服务器错误')
          break
        default:
          message.error(error.response.data.message || '请求失败')
      }
    } else {
      message.error('网络错误，请检查您的网络连接')
    }
    return Promise.reject(error)
  }
)

export default instance
