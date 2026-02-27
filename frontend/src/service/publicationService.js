import axios from './axios'

export default {
  // 创建学术成果
  createPublication(publicationData) {
    return axios.post('/publications', publicationData)
  },

  // 更新学术成果
  updatePublication(id, publicationData) {
    return axios.put(`/publications/${id}`, publicationData)
  },

  // 删除学术成果
  deletePublication(id) {
    return axios.delete(`/publications/${id}`)
  },

  // 获取单个学术成果
  getPublicationById(id) {
    return axios.get(`/publications/${id}`)
  },

  // 获取导师的学术成果列表
  getPublicationsByMentor(mentorId, params) {
    return axios.get(`/publications/mentor/${mentorId}`, { params })
  },

  // 搜索学术成果
  searchPublications(params) {
    return axios.get('/publications/search', { params })
  },

  // 按年份获取学术成果
  getPublicationsByYear(year, params) {
    return axios.get(`/publications/year/${year}`, { params })
  },

  // 为论文添加作者
  addAuthorToPublication(publicationId, data) {
    return axios.post(`/publications/${publicationId}/authors`, data)
  },

  // 从论文中移除作者
  removeAuthorFromPublication(publicationId, scholarId) {
    return axios.delete(`/publications/${publicationId}/authors/${scholarId}`)
  },

  // 更新论文作者关系
  updatePublicationAuthor(authorId, data) {
    return axios.put(`/publications/authors/${authorId}`, data)
  },

  // 获取论文的作者列表
  getAuthorsForPublication(publicationId) {
    return axios.get(`/publications/${publicationId}/authors`)
  }
}
