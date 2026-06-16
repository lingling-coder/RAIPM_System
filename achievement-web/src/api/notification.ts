import http from '@/api/index'

/**
 * List notifications by type with pagination.
 */
export function getList(type: string, page: number, size: number) {
  return http.get('/api/notifications', {
    params: { type, page, size },
  })
}

/**
 * Get unread notification count.
 */
export function getUnreadCount() {
  return http.get('/api/notifications/unread-count')
}

/**
 * Mark a single notification as read.
 */
export function markAsRead(id: number) {
  return http.put(`/api/notifications/${id}/read`)
}

/**
 * Mark all notifications as read for current user.
 */
export function markAllAsRead() {
  return http.put('/api/notifications/read-all')
}
