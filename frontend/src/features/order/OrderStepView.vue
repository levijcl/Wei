<script setup>
import { useRouter } from 'vue-router'
import TubeSpinner from '@/src/components/TubeSpinner.vue'
import OrderStep from './OrderStep.vue'
import { getTransactionDetail, reserveInventory } from '@/src/apis/inventory.js'

const router = useRouter()
const config = {
  ReservationFailedEvent: [
    {
      label: 'Retry',
      action: async (eventPayload) => {
        const { transactionId } = eventPayload
        const { transactionLines, sourceReferenceId } =
          await getTransactionDetail(transactionId)
        transactionLines.forEach(async ({ sku, quantity }) => {
          await reserveInventory({
            orderId: sourceReferenceId,
            sku,
            quantity,
          })

          router.back()
        })
      },
    },
  ],
}

const handleClickBack = () => {
  router.back()
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
      <div><OrderStep :config="config" /></div>
      <template #fallback>
        <div class="relative top-64 left-1/2 -trnaslate-1/2 w-24">
          <TubeSpinner />
        </div>
      </template>
    </Suspense>
  </div>
</template>
