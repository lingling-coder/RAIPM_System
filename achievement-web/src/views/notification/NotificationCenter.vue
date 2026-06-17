<template>
  <div class="notification-center" v-loading="loading">
    <h2 class="page-title">通知中心</h2>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="审批待办" name="APPROVAL" />
      <el-tab-pane label="系统通知" name="SYSTEM" />
      <el-tab-pane label="费用预警" name="ALERT" />
      <el-tab-pane name="REMINDER">
        <template #label>
          <span>
            <el-icon :size="16" style="margin-right: 4px; vertical-align: middle">
              <Bell />
            </el-icon>
            申报提醒
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- Notification List -->
    <div v-if="activeTab !== 'REMINDER' && notifications.length > 0" class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.readFlag }"
        @click="handleItemClick(item)"
      >
        <el-icon :size="20" class="notification-icon" :class="typeIconClass(item.type)">
          <Bell v-if="item.type === 'APPROVAL'" />
          <WarningFilled v-else-if="item.type === 'ALERT'" />
          <Message v-else />
        </el-icon>
        <div class="notification-content">
          <div class="notification-title">{{ item.title }}</div>
          <div class="notification-meta">
            <span class="notification-time">{{ formatTime(item.createdTime) }}</span>
          </div>
        </div>
        <el-tag v-if="!item.readFlag" size="small" type="danger" effect="dark">未读</el-tag>
        <el-tag v-else size="small" type="info">已读</el-tag>
      </div>
    </div>

    <!-- Reminder List -->
    <div v-if="activeTab === 'REMINDER' && reminders.length > 0" class="notification-list">
      <div
        v-for="item in reminders"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.readFlag, confirmed: item.confirmedFlag }"
        @click="handleReminderClick(item)"
      >
        <el-icon :size="20" class="reminder-type-icon" :style="{ color: getTypeColor(item.typeCode) }">
          <BellFilled v-if="item.typeCode === 'PROJECT_APPLICATION'" />
          <Medal v-else-if="item.typeCode === 'AWARD_APPLICATION'" />
          <WalletFilled v-else-if="item.typeCode === 'PATENT_ANNUAL_FEE'" />
          <CopyDocument v-else-if="item.typeCode === 'COPYRIGHT_MAINTENANCE'" />
          <DataBoard v-else-if="item.typeCode === 'TRANSFORMATION_EVAL'" />
          <Lock v-else-if="item.typeCode === 'CLASSIFIED_AUDIT'" />
          <BellFilled v-else />
        </el-icon>
        <div class="notification-content">
          <div class="notification-title">{{ item.title }}</div>
          <div class="notification-meta">
            <span class="deadline-countdown" :style="{ color: getCountdownColor(item.daysUntilDeadline) }">
              {{ getCountdownText(item.daysUntilDeadline) }}
            </span>
            <el-tag :type="urgencyTagType(item.urgency)" size="small" effect="dark">
              {{ urgencyLabel(item.urgency) }}
            </el-tag>
            <span v-if="item.typeName" class="reminder-type-label">{{ item.typeName }}</span>
          </div>
        </div>
        <div class="reminder-actions" @click.stop>
          <ReminderConfirmButton
            v-if="!item.confirmedFlag"
            :task-id="item.id"
            :confirmed="false"
            @confirmed="handleConfirmed"
          />
          <el-tag v-else type="success" size="small" effect="plain">已确认</el-tag>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <el-empty
      v-if="!loading && ((activeTab !== 'REMINDER' && notifications.length === 0) || (activeTab === 'REMINDER' && reminders.length === 0))"
      :description="emptyDescription"
    >
      <el-icon :size="40" :color="emptyIconColor">
        <Bell v-if="activeTab === 'APPROVAL'" />
        <WarningFilled v-else-if="activeTab === 'ALERT'" />
        <BellFilled v-else-if="activeTab === 'REMINDER'" />
        <Message v-else />
      </el-icon>
    </el-empty>

    <!-- Pagination -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="fetchNotifications"
      @current-change="fetchNotifications"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell, Message, WarningFilled,
  BellFilled, Medal, WalletFilled,
  CopyDocument, DataBoard, Lock,
} from '@element-plus/icons-vue'
import * as notificationApi from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import * as reminderApi from '@/api/reminder/reminder-task'
import ReminderConfirmButton from '@/components/reminder/ReminderConfirmButton.vue'

const router = useRouter()
const store = useNotificationStore()

const loading = ref(false)
const activeTab = ref('APPROVAL')
const notifications = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const reminders = ref<any[]>([])
const reminderTotal = ref(0)

const emptyDescription = computed(() => {
  const map: Record<string, string> = {
    APPROVAL: '暂无审批待办通知',
    SYSTEM: '暂无系统通知',
    ALERT: '暂无费用预警',
    REMINDER: '暂无申报提醒',
  }
  return map[activeTab.value] || '暂无通知'
})

const emptyIconColor = computed(() => {
  return activeTab.value === 'ALERT' ? '#e6a23c' : '#909399'
})

onMounted(() => {
  fetchNotifications()
})

async function fetchNotifications() {
  loading.value = true
  try {
    if (activeTab.value === 'REMINDER') {
      const res: any = await reminderApi.page({ page: currentPage.value, pageSize: pageSize.value })
      if (res?.data) {
        reminders.value = res.data.records || []
        reminderTotal.value = res.data.total || 0
        total.value = reminderTotal.value
      }
    } else {
      const res: any = await notificationApi.getList(activeTab.value, currentPage.value, pageSize.value)
      if (res?.data) {
        notifications.value = res.data.records || []
        total.value = res.data.total || 0
      }
    }
  } catch {
    notifications.value = []
    reminders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  currentPage.value = 1
  fetchNotifications()
}

function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

function typeIconClass(type: string): string {
  if (type === 'APPROVAL') return 'icon-approval'
  if (type === 'ALERT') return 'icon-alert'
  return 'icon-system'
}

function urgencyTagType(urgency: string): string {
  if (urgency === 'HIGH') return 'danger'
  if (urgency === 'MEDIUM') return 'warning'
  return 'primary'
}

function urgencyLabel(urgency: string): string {
  if (urgency === 'HIGH') return '高'
  if (urgency === 'MEDIUM') return '中'
  return '低'
}

function getCountdownColor(days: number): string {
  if (days < 0) return '#f56c6c'
  if (days < 7) return '#f56c6c'
  if (days <= 14) return '#e6a23c'
  return '#606266'
}

function getCountdownText(days: number): string {
  if (days >= 0) {
    return `${days}天后截止`
  }
  return `已逾期${Math.abs(days)}天`
}

function getTypeColor(typeCode: string): string {
  const colorMap: Record<string, string> = {
    PROJECT_APPLICATION: '#409eff',
    AWARD_APPLICATION: '#e6a23c',
    PATENT_ANNUAL_FEE: '#f56c6c',
    COPYRIGHT_MAINTENANCE: '#67c23a',
    TRANSFORMATION_EVAL: '#409eff',
    CLASSIFIED_AUDIT: '#f56c6c',
  }
  return colorMap[typeCode] || '#909399'
}

function handleReminderClick(item: any) {
  // Mark as read if unread — but do NOT auto-confirm (D-17)
  if (!item.readFlag) {
    store.markAsRead(item.id)
    item.readFlag = 1
  }
  // Phase 1: no specific route navigation for reminder items
}

function handleConfirmed(taskId: number) {
  const task = reminders.value.find((t: any) => t.id === taskId)
  if (task) {
    task.confirmedFlag = 1
  }
}

async function handleItemClick(item: any) {
  // Mark as read
  if (!item.readFlag) {
    await store.markAsRead(item.id)
    item.readFlag = 1
  }

  // Navigate based on notification type
  if (item.type === 'APPROVAL' && item.relatedAchievementId) {
    router.push(`/approval/detail/${item.relatedAchievementId}?type=${item.relatedAchievementType || 'paper'}`)
  } else if (item.type === 'ALERT' && item.relatedAchievementId) {
    router.push(`/fee/detail/${item.relatedAchievementId}`)
  }
}
</script>

<style scoped>
.notification-center {
  width: 100%;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 20px 0;
}

.notification-list {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: #f5f7fa;
}

.notification-item.unread {
  background: #ecf5ff;
}

.notification-item.unread:hover {
  background: #d9ecff;
}

.notification-item.confirmed {
  opacity: 0.8;
}

.notification-icon {
  flex-shrink: 0;
  margin-right: 12px;
  color: #909399;
}

.icon-approval {
  color: #409eff;
}

.icon-system {
  color: #67c23a;
}

.icon-alert {
  color: #e6a23c;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.deadline-countdown {
  font-size: 12px;
  font-weight: 500;
  margin-right: 8px;
}

.reminder-type-icon {
  flex-shrink: 0;
  margin-right: 12px;
}

.reminder-type-label {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.reminder-actions {
  flex-shrink: 0;
  margin-left: 12px;
}
</style>
