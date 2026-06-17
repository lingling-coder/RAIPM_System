import http from '@/api/index'

export interface ReminderConfigDTO {
  id?: number
  typeCode: string
  achievementName?: string
  titleTemplate?: string
  bodyTemplate?: string
  urgency?: string
  advanceDays?: number
  deadline?: string
  schedulingRule?: string
  responsibleUserId?: number
  responsibleRoleCode?: string
  status?: number
}

export interface ReminderConfigVO {
  id: number
  typeCode: string
  typeName: string
  achievementName: string
  titleTemplate: string
  bodyTemplate?: string
  urgency: string
  advanceDays: number
  deadline?: string
  computedDeadline?: string
  schedulingRule?: string
  responsibleUserId?: number
  responsibleUserName?: string
  responsibleRoleCode?: string
  responsibleRoleName?: string
  status: number
  deptId?: number
  createdTime: string
  updatedTime?: string
}

export interface PageParams {
  page?: number
  pageSize?: number
  typeCode?: string
}

/** Get paginated list of reminder configurations */
export function page(params: PageParams) {
  return http.get('/api/reminder/configs/page', { params })
}

/** Get single reminder configuration by ID */
export function getById(id: number) {
  return http.get(`/api/reminder/configs/${id}`)
}

/** Create new reminder configuration */
export function create(data: ReminderConfigDTO) {
  return http.post('/api/reminder/configs', data)
}

/** Update existing reminder configuration */
export function update(id: number, data: ReminderConfigDTO) {
  return http.put(`/api/reminder/configs/${id}`, data)
}

/** Delete reminder configuration */
export function remove(id: number) {
  return http.delete(`/api/reminder/configs/${id}`)
}
