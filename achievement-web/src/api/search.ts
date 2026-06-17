import http from '@/api/index'

/** Search query parameters */
export interface SearchQueryDTO {
  keyword: string
  type?: 'paper' | 'patent' | 'software'
  deptId?: number
  yearFrom?: number
  yearTo?: number
  classification?: 'NORMAL' | 'CLASSIFIED'
  page: number
  size: number
}

/** A single highlight range in a text field */
export interface HighlightRange {
  field: 'title' | 'authors'
  start: number
  end: number
}

/** A single search result entry */
export interface SearchResultVO {
  id: number
  title: string
  achievementType: 'paper' | 'patent' | 'software'
  status: string
  deptName: string
  authors?: string
  publishYear?: number
  relevanceScore: number
  highlightRanges: HighlightRange[]
  isClassified?: boolean
}

/** Paginated search response */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

/**
 * Execute a full-text search across all achievement types.
 * Returns paginated results sorted by relevance score.
 */
export function search(params: SearchQueryDTO) {
  return http.get('/api/search', { params })
}
