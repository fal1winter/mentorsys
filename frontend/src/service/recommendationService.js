import axios from './axios'

export default {
  // 学生获取导师推荐（使用语义检索）
  getMentorRecommendations(studentId, limit = 10, useSemantic = true) {
    return axios.get('/recommendations/mentors', { 
      params: { studentId, limit, useSemantic } 
    })
  },

  // 导师获取学生推荐（使用语义检索）
  getStudentRecommendations(mentorId, limit = 10, useSemantic = true) {
    return axios.get('/recommendations/students', { 
      params: { mentorId, limit, useSemantic } 
    })
  },

  // 获取用户偏好
  getUserPreferences(userId) {
    return axios.get(`/recommendations/preferences/${userId}`)
  },

  // 获取完整用户档案（包含用户填写的偏好和系统分析的偏好）
  getUserProfile(userId) {
    return axios.get(`/recommendations/profile/${userId}`)
  },

  // 分析用户偏好
  analyzePreferences(userId) {
    return axios.post(`/recommendations/analyze/${userId}`)
  },

  // 记录用户行为
  trackBehavior(data) {
    return axios.post('/recommendations/track', data)
  },

  // 清除推荐缓存
  invalidateCache(studentId, mentorId) {
    return axios.post('/recommendations/invalidate-cache', null, {
      params: { studentId, mentorId }
    })
  },

  // 兼容旧接口
  getRecommendations(userId) {
    return this.getMentorRecommendations(userId)
  }
}
