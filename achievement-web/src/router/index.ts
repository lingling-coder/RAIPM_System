import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'

// ── Route Definitions ────────────────────────────────────────────────────
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', noLayout: true },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '首页', icon: 'HomeFilled', permission: 'dashboard' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
    meta: { title: '个人中心', icon: 'User', permission: 'profile' },
  },
  // ── Achievement Management ────────────────────────────────────────
  {
    path: '/achievement',
    redirect: '/achievement/register',
    meta: { title: '成果管理', icon: 'Document' },
    children: [
      {
        path: 'register',
        name: 'AchievementRegister',
        component: () => import('@/views/achievement/AchievementRegister.vue'),
        meta: { title: '成果登记', icon: 'Edit' },
      },
      {
        path: 'list',
        name: 'AchievementList',
        component: () => import('@/views/achievement/AchievementList.vue'),
        meta: { title: '成果列表', icon: 'List' },
      },
      {
        path: 'detail/:id',
        name: 'AchievementDetail',
        component: () => import('@/views/achievement/AchievementDetail.vue'),
        meta: { title: '成果详情', hidden: true },
      },
    ],
  },
  // ── System Management ──────────────────────────────────────────────
  {
    path: '/system',
    redirect: '/system/user',
    meta: { title: '系统管理', permission: 'system' },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', permission: 'system:user:list' },
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar', permission: 'system:role:list' },
      },
      {
        path: 'department',
        name: 'SystemDepartment',
        component: () => import('@/views/system/department/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', permission: 'system:dept:list' },
      },
      {
        path: 'dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '数据字典', icon: 'Notebook', permission: 'system:dict:list' },
      },
      {
        path: 'audit-log',
        name: 'SystemAuditLog',
        component: () => import('@/views/system/audit-log/index.vue'),
        meta: { title: '审计日志', icon: 'Clock', permission: 'system:audit:list' },
      },
      {
        path: 'api-config',
        name: 'SystemApiConfig',
        component: () => import('@/views/system/api-config/index.vue'),
        meta: { title: 'API集成配置', icon: 'Setting', permission: 'system:api:list' },
      },
    ],
  },
]

// ── Router Instance ─────────────────────────────────────────────────────
const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// ── Navigation Guards (delegated to permission.ts) ──────────────────────
router.beforeEach((_to, _from, next) => {
  NProgress.start()
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
