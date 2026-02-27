import axios from './axios'

export default {
  getKeywords(params) {
    return axios.get('/keywords', { params })
  },

  getKeywordById(id) {
    return axios.get(`/keywords/${id}`)
  },

  getAllKeywords() {
    return axios.get('/keywords/all')
  },

  searchKeywords(params) {
    return axios.get('/keywords/search', { params })
  },

  getKeywordsByCategory(category) {
    return axios.get(`/keywords/category/${category}`)
  },

  createKeyword(keywordData) {
    return axios.post('/keywords', keywordData)
  },

  updateKeyword(id, keywordData) {
    return axios.put(`/keywords/${id}`, keywordData)
  },

  deleteKeyword(id) {
    return axios.delete(`/keywords/${id}`)
  }
}
