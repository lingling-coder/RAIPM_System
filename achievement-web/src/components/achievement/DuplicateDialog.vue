<template>
  <el-dialog
    v-model="dialogVisible"
    title="重复提示"
    width="500px"
    :close-on-click-modal="false"
    :show-close="false"
  >
    <div class="duplicate-warning">
      <el-icon class="warning-icon" :size="24" color="#E6A23C">
        <WarningFilled />
      </el-icon>
      <span class="warning-text">检测到重复成果：</span>
    </div>

    <div class="existing-info" v-if="duplicateData">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="已有成果">
          <el-link
            type="primary"
            @click="emit('view-existing', duplicateData.existingId)"
          >
            {{ duplicateData.existingTitle }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="类型">{{ duplicateData.existingType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ duplicateData.existingStatus }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ duplicateData.existingSubmitTime }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <p class="duplicate-message">
      您正在登记的成果与上述成果{{ fieldLabel }}相同。
    </p>

    <template #footer>
      <el-button @click="emit('view-existing', duplicateData?.existingId)">
        查看已有成果
      </el-button>
      <el-button type="primary" @click="emit('continue-submit')">
        继续填写并提交
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import type { DuplicateCheckResult } from '@/api/achievement/invalidation'

const props = defineProps<{
  visible: boolean
  duplicateData: DuplicateCheckResult | null
  fieldLabel?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'view-existing': [id: number | undefined]
  'continue-submit': []
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})
</script>

<style scoped>
.duplicate-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.warning-icon {
  flex-shrink: 0;
}

.warning-text {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.existing-info {
  margin-bottom: 16px;
}

.duplicate-message {
  font-size: 14px;
  color: #606266;
  margin: 0;
}
</style>
