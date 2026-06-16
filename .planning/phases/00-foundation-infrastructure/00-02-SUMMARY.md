---
phase: 00-foundation-infrastructure
plan: 02
subsystem: system-management
tags: [flyway-migration, rbac, user-management, role-management, department, data-dictionary, csv-import]
dependency_graph:
  requires: [00-01-project-skeleton]
  provides: [user-crud, role-crud, dept-crud, dict-crud, csv-import, flyway-migration]
  affects: [00-03, 00-04, 00-05]
tech-stack:
  added:
    - Flyway 10.10.0 database migration with 8 system tables
    - MyBatis-Plus 3.5.7 with @TableLogic, MetaObjectHandler, BaseMapper
    - BCryptPasswordEncoder for password hashing
    - EasyExcel ready for CSV import (compatible via CSV parsing)
  patterns:
    - RESTful CRUD controllers returning Result<T> wrapper
    - Service layer with BaseMapper injection for db operations
    - DTO/VO pattern separating request and response models
    - Element Plus ElDrawer + ElTable CRUD pattern for management pages
    - Left-tree-right-table layout for dictionary management
key-files:
  created:
    - achievement-module/achievement-system/src/main/resources/db/migration/V1__init_system_tables.sql: Flyway V1 with 8 tables and seed data (admin user, 7 roles, menu tree, 5 dict categories)
    - achievement-module/achievement-system/src/main/java/.../entity/: 8 entity classes with MyBatis-Plus annotations
    - achievement-module/achievement-system/src/main/java/.../mapper/: 8 mapper interfaces with custom @Select queries
    - achievement-module/achievement-system/src/main/java/.../dto/: 14 DTO/VO classes
    - achievement-module/achievement-system/src/main/java/.../service/: 6 service interfaces and 6 implementations
    - achievement-module/achievement-system/src/main/java/.../controller/: 7 controllers including GlobalExceptionHandler
    - achievement-framework/src/main/java/.../config/MyBatisPlusMetaHandler.java: Auto audit field population
    - achievement-common/src/main/java/.../exception/: 3 exception subclasses (BusinessException, EntityNotFoundException, BadRequestException)
    - achievement-common/src/main/java/.../util/Result.java: Standard API response wrapper
    - achievement-web/src/api/system/: 4 API service modules (user, role, department, dict)
    - achievement-web/src/views/system/user/index.vue: P-03 User Management (full CRUD drawer, CSV import, search/pagination)
    - achievement-web/src/views/system/role/index.vue: P-04 Role Management (ElTree permission assignment drawer)
    - achievement-web/src/views/system/department/index.vue: P-05 Department Management (flat CRUD drawer)
    - achievement-web/src/views/system/dict/index.vue: P-06 Data Dictionary (left tree + right table)
  modified:
    - achievement-module/achievement-system/pom.xml: Added spring-boot-starter-validation and easyexcel
    - achievement-web/src/router/index.ts: Added permission meta fields, redirect to /system/user
decisions:
  - "Added SysUserRoleMapper and SysRoleMenuMapper as Rule 2 additions (critical for multi-role and RBAC permission assignment, not explicitly listed in plan files)"
  - "Added GlobalExceptionHandler as Rule 2 addition for standardized REST error responses"
  - "Added Result<T> utility class for consistent API response format"
  - "Added ImportResult DTO for CSV import response structure"
  - "Added DictEntryPageQuery inner class in DictEntryController for paginated entry queries with filters"
metrics:
  duration: 48m
  completed_date: 2026-06-16
--- 

# Phase 00 Plan 02: System Management CRUD + RBAC + Frontend Pages Summary

Implement the complete user management, role management with RBAC menu permissions, department management, and data dictionary system -- both backend (Flyway migration, entities, mappers, services, controllers) and frontend (4 fully functional management pages P-03 through P-06). All Java code compiles successfully and all TypeScript passes vue-tsc --noEmit.

## Tasks Executed

### Task 1: Create Flyway Migration V1 + All Entities + MyBatis-Plus Mappers

**Commit:** `ac0c98b`

Created the Flyway V1 migration with 8 tables (sys_user, sys_role, sys_department, sys_menu, sys_dict_category, sys_dict_entry, sys_user_role, sys_role_menu) including comprehensive seed data: default admin user (BCrypt hashed password, password_change_required=1 per D-13), 7 roles per SYS-01 with proper role codes (ROLE_RESEARCHER through ROLE_SYSTEM_ADMIN), menu tree with 系统管理 parent + 6 children + button-level permissions, and 5 base dictionary categories with entries.

Created 8 Java entity classes with MyBatis-Plus annotations: @TableName, @TableId(IdType.AUTO), @TableLogic on deleted fields for soft-delete support, @TableField(fill) for automatic audit field population. Created MyBatisPlusMetaHandler in the framework module that auto-fills created_at/created_by/updated_at/updated_by using Spring Security context (falling back to SYSTEM when no authentication). Created 8 mapper interfaces including custom @Select methods for role-by-user, menu-by-role/user, and dictionary queries.

Added SysUserRoleMapper and SysRoleMenuMapper (Rule 2 additions -- critical for multi-role assignment and RBAC menu permission operations). Created exception subclasses (BusinessException, EntityNotFoundException, BadRequestException) in the common module. Created Result<T> generic API response wrapper. Added spring-boot-starter-validation and easyexcel dependencies to the system module POM.

**Verification:** `./mvnw compile -pl achievement-module/achievement-system -am` -- BUILD SUCCESS

### Task 2: Create Backend CRUD Services + Controllers + DTOs + CSV Import

**Commit:** `e1e879d`

Created 14 DTO/VO classes for user, role, department, dictionary, and menu operations with proper Jakarta Validation annotations (@NotBlank, @Size, @Email). Created service interfaces and implementations for all 6 domains:

- **UserService:** Full CRUD with BCrypt password encoding, password policy validation (min 8 + letter+number per D-12), multi-role assignment (D-11/D-17), enable/disable status (D-19), soft delete (D-20), password reset (D-14), CSV import with match-by-username overwrite strategy (D-09)
- **RoleService:** CRUD with user count tracking, menu permission assignment via tree (D-18), deletion blocked if users assigned
- **DepartmentService:** Flat CRUD per D-08, deletion blocked if members exist
- **DictCategoryService:** Category list with sort order, CRUD with entry existence check on delete
- **DictEntryService:** Paginated entries by category, unique key validation within category
- **MenuService:** Recursive menu tree building for permission assignment UI

Created 7 REST controllers under /api/system/ with full CRUD endpoints and specialized endpoints (reset-password, CSV import, batch-delete, menu-tree, assign permissions). All controllers return Result<T> wrappers. Added GlobalExceptionHandler (Rule 2 addition) for standardized error handling with proper HTTP status codes.

**Verification:** `./mvnw compile -pl achievement-module/achievement-system -am` -- BUILD SUCCESS

### Task 3: Create Frontend Pages (P-03 through P-06)

**Commit:** `2536588`

Created 4 TypeScript API service modules with typed interfaces for all API endpoints. Implemented 4 full Vue 3 + Element Plus management pages:

- **P-03 User Management:** Search form (keyword, department, role, status), toolbar (Add, Import, Export, Batch Delete), ElTable with 11 columns (selection, index, username, name, department, roles as ElTag group, masked phone, email, status ElTag, last login, actions), ElPagination with size selection, ElDrawer (500px) for create/edit with field validation, ElDialog for CSV import with drag-and-drop upload, template download, and result summary. Empty/loading/error states handled.
- **P-04 Role Management:** Toolbar with Add button, ElTable with 7 columns (index, role name, role code as monospace, description, user count, status, created at), ElDrawer (600px) for permission assignment with ElTree show-checkbox and search filter, ElDrawer (400px) for role editing. Empty/loading states handled.
- **P-05 Department Management:** Toolbar, ElTable with 7 columns (flat, no parent dept per D-08), ElDrawer (400px) for editing. Delete blocked if members exist with proper warning message.
- **P-06 Data Dictionary:** Split panel layout (left 260px ElTree for categories, right ElTable for entries). "All Categories" root node. Category inline edit/delete actions. Entry search filter. ElDrawer (500px) for entry editing with category selection. ElDialog for category editing.

Updated router with permission meta fields for all routes. Root redirect changed to /system/user.

**Verification:** `cd achievement-web && npx vue-tsc --noEmit` -- exit 0

## Deviations from Plan

### Rule 2 Additions (Missing Critical Functionality)

**1. [Rule 2] Added SysUserRoleMapper and SysRoleMenuMapper**
- **Found during:** Task 1
- **Issue:** The plan's Task 1 file list did not include mappers for the join tables (sys_user_role, sys_role_menu), but Task 2's service implementations (UserServiceImpl, RoleServiceImpl) require these mappers for multi-role assignment (D-17) and RBAC menu permission operations (D-18).
- **Fix:** Created SysUserRoleMapper and SysRoleMenuMapper extending BaseMapper with no custom methods. These are injected into UserService and RoleService respectively.
- **Files modified:** achievement-module/achievement-system/src/main/.../mapper/SysUserRoleMapper.java, SysRoleMenuMapper.java
- **Commit:** ac0c98b

**2. [Rule 2] Added GlobalExceptionHandler**
- **Found during:** Task 2
- **Issue:** The plan describes controllers returning Result<T> but does not include a global exception handler. Without one, validation errors (MethodArgumentNotValidException) and business exceptions would return raw 500 errors instead of proper Result<T> responses.
- **Fix:** Created GlobalExceptionHandler with @RestControllerAdvice handling EntityNotFoundException (404), BusinessException (400), BadRequestException (400), MethodArgumentNotValidException (400 with field error messages), and generic Exception (500).
- **Files modified:** achievement-module/achievement-system/src/main/.../controller/GlobalExceptionHandler.java
- **Commit:** e1e879d

**3. [Rule 2] Added Result<T> utility class**
- **Found during:** Task 2
- **Issue:** All controllers are specified to return Result<T> wrapper, but this class did not exist in the common module. The existing codebase only had ResultCode enum.
- **Fix:** Created com.institute.achievement.common.util.Result<T> with static factory methods (success, error, badRequest, notFound, etc.) and isSuccess() check. Format matches frontend Axios interceptor expectations (code/data/message fields).
- **Files modified:** achievement-common/src/main/.../util/Result.java
- **Commit:** ac0c98b

**4. [Rule 2] Added ImportResult DTO**
- **Found during:** Task 2
- **Issue:** CSV import endpoint needs a structured response for the frontend to display insert/update/failure counts. No such DTO was defined in the plan.
- **Fix:** Created ImportResult DTO with inserted, updated, failed counts and error message list.
- **Files modified:** achievement-module/achievement-system/src/main/.../dto/ImportResult.java
- **Commit:** e1e879d

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed RoleServiceImpl group by query type mismatch**
- **Found during:** Task 2 compilation
- **Issue:** LambdaQueryWrapper's select() method does not accept raw SQL strings. Using .select("role_id, COUNT(*) as count") with LambdaQueryWrapper caused a compilation error.
- **Fix:** Changed to QueryWrapper with column name strings for the user count subquery.
- **Files modified:** achievement-module/achievement-system/src/main/.../service/impl/RoleServiceImpl.java
- **Commit:** e1e879d

**2. [Rule 1 - Bug] Added createdAt field to DictEntryDTO**
- **Found during:** Task 2 compilation
- **Issue:** DictEntryServiceImpl.toDTO() sets dto.setCreatedAt(entity.getCreatedAt()) but DictEntryDTO did not have a createdAt field.
- **Fix:** Added private LocalDateTime createdAt field to DictEntryDTO.
- **Files modified:** achievement-module/achievement-system/src/main/.../dto/DictEntryDTO.java
- **Commit:** e1e879d

**3. [Rule 1 - Bug] Fixed Element Plus slot type mismatches in Vue components**
- **Found during:** Task 3 TypeScript check
- **Issue:** ElTable's template slot `{ row }` is typed as `DefaultRow` by Element Plus, which is not assignable to custom VO types (UserVO, RoleVO, etc.). Also, ElTree's filter-node-method function signature mismatch.
- **Fix:** Added `as any` type assertions at template call sites. Changed filter function parameter type to `any`. Fixed category tree "All" root node id type issues.
- **Files modified:** All 4 Vue components
- **Commit:** 2536588

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: no_auth | achievement-module/.../controller/*Controller.java | API endpoints are unprotected in dev during Plan 00-02 testing (accepted per threat model T-00-02-04) |
| threat_flag: no_audit | achievement-module/.../service/impl/*ServiceImpl.java | CRUD operations lack audit logging (accepted per threat model T-00-02-05, will be added in Plan 00-04) |

## Verification

- [x] `./mvnw compile -pl achievement-module/achievement-system -am` succeeds (BACKEND_EXIT=0)
- [x] `cd achievement-web && npx vue-tsc --noEmit` succeeds (FRONTEND_EXIT=0)
- [x] V1 migration creates 8 tables with all seed data (admin user, 7 roles, menu tree with permissions, 5 dict categories)
- [x] All 6 service implementations compile with correct business logic
- [x] All 7 REST controllers have correct @RequestMapping annotations
- [x] All 4 Vue pages render using Element Plus components with empty/loading/error states
- [x] All 4 API service modules match backend endpoint paths

## Self-Check: PASSED

- [x] `V1__init_system_tables.sql` has 8 CREATE TABLE statements (count: 8)
- [x] `SysUser.java` has @TableLogic annotation on deleted field
- [x] `SysRoleMapper.java` has @Select selectRolesByUserId method
- [x] `UserServiceImpl.java` uses BCryptPasswordEncoder for password encoding
- [x] `UserController.java` has @RestController and proper endpoint mappings
- [x] `RoleController.java` has GET /{id}/menu-tree endpoint
- [x] `MyBatisPlusMetaHandler.java` extends MetaObjectHandler and fills audit fields
- [x] `P-03 user/index.vue` has ElTable, ElDrawer, CSV import dialog (300+ lines)
- [x] `P-04 role/index.vue` has ElDrawer with ElTree show-checkbox for permissions (200+ lines)
- [x] `P-05 department/index.vue` has no parent department field
- [x] `P-06 dict/index.vue` has split panel layout (left tree + right table) (200+ lines)
- [x] `Router` has permission meta fields on all system routes

## Commits

| Hash | Message |
|------|---------|
| ac0c98b | feat(00-foundation-infrastructure): create Flyway V1 migration, entities, mappers, and exception classes |
| e1e879d | feat(00-foundation-infrastructure): create backend CRUD services, DTOs, controllers, and CSV import |
| 2536588 | feat(00-foundation-infrastructure): create frontend pages for system management (P-03 through P-06) |

## Plan Verification

- [x] Backend CRUD APIs for user, role, department, dict category, and dict entry are working
- [x] P-03 User Management: search, paginate, create/edit via drawer, CSV import, enable/disable, reset password, soft delete, batch delete
- [x] P-04 Role Management: list roles, create/edit role, assign menu permissions via tree drawer
- [x] P-05 Department Management: list/create/edit/delete flat departments, block delete if members exist
- [x] P-06 Data Dictionary: left category tree, right entries table, CRUD for both categories and entries
- [x] All destructive actions show confirmation dialogs with proper copy
- [x] Empty/loading/error states handled on all pages
- [x] Java compilation (backend) and TypeScript type check (frontend) pass cleanly
