import { createRouter, createWebHashHistory } from 'vue-router'
import InventoryAdjustmentView from '@/src/features/inventory/InventoryAdjustmentView.vue'

const routes = [{ path: '/', component: InventoryAdjustmentView }]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
