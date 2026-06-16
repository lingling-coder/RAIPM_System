import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
  permission?: string
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const menuList = ref<MenuItem[]>([])

  /**
   * Default menu structure per D-36.
   * Filtered by user permissions in layout/index.vue or via setMenuList.
   */
  const defaultMenus: MenuItem[] = [
    {
      path: '/dashboard',
      title: '首页',
      icon: 'HomeFilled',
    },
    {
      path: '/system',
      title: '系统管理',
      icon: 'Setting',
      children: [
        { path: '/system/user', title: '用户管理', permission: 'system:user:list' },
        { path: '/system/role', title: '角色管理', permission: 'system:role:list' },
        { path: '/system/department', title: '部门管理', permission: 'system:dept:list' },
        { path: '/system/dict', title: '数据字典', permission: 'system:dict:list' },
        { path: '/system/audit-log', title: '审计日志', permission: 'system:audit:list' },
        { path: '/system/api-config', title: 'API集成配置', permission: 'system:api:list' },
      ],
    },
  ]

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  function setMenuList(menus: MenuItem[]) {
    menuList.value = menus
  }

  function resetToDefault() {
    menuList.value = defaultMenus
  }

  return {
    sidebarCollapsed,
    menuList,
    defaultMenus,
    toggleSidebar,
    setSidebarCollapsed,
    setMenuList,
    resetToDefault,
  }
})
