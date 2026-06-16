import http from '@/api/index'

/**
 * Fee record API response interface matching backend FeeRecordVO.
 */
export interface FeeRecordVO {
  id: number
  feeType: string
  amount: number
  paidAmount?: number
  dueDate: string
  paidDate?: string
  voucherNo?: string
  status: string
  fundingSource?: string
  ownerType: string
  ownerId: number
  source: string
  slipNo?: string
  slipGeneratedTime?: string
  slipGeneratedBy?: number
  deptId?: number
  createdBy?: number
  createdTime?: string
  updatedBy?: number
  updatedTime?: string

  // Computed fields
  ownerName?: string
  alertLevel?: string
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
 * Fee record paginated listing params.
 */
export interface FeeRecordPageParams {
  page: number
  size: number
  status?: string
  feeType?: string
  fundingSource?: string
  keyword?: string
  dueDateFrom?: string
  dueDateTo?: string
  ownerType?: string
}

/**
 * Fee record create/update DTO.
 */
export interface FeeRecordDTO {
  feeType: string
  amount: number
  paidAmount?: number
  dueDate: string
  voucherNo?: string
  status?: string
  fundingSource?: string
  ownerType: string
  ownerId: number
}

/**
 * Get paginated fee records with multi-dimensional filtering.
 */
export function getPage(params: FeeRecordPageParams) {
  return http.get('/api/fees/page', { params })
}

/**
 * Get a fee record by ID.
 */
export function getById(id: number) {
  return http.get(`/api/fees/${id}`)
}

/**
 * Create a new fee record.
 */
export function create(data: FeeRecordDTO) {
  return http.post('/api/fees', data)
}

/**
 * Update an existing fee record.
 */
export function update(id: number, data: FeeRecordDTO) {
  return http.put(`/api/fees/${id}`, data)
}

/**
 * Delete a fee record (only allowed for paused records).
 */
export function remove(id: number) {
  return http.delete(`/api/fees/${id}`)
}

// ── Batch Payment Operations (02-04 Task 3) ──────────────────────────

/**
 * Batch generate slip numbers for selected pending fee records.
 */
export function batchGenerateSlips(ids: number[]) {
  return http.post('/api/fees/batch-generate-slips', { ids })
}

/**
 * Batch mark fee records as paid.
 */
export function batchPay(data: { ids: number[], paidDate: string, voucherNo: string, slipNo: string }) {
  return http.put('/api/fees/batch-pay', data)
}
