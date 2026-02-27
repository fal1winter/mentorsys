import studentService from '../../service/studentService'

const state = {
  profile: null,
  loading: false
}

const getters = {
  profile: state => state.profile,
  isLoading: state => state.loading
}

const mutations = {
  SET_PROFILE(state, profile) {
    state.profile = profile
  },
  SET_LOADING(state, loading) {
    state.loading = loading
  }
}

const actions = {
  async fetchProfile({ commit }, userId) {
    commit('SET_LOADING', true)
    try {
      const response = await studentService.getProfile(userId)
      if (response.code === 0) {
        commit('SET_PROFILE', response.data)
      }
      return response
    } finally {
      commit('SET_LOADING', false)
    }
  },

  async updateProfile({ commit }, profileData) {
    commit('SET_LOADING', true)
    try {
      const response = await studentService.updateProfile(profileData)
      if (response.code === 0) {
        commit('SET_PROFILE', response.data)
      }
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
