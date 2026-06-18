import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, logout as apiLogout, type LoginParams, type LoginResult, type UserInfo } from '@/api/auth'
import { getProfile, type ProfileInfo } from '@/api/profile'
import router from '@/router'
import { ElMessage } from 'element-plus'

export interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
  permission?: string
}

export const useUserStore = defineStore('user', () => {
  // ── State ──────────────────────────────────────────────────────────────
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const userInfo = ref<ProfileInfo | null>(null)
  const deptList = ref<Array<{ id: number; deptName: string }>>([])

  // ── Getters ────────────────────────────────────────────────────────────
  function isLoggedIn(): boolean {
    return token.value !== null
  }

  function hasPermission(perm: string): boolean {
    if (roles.value.includes('ROLE_SYSTEM_ADMIN')) return true
    return permissions.value.includes(perm)
  }

  // ── Actions ────────────────────────────────────────────────────────────
  async function login(params: LoginParams): Promise<LoginResult> {
    const res = await apiLogin(params)
    if (res.code === 200 && res.data) {
      token.value = res.data.accessToken
      if (res.data.userInfo) {
        roles.value = res.data.userInfo.roles || []
        permissions.value = res.data.userInfo.permissions || []
        userInfo.value = {
          id: res.data.userInfo.id,
          username: res.data.userInfo.username,
          realName: res.data.userInfo.realName,
          email: '',
          phone: '',
          deptId: res.data.userInfo.deptId,
          deptName: res.data.userInfo.deptName || '',
          roleNames: res.data.userInfo.roles || [],
          lastLoginIp: '',
          lastLoginTime: '',
          passwordChangeRequired: false,
        }
      }
      return res.data
    }
    throw new Error(res.message || 'Login failed')
  }

  async function fetchUserInfo(): Promise<void> {
    if (!token.value) return
    try {
      const res = await getProfile()
      if (res.code === 200 && res.data) {
        const profile = res.data
        userInfo.value = profile
        // Update roles and permissions from profile if not already loaded
        if (roles.value.length === 0 && profile.roleNames) {
          roles.value = profile.roleNames.map(r => 'ROLE_' + r.toUpperCase().replace(/\s+/g, '_'))
        }
      }
    } catch (e) {
      console.warn('Failed to fetch user info:', e)
    }
  }

  async function logout(): Promise<void> {
    try {
      await apiLogout()
    } catch (e) {
      console.warn('Logout API error:', e)
    } finally {
      resetState()
      router.push('/login')
    }
  }

  function setToken(tokenValue: string | null) {
    token.value = tokenValue
  }

  function setPermissions(perms: string[]) {
    permissions.value = perms
  }

  function resetState() {
    token.value = null
    refreshToken.value = null
    roles.value = []
    permissions.value = []
    userInfo.value = null
    deptList.value = []
  }

  return {
    // State
    token,
    refreshToken,
    roles,
    permissions,
    userInfo,
    deptList,
    // Getters
    isLoggedIn,
    hasPermission,
    // Actions
    login,
    fetchUserInfo,
    logout,
    setToken,
    setPermissions,
    resetState,
  }
})
