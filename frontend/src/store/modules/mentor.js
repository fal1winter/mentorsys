import mentorService from '../../service/mentorService'

const state = {
  mentors: [],
  currentMentor: null,
  total: 0,
  loading: false
}

const getters = {
  mentorList: state => state.mentors,
  currentMentor: state => state.currentMentor,
  total: state => state.total,
  isLoading: state => state.loading
}

const mutations = {
  SET_MENTORS(state, mentors) {
    state.mentors = mentors
  },
  SET_CURRENT_MENTOR(state, mentor) {
    state.currentMentor = mentor
  },
  SET_TOTAL(state, total) {
    state.total = total
  },
  SET_LOADING(state, loading) {
    state.loading = loading
  }
}

const actions = {
  async fetchMentors({ commit }, params) {
    commit('SET_LOADING', true)
    try {
      const response = await mentorService.getMentors(params)
      if (response.code === 0) {
        commit('SET_MENTORS', response.data)
        commit('SET_TOTAL', response.total)
      }
      return response
    } finally {
      commit('SET_LOADING', false)
    }
  },

  async fetchMentorById({ commit }, id) {
    commit('SET_LOADING', true)
    try {
      const response = await mentorService.getMentorById(id)
      if (response.code === 0) {
        commit('SET_CURRENT_MENTOR', response.data)
      }
      return response
    } finally {
      commit('SET_LOADING', false)
    }
  },

  async searchMentors({ commit }, params) {
    commit('SET_LOADING', true)
    try {
      const response = await mentorService.searchMentors(params)
      if (response.code === 0) {
        commit('SET_MENTORS', response.data)
        commit('SET_TOTAL', response.total)
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
