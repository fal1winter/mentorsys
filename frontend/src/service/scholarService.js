import axios from './axios'

export default {
  /**
   * Search scholars
   * 搜索学者
   */
  searchScholars(params) {
    return axios.get('/api/scholars/search', { params })
  },

  /**
   * Get scholar by ID
   * 根据ID获取学者
   */
  getScholarById(id) {
    return axios.get(`/api/scholars/${id}`)
  },

  /**
   * Create scholar
   * 创建学者
   */
  createScholar(data) {
    return axios.post('/api/scholars', data)
  },

  /**
   * Update scholar
   * 更新学者
   */
  updateScholar(id, data) {
    return axios.put(`/api/scholars/${id}`, data)
  },

  /**
   * Delete scholar
   * 删除学者
   */
  deleteScholar(id) {
    return axios.delete(`/api/scholars/${id}`)
  },

  /**
   * Get scholars by publication ID
   * 根据论文ID获取学者列表
   */
  getScholarsByPublicationId(publicationId) {
    return axios.get(`/api/scholars/publication/${publicationId}`)
  }
}
