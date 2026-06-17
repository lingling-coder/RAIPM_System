import http from '@/api/index'

/** SMTP email configuration DTO */
export interface EmailConfigDTO {
  host: string
  port: number
  username: string
  password?: string
  senderName: string
  tls: boolean
  testEmail?: string
}

/** SMTP test connection result */
export interface TestResult {
  success: boolean
  message: string
  responseTimeMs: number
}

/** Get current SMTP configuration */
export function getConfig() {
  return http.get('/api/system/email-config')
}

/** Save/update SMTP configuration */
export function saveConfig(data: EmailConfigDTO) {
  return http.put('/api/system/email-config', data)
}

/** Test SMTP connection by sending test email */
export function testConnection(testEmail: string) {
  return http.post('/api/system/email-config/test', { testEmail })
}
