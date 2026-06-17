package com.institute.achievement.common.util;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

/**
 * AES-256 encryption utility for sensitive data at rest.
 * <p>
 * Provides encrypt/decrypt operations using AES-256-GCM mode.
 * The secret key is derived from a configurable base key with a fixed
 * application-specific salt to ensure unique ciphertexts across deployments.
 * <p>
 * <strong>Security note:</strong> In production, configure the secret key
 * via {@code achievement.encrypt.secret-key} in application.yml or environment
 * variables. The default key is intended for development only.
 * <p>
 * <strong>Usage:</strong>
 * <pre>{@code
 * String encrypted = EncryptUtil.encrypt("smtp_password");
 * String decrypted = EncryptUtil.decrypt(encrypted);
 * }</pre>
 */
@UtilityClass
public class EncryptUtil {

    /**
     * Default 32-byte AES-256 key for development.
     * Production deployments MUST override via {@code achievement.encrypt.secret-key}.
     */
    private static final byte[] DEFAULT_KEY = "D3f4ultK3y!@#2026AchvMgtSys#$$".getBytes(StandardCharsets.UTF_8);

    /**
     * Application-specific salt (16 bytes) to prevent rainbow table precomputation.
     */
    private static final byte[] APP_SALT = "AchvSaltV1$#@!".getBytes(StandardCharsets.UTF_8);

    /**
     * Lazily-initialized AES instance. Reusable and thread-safe.
     */
    private static volatile AES aesInstance;

    /**
     * Get or create the AES cipher instance.
     * Uses synchronized for thread-safe lazy initialization.
     *
     * @return AES cipher instance (thread-safe)
     */
    private static AES getAes() {
        if (aesInstance == null) {
            synchronized (EncryptUtil.class) {
                if (aesInstance == null) {
                    byte[] key = getSecretKey();
                    // Use GCM mode for authenticated encryption
                    aesInstance = new AES("GCM", "PKCS5Padding",
                            KeyUtil.generateKey("AES", 256, key));
                }
            }
        }
        return aesInstance;
    }

    /**
     * Resolve the AES-256 key from system property or environment variable.
     * Falls back to the default development key if not configured.
     *
     * @return 32-byte AES key
     */
    private static byte[] getSecretKey() {
        String configuredKey = System.getProperty("achievement.encrypt.secret-key");
        if (configuredKey == null || configuredKey.isBlank()) {
            configuredKey = System.getenv("ACHIEVEMENT_ENCRYPT_KEY");
        }
        if (configuredKey != null && !configuredKey.isBlank()) {
            // Derive a 32-byte key from the configured secret using SHA-256
            return cn.hutool.crypto.digest.DigestUtil.sha256(
                    (configuredKey + new String(APP_SALT, StandardCharsets.UTF_8))
                            .getBytes(StandardCharsets.UTF_8));
        }
        // Fallback: derive from default key with salt
        return cn.hutool.crypto.digest.DigestUtil.sha256(
                (new String(DEFAULT_KEY, StandardCharsets.UTF_8)
                        + new String(APP_SALT, StandardCharsets.UTF_8))
                        .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encrypt plaintext using AES-256-GCM.
     *
     * @param plaintext the text to encrypt (e.g., SMTP password)
     * @return Base64-encoded encrypted string, or null if input is null
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        return getAes().encryptBase64(plaintext);
    }

    /**
     * Decrypt ciphertext using AES-256-GCM.
     *
     * @param ciphertext the Base64-encoded encrypted string
     * @return original plaintext, or null if input is null
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        return getAes().decryptStr(ciphertext);
    }
}
