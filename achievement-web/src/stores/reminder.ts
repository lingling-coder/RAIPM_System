import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as reminderApi from '@/api/reminder/reminder-task'

/**
 * Reminder store — manages high-urgency task state and one-time dismiss tracking (D-20, D-21).
 *
 * Provides:
 * - highUrgencyTasks: list of unconfirmed HIGH urgency tasks (pre-filtered by dismissedIds)
 * - dismissedIds: in-memory Set to prevent re-showing dismissed tasks per session
 * - fetchHighUrgencyTasks: fetches from backend, excludes dismissed IDs
 * - dismissLocally: removes task from highUrgencyTasks (local only)
 * - dismissTask: calls backend dismiss API then removes locally
 */
export const useReminderStore = defineStore('reminder', () => {
  const highUrgencyTasks = ref<any[]>([])
  const dismissedIds = ref<Set<number>>(new Set())

  /**
   * Fetch unconfirmed HIGH urgency tasks from the backend.
   * Filters out tasks that have been dismissed in this session.
   */
  async function fetchHighUrgencyTasks() {
    try {
      const res: any = await reminderApi.getHighUrgencyUnconfirmed()
      if (res?.data) {
        highUrgencyTasks.value = (res.data as any[])
          .filter((t: any) => !dismissedIds.value.has(t.id))
      }
    } catch {
      // Non-critical — silent fail
    }
  }

  /**
   * Dismiss a task locally (in-memory only, no API call).
   * Adds the task ID to dismissedIds and removes it from the displayed list.
   */
  function dismissLocally(taskId: number) {
    dismissedIds.value.add(taskId)
    highUrgencyTasks.value = highUrgencyTasks.value.filter(t => t.id !== taskId)
  }

  /**
   * Dismiss a task via the backend API, then update local state.
   */
  async function dismissTask(taskId: number) {
    try {
      await reminderApi.dismissTask(taskId)
      dismissLocally(taskId)
    } catch {
      // Silent — non-critical
    }
  }

  return { highUrgencyTasks, dismissedIds, fetchHighUrgencyTasks, dismissLocally, dismissTask }
})
