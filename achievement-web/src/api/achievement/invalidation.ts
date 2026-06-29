import http from '@/api/index'

export interface InvalidationDTO {
  achievementType: string
  achievementId: number
  reason: string
}

export interface InvalidationVO {
  id: number
  achievementType: string
  achievementId: number
  invalidatorName: string
  reason: string
  createdTime: string
}

export interface DuplicateCheckResult {
  duplicate: boolean
  existingId?: number
  existingTitle?: string
  existingType?: string
  existingStatus?: string
  existingSubmitTime?: string
}

/**
 * Invalidate an archived achievement.
 * POST /api/achievement/invalidate
 */
export function invalidate(type: string, id: number, reason: string) {
  return http.post('/api/achievement/invalidate', {
    achievementType: type,
    achievementId: id,
    reason: reason,
  })
}

/**
 * Get invalidation record for a specific achievement.
 * GET /api/achievement/invalidation?type=paper&id=1
 */
export function getInvalidationRecord(type: string, id: number) {
  return http.get('/api/achievement/invalidation', {
    params: { type, id },
  })
}

/**
 * List all invalidation records for the current user.
 * GET /api/achievement/invalidations
 */
export function getInvalidations() {
  return http.get('/api/achievement/invalidations')
}

/**
 * Check for duplicate achievement at submit time.
 * GET /api/achievement/check-duplicate?type=paper&field=10.1234/test&excludeId=42
 *
 * @param type      achievement type (paper/patent/copyright)
 * @param field     unique field value (DOI/applicationNo/registrationNo)
 * @param excludeId optional current record ID to exclude (edit scenarios)
 */
export function checkDuplicate(type: string, field: string, excludeId?: number) {
  const params: Record<string, string | number> = { type, field }
  if (excludeId !== undefined) {
    params.excludeId = excludeId
  }
  return http.get('/api/achievement/check-duplicate', { params })
}
