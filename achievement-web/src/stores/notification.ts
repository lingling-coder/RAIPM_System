import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as notificationApi from '@/api/notification'

/**
 * Notification state store.
 * Manages unread count with 30-second polling.
 */
export const useNotificationStore = defineStore('notification', () => {
  // ── State ──────────────────────────────────────────────────────────
  const unreadCount = ref(0)
  let pollingTimer: ReturnType<typeof setInterval> | null = null

  // ── Actions ────────────────────────────────────────────────────────

  /**
   * Start polling unread count every 30 seconds.
   */
  function startPolling() {
    fetchUnreadCount()
    if (pollingTimer === null) {
      pollingTimer = setInterval(fetchUnreadCount, 30000)
    }
  }

  /**
   * Stop polling.
   */
  function stopPolling() {
    if (pollingTimer !== null) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  /**
   * Fetch unread count from API.
   */
  async function fetchUnreadCount() {
    try {
      const res: any = await notificationApi.getUnreadCount()
      if (res?.data !== undefined) {
        unreadCount.value = res.data
      }
    } catch {
      // Silent fail — keep current count
    }
  }

  /**
   * Mark a notification as read and refresh count from server.
   */
  async function markAsRead(id: number) {
    try {
      await notificationApi.markAsRead(id)
      // Re-fetch from server for accuracy instead of local decrement
      await fetchUnreadCount()
    } catch {
      // Silent fail
    }
  }

  return {
    unreadCount,
    startPolling,
    stopPolling,
    fetchUnreadCount,
    markAsRead,
  }
})
