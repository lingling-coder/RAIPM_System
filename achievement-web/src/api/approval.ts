import http from '@/api/index'

export interface ApprovalActionDTO {
  achievementType: string
  achievementId: number
  action?: string
  comment?: string
  archiveNo?: string
}

export interface ApprovalRecordVO {
  id: number
  action: string
  actionLabel: string
  operatorName: string
  comment?: string
  fromStatus: string
  fromStatusLabel: string
  toStatus: string
  toStatusLabel: string
  createdTime: string
}

/**
 * Submit an achievement for approval.
 */
export function submit(type: string, id: number) {
  return http.post('/api/approval/submit', {
    achievementType: type,
    achievementId: id,
  })
}

/**
 * Approve an achievement (dept secretary or admin archive).
 */
export function approve(type: string, id: number, comment?: string, archiveNo?: string) {
  return http.post('/api/approval/approve', {
    achievementType: type,
    achievementId: id,
    comment: comment || '',
    archiveNo: archiveNo || '',
  })
}

/**
 * Reject an achievement during approval.
 */
export function reject(type: string, id: number, comment: string) {
  return http.post('/api/approval/reject', {
    achievementType: type,
    achievementId: id,
    comment: comment,
  })
}

/**
 * Withdraw a submitted achievement (submitter only).
 */
export function withdraw(type: string, id: number) {
  return http.post('/api/approval/withdraw', {
    achievementType: type,
    achievementId: id,
  })
}

/**
 * Get paginated pending approvals.
 */
export function getPending(params: {
  page: number
  size: number
  type?: string
  dateRange?: string
}) {
  return http.get('/api/approval/pending', { params })
}

/**
 * Get approval history for an achievement.
 */
export function getHistory(type: string, id: number) {
  return http.get('/api/approval/history', {
    params: { type, id },
  })
}
