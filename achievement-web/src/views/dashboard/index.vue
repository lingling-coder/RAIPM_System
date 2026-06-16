<template>
  <div class="dashboard-page">
    <!-- Loading state -->
    <template v-if="loading">
      <el-skeleton :rows="3" animated class="page-skeleton" />
    </template>

    <!-- Error state -->
    <template v-else-if="error">
      <el-alert
        title="数据加载失败"
        type="error"
        show-icon
      >
        <template #default>
          <el-button type="text" @click="loadData">点击重试</el-button>
        </template>
      </el-alert>
    </template>

    <!-- Content -->
    <template v-else>
      <!-- Welcome section -->
      <div class="welcome-section">
        <h1 class="welcome-title">欢迎回来，{{ userInfo?.realName || userInfo?.username || '用户' }}</h1>
        <p class="welcome-subtitle">
          {{ roleLabel }}
          <template v-if="userInfo?.deptName"> · {{ userInfo.deptName }}</template>
        </p>
      </div>

      <!-- Stat cards (role-based) -->
      <el-row :gutter="16" class="stat-row">
        <!-- Admin stats -->
        <template v-if="isAdmin">
          <el-col :span="8" v-for="stat in adminStats" :key="stat.label">
            <el-card shadow="never" class="stat-card" @click="stat.action">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </el-card>
          </el-col>
        </template>

        <!-- Other role stats -->
        <template v-else>
          <el-col :span="8" v-for="stat in defaultStats" :key="stat.label">
            <el-card shadow="never" class="stat-card">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </el-card>
          </el-col>
        </template>
      </el-row>

      <!-- Quick action buttons -->
      <el-card shadow="never" class="quick-actions">
        <template #header>
          <span>快捷操作</span>
        </template>
        <el-space wrap>
          <el-button
            v-if="hasPermission('system:user:list')"
            @click="$router.push('/system/user')"
          >
            前往用户管理
          </el-button>
          <el-button
            v-if="hasPermission('system:audit:list')"
            @click="$router.push('/system/audit-log')"
          >
            查看审计日志
          </el-button>
          <el-button @click="$router.push('/profile')">
            个人中心
          </el-button>
        </el-space>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const error = ref(false)

const userInfo = computed(() => userStore.userInfo)
const isAdmin = computed(() => userStore.roles.includes('ROLE_SYSTEM_ADMIN'))

function hasPermission(perm: string): boolean {
  return userStore.hasPermission(perm)
}

const roleLabel = computed(() => {
  if (!userInfo.value?.roleNames?.length) return '用户'
  return userInfo.value.roleNames.join('、')
})

const adminStats = ref([
  { label: '总用户数', value: '-', action: () => router.push('/system/user') },
  { label: '总部门数', value: '-', action: () => router.push('/system/department') },
  { label: '总角色数', value: '-', action: () => router.push('/system/role') },
])

const defaultStats = ref([
  { label: '待处理事项', value: '-' },
  { label: '我的成果', value: '-' },
  { label: '系统通知', value: '-' },
])

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  error.value = false
  try {
    // Phase 0: static stats for now. Phase 3 will add ECharts and real data.
    if (isAdmin.value) {
      // Admin stats will be populated when backend dashboard API is available
      adminStats.value = [
        { label: '总用户数', value: '-', action: () => router.push('/system/user') },
        { label: '总部门数', value: '-', action: () => router.push('/system/department') },
        { label: '总角色数', value: '-', action: () => router.push('/system/role') },
      ]
    }
  } catch (e) {
    error.value = true
    console.error('Failed to load dashboard data:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.dashboard-page {
  .page-skeleton {
    padding: 16px;
  }

  .welcome-section {
    margin-bottom: 24px;

    .welcome-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }

    .welcome-subtitle {
      font-size: 14px;
      color: #909399;
      margin: 8px 0 0 0;
    }
  }

  .stat-row {
    margin-bottom: 16px;
  }

  .stat-card {
    text-align: center;
    cursor: pointer;
    transition: transform 0.2s;

    &:hover {
      transform: translateY(-2px);
    }

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

  .quick-actions {
    margin-top: 16px;
  }
}
</style>
