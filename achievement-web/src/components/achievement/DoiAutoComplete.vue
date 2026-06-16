<template>
  <div class="doi-auto-complete">
    <el-input
      :model-value="modelValue"
      placeholder="输入DOI后移出输入框可自动补全"
      :disabled="loading"
      @blur="onBlur"
      @input="onInput"
      clearable
    >
      <template #suffix>
        <el-icon v-if="loading" class="is-loading">
          <Loading />
        </el-icon>
      </template>
    </el-input>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { lookupDoi } from '@/api/achievement/paper'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'doi-result', data: any): void
}>()

const loading = ref(false)
let lastValue = ''

const DOI_PATTERN = /^10\.\d{4,}\/.*$/

function onInput(value: string) {
  emit('update:modelValue', value)
}

async function onBlur() {
  const value = props.modelValue?.trim()
  if (!value || value === lastValue) return
  if (!DOI_PATTERN.test(value)) {
    ElMessage.warning('DOI 格式不正确 (格式: 10.xxxx/xxxx)')
    return
  }

  lastValue = value
  loading.value = true

  try {
    const res: any = await lookupDoi(value)
    if (res?.data?.found) {
      emit('doi-result', res.data)
    } else {
      ElMessage.warning('DOI 补全失败，请检查 DOI 格式或手动填写')
    }
  } catch {
    ElMessage.warning('DOI 补全服务暂时不可用，已切换备选数据源，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.doi-auto-complete {
  width: 100%;
}
</style>
