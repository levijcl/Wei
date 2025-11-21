<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail } from '@/src/apis/order.js'
import OrderDetailInformation from './OrderDetailInformation.vue'
import OrderDetailTable from './OrderDetailTable.vue'

const route = useRoute()
const order = ref(await getOrderDetail(route.params.id))
</script>
<template>
  <OrderDetailInformation :order="order" />
  <hr class="border-2 my-4" />
  <OrderDetailTable :order="order" />
</template>
