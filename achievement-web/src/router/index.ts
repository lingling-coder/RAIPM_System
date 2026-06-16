import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'

// ── Route Definitions ────────────────────────────────────────────────────
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/system/user',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: 'Login', noLayout: true },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: 'Dashboard', icon: 'HomeFilled', permission: 'dashboard' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
    meta: { title: 'Personal Center', icon: 'User', permission: 'profile' },
  },
  // ── System Management ──────────────────────────────────────────────
  {
    path: '/system',
    redirect: '/system/user',
    meta: { title: 'System Management', permission: 'system' },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: 'User Management', icon: 'UserFilled', permission: 'system:user:list' },
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: 'Role Management', icon: 'Avatar', permission: 'system:role:list' },
      },
      {
        path: 'department',
        name: 'SystemDepartment',
        component: () => import('@/views/system/department/index.vue'),
        meta: { title: 'Department Management', icon: 'OfficeBuilding', permission: 'system:dept:list' },
      },
      {
        path: 'dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: 'Data Dictionary', icon: 'Notebook', permission: 'system:dict:list' },
      },
      {
        path: 'audit-log',
        name: 'SystemAuditLog',
        component: () => import('@/views/system/audit-log/index.vue'),
        meta: { title: 'Audit Log', icon: 'Clock', permission: 'system:audit:list' },
      },
      {
        path: 'api-config',
        name: 'SystemApiConfig',
        component: () => import('@/views/system/api-config/index.vue'),
        meta: { title: 'API Config', icon: 'Setting', permission: 'system:api:list' },
      },
    ],
  },
]

// ── Router Instance ─────────────────────────────────────────────────────
const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// ── Navigation Guards ───────────────────────────────────────────────────
router.beforeEach((_to, _from, next) => {
  NProgress.start()
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
