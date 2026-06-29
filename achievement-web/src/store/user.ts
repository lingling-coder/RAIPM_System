import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, logout as apiLogout, type LoginParams, type LoginResult, type UserInfo } from '@/api/auth'
import { getProfile, type ProfileInfo } from '@/api/profile'
import router from '@/router'
import { ElMessage } from 'element-plus'

/**
 * Decode JWT payload without a library.
 * Returns null for invalid tokens.
 */
function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const jsonStr = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonStr)
  } catch {
    return null
  }
}

export interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
  permission?: string
}

export const useUserStore = defineStore('user', () => {
  // ── State ──────────────────────────────────────────────────────────────
  // Restore token from localStorage so it survives page refresh
  const token = ref<string | null>(localStorage.getItem('accessToken'))
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
      localStorage.setItem('accessToken', res.data.accessToken)
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
        // Priority: profile.roles (role codes) > JWT decode > roleNames mapping
        if (roles.value.length === 0) {
          if (profile.roles && profile.roles.length > 0) {
            roles.value = profile.roles
          } else {
            // Fallback: decode from JWT
            const claims = parseJwt(token.value)
            if (claims && Array.isArray(claims.roles)) {
              roles.value = claims.roles as string[]
            } else if (profile.roleNames) {
              // Last resort: map Chinese role names
              roles.value = profile.roleNames.map(r => 'ROLE_' + r.toUpperCase().replace(/\s+/g, '_'))
            }
          }
        }

        if (permissions.value.length === 0) {
          if (profile.permissions && profile.permissions.length > 0) {
            permissions.value = profile.permissions
          } else {
            // Fallback: decode from JWT
            const claims = parseJwt(token.value)
            if (claims && Array.isArray(claims.permissions)) {
              permissions.value = claims.permissions as string[]
            }
          }
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
    localStorage.removeItem('accessToken')
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
