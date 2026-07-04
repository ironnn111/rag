import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'rag-qa',
      component: () => import('@/views/RagQAView.vue'),
    },
    {
      path: '/documents',
      name: 'documents',
      component: () => import('@/views/DocumentManageView.vue'),
    },
    {
      path: '/eval',
      name: 'eval',
      component: () => import('@/views/EvalView.vue'),
    },
  ],
})

export default router
