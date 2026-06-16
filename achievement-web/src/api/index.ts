import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { useUserStore } from '@/store/user'
import router from '@/router'
import { ElMessage } from 'element-plus'

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL as string || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
  },
})

// ── Request Interceptor ──────────────────────────────────────────────────
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ── Response Interceptor ─────────────────────────────────────────────────
http.interceptors.response.use(
  (response: AxiosResponse) => {
    // Unwrap response data
    const data = response.data
    // If the response has code/data structure, unwrap it
    if (data && data.code !== undefined) {
      if (data.code === 200) {
        return data
      }
      // Handle business errors
      ElMessage.error(data.message || 'Business error')
      return Promise.reject(new Error(data.message || 'Business error'))
    }
    return data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          // Unauthorized - redirect to login
          const userStore = useUserStore()
          userStore.resetState()
          router.push('/login')
          break
        case 403:
          ElMessage.error('Forbidden: insufficient permissions')
          break
        case 404:
          ElMessage.error('Resource not found')
          break
        case 500:
          ElMessage.error('Server error')
          break
        default:
          ElMessage.error(error.message || 'Request error')
      }
    } else {
      ElMessage.error('Network error: unable to connect to server')
    }
    return Promise.reject(error)
  }
)

export default http
