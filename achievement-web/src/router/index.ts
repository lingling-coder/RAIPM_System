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
  // ── Approval Management ────────────────────────────────────────
  {
    path: '/approval',
    redirect: '/approval/pending',
    meta: { title: '审批管理', icon: 'Check' },
    children: [
      {
        path: 'pending',
        name: 'ApprovalPending',
        component: () => import('@/views/approval/ApprovalList.vue'),
        meta: { title: '审批待办', icon: 'Clock' },
      },
      {
        path: 'detail/:id',
        name: 'ApprovalDetail',
        component: () => import('@/views/approval/ApprovalDetail.vue'),
        meta: { title: '审批详情', hidden: true },
      },
    ],
  },
  // ── Fee Management (D-02) ────────────────────────────────────
  {
    path: '/fee',
    redirect: '/fee/ledger',
    meta: { title: '费用管理', icon: 'Money' },
    children: [
      {
        path: 'ledger',
        name: 'FeeLedger',
        component: () => import('@/views/fee/FeeLedger.vue'),
        meta: { title: '费用台账' },
      },
      {
        path: 'plan',
        name: 'FeePlan',
        component: () => import('@/views/fee/FeePlan.vue'),
        meta: { title: '缴费计划' },
      },
      {
        path: 'stats',
        name: 'FeeStats',
        component: () => import('@/views/fee/FeeStats.vue'),
        meta: { title: '费用统计' },
      },
      {
        path: 'detail/:id',
        name: 'FeeDetail',
        component: () => import('@/views/fee/FeeDetail.vue'),
        meta: { title: '费用详情', hidden: true },
      },
    ],
  },
  // ── Notification Center ─────────────────────────────────────
  {
    path: '/notification',
    name: 'NotificationCenter',
    component: () => import('@/views/notification/NotificationCenter.vue'),
    meta: { title: '通知中心', hidden: true },
  },
  // ── Batch Import ──────────────────────────────────────────
  {
    path: '/batch-import',
    name: 'BatchImport',
    component: () => import('@/views/batch/BatchImport.vue'),
    meta: { title: '批量导入', icon: 'Upload' },
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
      {
        path: 'reminder-config',
        name: 'SystemReminderConfig',
        component: () => import('@/views/system/reminder-config/index.vue'),
        meta: { title: '提醒配置', icon: 'Bell', permission: 'system:reminder:list' },
      },
      {
        path: 'email-config',
        name: 'SystemEmailConfig',
        component: () => import('@/views/system/email-config/index.vue'),
        meta: { title: '邮件服务配置', icon: 'Message', permission: 'system:email:list' },
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
