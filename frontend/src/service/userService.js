import axios from './axios'

export default {
  // Get current user's profile
  getCurrentUserProfile() {
    return axios.get('/users/profile')
  },

  // Get user profile by ID
  getUserProfile(userId) {
    return axios.get(`/users/${userId}`)
  },

  // Update current user's own profile
  updateOwnProfile(userData) {
    return axios.put('/users/profile', userData)
  },

  // Update any user's profile (admin only)
  updateUserProfileByAdmin(userId, userData) {
    return axios.put(`/users/${userId}`, userData)
  },

  // Get all users with pagination (admin only)
  getAllUsers(params) {
    return axios.get('/users', { params })
  },

  // Get complete user profile with role-specific data
  getCompleteUserProfile() {
    return axios.get('/users/profile/complete')
  },

  // Update complete user profile with role-specific data
  updateCompleteUserProfile(profileData) {
    return axios.put('/users/profile/complete', profileData)
  },

  // 下载导师导入模板
  downloadMentorTemplate() {
    return axios.get('/admin/batch/template/mentor', { responseType: 'blob' })
  },

  // 下载学生导入模板
  downloadStudentTemplate() {
    return axios.get('/admin/batch/template/student', { responseType: 'blob' })
  },

  // 批量导入导师
  importMentors(file) {
    const formData = new FormData()
    formData.append('file', file)
    return axios.post('/admin/batch/import/mentor', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },

  // 批量导入学生
  importStudents(file) {
    const formData = new FormData()
    formData.append('file', file)
    return axios.post('/admin/batch/import/student', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  }
}
