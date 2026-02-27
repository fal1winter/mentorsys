import axios from './axios'

export default {
  getProfile(userId) {
    return axios.get(`/students/${userId}`)
  },

  // 获取学生列表（支持筛选）
  getStudentList(params) {
    return axios.get('/students', { params })
  },

  getStudentByUserId(userId) {
    return axios.get(`/students/user/${userId}`)
  },

  updateProfile(profileData) {
    return axios.put(`/students/${profileData.id}`, profileData)
  },

  // 更新学生信息（包括偏好）
  updateStudent(studentId, updateData) {
    return axios.put(`/students/${studentId}`, updateData)
  },

  createProfile(profileData) {
    return axios.post('/students', profileData)
  },

  getStudentsByMentor(mentorId, page = 1, limit = 20) {
    return axios.get(`/students/mentor/${mentorId}`, {
      params: { page, limit }
    })
  }
}
