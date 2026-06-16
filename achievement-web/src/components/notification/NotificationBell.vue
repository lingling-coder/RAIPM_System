<template>
  <div class="notification-bell" @click="goToNotification">
    <el-badge :value="store.unreadCount" :hidden="store.unreadCount === 0" :max="99">
      <el-icon :size="20" class="bell-icon">
        <Bell />
      </el-icon>
    </el-badge>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { Bell } from '@element-plus/icons-vue'

const router = useRouter()
const store = useNotificationStore()

onMounted(() => {
  store.startPolling()
})

onUnmounted(() => {
  store.stopPolling()
})

function goToNotification() {
  router.push('/notification')
}
</script>

<style scoped>
.notification-bell {
  cursor: pointer;
  padding: 4px 8px;
  display: flex;
  align-items: center;
}

.notification-bell:hover {
  background: #f5f7fa;
  border-radius: 4px;
}

.bell-icon {
  color: #606266;
}

.notification-bell:hover .bell-icon {
  color: #409eff;
}
</style>
