import http from './index'

export interface LoginParams {
  username: string
  password: string
  remember?: boolean
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  deptId: number | null
  deptName: string
  roles: string[]
  permissions: string[]
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  userInfo?: UserInfo
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/**
 * Login with username and password.
 * Returns accessToken + userInfo. Refresh token is set as httpOnly cookie.
 */
export function login(data: LoginParams): Promise<ApiResult<LoginResult>> {
  return http.post('/api/auth/login', data)
}

/**
 * Refresh access token using httpOnly cookie.
 */
export function refresh(): Promise<ApiResult<LoginResult>> {
  return http.post('/api/auth/refresh')
}

/**
 * Logout: blacklists tokens on server and clears cookie.
 */
export function logout(): Promise<ApiResult<null>> {
  return http.post('/api/auth/logout')
}
