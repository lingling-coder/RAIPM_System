<template>
  <div class="approval-history">
    <h3 class="section-header">审批历史</h3>
    <AchievementTimeline
      :records="records"
      :compact="true"
      :show-pending="showPending"
    />
  </div>
</template>

<script setup lang="ts">
import AchievementTimeline from '@/components/achievement/AchievementTimeline.vue'
import type { ApprovalRecordVO } from '@/api/approval'

defineProps<{
  records: ApprovalRecordVO[]
  showPending?: boolean
}>()
</script>

<style scoped>
.approval-history {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
}

.section-header {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px 0;
}
</style>
