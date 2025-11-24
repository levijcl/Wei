<script setup>
import { ref } from 'vue'
import { getInventoryAdjustmentDiscrepancy } from '@/src/apis/inventory.js'
import { useRoute } from 'vue-router'

const route = useRoute()
const discrepancies = ref(
  await getInventoryAdjustmentDiscrepancy(route.params.id)
)
</script>
<template>
  <table>
    <thead>
      <tr>
        <th scope="col">Sku</th>
        <th scope="col">Expected quantity</th>
        <th scope="col">Actual quantity</th>
        <th scope="col">Difference</th>
        <th scope="col">Type</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="item in discrepancies" :key="item.id">
        <td>
          {{ item.sku }}
        </td>
        <td>
          {{ item.expectedQuantity }}
        </td>
        <td>
          {{ item.actualQuantity }}
        </td>
        <td>
          {{ item.difference }}
        </td>
        <td>
          {{ item.discrepancyType }}
        </td>
      </tr>
    </tbody>
  </table>
</template>
