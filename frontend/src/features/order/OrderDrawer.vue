<script setup>
import { RouterLink } from 'vue-router'
</script>
<template>
  <section class="overflow-x-auto whitespace-nowrap px-4">
    <div class="inline-block lg:block">
      <RouterLink to="/" class="underline mr-4">Dashboard</RouterLink>
    </div>
    <div class="inline-block lg:block">
      <RouterLink to="/orders" class="underline">Order List</RouterLink>
    </div>
  </section>
</template>
