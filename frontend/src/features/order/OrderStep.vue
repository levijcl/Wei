<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetailStatusStep } from '@/src/apis/order.js'
import JsonTreeView from '@/src/components/JsonTreeView.vue'

const { config } = defineProps({
  config: {
    type: Object,
    default: () => ({}),
  },
})
const handleFindActions = (event) => (config[event] ? config[event] : [])

const route = useRoute()

const step = ref(
  await getOrderDetailStatusStep(route.params.id, route.params.step)
)

const statusColor = computed(() => {
  const status = step.value?.overallStatus
  if (status === 'SUCCESS') return 'text-green-600 bg-green-50 border-green-200'
  if (status === 'FAILED') return 'text-red-600 bg-red-50 border-red-200'
  if (status === 'PENDING') return 'text-gray-600 bg-gray-50 border-gray-200'
  return 'text-gray-600 bg-gray-50 border-gray-200'
})
</script>
<template>
  <div class="w-4xl m-auto">
    <div class="bg-white rounded-lg border border-gray-200 shadow-sm">
      <div class="border-b border-gray-200 p-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-semibold text-gray-900">
              Step {{ step.stepNumber }}: {{ step.stepName }}
            </h1>
          </div>
          <div>
            <span
              :class="statusColor"
              class="px-3 py-1 rounded-full text-sm font-medium border"
            >
              {{ step.overallStatus }}
            </span>
          </div>
        </div>
      </div>
      <div class="p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">
          Events ({{ step.events?.length || 0 }})
        </h2>
        <div v-if="step.events && step.events.length > 0" class="space-y-4">
          <div
            v-for="event in step.events"
            :key="event.recordId"
            class="border border-gray-200 rounded-lg p-4 bg-gray-50"
          >
            <div class="mb-3 pb-3 border-b border-gray-300">
              <div class="flex items-center justify-between mb-2">
                <h3 class="text-base font-semibold text-gray-900">
                  {{ event.eventName }}
                </h3>
                <span class="text-xs text-gray-500 font-mono">
                  {{ new Date(event.eventTimestamp).toLocaleString() }}
                </span>
              </div>
              <p class="text-xs text-gray-500 font-mono">
                ID: {{ event.recordId }}
              </p>
              <div class="flex mt-2 overflow-x-auto">
                <button
                  v-for="action in handleFindActions(event.eventName)"
                  :key="action.label"
                  class="bg-gray-400 text-white px-4 rounded h-8 hover:cursor-pointer mr-2"
                  @click="action.action"
                >
                  {{ action.label }}
                </button>
              </div>
            </div>
            <div
              class="bg-white rounded border border-gray-200 p-4 font-mono text-sm"
            >
              <JsonTreeView :data="event" />
            </div>
          </div>
        </div>
        <div v-else class="text-center py-8 text-gray-500">
          No events recorded for this step
        </div>
      </div>
    </div>
  </div>
</template>
