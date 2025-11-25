<script setup>
const { order } = defineProps({
  order: {
    type: Object,
    default: () => ({}),
  },
})
</script>
<template>
  <div
    class="grid grid-cols-4 grid-rows-4 bg-white rounded border-2 border-dashed text-black p-4"
  >
    <div class="row-start-1">Id</div>
    <div class="row-start-1 col-span-4">{{ order.orderId }}</div>
    <div class="row-start-3">Scheduled Pickup At</div>
    <div class="row-start-3">{{ order.scheduledPickupTime }}</div>
    <div class="row-start-4">Status</div>
    <div class="row-start-4">{{ order.status }}</div>
  </div>
</template>
