<template>
  <div class="achievement-timeline" :class="{ compact: compact }">
    <el-timeline v-if="records && records.length > 0">
      <el-timeline-item
        v-for="record in records"
        :key="record.id"
        :timestamp="record.createdTime"
        :type="nodeType(record.action)"
        placement="top"
      >
        <div class="timeline-item-header">
          <span class="operator-name">{{ record.operatorName }}</span>
          <el-tag :type="tagType(record.action)" size="small">
            {{ record.actionLabel }}
          </el-tag>
        </div>
        <div v-if="record.comment" class="timeline-item-comment">
          {{ record.comment }}
        </div>
        <div class="timeline-item-time" v-if="!compact">
          {{ formatTime(record.createdTime) }}
        </div>
      </el-timeline-item>
      <el-timeline-item
        v-if="!compact && showPending"
        timestamp="当前待审"
        type="primary"
        placement="top"
        :hollow="true"
      >
        <div class="timeline-item-header">
          <span class="pending-label">等待审批</span>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无审批记录">
      <el-icon><Clock /></el-icon>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Clock } from '@element-plus/icons-vue'
import type { ApprovalRecordVO } from '@/api/approval'

const props = withDefaults(defineProps<{
  records: ApprovalRecordVO[]
  compact?: boolean
  showPending?: boolean
}>(), {
  compact: false,
  showPending: false,
})

function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

function nodeType(action: string): "primary" | "success" | "warning" | "info" | "danger" | undefined {
  const map: Record<string, "primary" | "success" | "warning" | "info" | "danger"> = {
    SUBMIT: 'primary',
    PASS_DEPT: 'success',
    PASS_ADMIN: 'success',
    REJECT_DEPT: 'danger',
    REJECT_ADMIN: 'danger',
    WITHDRAW: 'warning',
    RESUBMIT: 'primary',
  }
  return map[action] || 'info'
}

function tagType(action: string): "primary" | "success" | "warning" | "info" | "danger" | undefined {
  const map: Record<string, "primary" | "success" | "warning" | "info" | "danger"> = {
    SUBMIT: 'primary',
    PASS_DEPT: 'success',
    PASS_ADMIN: 'success',
    REJECT_DEPT: 'danger',
    REJECT_ADMIN: 'danger',
    WITHDRAW: 'warning',
    RESUBMIT: 'primary',
  }
  return map[action] || 'info'
}
</script>

<style scoped>
.achievement-timeline {
  width: 100%;
}

.compact .timeline-item-header {
  font-size: 13px;
}

.compact .el-timeline-item {
  padding-bottom: 8px;
}

.operator-name {
  font-weight: 500;
  margin-right: 8px;
}

.timeline-item-comment {
  color: #606266;
  font-size: 13px;
  margin-top: 4px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.timeline-item-time {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}

.pending-label {
  color: #909399;
  font-style: italic;
}
</style>
