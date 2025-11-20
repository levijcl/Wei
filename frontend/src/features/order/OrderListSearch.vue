<script setup>
import { reactive, toRaw } from 'vue'

const emits = defineEmits(['search'])
const statuses = {
  CREATED: 'CREATED',
  SCHEDULED: 'SCHEDULED',
  'AWAITING FULFILLMENT': 'AWAITING_FULFILLMENT',
  'PARTIALLY RESERVED': 'PARTIALLY_RESERVED',
  RESERVED: 'RESERVED',
  'PARTIALLY COMMITTED': 'PARTIALLY_COMMITTED',
  COMMITTED: 'COMMITTED',
  'FAILED TO RESERVE': 'FAILED_TO_RESERVE',
  'FAILED TO COMMIT': 'FAILED_TO_COMMIT',
}
const criteria = reactive({
  status: [statuses.CREATED],
})
const handleClickSearch = () => {
  emits('search', toRaw(criteria))
}
</script>
<template>
  <form
    class="grid grid-cols-8 grid-rows-4 bg-white rounded border-2 border-dashed text-black p-4"
    @submit.prevent="onSubmit"
  >
    <div class="row-start-1 col-start-1">Status</div>
    <div class="row-start-1 col-start-2 row-span-4 col-span-2">
      <select
        id="status"
        v-model="criteria.status"
        name="cars"
        class="border border-1 h-44 px-1"
        multiple
      >
        <option v-for="(value, key) in statuses" :key="key" :value="value">
          {{ key }}
        </option>
      </select>
    </div>
    <div class="row-start-4 col-start-8 flex items-end jusitfy-end">
      <button
        type="submit"
        class="bg-gray-400 px-4 rounded hover:cursor-pointer text-white"
        @click="() => handleClickSearch()"
      >
        Search
      </button>
    </div>
  </form>
</template>
