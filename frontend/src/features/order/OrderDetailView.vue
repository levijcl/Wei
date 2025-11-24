<script setup>
import OrderDetail from './OrderDetail.vue'
import TubeSpinner from '@/src/components/TubeSpinner.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const handleClickBack = () => {
  router.push('/orders')
}
</script>
<template>
  <div class="m-auto">
    <header class="h-16 flex items-center">
      <button
        class="bg-white text-black px-4 rounded h-8 hover:cursor-pointer"
        @click="() => handleClickBack()"
      >
        Back
      </button>
    </header>
    <Suspense>
      <div>
        <OrderDetail />
      </div>
      <template #fallback>
        <div class="relative top-64 left-1/2 -trnaslate-1/2 w-24">
          <TubeSpinner />
        </div>
      </template>
    </Suspense>
  </div>
</template>
