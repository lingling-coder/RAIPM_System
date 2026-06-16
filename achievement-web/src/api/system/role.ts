import http from '@/api/index'

export interface RolePageParams {
  page?: number
  pageSize?: number
}

export interface RoleCreateData {
  roleName: string
  roleCode: string
  description?: string
}

export interface RoleUpdateData {
  roleName?: string
  description?: string
  status?: number
}

export interface RoleVO {
  id: number
  roleName: string
  roleCode: string
  description: string
  status: number
  userCount: number
  createdAt: string
}

export interface MenuTreeNode {
  id: number
  parentId: number
  label: string
  permission: string
  type: number
  icon: string
  sortOrder: number
  children: MenuTreeNode[]
  checked: boolean
}

/** Paginated role list */
export function page(params: RolePageParams) {
  return http.post('/api/system/role/page', params)
}

/** Get role by ID */
export function getById(id: number) {
  return http.get(`/api/system/role/${id}`)
}

/** Create new role */
export function create(data: RoleCreateData) {
  return http.post('/api/system/role', data)
}

/** Update role */
export function update(id: number, data: RoleUpdateData) {
  return http.put(`/api/system/role/${id}`, data)
}

/** Delete role */
export function remove(id: number) {
  return http.delete(`/api/system/role/${id}`)
}

/** Get menu tree with checked state for role */
export function getMenuTree(id: number) {
  return http.get(`/api/system/role/${id}/menu-tree`)
}

/** Assign menu permissions to role */
export function assignMenuPermissions(id: number, menuIds: number[]) {
  return http.put(`/api/system/role/${id}/menu-permissions`, menuIds)
}

/** List all roles for dropdown */
export function listAll() {
  return http.get('/api/system/role/list-all')
}
