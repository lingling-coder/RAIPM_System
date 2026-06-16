import http from '@/api/index'

/**
 * Fee statistics overview response matching backend FeeStatsVO.
 */
export interface FeeStatsVO {
  totalAmount: number
  totalPaid: number
  totalPending: number
  totalOverdue: number
  recordCount: number
  dimensionValue?: string
  dimensionCode?: string
  deptId?: number
  year?: number
  feeType?: string
  fundingSource?: string
}

/**
 * Fee statistics filter parameters.
 */
export interface FeeStatsParams {
  deptId?: number
  year?: number
  patentType?: string
  fundingSource?: string
}

/**
 * Get fee statistics overview summary.
 * Returns 4 metric values: totalAmount, totalPaid, totalPending, totalOverdue, recordCount.
 */
export function getOverview(params?: FeeStatsParams) {
  return http.get('/api/fees/stats/overview', { params })
}

/**
 * Get fee statistics grouped by a dimension.
 * Valid dimensions: dept_id, YEAR(due_date), patent_type, funding_source.
 */
export function getDimensionStats(dimension: string, params?: FeeStatsParams) {
  return http.get('/api/fees/stats/dimension', {
    params: { dimension, ...params },
  })
}

/**
 * Export fee statistics to Excel.
 * Downloads the Excel file with current filter parameters applied.
 */
export function exportExcel(params?: FeeStatsParams) {
  return http.get('/api/fees/stats/export', {
    params,
    responseType: 'blob',
  })
}
