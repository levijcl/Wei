<script setup>
import { ref, computed } from 'vue'
import { getInventoryAdjustment } from '@/src/apis/inventory.js'
import { useRouter } from 'vue-router'

const response = ref(await getInventoryAdjustment())
const router = useRouter()
const handleClickDetail = (id) => {
  router.push(`/adjustments/${id}/discrepancy`)
}
const adjustments = computed(() => {
  if (!response.value?.content) return []
  return response.value.content.map(
    ({ adjustmentId, discrepancyCount, description, createdAt }) => ({
      adjustmentId,
      createdAt,
      discrepancyCount,
      description,
    })
  )
})
</script>
<template>
  <table>
    <thead>
      <tr>
        <th scope="col">Detect Time</th>
        <th scope="col">Id</th>
        <th scope="col">Discrepancy count</th>
        <th scope="col">Description</th>
        <th scope="col">Detail</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="item in adjustments" :key="item.adjustmentId">
        <td>
          {{ new Date(item.createdAt).toLocaleString() }}
        </td>
        <td>
          {{ item.adjustmentId }}
        </td>
        <td>
          {{ item.discrepancyCount }}
        </td>
        <td>
          {{ item.description }}
        </td>
        <td>
          <button
            class="bg-gray-400 text-white rounded h-8 px-4 my-2 hover:cursor-pointer"
            @click="handleClickDetail(item.adjustmentId)"
          >
            Detail
          </button>
        </td>
      </tr>
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
