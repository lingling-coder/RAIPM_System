import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as approvalApi from '@/api/approval'
import { ElMessage } from 'element-plus'

/**
 * Approval state store.
 * Manages pending count, filters, and approval actions.
 */
export const useApprovalStore = defineStore('approval', () => {
  // ── State ──────────────────────────────────────────────────────────
  const pendingCount = ref(0)
  const filters = ref<{
    type: string | null
    dateRange: [string, string] | null
    keyword: string
  }>({
    type: null,
    dateRange: null,
    keyword: '',
  })

  // ── Actions ────────────────────────────────────────────────────────

  /**
   * Fetch pending approval count.
   */
  async function fetchPendingCount() {
    try {
      const res: any = await approvalApi.getPending({ page: 1, size: 1 })
      if (res?.data?.total !== undefined) {
        pendingCount.value = res.data.total
      }
    } catch {
      // Silent fail
    }
  }

  /**
   * Approve an achievement.
   */
  async function approveAction(id: number, type: string, comment?: string, archiveNo?: string) {
    const res: any = await approvalApi.approve(type, id, comment, archiveNo)
    if (res?.code === 200) {
      ElMessage.success(res.message || '审批通过')
    }
    return res
  }

  /**
   * Reject an achievement.
   */
  async function rejectAction(id: number, type: string, reason: string) {
    const res: any = await approvalApi.reject(type, id, reason)
    if (res?.code === 200) {
      ElMessage.warning(res.message || '已退回至提交人')
    }
    return res
  }

  return {
    pendingCount,
    filters,
    fetchPendingCount,
    approve: approveAction,
    reject: rejectAction,
  }
})
