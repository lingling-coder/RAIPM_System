import http from '@/api/index'

/**
 * Alert record view object matching backend AlertRecordVO.
 */
export interface AlertRecordVO {
  id: number
  feeRecordId: number
  alertLevel: string
  alertLevelLabel?: string
  triggeredDate: string
  triggeredAt: string
  resolvedAt?: string
  status: string
  statusLabel?: string
  escalationLevel?: string
  escalationLevelLabel?: string
  feeAmount?: number
  dueDate: string
  ownerType: string
  ownerId: number
  ownerName?: string
}

/**
 * Alert record paginated listing params.
 */
export interface AlertRecordPageParams {
  page: number
  size: number
  status?: string
  alertLevel?: string
}

/**
 * Get paginated alert records with optional filtering.
 */
export function getPage(params: AlertRecordPageParams) {
  return http.get('/api/alert-records/page', { params })
}

/**
 * Get an alert record by ID.
 */
export function getById(id: number) {
  return http.get(`/api/alert-records/${id}`)
}

/**
 * Resolve a single alert record.
 */
export function resolve(id: number) {
  return http.put(`/api/alert-records/${id}/resolve`)
}

/**
 * Batch resolve multiple alert records.
 */
export function batchResolve(ids: number[]) {
  return http.put('/api/alert-records/batch-resolve', ids)
}
