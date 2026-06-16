import http from '@/api/index'

export interface PatentFormDTO {
  patentName: string
  inventors: string
  applicationNo: string
  authorizationNo?: string
  applicationDate: string
  authorizationDate?: string
  patentType: string
  country: string
  nextFeeDate?: string
  legalStatus: string
  isClassified?: number
  classifiedLevel?: string
  projectRef?: string
}

export interface PatentVO {
  id: number
  patentName: string
  inventors: string
  applicationNo: string
  authorizationNo?: string
  applicationDate: string
  authorizationDate?: string
  patentType: string
  country: string
  nextFeeDate?: string
  legalStatus: string
  isClassified?: number
  classifiedLevel?: string
  projectRef?: string
  status: string
  statusLabel: string
  archiveNo?: string
  deptId?: number
  createdBy?: number
  createdTime?: string
  updatedBy?: number
  updatedTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * Submit patent for approval (DRAFT -> PENDING_DEPT_REVIEW)
 */
export function submit(data: PatentFormDTO) {
  return http.post('/api/patents/submit', data)
}

/**
 * Create patent as draft
 */
export function createDraft(data: PatentFormDTO) {
  return http.post('/api/patents', data)
}

/**
 * Update an existing patent
 */
export function updateDraft(id: number, data: PatentFormDTO) {
  return http.put(`/api/patents/${id}`, data)
}

/**
 * Save current form as draft
 */
export function saveDraft(data: PatentFormDTO) {
  return http.post('/api/patents/draft', data)
}

/**
 * Get a patent by ID
 */
export function getById(id: number) {
  return http.get(`/api/patents/${id}`)
}

/**
 * Paginated patent listing with filters
 */
export function getPage(params: {
  page: number
  size: number
  status?: string
  keyword?: string
}) {
  return http.get('/api/patents/page', { params })
}
