import http from '@/api/index'

export interface DictCategoryData {
  id?: number
  categoryName: string
  categoryCode: string
  description?: string
  sortOrder?: number
  status?: number
}

export interface DictEntryData {
  id?: number
  categoryId: number
  categoryName?: string
  dictKey: string
  dictValue: string
  sortOrder?: number
  status?: number
}

export interface DictEntryPageParams {
  page?: number
  pageSize?: number
  categoryId?: number | null
  keyword?: string
}

/** List all dictionary categories */
export function listCategories() {
  return http.get('/api/system/dict-category/list')
}

/** Create dictionary category */
export function createCategory(data: DictCategoryData) {
  return http.post('/api/system/dict-category', data)
}

/** Update dictionary category */
export function updateCategory(id: number, data: DictCategoryData) {
  return http.put(`/api/system/dict-category/${id}`, data)
}

/** Delete dictionary category */
export function removeCategory(id: number) {
  return http.delete(`/api/system/dict-category/${id}`)
}

/** Paginated dictionary entries */
export function pageEntries(params: DictEntryPageParams) {
  return http.post('/api/system/dict-entry/page', params)
}

/** Create dictionary entry */
export function createEntry(data: DictEntryData) {
  return http.post('/api/system/dict-entry', data)
}

/** Update dictionary entry */
export function updateEntry(id: number, data: DictEntryData) {
  return http.put(`/api/system/dict-entry/${id}`, data)
}

/** Delete dictionary entry */
export function removeEntry(id: number) {
  return http.delete(`/api/system/dict-entry/${id}`)
}
