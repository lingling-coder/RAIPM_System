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
let isRefreshing = false
let failedQueue: Array<{
  resolve: (value: any) => void
  reject: (reason?: any) => void
}> = []

function processQueue(error: any, token: string | null = null) {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

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
      ElMessage.error(data.message || '业务处理失败')
      return Promise.reject(new Error(data.message || '业务处理失败'))
    }
    return data
  },
  async (error) => {
    const originalRequest = error.config

    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue the request while refresh is in progress
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers['Authorization'] = `Bearer ${token}`
          return http(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // Try to refresh the token
        const response = await axios.post('/api/auth/refresh', {}, {
          baseURL: import.meta.env.VITE_API_BASE_URL as string || '/api',
        })

        if (response.data?.code === 200 && response.data?.data?.accessToken) {
          const newToken = response.data.data.accessToken
          const userStore = useUserStore()
          userStore.setToken(newToken)

          processQueue(null, newToken)

          // Retry original request with new token
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`
          return http(originalRequest)
        }
      } catch (refreshError) {
        processQueue(refreshError, null)

        // Refresh failed, redirect to login
        const userStore = useUserStore()
        userStore.resetState()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 403:
          ElMessage.error('无权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          if (status !== 401) {
            ElMessage.error(error.message || '请求失败')
          }
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络后重试')
    }

    return Promise.reject(error)
  }
)

export default http
