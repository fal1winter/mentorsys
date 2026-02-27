import { createStore } from 'vuex'
import auth from './modules/auth'
import mentor from './modules/mentor'
import student from './modules/student'
import application from './modules/application'
import chat from './modules/chat'

export default createStore({
  modules: {
    auth,
    mentor,
    student,
    application,
    chat
  }
})
