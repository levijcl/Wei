<script setup>
import { useRouter } from 'vue-router'

const { orders } = defineProps({
  orders: {
    type: Array,
    default: () => [],
  },
})

const router = useRouter()
const handleClick = (orderId) => {
  router.push(`/orders/${orderId}`)
}
</script>
<template>
  <table>
    <thead>
      <tr>
        <th>Order</th>
        <th>Status</th>
        <th>Scheduled Pickup Time</th>
        <th>Amount</th>
        <th>Detail</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="order in orders" :key="order.orderId">
        <td>{{ order.orderId }}</td>
        <td>{{ order.status }}</td>
        <td>{{ order.pickupAt }}</td>
        <td>{{ order.amount }}</td>
        <td>
          <button
            class="bg-gray-400 px-4 roudned hover:cursor-pointer rounded my-2"
            @click="() => handleClick(order.orderId)"
          >
            Detail
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>
