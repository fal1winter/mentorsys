const state = {
  messages: {},
  unreadCounts: {},
  connected: false
}

const getters = {
  getMessagesByApplicationId: state => applicationId => {
    return state.messages[applicationId] || []
  },
  getUnreadCount: state => applicationId => {
    return state.unreadCounts[applicationId] || 0
  },
  isConnected: state => state.connected
}

const mutations = {
  ADD_MESSAGE(state, { applicationId, message }) {
    if (!state.messages[applicationId]) {
      state.messages[applicationId] = []
    }
    state.messages[applicationId].push(message)
  },
  SET_MESSAGES(state, { applicationId, messages }) {
    state.messages[applicationId] = messages
  },
  SET_UNREAD_COUNT(state, { applicationId, count }) {
    state.unreadCounts[applicationId] = count
  },
  SET_CONNECTED(state, connected) {
    state.connected = connected
  },
  CLEAR_MESSAGES(state, applicationId) {
    if (state.messages[applicationId]) {
      delete state.messages[applicationId]
    }
  }
}

const actions = {
  addMessage({ commit }, payload) {
    commit('ADD_MESSAGE', payload)
  },
  setMessages({ commit }, payload) {
    commit('SET_MESSAGES', payload)
  },
  setUnreadCount({ commit }, payload) {
    commit('SET_UNREAD_COUNT', payload)
  },
  setConnected({ commit }, connected) {
    commit('SET_CONNECTED', connected)
  },
  clearMessages({ commit }, applicationId) {
    commit('CLEAR_MESSAGES', applicationId)
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
