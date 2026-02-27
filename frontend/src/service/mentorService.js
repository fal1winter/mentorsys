import axios from './axios'

export default {
  getMentors(params) {
    return axios.get('/mentors', { params })
  },

  getMentorById(id) {
    return axios.get(`/mentors/${id}`)
  },

  getMentorByUserId(userId) {
    return axios.get(`/mentors/user/${userId}`)
  },

  searchMentors(params) {
    return axios.get('/mentors/search', { params })
  },

  createMentor(mentorData) {
    return axios.post('/mentors', mentorData)
  },

  updateMentor(id, mentorData) {
    return axios.put(`/mentors/${id}`, mentorData)
  },

  deleteMentor(id) {
    return axios.delete(`/mentors/${id}`)
  },

  getPublications(mentorId) {
    return axios.get(`/publications/mentor/${mentorId}`)
  },

  getRatings(mentorId, params) {
    return axios.get(`/ratings/mentor/${mentorId}`, { params })
  }
}
