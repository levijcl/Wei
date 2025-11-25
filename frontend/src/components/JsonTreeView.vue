<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  data: {
    type: [Object, Array, String, Number, Boolean, null],
    required: true,
  },
  keyName: {
    type: String,
    default: '',
  },
  depth: {
    type: Number,
    default: 0,
  },
})

const isExpanded = ref(false)

const dataType = computed(() => {
  if (props.data === null) return 'null'
  if (Array.isArray(props.data)) return 'array'
  return typeof props.data
})

const isExpandable = computed(() => {
  return dataType.value === 'object' || dataType.value === 'array'
})

const entries = computed(() => {
  if (dataType.value === 'object') {
    return Object.entries(props.data)
  }
  if (dataType.value === 'array') {
    return props.data.map((item, index) => [index, item])
  }
  return []
})

const previewValue = computed(() => {
  if (dataType.value === 'object') {
    const keys = Object.keys(props.data)
    return keys.length === 0 ? '{}' : `{ ${keys.length} fields }`
  }
  if (dataType.value === 'array') {
    return props.data.length === 0 ? '[]' : `[ ${props.data.length} items ]`
  }
  return ''
})

const typeColor = computed(() => {
  const colors = {
    string: 'text-green-600',
    number: 'text-blue-600',
    boolean: 'text-purple-600',
    null: 'text-gray-400',
    object: 'text-orange-600',
    array: 'text-orange-600',
  }
  return colors[dataType.value] || 'text-black'
})

const typeLabel = computed(() => {
  const labels = {
    string: 'str',
    number: 'num',
    boolean: 'bool',
    null: 'null',
    object: 'obj',
    array: 'arr',
  }
  return labels[dataType.value] || ''
})

const formattedValue = computed(() => {
  if (dataType.value === 'string') return `"${props.data}"`
  if (dataType.value === 'null') return 'null'
  if (dataType.value === 'boolean') return props.data.toString()
  return props.data
})

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}
</script>

<template>
  <div :style="{ paddingLeft: depth > 0 ? '20px' : '0' }">
    <div class="flex items-start gap-2 py-1">
      <button
        v-if="isExpandable"
        class="flex-shrink-0 w-5 h-5 flex items-center justify-center text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded transition-colors"
        @click="toggleExpand"
      >
        <span class="text-xs font-mono">{{ isExpanded ? '▼' : '▶' }}</span>
      </button>
      <div v-else class="w-5 flex-shrink-0"></div>
      <div class="flex-1 min-w-0">
        <div class="flex items-baseline gap-2 flex-wrap">
          <span v-if="keyName" class="font-medium text-gray-700"
            >{{ keyName }}:</span
          >
          <span
            class="text-xs px-1.5 py-0.5 rounded font-mono bg-gray-100 text-gray-600"
          >
            {{ typeLabel }}
          </span>
          <span
            v-if="!isExpandable"
            :class="typeColor"
            class="font-mono text-sm break-all"
          >
            {{ formattedValue }}
          </span>
          <span v-else-if="!isExpanded" class="text-gray-400 text-sm font-mono">
            {{ previewValue }}
          </span>
        </div>
        <div v-if="isExpandable && isExpanded" class="mt-1">
          <div
            v-for="[key, value] in entries"
            :key="key"
            class="border-l-2 border-gray-200"
          >
            <JsonTreeView
              :data="value"
              :key-name="String(key)"
              :depth="depth + 1"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
