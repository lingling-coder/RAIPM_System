<template>
  <div class="layout-container">
    <!-- Left Sidebar -->
    <div class="layout-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="sidebar-logo">
        <span class="logo-text" v-if="!appStore.sidebarCollapsed">科研成果管理系统</span>
        <span class="logo-icon" v-else>A</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        background-color="#1d1e1f"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <!-- Dashboard -->
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <!-- Achievement Management Submenu -->
        <el-sub-menu index="/achievement">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>成果管理</span>
          </template>
          <el-menu-item index="/achievement/register">
            <el-icon><Edit /></el-icon>
            <template #title>成果登记</template>
          </el-menu-item>
          <el-menu-item index="/achievement/list">
            <el-icon><List /></el-icon>
            <template #title>成果列表</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- Approval Management Submenu -->
        <el-sub-menu index="/approval">
          <template #title>
            <el-icon><Check /></el-icon>
            <span>审批管理</span>
          </template>
          <el-menu-item index="/approval/pending">
            <el-icon><Clock /></el-icon>
            <template #title>审批待办</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- System Management Submenu (filtered by RBAC per D-07) -->
        <el-sub-menu v-if="hasSystemPermission" index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item
            v-for="item in systemMenuItems"
            :key="item.path"
            :index="item.path"
          >
            {{ item.title }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <!-- Collapse toggle at sidebar bottom -->
      <div class="sidebar-collapse-btn" @click="appStore.toggleSidebar">
        <el-icon>
          <Fold v-if="!appStore.sidebarCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
    </div>

    <!-- Right Content Area -->
    <div class="layout-main" :class="{ collapsed: appStore.sidebarCollapsed }">
      <!-- Top Navbar -->
      <header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-trigger" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- Notification Bell -->
          <NotificationBell />

          <el-dropdown trigger="click">
            <span class="user-dropdown-trigger">
              <el-avatar :size="28" icon="UserFilled" />
              <span class="username">{{ displayName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Main Content Area -->
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import {
  HomeFilled,
  Setting,
  Document,
  Edit,
  List,
  Check,
  Clock,
  Fold,
  Expand,
  UserFilled,
  User,
  SwitchButton,
} from '@element-plus/icons-vue'
import NotificationBell from '@/components/notification/NotificationBell.vue'

interface SystemMenuItem {
  path: string
  title: string
  permission: string
}

/**
 * System management menu items per D-36 with their required permissions.
 */
const systemMenuItemsFull: SystemMenuItem[] = [
  { path: '/system/user', title: '用户管理', permission: 'system:user:list' },
  { path: '/system/role', title: '角色管理', permission: 'system:role:list' },
  { path: '/system/department', title: '部门管理', permission: 'system:dept:list' },
  { path: '/system/dict', title: '数据字典', permission: 'system:dict:list' },
  { path: '/system/audit-log', title: '审计日志', permission: 'system:audit:list' },
  { path: '/system/api-config', title: 'API集成配置', permission: 'system:api:list' },
  { path: '/system/reminder-config', title: '提醒配置', permission: 'system:reminder:list' },
]

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

// Active menu matches current route
const activeMenu = computed(() => route.path)

// Current breadcrumb title
const currentTitle = computed(() => {
  return (route.meta?.title as string) || ''
})

// Display name for the user dropdown
const displayName = computed(() => {
  return userStore.userInfo?.realName || userStore.userInfo?.username || '用户'
})

// Check if user has any system management permission
const hasSystemPermission = computed(() => {
  return systemMenuItemsFull.some(item => userStore.hasPermission(item.permission))
})

// Filtered system menu items based on user permissions (RBAC D-07)
const systemMenuItems = computed(() => {
  return systemMenuItemsFull.filter(item => userStore.hasPermission(item.permission))
})

// Navigation functions
function goToProfile() {
  router.push('/profile')
}

async function handleLogout() {
  try {
    await userStore.logout()
  } catch {
    // Ensure navigation even if API fails
    userStore.resetState()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

// ── Sidebar ───────────────────────────────────────────────────────────
.layout-sidebar {
  width: 220px;
  background-color: #1d1e1f;
  transition: width 0.3s;
  overflow-y: auto;
  overflow-x: hidden;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;

  &.collapsed {
    width: 64px;
  }

  .sidebar-logo {
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    flex-shrink: 0;

    .logo-icon {
      font-size: 22px;
    }
  }

  .el-menu {
    border-right: none;
    flex: 1;
  }

  .sidebar-collapse-btn {
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #bfcbd9;
    cursor: pointer;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    flex-shrink: 0;

    &:hover {
      color: #409eff;
    }
  }
}

// ── Main Content ──────────────────────────────────────────────────────
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.collapsed {
    margin-left: 0;
  }
}

// ── Header ────────────────────────────────────────────────────────────
.layout-header {
  height: 48px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .collapse-trigger {
      font-size: 18px;
      cursor: pointer;
      color: #606266;

      &:hover {
        color: #409eff;
      }
    }
  }

  .header-right {
    .user-dropdown-trigger {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username {
        font-size: 14px;
        color: #303133;
      }
    }
  }
}

// ── Content ───────────────────────────────────────────────────────────
.layout-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #f5f7fa;
}
</style>
