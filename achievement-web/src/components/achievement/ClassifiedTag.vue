<template>
  <el-tag
    v-if="level"
    :type="tagType"
    size="small"
    effect="dark"
  >
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  level?: string | null
}>()

const tagType = computed(() => {
  if (props.level === '机密') return 'danger'
  if (props.level === 'confidential') return 'danger'
  return 'warning' // 秘密/secret defaults to orange
})

const label = computed(() => {
  if (props.level === '秘密' || props.level === 'secret') return '秘密'
  if (props.level === '机密' || props.level === 'confidential') return '机密'
  return props.level || ''
})
</script>
