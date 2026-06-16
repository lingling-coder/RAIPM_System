import http from './index'

export interface ProfileInfo {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  deptId: number | null
  deptName: string
  roleNames: string[]
  lastLoginIp: string
  lastLoginTime: string
  passwordChangeRequired: boolean
}

export interface ProfileUpdateData {
  realName?: string
  email?: string
  phone?: string
}

export interface PasswordChangeData {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/**
 * Get the authenticated user's profile.
 */
export function getProfile(): Promise<ApiResult<ProfileInfo>> {
  return http.get('/api/system/profile')
}

/**
 * Update personal profile info (name, email, phone only).
 */
export function updateProfile(data: ProfileUpdateData): Promise<ApiResult<null>> {
  return http.put('/api/system/profile', data)
}

/**
 * Change password with old password verification.
 * On success, forces re-login by invalidating all tokens.
 */
export function changePassword(data: PasswordChangeData): Promise<ApiResult<null>> {
  return http.put('/api/system/profile/password', data)
}
