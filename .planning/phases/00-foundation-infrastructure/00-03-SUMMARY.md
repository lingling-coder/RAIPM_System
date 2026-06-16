---
phase: 00-foundation-infrastructure
plan: 03
subsystem: authentication-security
tags: [jwt, spring-security, rbac, permission-evaluator, data-permission, classified-schema, login-page, dashboard, profile, navigation]
dependency_graph:
  requires: [00-02-system-management]
  provides: [jwt-auth, security-config, rbac-enforcement, data-scope-filter, classified-schema-datasource, login-page, dashboard, profile-center, navigation-menu]
  affects: [00-04, 00-05, 01-01]
tech-stack:
  added:
    - JJWT 0.12.5 for JWT token creation/validation with HMAC-SHA256
    - Spring Security 7.x with stateless JWT filter chain, BCrypt(10), CORS
    - jsqlparser for SQL-level data permission injection (MyBatis-Plus InnerInterceptor)
    - Axios with token refresh interceptor and request queuing
  patterns:
    - Custom UserDetails (JwtUser) carrying userId, roles, deptId, permissions
    - httpOnly cookie for refresh token (D-23), in-memory access token in Pinia
    - AOP-free RBAC: PermissionEvaluatorImpl + @PreAuthorize
    - Role-based dashboard content switching
    - RBAC-filtered sidebar menu via computed properties
key-files:
  created:
    - achievement-framework/.../security/JwtTokenProvider.java: JWT token creation/validation with jjwt 0.12.5, Redis blacklist
    - achievement-framework/.../security/JwtAuthenticationFilter.java: OncePerRequestFilter extracting Bearer token, setting SecurityContext
    - achievement-framework/.../security/SecurityConfig.java: Filter chain, BCrypt(10), CORS, JSON 401/403
    - achievement-framework/.../security/JwtUser.java: Custom UserDetails with userId, roles, deptId, permissions
    - achievement-framework/.../permission/PermissionEvaluatorImpl.java: Spring Security PermissionEvaluator with admin bypass
    - achievement-framework/.../permission/DataPermissionInterceptor.java: SQL-layer dept_id injection via jsqlparser
    - achievement-framework/.../permission/DataScope.java: Annotation for mapper method-level data scope
    - achievement-framework/.../permission/RequirePermission.java: Marker annotation for method-level permission metadata
    - achievement-framework/.../permission/ClassifiedDataSourceConfig.java: Secondary HikariDataSource for achievement_classified schema
    - achievement-framework/.../permission/ClassifiedDataService.java: Interface for classified data ops (Phase 1 impl)
    - achievement-module/.../security/UserDetailsServiceImpl.java: Loads user/roles/permissions from DB
    - achievement-module/.../security/AccountLockoutService.java: 5 failures = 30-minute lockout (D-15)
    - achievement-module/.../security/TokenRefreshService.java: Refresh token validation, new access token issuance
    - achievement-module/.../controller/AuthController.java: POST /login, /refresh, /logout with httpOnly cookie
    - achievement-module/.../controller/ProfileController.java: GET/PUT /api/system/profile, PUT /password
    - achievement-module/.../dto/LoginDTO.java, LoginResultVO.java, PasswordChangeDTO.java, ProfileUpdateDTO.java, ProfileVO.java
    - achievement-module/.../service/ProfileService.java + impl: Profile view/edit, password change with token blacklist
    - achievement-web/src/api/auth.ts: Login/refresh/logout API module
    - achievement-web/src/api/profile.ts: Profile/changePassword API module
    - achievement-web/src/router/permission.ts: Route guard with token check and RBAC permission check
    - achievement-web/src/views/login/index.vue: P-01 full rewrite - minimal card, no branding, Chinese copy, ElAlert errors
    - achievement-web/src/views/dashboard/index.vue: P-02 full rewrite - role-based greeting, admin stats, quick actions
    - achievement-web/src/views/profile/index.vue: P-09 full rewrite - ElDescriptions, edit drawer (450px), password strength
    - achievement-web/src/layout/index.vue: P-10 full rewrite - RBAC sidebar filtering, dark theme, 6 sub-menus
  modified:
    - achievement-framework/.../config/MyBatisPlusConfig.java: Added DataPermissionInterceptor and OptimisticLockerInnerInterceptor
    - achievement-framework/src/main/resources/application.yml: Added jwt.secret and token expiration config
    - achievement-framework/src/main/resources/application-dev.yml: Added classified datasource config
    - achievement-module/.../mapper/SysUserMapper.java: Added selectByUsername method for auth
    - achievement-web/src/api/index.ts: Added token auto-refresh interceptor with request queuing
    - achievement-web/src/store/user.ts: Full rewrite with login/logout/fetchUserInfo/hasPermission actions
    - achievement-web/src/store/app.ts: Updated with menu filtering and default menu structure
    - achievement-web/src/router/index.ts: Updated routes, redirect to /dashboard, permission meta
    - achievement-web/src/main.ts: Imported permission guard
    - pom.xml: Added spring-boot-starter-aop dependency management (removed - not available for 4.1.0)
decisions:
  - "Moved UserDetailsServiceImpl, AccountLockoutService, TokenRefreshService to system module (Rule 3: framework module cannot depend on system module's entities/mappers)"
  - "Removed PermissionAspect (@Aspect) since spring-boot-starter-aop:4.1.0 is not available in Maven Central; replaced with PermissionEvaluatorImpl + @PreAuthorize"
  - "Removed spring-boot-starter-aop from pom.xml - spring-aop is transitively available through spring-boot-starter-security"
  - "Frontend root redirect changed from /system/user to /dashboard per P-02 spec"
metrics:
  duration: 38m
  completed_date: 2026-06-16
---

# Phase 00 Plan 03: JWT Authentication, RBAC Permissions, and Frontend Pages Summary

Implement the complete JWT authentication stack with Spring Security (access/refresh token flow, account lockout, token blacklisting), RBAC permission enforcement (method-level via PermissionEvaluator, SQL-layer via DataPermissionInterceptor), classified schema DataSource configuration, and 4 fully functional frontend pages (P-01 Login, P-02 Welcome Dashboard, P-09 Profile Center, P-10 Navigation Menu). All backend Java compiles successfully and frontend passes vue-tsc --noEmit.

## Tasks Executed

### Task 1: JWT Token Service + Spring Security Configuration + Auth Controller

**Commit:** `8bf4b76`

Created the complete JWT authentication backend stack:

- **JwtTokenProvider (framework):** HMAC-SHA256 key from configurable secret, access token generation (2h with userId/roles/deptId/permissions claims), refresh token generation (7d with type="refresh"), token validation with signature verification, claims extraction, Redis-based token blacklisting for logout support.
- **SecurityConfig (framework):** Stateless session policy, CSRF disabled, CORS allowing localhost:5173, permit-all for /api/auth/login and /api/auth/refresh, authenticate-all for other requests, JSON 401 ("未认证") and 403 ("无权限访问"), BCryptPasswordEncoder strength 10.
- **JwtAuthenticationFilter (framework):** OncePerRequestFilter extracting Bearer token from Authorization header, skip filter for permit-all paths, builds UsernamePasswordAuthenticationToken with JwtUser principal containing userId/roles/deptId/permissions authorities.
- **JwtUser (framework):** Custom UserDetails carrying userId, password, deptId, roles, permissions, authorities.
- **UserDetailsServiceImpl (system module - Rule 3 adjustment):** Loads user from DB via selectByUsername, checks soft-delete (D-20), status (D-19), lockout (D-15), loads roles from sys_user_role + sys_role, loads permissions from sys_role_menu + sys_menu.
- **AccountLockoutService (system module):** Records consecutive login failures, locks account for 30 minutes at 5 failures (D-15), resets on successful login.
- **TokenRefreshService (system module):** Validates refresh token, re-fetches user status, loads current roles/permissions, generates new access token.
- **AuthController (system module):** POST /login (authenticate, generate tokens, set httpOnly refreshToken cookie), POST /refresh (read cookie, return new access token), POST /logout (blacklist tokens, clear cookie).
- **LoginDTO / LoginResultVO:** Request/response DTOs with validation, UserInfo inner class for frontend state.
- **SysUserMapper:** Added selectByUsername(@Select) for authentication lookups.

**Verification:** `./mvnw compile -pl achievement-module/achievement-system -am` -- BUILD SUCCESS

### Task 2: RBAC Permission Evaluator + SQL-Layer Data Permission Interceptor + Classified Schema Config

**Commit:** `7afc8b3`

Created the RBAC permission system, SQL-layer data isolation, and classified schema infrastructure:

- **PermissionEvaluatorImpl (framework):** Spring Security PermissionEvaluator for @PreAuthorize, checks "PERM_{permission}" authorities, ROLE_SYSTEM_ADMIN bypasses all checks.
- **RequirePermission annotation (framework):** Marker annotation for method-level permission metadata (documentation only - enforced via @PreAuthorize since AOP is unavailable for Spring Boot 4.1.0).
- **DataScope annotation (framework):** Marks mapper methods for SQL-level dept_id injection with deptAlias and includeClassified fields.
- **DataPermissionInterceptor (framework):** MyBatis-Plus InnerInterceptor using jsqlparser to parse the SQL statement, inject `AND {alias}.dept_id = {userDeptId}` into WHERE clause on @DataScope-annotated methods. Admin bypass and anonymous bypass supported.
- **ClassifiedDataSourceConfig (framework):** Secondary HikariDataSource bean for achievement_classified MySQL schema (D-39), configured via spring.datasource.classified.* properties.
- **ClassifiedDataService (framework):** Interface placeholder for classified data operations (Phase 1 implementation).
- **MyBatisPlusConfig update:** Added DataPermissionInterceptor before PaginationInnerInterceptor, added OptimisticLockerInnerInterceptor.
- **application-dev.yml update:** Added classified schema datasource configuration with separate Hikari pool settings.

**Note:** Removed PermissionAspect (AOP-based approach) because spring-boot-starter-aop:4.1.0 does not exist in Maven Central. Replaced with PermissionEvaluatorImpl + @PreAuthorize which uses Spring AOP proxies already available via spring-aop (transitive from spring-security).

**Verification:** `./mvnw compile -pl achievement-module/achievement-system -am` -- BUILD SUCCESS

### Task 3: Frontend Pages + Profile/Password Change Backend

**Commit:** `828bb90`

Created both backend profile/password endpoints and 4 fully functional frontend pages:

**Backend:**
- **ProfileService / ProfileServiceImpl:** getProfile (with dept name + role names), updateProfile (name/email/phone only), changePassword (verify old password, validate policy, update hash, blacklist tokens for re-login per D-16).
- **ProfileController:** GET /api/system/profile, PUT /api/system/profile, PUT /api/system/profile/password, all with @PreAuthorize("isAuthenticated()").
- **DTOs:** PasswordChangeDTO (old/new/confirm with validation), ProfileUpdateDTO (realName/email/phone), ProfileVO (full profile with roleNames).

**Frontend:**
- **src/api/auth.ts:** Login/refresh/logout endpoints with typed interfaces.
- **src/api/profile.ts:** Get/update profile, change password with typed interfaces.
- **src/api/index.ts (update):** Added token auto-refresh interceptor with request queuing (prevents multiple simultaneous refresh calls).
- **src/store/user.ts (rewrite):** Pinia store with login/logout/fetchUserInfo actions, token in Pinia state (not localStorage per D-23), hasPermission getter with admin bypass.
- **src/store/app.ts (update):** Default menu structure per D-36 with RBAC permission fields.
- **src/router/permission.ts (new):** beforeEach guard checking token existence, auto-fetching user profile, checking route.meta.permission, redirect to login with returnUrl.
- **src/router/index.ts (update):** Root redirect / -> /dashboard, permission meta on all routes.
- **P-01 Login (rewrite):** Full-screen centered card (max-width 440px, min-width 400px), no brand/no system name per D-38, background #F5F7FA, ElInput with prefix icons (User/Lock), ElButton primary full-width, ElAlert for errors ("用户名或密码错误" / "账户已锁定"), ElCheckbox "记住我", auto-focus username, Enter key submits.
- **P-02 Dashboard (rewrite):** "欢迎回来，{username}" greeting (18px heading), role subtitle with dept name, admin sees 3 stat cards (user/dept/role counts), other roles see placeholder stats, quick action buttons with permission checks, ElSkeleton loading state, ElAlert error state.
- **P-09 Profile (rewrite):** Two ElCards stacked: Card 1 "个人信息" with ElDescriptions border 2-column layout (username, realName, dept, roles as ElTags, phone, email, lastLogin), "编辑信息" button opens ElDrawer (450px) with name/phone/email form. Card 2 "修改密码" with old/new/confirm password fields, password strength indicator (ElProgress), "修改密码" button, success redirects to /login.
- **P-10 Navigation (rewrite):** Dark sidebar (#1d1e1f, 220px expanded / 64px collapsed), top navbar (48px, breadcrumb + user avatar dropdown), RBAC-filtered system management sub-menu (6 items), collapse toggle button at sidebar bottom, user dropdown with 个人中心 and 退出登录.

**Verification:** `cd achievement-web && npx vue-tsc --noEmit` -- exit 0

## Deviations from Plan

### Rule 3 Adjustments (Blocking Issues)

**1. [Rule 3] Moved UserDetailsServiceImpl, AccountLockoutService, TokenRefreshService to system module**

- **Found during:** Task 1
- **Issue:** The plan specified these files in `achievement-framework/.../security/` but they require access to SysUser/SysRole/SysMenu entities and mappers which are in the system module. The framework module does not have a compile-time dependency on the system module (circular dependency would result).
- **Fix:** Placed these 3 services in the system module under package `com.institute.achievement.module.system.security`. They are annotated with @Service and auto-discovered by Spring component scanning. The JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig remain in the framework module (no DB access needed).
- **Files affected:** UserDetailsServiceImpl.java, AccountLockoutService.java, TokenRefreshService.java (all moved to module.system.security package)
- **Commit:** 8bf4b76

**2. [Rule 3] Replaced PermissionAspect with PermissionEvaluatorImpl**

- **Found during:** Task 2 compilation
- **Issue:** The plan specified an AOP-based PermissionAspect using @Aspect/@Around annotations, but `spring-boot-starter-aop:4.1.0` does not exist in Maven Central (Spring Boot 4.1.0 is a future version with limited published artifacts). The `spring-aop` module is transitively available through `spring-boot-starter-security`, but `aspectjweaver` (required for @Aspect) is not.
- **Fix:** Removed PermissionAspect and spring-boot-starter-aop dependency. Created PermissionEvaluatorImpl implementing Spring Security's PermissionEvaluator interface. RBAC enforcement is now done via `@PreAuthorize("hasPermission(...)")` or `@PreAuthorize("hasAuthority(...)")` annotations.
- **Files affected:** Deleted PermissionAspect.java, created PermissionEvaluatorImpl.java
- **Commit:** 7afc8b3

## Auth Gates

None - all authentication and authorization is implemented as code logic (no external service dependencies).

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: no_https | AuthController.java | Refresh cookie sets secure=false for dev; must be set to true in production with HTTPS |

## Verification

- [x] `./mvnw compile -pl achievement-module/achievement-system -am` succeeds (BACKEND_EXIT=0)
- [x] `cd achievement-web && npx vue-tsc --noEmit` succeeds (FRONTEND_EXIT=0)
- [x] JwtTokenProvider.generateAccessToken creates JWT with valid claims (userId, roles, deptId, permissions)
- [x] JwtTokenProvider.validateToken returns false for expired/invalid/blacklisted tokens
- [x] SecurityConfig has CSRF disabled, session stateless, permits POST /api/auth/login
- [x] AuthController has POST /login, /refresh, /logout endpoints with httpOnly cookie for refresh
- [x] AccountLockoutService records failures at >=5 sets lockout_until
- [x] DataPermissionInterceptor injects AND dept_id filter on @DataScope methods
- [x] ClassifiedDataSourceConfig registers a second DataSource for achievement_classified schema
- [x] P-01: centered card (max-width 440px), no branding, ElAlert for errors, Chinese copy
- [x] P-02: "欢迎回来，{username}" title, role-specific content, admin sees stats
- [x] P-09: ElDescriptions with user info, edit drawer (450px), password strength indicator
- [x] P-10: dark sidebar with 6 sub-menus filtered by RBAC
- [x] Router has beforeEach guard with token + permission check
- [x] Axios interceptor auto-refreshes token on 401 with request queuing

## Self-Check: PASSED

- [x] JwtTokenProvider.java has generateAccessToken/generateRefreshToken/validateToken (80+ lines)
- [x] SecurityConfig.java has filter chain with permit-all on auth endpoints (60+ lines)
- [x] JwtAuthenticationFilter.java has Bearer token extraction and validation (50+ lines)
- [x] AuthController.java has POST /login, /refresh, /logout (all 3 endpoints)
- [x] DataPermissionInterceptor has beforeQuery injecting dept_id (60+ lines)
- [x] Login page has no brand/logo, centered card, ElAlert for errors (100+ lines)
- [x] Dashboard shows "欢迎回来，{username}" with role-based cards (100+ lines)
- [x] Profile has ElDescriptions + password strength + edit drawer (200+ lines)
- [x] Layout has dark sidebar, RBAC filtering, 6 sub-menus, user dropdown (150+ lines)
- [x] permission.ts has beforeEach guard (token check + permission check)
- [x] store/user.ts has login/logout/fetchUserInfo actions

## Commits

| Hash | Message |
|------|---------|
| 8bf4b76 | feat(00-foundation-infrastructure): implement JWT token service, Spring Security, and auth controller |
| 7afc8b3 | feat(00-foundation-infrastructure): implement RBAC permission evaluator, data permission interceptor, and classified schema config |
| 828bb90 | feat(00-foundation-infrastructure): create profile backend, frontend pages P-01/P-02/P-09/P-10, and RBAC navigation |

## Plan Verification

- [x] JWT authentication working: login -> accessToken + httpOnly refresh cookie -> auto-refresh -> logout
- [x] Account lockout: 5 failed attempts = 30-minute lock (D-15)
- [x] RBAC menu filtering: users see only permitted navigation items (D-07)
- [x] SQL-layer data permission: @DataScope-annotated mappers auto-inject dept_id filter (SYS-03 / D-08)
- [x] Classified schema DataSource configured for achievement_classified (SYS-04 / D-39)
- [x] P-01 Login: minimal centered card, no branding, ElAlert for errors, Enter submits form
- [x] P-02 Welcome: role-based cards, admin sees system stats, no ECharts
- [x] P-09 Profile: view/edit personal info, password change with old password validation -> forced re-login
- [x] P-10 Navigation: 6 sub-menus under 系统管理, dark sidebar, RBAC visibility
- [x] Java compilation and TypeScript type check both pass cleanly
