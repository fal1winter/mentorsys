import axios from './axios'

export default {
  createRating(ratingData) {
    return axios.post('/ratings', ratingData)
  },

  updateRating(ratingId, ratingData) {
    return axios.put(`/ratings/${ratingId}`, ratingData)
  },

  deleteRating(ratingId) {
    return axios.delete(`/ratings/${ratingId}`)
  },

  getRatingsByMentor(mentorId, params) {
    return axios.get(`/ratings/mentor/${mentorId}`, { params })
  },

  getRatingsByStudent(studentId) {
    return axios.get(`/ratings/student/${studentId}`)
  },

  markRatingHelpful(ratingId) {
    return axios.post(`/ratings/${ratingId}/helpful`)
  }
}
