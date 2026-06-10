import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/chat',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/student/Chat.vue'),
        meta: { requiresAuth: true, role: 'STUDENT' }
      },
      {
        path: 'my-stats',
        name: 'MyStats',
        component: () => import('@/views/student/MyStats.vue'),
        meta: { requiresAuth: true, role: 'STUDENT' }
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/AdminLayout.vue'),
        redirect: '/admin/students',
        meta: { requiresAuth: true, role: 'TEACHER' },
        children: [
          {
            path: 'students',
            name: 'StudentManage',
            component: () => import('@/views/admin/StudentManage.vue'),
            meta: { requiresAuth: true, role: 'TEACHER' }
          },
          {
            path: 'statistics',
            name: 'QAStatistics',
            component: () => import('@/views/admin/QAStatistics.vue'),
            meta: { requiresAuth: true, role: 'TEACHER' }
          },
          {
            path: 'knowledge',
            name: 'KnowledgeManage',
            component: () => import('@/views/admin/KnowledgeManage.vue'),
            meta: { requiresAuth: true, role: 'TEACHER' }
          },
          {
            path: 'videos',
            name: 'VideoManage',
            component: () => import('@/views/admin/VideoManage.vue'),
            meta: { requiresAuth: true, role: 'TEACHER' }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.role && to.meta.role !== userStore.role) {
    if (userStore.role === 'STUDENT') {
      next('/chat')
    } else if (userStore.role === 'TEACHER') {
      next('/admin/students')
    } else {
      next('/login')
    }
  } else {
    next()
  }
})

export default router
