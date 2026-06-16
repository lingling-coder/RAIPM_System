<template>
  <!-- Full-screen standalone layout for login page -->
  <template v-if="route.meta?.noLayout">
    <router-view />
  </template>

  <!-- Standard layout for all other pages -->
  <template v-else>
    <LayoutView />
  </template>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import LayoutView from '@/layout/index.vue'

const route = useRoute()
</script>
