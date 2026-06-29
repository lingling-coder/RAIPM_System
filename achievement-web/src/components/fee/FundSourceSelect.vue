<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    clearable
    style="width: 100%"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-option
      v-for="option in options"
      :key="option.value"
      :label="option.label"
      :value="option.value"
    />
  </el-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '@/api/index'

/**
 * Reusable funding source selector component.
 *
 * Loads options from the FUND_SOURCE data dictionary.
 * Can be used standalone outside the fee module.
 *
 * @prop modelValue - currently selected funding source code
 * @prop placeholder - placeholder text for the select
 * @emits update:modelValue - when selection changes
 */
defineProps<{
  modelValue?: string | null
  placeholder?: string
}>()

defineEmits<{
  'update:modelValue': [value: string | null | undefined]
}>()

interface DictOption {
  value: string
  label: string
}

const options = ref<DictOption[]>([])

async function loadOptions() {
  try {
    // Query data dictionary by category code 'FUND_SOURCE'
    const res: any = await http.get('/api/dict/entries/FUND_SOURCE')
    if (res?.data) {
      // The API returns dict entries with dictValue and dictLabel fields
      options.value = (res.data as any[]).map((item: any) => ({
        value: item.dictValue || item.value,
        label: item.dictLabel || item.label,
      }))
    }
  } catch {
    // Fallback options in case dict API fails
    options.value = [
      { value: 'vertical', label: '纵向科研经费' },
      { value: 'horizontal', label: '横向科研经费' },
      { value: 'institute', label: '院配套' },
      { value: 'self', label: '自筹' },
    ]
  }
}

onMounted(() => {
  loadOptions()
})
</script>
