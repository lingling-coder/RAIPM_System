<template>
  <div class="notification-center" v-loading="loading">
    <h2 class="page-title">通知中心</h2>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="审批待办" name="APPROVAL" />
      <el-tab-pane label="系统通知" name="SYSTEM" />
      <el-tab-pane label="费用预警" name="ALERT" />
    </el-tabs>

    <!-- Notification List -->
    <div class="notification-list" v-if="notifications.length > 0">
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

    <!-- Empty State -->
    <el-empty
      v-if="!loading && notifications.length === 0"
      :description="emptyDescription"
    >
      <el-icon :size="40" :color="emptyIconColor">
        <Bell v-if="activeTab === 'APPROVAL'" />
        <WarningFilled v-else-if="activeTab === 'ALERT'" />
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
import { Bell, Message, WarningFilled } from '@element-plus/icons-vue'
import * as notificationApi from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const store = useNotificationStore()

const loading = ref(false)
const activeTab = ref('APPROVAL')
const notifications = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const emptyDescription = computed(() => {
  const map: Record<string, string> = {
    APPROVAL: '暂无审批待办通知',
    SYSTEM: '暂无系统通知',
    ALERT: '暂无费用预警',
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
    const res: any = await notificationApi.getList(activeTab.value, currentPage.value, pageSize.value)
    if (res?.data) {
      notifications.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    notifications.value = []
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
</style>
