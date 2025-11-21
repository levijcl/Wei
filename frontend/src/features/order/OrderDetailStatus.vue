<script setup>
const { status } = defineProps({
  status: {
    type: Object,
    default: () => ({ orderId: '', steps: [] }),
  },
})
const canClickable = (status) => status != 'PENDING'
</script>
<template>
  <div class="flex flex-col">
    <div
      v-for="(step, index) in status.steps"
      :key="step.stepNumber"
      :class="[
        'border-2',
        'text-black',
        'rounded',
        'pl-1',
        'h-10',
        'flex',
        'items-center',
        'pr-4',
        canClickable(step.status) ? 'bg-white' : 'bg-gray-300',
      ]"
    >
      <svg height="40" width="16" xmlns="http://www.w3.org/2000/svg">
        <line
          v-if="index != 0"
          x1="8"
          y1="2"
          x2="8"
          y2="20"
          :style="[
            'stroke-width: 2',
            canClickable(step.status) ? 'stroke: black' : 'stroke: gray',
          ]"
        />
        <line
          v-if="index != status.steps.length - 1"
          x1="8"
          y1="20"
          x2="8"
          y2="38"
          :style="[
            'stroke-width: 2',
            canClickable(step.status) ? 'stroke: black' : 'stroke: gray',
          ]"
        />
        <circle
          r="4"
          cx="8"
          cy="20"
          :fill="[canClickable(step.status) ? 'black' : 'gray']"
        />
      </svg>
      <div :class="['ml-2']">
        {{ step.stepName }}
        <span v-if="canClickable(step.status)" class="font-bold">
          : {{ step.status }}
        </span>
      </div>
      <div class="flex-1"></div>
      <button
        v-if="canClickable(step.status)"
        class="px-4 h-8 rounded bg-gray-400 text-white hover:cursor-pointer"
      >
        Detail
      </button>
    </div>
  </div>
</template>
