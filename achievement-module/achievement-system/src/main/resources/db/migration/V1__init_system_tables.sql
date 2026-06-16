-- ============================================================
-- V1: Initialize System Tables
-- Phase 0 Foundation: User/RBAC/Department/Dictionary tables
-- ============================================================

-- ── Department (flat structure, no hierarchy per D-08) ──────
CREATE TABLE IF NOT EXISTS sys_department (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_name   VARCHAR(100) NOT NULL               COMMENT 'Department name',
    dept_code   VARCHAR(50)  NOT NULL               COMMENT 'Department code',
    leader      VARCHAR(100)                         COMMENT 'Department head',
    phone       VARCHAR(20)                          COMMENT 'Contact phone',
    status      TINYINT      DEFAULT 1               COMMENT '1=normal, 0=disabled',
    deleted     TINYINT      DEFAULT 0               COMMENT 'Soft delete flag (1=deleted)',
    created_at  DATETIME                             COMMENT 'Creation time',
    created_by  VARCHAR(50)                          COMMENT 'Creator',
    updated_at  DATETIME                             COMMENT 'Update time',
    updated_by  VARCHAR(50)                          COMMENT 'Updater',
    UNIQUE KEY uk_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Department table';

-- ── Menu (RBAC permission tree) ─────────────────────────────
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT       DEFAULT 0               COMMENT 'Parent menu ID (0=root)',
    name        VARCHAR(100) NOT NULL                COMMENT 'Menu display name',
    permission  VARCHAR(200)                         COMMENT 'Permission identifier e.g. system:user:list',
    path        VARCHAR(200)                         COMMENT 'Route path',
    component   VARCHAR(200)                         COMMENT 'Vue component path',
    type        TINYINT      NOT NULL                COMMENT '0=directory, 1=menu, 2=button',
    icon        VARCHAR(100)                         COMMENT 'Element Plus icon name',
    sort_order  INT          DEFAULT 0               COMMENT 'Sort order',
    status      TINYINT      DEFAULT 1               COMMENT '1=visible, 0=hidden',
    created_at  DATETIME                             COMMENT 'Creation time',
    updated_at  DATETIME                             COMMENT 'Update time',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Menu/Permission table';

-- ── Role (7 roles per SYS-01) ───────────────────────────────
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50)  NOT NULL                COMMENT 'Role display name',
    role_code   VARCHAR(50)  NOT NULL                COMMENT 'Role code e.g. ROLE_ADMIN',
    description VARCHAR(500)                         COMMENT 'Role description',
    status      TINYINT      DEFAULT 1               COMMENT '1=normal, 0=disabled',
    deleted     TINYINT      DEFAULT 0               COMMENT 'Soft delete flag',
    created_at  DATETIME                             COMMENT 'Creation time',
    created_by  VARCHAR(50)                          COMMENT 'Creator',
    updated_at  DATETIME                             COMMENT 'Update time',
    updated_by  VARCHAR(50)                          COMMENT 'Updater',
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role table (7 default roles)';

-- ── User (self-contained account per D-06/D-19/D-20) ────────
CREATE TABLE IF NOT EXISTS sys_user (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    username                VARCHAR(50)  NOT NULL                COMMENT 'Login username (employee ID)',
    password                VARCHAR(255) NOT NULL                COMMENT 'BCrypt hashed password',
    real_name               VARCHAR(100)                         COMMENT 'Real name',
    email                   VARCHAR(100)                         COMMENT 'Email address',
    phone                   VARCHAR(20)                          COMMENT 'Phone number',
    dept_id                 BIGINT                               COMMENT 'FK to sys_department.id',
    status                  TINYINT      DEFAULT 1               COMMENT '1=normal, 0=disabled (D-19)',
    deleted                 TINYINT      DEFAULT 0               COMMENT 'Soft delete flag (D-20)',
    lockout_until           DATETIME                             COMMENT 'Account lockout expiration (D-15)',
    login_failures          INT          DEFAULT 0               COMMENT 'Consecutive login failures (D-15)',
    last_login_ip           VARCHAR(50)                          COMMENT 'Last login IP',
    last_login_time         DATETIME                             COMMENT 'Last login time',
    password_change_required TINYINT     DEFAULT 1               COMMENT 'Force password change on login (D-13)',
    auth_source             VARCHAR(20)  DEFAULT 'LOCAL'         COMMENT 'Authentication source: LOCAL, LDAP, OAUTH',
    external_ref_id         VARCHAR(200)                         COMMENT 'External system reference ID',
    created_at              DATETIME                             COMMENT 'Creation time',
    created_by              VARCHAR(50)                          COMMENT 'Creator',
    updated_at              DATETIME                             COMMENT 'Update time',
    updated_by              VARCHAR(50)                          COMMENT 'Updater',
    UNIQUE KEY uk_username (username),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

-- ── User-Role mapping (multi-role per D-17) ─────────────────
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL                     COMMENT 'FK to sys_user.id',
    role_id     BIGINT NOT NULL                     COMMENT 'FK to sys_role.id',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-Role association (multi-role)';

-- ── Role-Menu mapping (RBAC) ────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT NOT NULL                     COMMENT 'FK to sys_role.id',
    menu_id     BIGINT NOT NULL                     COMMENT 'FK to sys_menu.id',
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-Menu permission mapping';

-- ── Dictionary Category (D-10 left tree) ────────────────────
CREATE TABLE IF NOT EXISTS sys_dict_category (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(100) NOT NULL            COMMENT 'Category display name',
    category_code   VARCHAR(50)  NOT NULL            COMMENT 'Category code',
    description     VARCHAR(500)                     COMMENT 'Category description',
    sort_order      INT          DEFAULT 0           COMMENT 'Sort order',
    status          TINYINT      DEFAULT 1           COMMENT '1=normal, 0=disabled',
    created_at      DATETIME                         COMMENT 'Creation time',
    created_by      VARCHAR(50)                      COMMENT 'Creator',
    updated_at      DATETIME                         COMMENT 'Update time',
    updated_by      VARCHAR(50)                      COMMENT 'Updater',
    UNIQUE KEY uk_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Data dictionary category';

-- ── Dictionary Entry (D-10 right table) ─────────────────────
CREATE TABLE IF NOT EXISTS sys_dict_entry (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT       NOT NULL                COMMENT 'FK to sys_dict_category.id',
    dict_key    VARCHAR(100) NOT NULL                COMMENT 'Dictionary key',
    dict_value  VARCHAR(200) NOT NULL                COMMENT 'Dictionary value',
    sort_order  INT          DEFAULT 0               COMMENT 'Sort order',
    status      TINYINT      DEFAULT 1               COMMENT '1=normal, 0=disabled',
    created_at  DATETIME                             COMMENT 'Creation time',
    created_by  VARCHAR(50)                          COMMENT 'Creator',
    updated_at  DATETIME                             COMMENT 'Update time',
    updated_by  VARCHAR(50)                          COMMENT 'Updater',
    UNIQUE KEY uk_category_key (category_id, dict_key),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Data dictionary entries';

-- ============================================================
-- Seed Data
-- ============================================================

-- ── Default Roles (7 roles per SYS-01) ──────────────────────
INSERT INTO sys_role (role_name, role_code, description, status, created_at, created_by) VALUES
('科研人员',       'ROLE_RESEARCHER',    'Register and maintain personal achievements, view statistics',   1, NOW(), 'SYSTEM'),
('科研秘书',       'ROLE_SECRETARY',     'Review department achievements, manage fees and transformations', 1, NOW(), 'SYSTEM'),
('部门管理员',     'ROLE_DEPT_ADMIN',    'Assist secretary with department permissions and data',           1, NOW(), 'SYSTEM'),
('主管/院领导',    'ROLE_LEADER',        'View institute-wide statistics and decision analysis',            1, NOW(), 'SYSTEM'),
('涉密成果管理员', 'ROLE_CLASSIFIED',    'Manage classified achievement workflows',                        1, NOW(), 'SYSTEM'),
('内审/审计人员',  'ROLE_AUDITOR',       'Read-only access to audit logs, ledgers, approvals',             1, NOW(), 'SYSTEM'),
('系统管理员',     'ROLE_SYSTEM_ADMIN',  'Global configuration, operations, permission management',        1, NOW(), 'SYSTEM');

-- ── Base Menu Tree ──────────────────────────────────────────
INSERT INTO sys_menu (parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
-- Directory: Dashboard
(0, '首页',           'dashboard',           '/dashboard',   '/dashboard/index.vue',      0, 'HomeFilled',    1, NOW()),
-- Directory: System Management
(0, '系统管理',       NULL,                  '/system',      NULL,                         0, 'Setting',       2, NOW());

-- System Management children (P-03 through P-08)
SET @sys_mgmt = (SELECT id FROM sys_menu WHERE name = '系统管理' AND parent_id = 0);

INSERT INTO sys_menu (parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(@sys_mgmt, '用户管理',     'system:user:list',     '/system/user',     '/system/user/index.vue',     1, 'UserFilled',       1, NOW()),
(@sys_mgmt, '角色管理',     'system:role:list',     '/system/role',     '/system/role/index.vue',     1, 'Avatar',           2, NOW()),
(@sys_mgmt, '部门管理',     'system:dept:list',     '/system/department','/system/department/index.vue',1, 'OfficeBuilding',   3, NOW()),
(@sys_mgmt, '数据字典',     'system:dict:list',     '/system/dict',     '/system/dict/index.vue',     1, 'Notebook',         4, NOW()),
(@sys_mgmt, '审计日志',     'system:audit:list',    '/system/audit-log','/system/audit-log/index.vue', 1, 'Clock',            5, NOW()),
(@sys_mgmt, 'API集成配置',  'system:api:list',      '/system/api-config','/system/api-config/index.vue',1, 'Setting',          6, NOW());

-- Button-level permissions for User Management
SET @user_menu = (SELECT id FROM sys_menu WHERE permission = 'system:user:list' AND parent_id = @sys_mgmt);
INSERT INTO sys_menu (parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(@user_menu, '新增用户',  'system:user:create',     NULL, NULL, 2, NULL, 1, NOW()),
(@user_menu, '编辑用户',  'system:user:update',     NULL, NULL, 2, NULL, 2, NOW()),
(@user_menu, '删除用户',  'system:user:delete',     NULL, NULL, 2, NULL, 3, NOW()),
(@user_menu, '导入用户',  'system:user:import',     NULL, NULL, 2, NULL, 4, NOW()),
(@user_menu, '重置密码',  'system:user:reset-pwd',  NULL, NULL, 2, NULL, 5, NOW());

-- Button-level permissions for Role Management
SET @role_menu = (SELECT id FROM sys_menu WHERE permission = 'system:role:list' AND parent_id = @sys_mgmt);
INSERT INTO sys_menu (parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(@role_menu, '新增角色',         'system:role:create',  NULL, NULL, 2, NULL, 1, NOW()),
(@role_menu, '编辑角色',         'system:role:update',  NULL, NULL, 2, NULL, 2, NOW()),
(@role_menu, '删除角色',         'system:role:delete',  NULL, NULL, 2, NULL, 3, NOW()),
(@role_menu, '分配菜单权限',     'system:role:assign',  NULL, NULL, 2, NULL, 4, NOW());

-- Button-level permissions for Department Management
SET @dept_menu = (SELECT id FROM sys_menu WHERE permission = 'system:dept:list' AND parent_id = @sys_mgmt);
INSERT INTO sys_menu (parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(@dept_menu, '新增部门',  'system:dept:create',  NULL, NULL, 2, NULL, 1, NOW()),
(@dept_menu, '编辑部门',  'system:dept:update',  NULL, NULL, 2, NULL, 2, NOW()),
(@dept_menu, '删除部门',  'system:dept:delete',  NULL, NULL, 2, NULL, 3, NOW());

-- Button-level permissions for Data Dictionary
SET @dict_menu = (SELECT id FROM sys_menu WHERE permission = 'system:dict:list' AND parent_id = @sys_mgmt);
INSERT INTO sys_menu (parent_id, name, permission, path, component, type, icon, sort_order, created_at) VALUES
(@dict_menu, '新增分类/条目',  'system:dict:create',  NULL, NULL, 2, NULL, 1, NOW()),
(@dict_menu, '编辑分类/条目',  'system:dict:update',  NULL, NULL, 2, NULL, 2, NOW()),
(@dict_menu, '删除分类/条目',  'system:dict:delete',  NULL, NULL, 2, NULL, 3, NOW());

-- Assign ALL menus to system admin role
SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_SYSTEM_ADMIN');
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role, id FROM sys_menu;

-- ── Default Admin User ──────────────────────────────────────
-- BCrypt hash for 'admin123'
INSERT INTO sys_user (username, password, real_name, status, password_change_required, created_at, created_by) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 1, 1, NOW(), 'SYSTEM');

-- Assign admin role to admin user
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_SYSTEM_ADMIN';

-- ── Base Dictionary Categories ──────────────────────────────
INSERT INTO sys_dict_category (category_name, category_code, description, sort_order, created_at, created_by) VALUES
('成果类型',       'ACHIEVEMENT_TYPE',  'Types of achievements: paper, patent, software copyright',  1, NOW(), 'SYSTEM'),
('专利类型',       'PATENT_TYPE',       'Patent types: invention, utility model, design',             2, NOW(), 'SYSTEM'),
('收录情况',       'INDEX_STATUS',      'Indexing status: SCI, EI, SSCI, CSSCI, 北大核心',           3, NOW(), 'SYSTEM'),
('经费来源',       'FUND_SOURCE',       'Funding sources: national, provincial, municipal, institutional', 4, NOW(), 'SYSTEM'),
('预警等级',       'WARN_LEVEL',        'Warning levels for fee deadlines and maintenance',          5, NOW(), 'SYSTEM');

-- Dictionary entries for achievement types
SET @ach_type = (SELECT id FROM sys_dict_category WHERE category_code = 'ACHIEVEMENT_TYPE');
INSERT INTO sys_dict_entry (category_id, dict_key, dict_value, sort_order, created_at, created_by) VALUES
(@ach_type, 'paper',            '论文',         1, NOW(), 'SYSTEM'),
(@ach_type, 'patent',           '专利',         2, NOW(), 'SYSTEM'),
(@ach_type, 'software',         '软件著作权',   3, NOW(), 'SYSTEM');

-- Dictionary entries for patent types
SET @patent_type = (SELECT id FROM sys_dict_category WHERE category_code = 'PATENT_TYPE');
INSERT INTO sys_dict_entry (category_id, dict_key, dict_value, sort_order, created_at, created_by) VALUES
(@patent_type, 'invention',     '发明专利',      1, NOW(), 'SYSTEM'),
(@patent_type, 'utility_model', '实用新型专利',  2, NOW(), 'SYSTEM'),
(@patent_type, 'design',        '外观设计专利',  3, NOW(), 'SYSTEM');
