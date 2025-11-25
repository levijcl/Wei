<script setup>
const { order } = defineProps({
  order: {
    type: Object,
    default: () => ({}),
  },
})
</script>
<template>
  <table class="w-full">
    <thead>
      <tr>
        <th scope="col">Sku</th>
        <th scope="col">Quantity</th>
        <th scope="col">Reservation</th>
        <th scope="col">Commitment</th>
      </tr>
    </thead>
    <tbody>
      <template v-for="item in order.lineItems" :key="item.id">
        <tr>
          <td rowspan="2">{{ item.sku }}</td>
          <td rowspan="2" class="text-center">{{ item.quantity }}</td>
          <td>{{ item.reservationInfo.status }}</td>
          <td>{{ item.commitmentInfo.status }}</td>
        </tr>
        <tr>
          <td>{{ item.reservationInfo.reservedAt }}</td>
          <td>{{ item.commitmentInfo.committedAt }}</td>
        </tr>
      </template>
    </tbody>
  </table>
</template>
<style scope>
table,
th,
td {
  border: 1px solid white;
  padding: 0 4px;
}
</style>
