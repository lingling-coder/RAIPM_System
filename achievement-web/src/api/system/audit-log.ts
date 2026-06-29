import http from '@/api/index'

export interface AuditLogPageParams {
  page?: number
  pageSize?: number
  operatorName?: string
  operationType?: string
  targetType?: string
  startTime?: string
  endTime?: string
}

export interface AuditLogVO {
  id: number
  operatorName: string
  operationType: string
  operationName: string
  targetType: string
  targetId: string
  ipAddress: string
  userAgent: string
  status: number
  originalContent: string
  targetContent: string
  previousHash: string
  currentHash: string
  integrityVerified: boolean | null
  createdAt: string
}

export interface ChainBreakVO {
  logId: number
  expectedHash: string
  actualHash: string
  position: string
}

export interface ChainVerificationResult {
  valid: boolean
  brokenLinks: ChainBreakVO[]
  totalChecked: number
}

// Helper type matching the interceptor-unwrapped response shape from api/index.ts
interface ApiResult<T> {
  code: number
  message: string
  data: T
  success: boolean
}

/** Paginated audit log query */
export function page(params: AuditLogPageParams): Promise<ApiResult<{ records: AuditLogVO[]; total: number }>> {
  return http.post('/api/system/audit-log/page', params) as any
}

/** Get audit log detail by ID */
export function getDetail(id: number): Promise<ApiResult<AuditLogVO>> {
  return http.get(`/api/system/audit-log/${id}`) as any
}

/** Verify hash chain integrity for a range of entries */
export function verifyChain(fromId: number, toId: number): Promise<ApiResult<ChainVerificationResult>> {
  return http.post('/api/system/audit-log/verify-chain', { fromId, toId }) as any
}
