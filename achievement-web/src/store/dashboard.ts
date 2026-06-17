import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dashboardApi from '@/api/dashboard'
import type {
  DashboardTrendVO,
  DashboardTypeDistVO,
  DashboardDeptRankVO,
  DashboardPatentStatusVO,
} from '@/api/dashboard'

/**
 * Pinia store for dashboard chart data.
 * <p>
 * Manages 4 chart data sets with client-side 5-minute cache (D-04 complement).
 * On fetchAll(), checks lastFetched timestamp — if < 5 min since last fetch,
 * skips API call and returns cached data. The refreshAll() action bypasses
 * the cache check for programmatic refresh.
 */
export const useDashboardStore = defineStore('dashboard', () => {
  // ── State ──
  const annualTrend = ref<DashboardTrendVO[]>([])
  const typeDist = ref<DashboardTypeDistVO[]>([])
  const deptRanking = ref<DashboardDeptRankVO[]>([])
  const patentStatus = ref<DashboardPatentStatusVO[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastFetched = ref<number | null>(null)

  const CACHE_TTL = 5 * 60 * 1000 // 5 minutes

  // ── Actions ──

  /**
   * Fetch all chart data in parallel.
   * Skips API call if data was fetched within the last 5 minutes.
   */
  async function fetchAll(deptId?: number): Promise<void> {
    const now = Date.now()
    if (lastFetched.value && now - lastFetched.value < CACHE_TTL) {
      return
    }

    loading.value = true
    error.value = null
    try {
      const params = deptId !== undefined ? { deptId } : undefined
      const [trendRes, typeRes, deptRes, patentRes] = await Promise.all([
        dashboardApi.getAnnualTrend(params),
        dashboardApi.getTypeDist(params),
        dashboardApi.getDeptRanking(params),
        dashboardApi.getPatentStatus(params),
      ])
      annualTrend.value = (trendRes as any)?.data || []
      typeDist.value = (typeRes as any)?.data || []
      deptRanking.value = (deptRes as any)?.data || []
      patentStatus.value = (patentRes as any)?.data || []
      lastFetched.value = now
    } catch (e) {
      error.value = '数据加载失败'
      console.error('Dashboard data fetch failed:', e)
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch all chart data, bypassing the client-side cache check.
   * Useful for manual refresh scenarios.
   */
  async function refreshAll(deptId?: number): Promise<void> {
    lastFetched.value = null
    await fetchAll(deptId)
  }

  /**
   * Clear the client-side cache timestamp.
   * Next fetchAll() call will re-fetch from API.
   */
  function clearCache(): void {
    lastFetched.value = null
  }

  return {
    // State
    annualTrend,
    typeDist,
    deptRanking,
    patentStatus,
    loading,
    error,
    lastFetched,
    // Actions
    fetchAll,
    refreshAll,
    clearCache,
  }
})
