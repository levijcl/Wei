<script setup>
import OrderDashboard from './OrderDashboard.vue'
import TubeSpinner from '@/src/components/TubeSpinner.vue'
</script>

<template>
  <Suspense>
    <OrderDashboard />
    <template #fallback>
      <div class="relative top-64 left-1/2 -trnaslate-1/2 w-24">
        <TubeSpinner />
      </div>
    </template>
  </Suspense>
</template>

<style scoped></style>
