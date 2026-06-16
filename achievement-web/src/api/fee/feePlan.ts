import http from '@/api/index'

/**
 * Fee plan API response interface matching backend FeePlanVO.
 */
export interface FeePlanVO {
  id: number
  patentId: number
  feeType: string
  amount: number
  dueDate: string
  status: string
  source: string
  fundingSource?: string
  deptId?: number
  createdBy?: number
  createdTime?: string
  updatedBy?: number
  updatedTime?: string

  // Computed / transient fields
  patentName?: string
  patentType?: string
  applicationNo?: string
  statusLabel?: string
  feeTypeLabel?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * Fee plan paginated listing params.
 */
export interface FeePlanPageParams {
  page: number
  size: number
  status?: string
  feeType?: string
  keyword?: string
  patentId?: number
}

/**
 * Fee plan create/update DTO.
 */
export interface FeePlanDTO {
  patentId: number
  feeType: string
  amount: number
  dueDate?: string
  fundingSource?: string
}

/**
 * Get paginated fee plans with multi-dimensional filtering.
 */
export function getPage(params: FeePlanPageParams) {
  return http.get('/api/fee-plans/page', { params })
}

/**
 * Get a fee plan by ID.
 */
export function getById(id: number) {
  return http.get(`/api/fee-plans/${id}`)
}

/**
 * Create a new fee plan (manual one-time plan).
 */
export function create(data: FeePlanDTO) {
  return http.post('/api/fee-plans', data)
}

/**
 * Update an existing fee plan (amount and fundingSource only).
 */
export function update(id: number, data: FeePlanDTO) {
  return http.put(`/api/fee-plans/${id}`, data)
}

/**
 * Delete a fee plan (only allowed for paused plans).
 */
export function remove(id: number) {
  return http.delete(`/api/fee-plans/${id}`)
}

/**
 * Pause a fee plan (only active plans).
 */
export function pausePlan(id: number) {
  return http.put(`/api/fee-plans/${id}/pause`)
}

/**
 * Restore a paused fee plan.
 */
export function restorePlan(id: number) {
  return http.put(`/api/fee-plans/${id}/restore`)
}
