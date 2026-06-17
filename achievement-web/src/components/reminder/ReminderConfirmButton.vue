<template>
  <el-button
    v-if="!confirmed"
    type="primary"
    size="small"
    :loading="loading"
    :disabled="loading"
    @click="handleConfirm"
  >
    确认收到
  </el-button>
  <el-tag v-else type="success" size="small" effect="plain">已确认</el-tag>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as reminderApi from '@/api/reminder/reminder-task'

const props = defineProps<{
  taskId: number
  confirmed: boolean
}>()

const emit = defineEmits<{
  confirmed: [taskId: number]
}>()

const loading = ref(false)

async function handleConfirm() {
  loading.value = true
  try {
    await reminderApi.confirmReceipt(props.taskId)
    ElMessage.success('确认成功')
    emit('confirmed', props.taskId)
  } catch {
    ElMessage.error('确认失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.el-button + .el-tag {
  margin-left: 8px;
}
</style>
