import http from '@/api/index'

export interface DepartmentPageParams {
  page?: number
  pageSize?: number
}

export interface DepartmentCreateData {
  deptName: string
  deptCode: string
  leader?: string
  phone?: string
  status?: number
}

export interface DepartmentUpdateData {
  deptName?: string
  deptCode?: string
  leader?: string
  phone?: string
  status?: number
}

export interface DepartmentVO {
  id: number
  deptName: string
  deptCode: string
  leader: string
  phone: string
  status: number
  memberCount: number
  createdAt: string
}

/** Paginated department list */
export function page(params: DepartmentPageParams) {
  return http.post('/api/system/department/page', params)
}

/** Get department by ID */
export function getById(id: number) {
  return http.get(`/api/system/department/${id}`)
}

/** Create new department */
export function create(data: DepartmentCreateData) {
  return http.post('/api/system/department', data)
}

/** Update department */
export function update(id: number, data: DepartmentUpdateData) {
  return http.put(`/api/system/department/${id}`, data)
}

/** Delete department */
export function remove(id: number) {
  return http.delete(`/api/system/department/${id}`)
}

/** List all departments for dropdown */
export function listAll() {
  return http.get('/api/system/department/list-all')
}
