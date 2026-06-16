import http from '@/api/index'

/**
 * Upload and import an Excel file for batch achievement import.
 * POST /api/batch/import
 */
export function importFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/api/batch/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // 2 minutes for large imports
  })
}

/**
 * Download the blank import template Excel file.
 * GET /api/batch/template
 */
export function downloadTemplate() {
  return http.get('/api/batch/template', {
    responseType: 'blob',
  })
}

/**
 * Download the error report for a specific import operation.
 * GET /api/batch/error-report/{importRecordId}
 */
export function getErrorReport(importRecordId: number) {
  return http.get(`/api/batch/error-report/${importRecordId}`, {
    responseType: 'blob',
  })
}

/**
 * Get import history for the current user.
 * GET /api/batch/records
 */
export function getImportRecords() {
  return http.get('/api/batch/records')
}
