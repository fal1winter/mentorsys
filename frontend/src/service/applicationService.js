import axios from './axios'

export default {
  getApplications(params) {
    return axios.get('/applications', { params })
  },

  getApplicationById(id) {
    return axios.get(`/applications/${id}`)
  },

  createApplication(applicationData) {
    return axios.post('/applications', applicationData)
  },

  acceptApplication(id, feedback) {
    return axios.post(`/applications/${id}/accept`, { mentorFeedback: feedback })
  },

  rejectApplication(id, feedback) {
    return axios.post(`/applications/${id}/reject`, { mentorFeedback: feedback })
  },

  withdrawApplication(id) {
    return axios.delete(`/applications/${id}`)
  }
}
