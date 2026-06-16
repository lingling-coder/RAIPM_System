import http from '@/api/index'

/**
 * Upload an attachment file
 * POST /api/attachments/upload (multipart)
 */
export function upload(file: File, achievementType: string, achievementId: number) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', achievementType)
  formData.append('typeId', String(achievementId))
  return http.post('/api/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * List attachments for a specific achievement
 */
export function getAttachments(achievementType: string, achievementId: number) {
  return http.get('/api/attachments', {
    params: { type: achievementType, typeId: achievementId },
  })
}

/**
 * Download an attachment file
 */
export function download(id: number) {
  return http.get(`/api/attachments/${id}/download`, { responseType: 'blob' })
}

/**
 * Delete an attachment
 */
export function deleteAttachment(id: number) {
  return http.delete(`/api/attachments/${id}`)
}
