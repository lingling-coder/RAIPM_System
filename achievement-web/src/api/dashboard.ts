import http from '@/api/index'

/**
 * Annual trend chart data (line chart).
 */
export interface DashboardTrendVO {
  year: number
  achievementType: 'paper' | 'patent' | 'software'
  count: number
}

/**
 * Type distribution chart data (pie chart).
 */
export interface DashboardTypeDistVO {
  achievementType: 'paper' | 'patent' | 'software'
  count: number
  percentage: number
}

/**
 * Department ranking chart data (bar chart).
 */
export interface DashboardDeptRankVO {
  deptName: string
  deptId: number
  paperCount: number
  patentCount: number
  softwareCount: number
  totalCount: number
}

/**
 * Patent status chart data (donut chart).
 */
export interface DashboardPatentStatusVO {
  status: 'VALID' | 'INVALID' | 'UNKNOWN'
  label: string
  count: number
  percentage: number
}

/**
 * Get annual trend data.
 */
export function getAnnualTrend(params?: { deptId?: number; yearFrom?: number; yearTo?: number }) {
  return http.get('/api/dashboard/annual-trend', { params })
}

/**
 * Get type distribution data.
 */
export function getTypeDist(params?: { deptId?: number }) {
  return http.get('/api/dashboard/type-dist', { params })
}

/**
 * Get department ranking data.
 */
export function getDeptRanking(params?: { deptId?: number }) {
  return http.get('/api/dashboard/dept-ranking', { params })
}

/**
 * Get patent status data.
 */
export function getPatentStatus(params?: { deptId?: number }) {
  return http.get('/api/dashboard/patent-status', { params })
}

/**
 * Export chart detail data to Excel.
 * Returns a Blob for download.
 */
export function exportExcel(chartType: string, params?: { deptId?: number }) {
  return http.get(`/api/dashboard/export/${chartType}`, {
    params,
    responseType: 'blob',
  })
}

/**
 * Export chart report to PDF.
 * Returns a Blob for download.
 */
export function exportPdf(chartType: string, params?: { deptId?: number }) {
  return http.get(`/api/dashboard/export-pdf/${chartType}`, {
    params,
    responseType: 'blob',
  })
}
