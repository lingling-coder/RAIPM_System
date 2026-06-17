import http from '@/api/index'

export interface ReminderTaskVO {
  id: number
  configId: number
  userId: number
  achievementName: string
  title: string
  content: string
  deadline: string
  daysRemaining: number
  urgency: string
  confirmedFlag: number
  confirmedTime: string | null
  escalationLevel: string
  typeCode: string
  typeName: string
  userName: string
  daysUntilDeadline: number
}

export interface PageParams {
  page?: number
  pageSize?: number
  urgency?: string
}

/** Get paginated list of reminder tasks */
export function page(params: PageParams) {
  return http.get('/api/reminder/tasks/page', { params })
}

/** Get a single reminder task by ID */
export function getById(id: number) {
  return http.get(`/api/reminder/tasks/${id}`)
}

/** Confirm receipt of a reminder task */
export function confirmReceipt(id: number) {
  return http.put(`/api/reminder/tasks/${id}/confirm`)
}

/** Dismiss a reminder task (mark as no longer needing attention) */
export function dismissTask(id: number) {
  return http.post(`/api/reminder/tasks/${id}/dismiss`)
}

/** Get all unconfirmed high urgency tasks (for global popup) */
export function getHighUrgencyUnconfirmed() {
  return http.get('/api/reminder/tasks/high-urgency-unconfirmed')
}

/** Get the count of unconfirmed reminder tasks (for bell badge) */
export function getUnconfirmedCount() {
  return http.get('/api/reminder/tasks/unconfirmed-count')
}
