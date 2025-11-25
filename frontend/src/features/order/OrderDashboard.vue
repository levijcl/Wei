<script setup>
import { getOrders } from '@/src/apis/order.js'
import { ref, computed } from 'vue'
import OrderDashboardItem from './OrderDashboardItem.vue'

const statuses = [
  'CREATED',
  'SCHEDULED',
  'AWAITING_FULFILLMENT',
  'PARTIALLY_RESERVED',
  'RESERVED',
  'PARTIALLY_COMMITTED',
  'FAILED_TO_RESERVE',
  'FAILED_TO_COMMIT',
]

const response = ref(await getOrders({ statuses }))

setInterval(async () => {
  response.value = await getOrders({ statuses })
}, 5000)

const orders = computed(() => {
  if (!response.value?.content) return []
  return response.value.content.map(
    ({
      orderId,
      status,
      scheduledPickupTime: pickupAt,
      description: amount,
    }) => ({
      orderId,
      status,
      pickupAt,
      amount,
    })
  )
})
</script>
<template>
  <section
    class="grid grid-cols-1 lg:grid-cols-4 lg:grid-cols-4 gap-4 overflow-y-auto"
  >
    <OrderDashboardItem
      v-for="order in orders"
      :key="order.orderId"
      :order-id="order.orderId"
      :status="order.status"
      :pickup-at="order.pickupAt"
      :amount="order.amount"
    />
  </section>
</template>
