<script setup>
import TubeSpinner from '@/src/components/TubeSpinner.vue'
import InventoryAdjustment from './InventoryAdjustment.vue'
</script>
<template>
  <Suspense>
    <div class="m-auto mt-8">
      <InventoryAdjustment />
    </div>
    <template #fallback>
      <div class="relative top-64 left-1/2 -trnaslate-1/2 w-24">
        <TubeSpinner />
      </div>
    </template>
  </Suspense>
</template>
