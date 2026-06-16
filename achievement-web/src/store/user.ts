import { defineStore } from 'pinia'
import { ref } from 'vue'

interface UserInfo {
  id?: number
  username?: string
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  deptName?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const userInfo = ref<UserInfo>({})

  function setToken(tokenValue: string | null) {
    token.value = tokenValue
  }

  function setRefreshToken(tokenValue: string | null) {
    refreshToken.value = tokenValue
  }

  function setRoles(rolesList: string[]) {
    roles.value = rolesList
  }

  function setPermissions(permsList: string[]) {
    permissions.value = permsList
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function resetState() {
    token.value = null
    refreshToken.value = null
    roles.value = []
    permissions.value = []
    userInfo.value = {}
  }

  return {
    token,
    refreshToken,
    roles,
    permissions,
    userInfo,
    setToken,
    setRefreshToken,
    setRoles,
    setPermissions,
    setUserInfo,
    resetState,
  }
})
