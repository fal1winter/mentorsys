import authService from '../../service/authService'

const state = {
  user: JSON.parse(localStorage.getItem('user')) || null
}

const getters = {
  isAuthenticated: state => !!state.user,
  currentUser: state => state.user,
  username: state => state.user?.username || null,
  userRole: state => state.user?.userType?.toUpperCase() || null
}

const mutations = {
  SET_USER(state, user) {
    state.user = user
    if (user) {
      localStorage.setItem('user', JSON.stringify(user))
    } else {
      localStorage.removeItem('user')
    }
  },
  LOGOUT(state) {
    state.user = null
    localStorage.removeItem('user')
  }
}

const actions = {
  async login({ commit }, credentials) {
    const response = await authService.login(credentials)
    if (response.code === 0) {
      commit('SET_USER', response.data)
      return response
    }
    throw new Error(response.message)
  },

  async register({ commit }, userData) {
    const response = await authService.register(userData)
    if (response.code === 0) {
      commit('SET_USER', response.data)
      return response
    }
    throw new Error(response.message)
  },

  async logout({ commit }) {
    await authService.logout()
    commit('LOGOUT')
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
