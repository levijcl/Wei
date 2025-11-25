<script setup>
import { ref, computed } from 'vue'
import OrderListSearch from './OrderListSearch.vue'
import OrderList from './OrderList.vue'
import { getOrders } from '@/src/apis/order.js'

const response = ref({})
const handleSearch = async ({ status }) => {
  response.value = await getOrders({ statuses: status })
}
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
  <div class="flex flex-col m-auto">
    <OrderListSearch @search="handleSearch" />
    <hr class="my-4" />
    <OrderList :orders="orders" />
  </div>
</template>
