import http from '@/api/index'

export interface UserPageParams {
  page?: number
  pageSize?: number
  keyword?: string
  deptId?: number | null
  roleId?: number | null
  status?: number | null
}

export interface UserCreateData {
  username: string
  password: string
  realName?: string
  email?: string
  phone?: string
  deptId?: number | null
  roleIds?: number[]
}

export interface UserUpdateData {
  realName?: string
  email?: string
  phone?: string
  deptId?: number | null
  roleIds?: number[]
  status?: number
}

export interface UserVO {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  deptId: number
  deptName: string
  roleIds: number[]
  roleNames: string[]
  status: number
  lastLoginIp: string
  lastLoginTime: string
  passwordChangeRequired: number
  createdAt: string
}

export interface ImportResult {
  inserted: number
  updated: number
  failed: number
  errors: string[]
  total: number
}

/** Paginated user list */
export function page(params: UserPageParams) {
  return http.post('/api/system/user/page', params)
}

/** Get user by ID */
export function getById(id: number) {
  return http.get(`/api/system/user/${id}`)
}

/** Create new user */
export function create(data: UserCreateData) {
  return http.post('/api/system/user', data)
}

/** Update user */
export function update(id: number, data: UserUpdateData) {
  return http.put(`/api/system/user/${id}`, data)
}

/** Delete user (soft delete) */
export function remove(id: number) {
  return http.delete(`/api/system/user/${id}`)
}

/** Batch delete users */
export function batchDelete(ids: number[]) {
  return http.post('/api/system/user/batch-delete', ids)
}

/** Set user enable/disable status */
export function setStatus(id: number, status: number) {
  return http.put(`/api/system/user/${id}/status`, status)
}

/** Reset user password */
export function resetPassword(id: number) {
  return http.post(`/api/system/user/${id}/reset-password`)
}

/** Import users from CSV */
export function importCsv(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/api/system/user/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
