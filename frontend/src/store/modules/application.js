import applicationService from '../../service/applicationService'

const state = {
  applications: [],
  currentApplication: null,
  total: 0,
  loading: false
}

const getters = {
  applications: state => state.applications,
  currentApplication: state => state.currentApplication,
  total: state => state.total,
  isLoading: state => state.loading
}

const mutations = {
  SET_APPLICATIONS(state, applications) {
    state.applications = applications
  },
  SET_CURRENT_APPLICATION(state, application) {
    state.currentApplication = application
  },
  SET_TOTAL(state, total) {
    state.total = total
  },
  SET_LOADING(state, loading) {
    state.loading = loading
  }
}

const actions = {
  async fetchApplications({ commit }, params) {
    commit('SET_LOADING', true)
    try {
      const response = await applicationService.getApplications(params)
      if (response.code === 0) {
        commit('SET_APPLICATIONS', response.data)
        commit('SET_TOTAL', response.total)
      }
      return response
    } finally {
      commit('SET_LOADING', false)
    }
  },

  async createApplication({ commit }, applicationData) {
    commit('SET_LOADING', true)
    try {
      const response = await applicationService.createApplication(applicationData)
      return response
    } finally {
      commit('SET_LOADING', false)
    }
  },

  async acceptApplication({ commit }, { id, feedback }) {
    commit('SET_LOADING', true)
    try {
      const response = await applicationService.acceptApplication(id, feedback)
      return response
    } finally {
      commit('SET_LOADING', false)
    }
  },

  async rejectApplication({ commit }, { id, feedback }) {
    commit('SET_LOADING', true)
    try {
      const response = await applicationService.rejectApplication(id, feedback)
      return response
    } finally {
      commit('SET_LOADING', false)
    }
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
