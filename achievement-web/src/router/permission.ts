import type { RouteLocationNormalized } from 'vue-router'
import router from './index'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

/**
 * Route permission guard.
 * Whitelist: login page is accessible without authentication.
 * All other routes require:
 *   1. A valid access token
 *   2. Loaded user profile (if not yet loaded)
 *   3. Required permission (if route.meta.permission is set)
 */
const whiteList: string[] = ['/login']

router.beforeEach(async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: any) => {
  const userStore = useUserStore()
  const hasToken = userStore.token !== null && userStore.token !== undefined

  if (hasToken) {
    if (to.path === '/login') {
      // Already logged in, redirect to dashboard
      next('/dashboard')
    } else {
      // Fetch user profile if not yet loaded
      if (!userStore.userInfo) {
        try {
          await userStore.fetchUserInfo()
        } catch (error) {
          // Token might be invalid, redirect to login
          userStore.resetState()
          next('/login')
          return
        }
      }

      // Check route permission if defined
      const requiredPermission = to.meta?.permission as string | undefined
      if (requiredPermission && !userStore.hasPermission(requiredPermission)) {
        ElMessage.error('无权限访问此页面')
        next('/dashboard')
        return
      }

      next()
    }
  } else {
    // Not logged in
    if (whiteList.includes(to.path)) {
      next()
    } else {
      // Redirect to login with return URL
      next(`/login?redirect=${to.path}`)
    }
  }
})

/**
 * After each route change hook.
 * Updates document title and scrolls to top.
 */
router.afterEach((to: RouteLocationNormalized) => {
  // Set page title
  const title = to.meta?.title as string
  if (title) {
    document.title = title + ' - 科研成果与知识产权管理系统'
  }
})
