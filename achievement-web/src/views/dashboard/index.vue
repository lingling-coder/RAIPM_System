<template>
  <div class="dashboard">
    <el-card shadow="never">
      <h2>欢迎回来，{{ userStore.userInfo.realName || 'User' }}</h2>
      <p class="welcome-text">Welcome to the Achievement Management System</p>
    </el-card>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const stats = [
  { label: 'Papers', value: '-' },
  { label: 'Patents', value: '-' },
  { label: 'Software Copyrights', value: '-' },
  { label: 'Pending Reviews', value: '-' },
]
</script>

<style scoped lang="scss">
.dashboard {
  .welcome-text {
    color: #909399;
    font-size: 14px;
  }

  .stat-cards {
    margin-top: 16px;
  }

  .stat-card {
    text-align: center;

    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 8px;
    }
  }
}
</style>
