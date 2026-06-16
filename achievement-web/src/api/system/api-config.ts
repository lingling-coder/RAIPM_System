import http from '@/api/index'

export interface ApiConfigDTO {
  id?: number
  configName: string
  configCode: string
  endpointUrl: string
  description?: string
  authType?: string
  apiKey?: string
  secretKey?: string
  tokenUrl?: string
  connectTimeout?: number
  readTimeout?: number
  retryCount?: number
  retryInterval?: number
  backoffStrategy?: string
  failureAlert?: number
  status?: number
  lastTestTime?: string
  lastTestResult?: number
  createdAt?: string
  createdBy?: string
  updatedAt?: string
  updatedBy?: string
}

export interface TestConnectionResultVO {
  success: boolean
  message: string
  responseTimeMs: number
  statusCode: number | null
}

export interface PageParams {
  page?: number
  pageSize?: number
}

/** Get paginated list of API configurations */
export function page(params: PageParams) {
  return http.post('/api/system/api-config/page', params)
}

/** Get single API configuration by ID */
export function getById(id: number) {
  return http.get(`/api/system/api-config/${id}`)
}

/** Create new API configuration */
export function create(data: ApiConfigDTO) {
  return http.post('/api/system/api-config', data)
}

/** Update existing API configuration */
export function update(id: number, data: ApiConfigDTO) {
  return http.put(`/api/system/api-config/${id}`, data)
}

/** Delete API configuration */
export function remove(id: number) {
  return http.delete(`/api/system/api-config/${id}`)
}

/** Test API connection */
export function testConnection(id: number) {
  return http.post(`/api/system/api-config/${id}/test`)
}
