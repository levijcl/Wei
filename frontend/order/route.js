import { createRouter, createWebHashHistory } from 'vue-router'

import OrderDashboardView from '@/src/features/order/OrderDashboardView.vue'
import OrderDetailView from '@/src/features/order/OrderDetailView.vue'
import OrderListView from '@/src/features/order/OrderListView.vue'

const routes = [
  { path: '/', component: OrderDashboardView },
  {
    path: '/orders',
    component: OrderListView,
  },
  { path: '/orders/:id', component: OrderDetailView },
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
