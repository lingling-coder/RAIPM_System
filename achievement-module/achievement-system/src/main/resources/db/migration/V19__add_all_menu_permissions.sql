-- ============================================================
-- V19: Add application menu permissions for all 7 roles
--
-- Adds the following menu tree entries:
--   成果管理 (achievement management)
--   审批管理 (approval management)
--   费用管理 (fee management)
--   批量导入 (batch import)
--
-- And assigns appropriate permissions to each role.
-- ============================================================

-- ── Step 1: Add parent menus (level 0, type=0) ──────────────

INSERT IGNORE INTO sys_menu (id, parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(24, 0, '成果管理',       NULL,                    '/achievement', NULL,                 0, 'Document',  3, NOW()),
(25, 0, '审批管理',       NULL,                    '/approval',    NULL,                 0, 'Check',     4, NOW()),
(26, 0, '费用管理',       NULL,                    '/fee',         NULL,                 0, 'Money',     5, NOW());

-- ── Step 2: Add child menu items (type=1) ────────────────────

-- 成果管理 children
INSERT IGNORE INTO sys_menu (id, parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(27, 24, '成果登记',       'achievement:register',   '/achievement/register', '/achievement/AchievementRegister.vue', 1, 'Edit',  1, NOW()),
(28, 24, '成果列表',       'achievement:list',       '/achievement/list',     '/achievement/AchievementList.vue',     1, 'List',  2, NOW()),
(29, 24, '成果详情',       'achievement:detail',     '/achievement/detail',   '/achievement/AchievementDetail.vue',   1, NULL,    3, NOW());

-- 审批管理 children
INSERT IGNORE INTO sys_menu (id, parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(30, 25, '审批待办',       'approval:pending',       '/approval/pending',     '/views/approval/ApprovalList.vue',     1, 'Clock', 1, NOW()),
(31, 25, '审批详情',       'approval:detail',        '/approval/detail',      '/views/approval/ApprovalDetail.vue',   1, NULL,    2, NOW());

-- 费用管理 children
INSERT IGNORE INTO sys_menu (id, parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(32, 26, '费用台账',       'fee:ledger',             '/fee/ledger',           '/views/fee/FeeLedger.vue',             1, NULL,    1, NOW()),
(33, 26, '缴费计划',       'fee:plan',               '/fee/plan',             '/views/fee/FeePlan.vue',               1, NULL,    2, NOW()),
(34, 26, '费用统计',       'fee:stats',              '/fee/stats',            '/views/fee/FeeStats.vue',              1, NULL,    3, NOW()),
(35, 26, '费用详情',       'fee:detail',             '/fee/detail',           '/views/fee/FeeDetail.vue',             1, NULL,    4, NOW());

-- ── Step 3: Standalone menu items (type=1, parent_id=0) ──────

INSERT IGNORE INTO sys_menu (id, parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(36, 0,  '批量导入',       'achievement:batch-import', '/batch-import',       '/views/batch/BatchImport.vue',         1, 'Upload', 6, NOW()),
(37, 0,  '通知中心',       'notification',           '/notification',         '/views/notification/NotificationCenter.vue', 1, NULL, 7, NOW()),
(38, 0,  '搜索',           'achievement:search',     '/search',               '/views/search/SearchResults.vue',      1, NULL,    8, NOW()),
(39, 0,  '个人中心',       'profile',                '/profile',              '/views/profile/index.vue',             1, 'User',   9, NOW());

-- ── Step 4: Assign permissions to roles ───────────────────────

-- Role ID references (role_id from sys_role):
--   1 = ROLE_RESEARCHER
--   2 = ROLE_DEPT_SECRETARY
--   3 = ROLE_DEPT_ADMIN
--   4 = ROLE_LEADER
--   5 = ROLE_CLASSIFIED_ADMIN
--   6 = ROLE_AUDITOR
--   7 = ROLE_SYSTEM_ADMIN

-- ROLE_RESEARCHER (id=1): personal achievement management only
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.permission IN (
  'dashboard',
  'achievement:register',
  'achievement:list',
  'achievement:detail',
  'notification',
  'achievement:search',
  'profile'
);

-- ROLE_DEPT_SECRETARY (id=2): full department management
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, m.id FROM sys_menu m WHERE m.permission IN (
  'dashboard',
  'achievement:register',
  'achievement:list',
  'achievement:detail',
  'approval:pending',
  'approval:detail',
  'fee:ledger',
  'fee:plan',
  'fee:stats',
  'fee:detail',
  'achievement:batch-import',
  'notification',
  'achievement:search',
  'profile'
);

-- ROLE_DEPT_ADMIN (id=3): department read-only + approvals
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 3, m.id FROM sys_menu m WHERE m.permission IN (
  'dashboard',
  'achievement:list',
  'achievement:detail',
  'approval:pending',
  'approval:detail',
  'notification',
  'achievement:search',
  'profile'
);

-- ROLE_LEADER (id=4): institute-wide view + stats
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 4, m.id FROM sys_menu m WHERE m.permission IN (
  'dashboard',
  'achievement:list',
  'achievement:detail',
  'approval:pending',
  'approval:detail',
  'fee:stats',
  'notification',
  'achievement:search',
  'profile'
);

-- ROLE_CLASSIFIED_ADMIN (id=5): classified achievement management
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 5, m.id FROM sys_menu m WHERE m.permission IN (
  'dashboard',
  'achievement:list',
  'achievement:detail',
  'approval:pending',
  'approval:detail',
  'notification',
  'achievement:search',
  'profile'
);

-- ROLE_AUDITOR (id=6): read-only audit access
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 6, m.id FROM sys_menu m WHERE m.permission IN (
  'dashboard',
  'achievement:list',
  'achievement:detail',
  'approval:pending',
  'approval:detail',
  'fee:ledger',
  'fee:stats',
  'fee:detail',
  'notification',
  'achievement:search',
  'profile'
);

-- ROLE_SYSTEM_ADMIN (id=7): all permissions (already has system menus)
-- Assign all NEW menu items (id >= 24)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 7, m.id FROM sys_menu m WHERE m.id >= 24;
