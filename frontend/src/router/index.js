import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'
import MainLayout from '../components/layout/MainLayout.vue'

const routes = [
  // 登录和注册页面（不使用MainLayout）
  {
    path: '/login',
    name: 'Login',
    component: () => import('../components/auth/Login.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../components/auth/Register.vue'),
    meta: { requiresGuest: true }
  },
  // 主布局路由（包含侧边栏）
  {
    path: '/',
    component: MainLayout,
    redirect: '/mentors',
    children: [
      {
        path: 'mentors',
        name: 'MentorList',
        component: () => import('../components/mentor/MentorList.vue')
      },
      {
        path: 'mentors/:id',
        name: 'MentorDetail',
        component: () => import('../components/mentor/MentorDetail.vue')
      },
      {
        path: 'students',
        name: 'StudentList',
        component: () => import('../components/student/StudentList.vue')
      },
      {
        path: 'students/:id',
        name: 'StudentDetail',
        component: () => import('../components/student/StudentDetail.vue')
      },
      {
        path: 'recommendations',
        name: 'Recommendations',
        component: () => import('../components/recommendation/RecommendedMentors.vue'),
        meta: { requiresAuth: true, role: 'STUDENT' }
      },
      {
        path: 'recommendations/students',
        name: 'StudentRecommendations',
        component: () => import('../components/recommendation/RecommendedStudents.vue'),
        meta: { requiresAuth: true, role: 'MENTOR' }
      },
      {
        path: 'student/profile',
        name: 'StudentProfile',
        component: () => import('../components/student/StudentProfile.vue'),
        meta: { requiresAuth: true, role: 'STUDENT' }
      },
      {
        path: 'student/dashboard',
        name: 'StudentDashboard',
        component: () => import('../components/student/StudentDashboard.vue'),
        meta: { requiresAuth: true, role: 'STUDENT' }
      },
      {
        path: 'mentor/students',
        name: 'MyStudents',
        component: () => import('../components/mentor/MyStudents.vue'),
        meta: { requiresAuth: true, role: 'MENTOR' }
      },
      {
        path: 'applications',
        name: 'ApplicationList',
        component: () => import('../components/application/ApplicationList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'chat/:applicationId',
        name: 'ChatRoom',
        component: () => import('../components/chat/ChatRoom.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'chat/direct/:studentId/:mentorId',
        name: 'DirectChat',
        component: () => import('../components/chat/DirectChat.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'chats',
        name: 'ChatList',
        component: () => import('../components/chat/ChatList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../components/UserProfile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('../components/admin/AdminDashboard.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/users',
        name: 'UserManagement',
        component: () => import('../components/admin/UserManagement.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/mentors',
        name: 'MentorManagement',
        component: () => import('../components/admin/MentorManagement.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/keywords',
        name: 'KeywordManagement',
        component: () => import('../components/admin/KeywordManagement.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/publications',
        name: 'PublicationManagement',
        component: () => import('../components/admin/PublicationManagement.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/ratings',
        name: 'RatingManagement',
        component: () => import('../components/admin/RatingManagement.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/batch-import',
        name: 'BatchImport',
        component: () => import('../components/admin/BatchImport.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// Navigation guards
router.beforeEach((to, from, next) => {
  const isAuthenticated = store.getters['auth/isAuthenticated']
  const userRole = store.getters['auth/userRole']

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.meta.requiresGuest && isAuthenticated) {
    next('/mentors')
  } else if (to.meta.role && userRole !== to.meta.role) {
    // 允许ADMIN访问所有页面
    if (userRole === 'ADMIN') {
      next()
    } else {
      next('/mentors')
    }
  } else {
    next()
  }
})

export default router
