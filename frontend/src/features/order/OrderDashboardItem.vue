<script setup>
const { orderId } = defineProps({
  orderId: { type: String, default: 'Empty Order Id' },
  status: { type: String, default: 'Empty Status' },
  pickupAt: { type: String, default: 'Not Specified' },
  amount: { type: String, default: 'Not Specified' },
})
const statusStyle = {
  CREATED: 'text-green-300',
  SCHEDULED: 'text-blue-400',
  AWAITING_FULFILLMENT: 'text-blue-400',
  PARTIALLY_RESERVED: 'text-orange-400',
  'IN PROGRESS': 'text-green-400',
  RESERVED: 'text-blue-400',
  PARTIALLY_COMMITTED: 'text-orange-500',
  FAILED_TO_RESERVE: 'text-red-500',
  FAILED_TO_COMMIT: 'text-red-500',
}
const backgroundStyle = {
  CREATED: 'bg-green-100',
  SCHEDULED: 'bg-blue-100',
  AWAITING_FULFILLMENT: 'bg-white',
  PARTIALLY_RESERVED: 'bg-orange-100',
  'IN PROGRESS': 'bg-white',
  RESERVED: 'bg-white',
  PARTIALLY_COMMITTED: 'bg-orange-100',
  FAILED_TO_RESERVE: 'bg-red-100',
  FAILED_TO_COMMIT: 'bg-red-100',
}
</script>
<template>
  <section
    :class="[
      'text-black rounded p-4 border-double border-4 h-48',
      backgroundStyle[status],
    ]"
  >
    <div class="grid grid-rows-6">
      <div class="row-start-1 truncate">{{ orderId }}</div>
      <div class="row-start-2 text-xs flex items-end">Pickup At</div>
      <div class="row-start-3 truncate">{{ pickupAt }}</div>
      <div class="row-start-4">Items</div>
      <div class="row-start-5">{{ amount }}</div>
      <div :class="['row-start-6', statusStyle[status] || 'text-blue-400']">
        {{ status }}
      </div>
    </div>
  </section>
</template>
