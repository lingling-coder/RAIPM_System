<template>
  <span class="highlighted-text">
    <span
      v-for="(seg, i) in segments"
      :key="i"
      :class="{ highlight: seg.highlighted }"
    >{{ seg.text }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text: string
  keyword: string
  ranges: Array<{ start: number; end: number }>
}>()

interface Segment {
  text: string
  highlighted: boolean
}

const segments = computed<Segment[]>(() => {
  if (!props.text || !props.ranges || props.ranges.length === 0) {
    return [{ text: props.text || '', highlighted: false }]
  }

  // Sort ranges by start position
  const sorted = [...props.ranges].sort((a, b) => a.start - b.start)

  const result: Segment[] = []
  let cursor = 0

  for (const range of sorted) {
    // Add non-highlighted text before this range
    if (cursor < range.start) {
      result.push({
        text: props.text.slice(cursor, range.start),
        highlighted: false,
      })
    }

    // Add highlighted text for this range
    if (range.start < props.text.length && range.end <= props.text.length) {
      result.push({
        text: props.text.slice(range.start, range.end),
        highlighted: true,
      })
    }

    cursor = Math.max(cursor, range.end)
  }

  // Add remaining text after the last range
  if (cursor < props.text.length) {
    result.push({
      text: props.text.slice(cursor),
      highlighted: false,
    })
  }

  return result.length > 0 ? result : [{ text: props.text, highlighted: false }]
})
</script>

<style scoped>
.highlighted-text .highlight {
  background: #fef0e6;
  color: #e6a23c;
}
</style>
