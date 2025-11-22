import { createRouter, createWebHashHistory } from 'vue-router'

import OrderDashboardView from '@/src/features/order/OrderDashboardView.vue'
import OrderDetailView from '@/src/features/order/OrderDetailView.vue'
import OrderListView from '@/src/features/order/OrderListView.vue'
import OrderStepView from '@/src/features/order/OrderStepView.vue'

const routes = [
  { path: '/', component: OrderDashboardView },
  {
    path: '/orders',
    component: OrderListView,
  },
  { path: '/orders/:id', component: OrderDetailView },
  { path: '/orders/:id/process-status/step/:step', component: OrderStepView },
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
