import http from '@/api/index'

export interface CopyrightFormDTO {
  name: string
  copyrightHolder: string
  registrationNo: string
  registrationDate: string
  softwareVersion: string
  softwareCategory: string
  isClassified?: number
  classifiedLevel?: string
  projectRef?: string
}

export interface CopyrightVO {
  id: number
  name: string
  copyrightHolder: string
  registrationNo: string
  registrationDate: string
  softwareVersion: string
  softwareCategory: string
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
 * Submit copyright for approval (DRAFT -> PENDING_DEPT_REVIEW)
 *
 * @param id copyright ID to submit (used as query param to match @RequestParam on backend)
 */
export function submit(id: number) {
  return http.post('/api/copyrights/submit', null, { params: { id } })
}

/**
 * Create copyright as draft
 */
export function createDraft(data: CopyrightFormDTO) {
  return http.post('/api/copyrights', data)
}

/**
 * Update an existing copyright
 */
export function updateDraft(id: number, data: CopyrightFormDTO) {
  return http.put(`/api/copyrights/${id}`, data)
}

/**
 * Save current form as draft
 */
export function saveDraft(data: CopyrightFormDTO) {
  return http.post('/api/copyrights/draft', data)
}

/**
 * Get a copyright by ID
 */
export function getById(id: number) {
  return http.get(`/api/copyrights/${id}`)
}

/**
 * Paginated copyright listing with filters
 */
export function getPage(params: {
  page: number
  size: number
  status?: string
  keyword?: string
}) {
  return http.get('/api/copyrights/page', { params })
}
