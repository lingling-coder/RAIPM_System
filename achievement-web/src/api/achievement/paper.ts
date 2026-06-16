import http from '@/api/index'

export interface PaperFormDTO {
  title: string
  authors: string
  journal: string
  doi?: string
  issn?: string
  volume?: number
  issue?: number
  pages?: string
  publishYear: number
  indexStatus: string
  impactFactor?: number
  zone?: string
  abstractText?: string
  isClassified?: number
  classifiedLevel?: string
  projectRef?: string
}

export interface PaperVO {
  id: number
  title: string
  authors: string
  journal: string
  doi?: string
  issn?: string
  volume?: number
  issue?: number
  pages?: string
  publishYear: number
  indexStatus: string
  impactFactor?: number
  zone?: string
  abstractText?: string
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

export interface DoiLookupResult {
  found: boolean
  doi: string
  title?: string
  authors?: string
  journal?: string
  volume?: number
  issue?: number
  pages?: string
  publishYear?: number
  abstractText?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * Submit paper for approval (DRAFT -> PENDING_DEPT_REVIEW)
 */
export function submit(data: PaperFormDTO) {
  return http.post('/api/papers/submit', data)
}

/**
 * Create paper as draft
 */
export function createDraft(data: PaperFormDTO) {
  return http.post('/api/papers', data)
}

/**
 * Update an existing paper
 */
export function updateDraft(id: number, data: PaperFormDTO) {
  return http.put(`/api/papers/${id}`, data)
}

/**
 * Save current form as draft
 */
export function saveDraft(data: PaperFormDTO) {
  return http.post('/api/papers/draft', data)
}

/**
 * List drafts for current user
 */
export function getDrafts() {
  return http.get('/api/papers/draft')
}

/**
 * Get a specific draft by ID
 */
export function getDraftById(id: number) {
  return http.get(`/api/papers/draft/${id}`)
}

/**
 * Delete a draft
 */
export function deleteDraft(id: number) {
  return http.delete(`/api/papers/draft/${id}`)
}

/**
 * Get a paper by ID
 */
export function getById(id: number) {
  return http.get(`/api/papers/${id}`)
}

/**
 * Paginated paper listing with filters
 */
export function getPage(params: {
  page: number
  size: number
  status?: string
  keyword?: string
}) {
  return http.get('/api/papers/page', { params })
}

/**
 * DOI auto-complete lookup
 */
export function lookupDoi(doi: string) {
  return http.get('/api/doi/lookup', { params: { doi } })
}
