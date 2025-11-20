<script setup>
import OrderDrawer from '@/src/features/order/OrderDrawer.vue'
</script>

<template>
  <div class="lg:flex">
    <aside class="basis-40"><OrderDrawer /></aside>
    <router-view v-slot="{ Component }">
      <KeepAlive :include="['OrderListView']">
        <component :is="Component" />
      </KeepAlive>
    </router-view>
  </div>
</template>

<style scoped></style>
