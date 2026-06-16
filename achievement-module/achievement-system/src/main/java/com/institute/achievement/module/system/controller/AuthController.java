package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.security.JwtTokenProvider;
import com.institute.achievement.framework.security.JwtUser;
import com.institute.achievement.module.system.dto.LoginDTO;
import com.institute.achievement.module.system.dto.LoginResultVO;
import com.institute.achievement.module.system.security.AccountLockoutService;
import com.institute.achievement.module.system.security.TokenRefreshService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Authentication controller.
 * Handles login, token refresh, and logout.
 * All endpoints prefixed with /api/auth.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccountLockoutService accountLockoutService;
    private final TokenRefreshService tokenRefreshService;

    /**
     * Login with username and password.
     * On success: returns accessToken + sets httpOnly refreshToken cookie.
     * On failure: records login attempt for lockout tracking (D-15).
     */
    @PostMapping("/login")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginDTO dto,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        // Check account lockout before attempting authentication
        if (accountLockoutService.isAccountLocked(dto.getUsername())) {
            long remainingMinutes = accountLockoutService.getRemainingLockoutMinutes(dto.getUsername());
            log.warn("Login attempt on locked account: {}", dto.getUsername());
            return Result.error(423, "账户已锁定，请" + remainingMinutes + "分钟后再试，或联系系统管理员");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();

            // Reset login attempts on success
            accountLockoutService.resetLoginAttempts(dto.getUsername());

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(
                    jwtUser.getUserId(),
                    jwtUser.getRoles(),
                    jwtUser.getDeptId(),
                    jwtUser.getPermissions()
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser.getUserId());

            // Set refresh token as httpOnly cookie (D-23)
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false) // true in production with HTTPS
                    .path("/api/auth/refresh")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Lax")
                    .build();
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // Build login result
            LoginResultVO result = LoginResultVO.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                    .userInfo(LoginResultVO.UserInfo.builder()
                            .id(jwtUser.getUserId())
                            .username(dto.getUsername())
                            .realName(jwtUser.getUsername()) // JwtUser stores userId as username, realName not in JWT
                            .deptId(jwtUser.getDeptId())
                            .roles(jwtUser.getRoles())
                            .permissions(jwtUser.getPermissions())
                            .build())
                    .build();

            log.info("User '{}' logged in successfully", dto.getUsername());
            return Result.success(result);

        } catch (BadCredentialsException e) {
            // Record login failure
            accountLockoutService.recordLoginFailure(dto.getUsername());
            log.warn("Failed login attempt for user: {}", dto.getUsername());
            return Result.unauthorized("用户名或密码错误，请重新输入");

        } catch (LockedException e) {
            return Result.error(423, e.getMessage());

        } catch (Exception e) {
            log.error("Login error for user '{}': {}", dto.getUsername(), e.getMessage());
            return Result.unauthorized("用户名或密码错误，请重新输入");
        }
    }

    /**
     * Refresh access token using httpOnly refresh cookie.
     */
    @PostMapping("/refresh")
    public Result<LoginResultVO> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            return Result.unauthorized("No refresh token found");
        }

        try {
            String newAccessToken = tokenRefreshService.refreshAccessToken(refreshToken);

            // Build minimal response
            LoginResultVO result = LoginResultVO.builder()
                    .accessToken(newAccessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                    .build();

            return Result.success(result);

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return Result.unauthorized("Refresh token invalid or expired");
        }
    }

    /**
     * Logout: blacklist tokens and clear cookie.
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // Get current access token from header
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        // Get refresh token from cookie
        String refreshToken = extractRefreshToken(request);

        // Blacklist tokens
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            Duration remaining = jwtTokenProvider.getRemainingDuration(accessToken);
            if (!remaining.isNegative() && !remaining.isZero()) {
                jwtTokenProvider.blacklistToken(accessToken, remaining);
            }
        }
        if (refreshToken != null) {
            tokenRefreshService.removeRefreshToken(refreshToken);
        }

        // Clear refresh cookie
        Cookie clearCookie = new Cookie("refreshToken", null);
        clearCookie.setHttpOnly(true);
        clearCookie.setPath("/api/auth/refresh");
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);

        // Clear security context
        SecurityContextHolder.clearContext();

        log.info("User logged out successfully");
        return Result.success();
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
