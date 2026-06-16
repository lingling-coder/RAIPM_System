package com.institute.achievement.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * JWT token provider.
 * Handles access token (2h) and refresh token (7d) generation and validation.
 * Uses HMAC-SHA256 with a configurable secret key.
 * Token blacklisting via Redis Set for logout support.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final RedisTemplate<String, String> redisTemplate;

    public JwtTokenProvider(
            @Value("${jwt.secret:AchievementJWTSecretKeyForHMACSHA256LengthRequirement2024}") String secret,
            @Value("${jwt.access-token-expiration:7200000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMs,
            RedisTemplate<String, String> redisTemplate) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate an access token with user claims.
     * Claims: sub (userId), roles, deptId, permissions.
     * Expiration: 2 hours.
     */
    public String generateAccessToken(Long userId, List<String> roles, Long deptId, List<String> permissions) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .claim("deptId", deptId)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate a refresh token.
     * Claims: sub (userId), type="refresh".
     * Expiration: 7 days.
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate a token: verify signature, check expiry, not blacklisted.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            // Check if token is blacklisted
            if (isBlacklisted(token)) {
                log.debug("Token is blacklisted");
                return false;
            }

            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get user ID from token (subject claim).
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims != null) {
            return Long.valueOf(claims.getSubject());
        }
        return null;
    }

    /**
     * Get roles list from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims != null && claims.get("roles") != null) {
            return (List<String>) claims.get("roles");
        }
        return List.of();
    }

    /**
     * Get department ID from token.
     */
    public Long getDeptIdFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims != null && claims.get("deptId") != null) {
            return ((Number) claims.get("deptId")).longValue();
        }
        return null;
    }

    /**
     * Get permissions list from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims != null && claims.get("permissions") != null) {
            return (List<String>) claims.get("permissions");
        }
        return List.of();
    }

    /**
     * Check if token is a refresh token.
     */
    public boolean isRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (claims != null) {
            return "refresh".equals(claims.get("type"));
        }
        return false;
    }

    /**
     * Blacklist a token in Redis with TTL matching its remaining validity.
     */
    public void blacklistToken(String token, Duration duration) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", duration);
        log.debug("Token blacklisted for {} ms", duration.toMillis());
    }

    /**
     * Get remaining TTL of a token from its expiration.
     */
    public Duration getRemainingDuration(String token) {
        Claims claims = parseClaims(token);
        if (claims != null && claims.getExpiration() != null) {
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            return remainingMs > 0 ? Duration.ofMillis(remainingMs) : Duration.ZERO;
        }
        return Duration.ZERO;
    }

    /**
     * Get access token expiration in milliseconds (for response).
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /**
     * Get refresh token expiration in milliseconds.
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("Failed to parse claims: {}", e.getMessage());
            return null;
        }
    }

    private boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
