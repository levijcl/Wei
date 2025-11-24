import { createRouter, createWebHashHistory } from 'vue-router'
import InventoryAdjustmentView from '@/src/features/inventory/InventoryAdjustmentView.vue'
import InventoryDiscrepancyView from '@/src/features/inventory/InventoryDiscrepancyView.vue'

const routes = [
  { path: '/', component: InventoryAdjustmentView },
  { path: '/adjustments/:id/discrepancy', component: InventoryDiscrepancyView },
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
