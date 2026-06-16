package com.institute.achievement.framework.audit;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SHA-256 hash chain utility for audit log tamper detection (D-27).
 * <p>
 * Each audit log entry stores:
 * <ul>
 *   <li>{@code previous_hash}: SHA-256 hash of the preceding log entry</li>
 *   <li>{@code current_hash}: SHA-256 hash of this entry's content + previous hash</li>
 * </ul>
 * This forms a cryptographic chain: any modification to an entry breaks all
 * subsequent hashes, making undetected tampering infeasible.
 */
@Slf4j
public final class HashChainUtil {

    private static final String SHA_256 = "SHA-256";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private HashChainUtil() {
        // Utility class, prevent instantiation
    }

    /**
     * Compute SHA-256 hex digest of the input string.
     *
     * @param input the string to hash
     * @return 64-character lowercase hex SHA-256 digest
     * @throws IllegalStateException if SHA-256 is not available on this JVM
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Compute the current hash for an audit log entry.
     * <p>
     * The hash input is a concatenation of: id + previousId + previousHash +
     * contentJson + createdAt. This ensures the hash binds together the
     * entry's identity, its content, and the link to the previous entry.
     *
     * @param id           the current entry's ID
     * @param previousHash the previous entry's current_hash (empty string for first entry)
     * @param contentJson  the JSON content being logged
     * @param createdAt    the timestamp of the current entry
     * @return SHA-256 hex digest of the combined input
     */
    public static String computeHash(Long id, String previousHash,
                                     String contentJson, LocalDateTime createdAt) {
        String input = id + "|" + previousHash + "|" +
                (contentJson != null ? contentJson : "") + "|" +
                (createdAt != null ? createdAt.toString() : "");
        return sha256(input);
    }

    /**
     * Verify the integrity of an ordered chain of audit log entries.
     * <p>
     * Checks two invariants:
     * <ol>
     *   <li>Each entry's {@code current_hash} matches the recomputed hash</li>
     *   <li>Each entry's {@code previous_hash} matches the previous entry's
     *       {@code current_hash} (except the first entry, which must have
     *       an empty previous_hash)</li>
     * </ol>
     *
     * @param logs ordered list of audit log entities (must be sorted by id ASC)
     * @return true if the entire chain is valid, false if any break is detected
     */
    public static boolean verifyChain(List<AuditLogEntity> logs) {
        if (logs == null || logs.isEmpty()) {
            return true;
        }

        for (int i = 0; i < logs.size(); i++) {
            AuditLogEntity current = logs.get(i);

            // Recompute expected hash
            String expectedHash = computeHash(
                    current.getId(),
                    current.getPreviousHash(),
                    current.getTargetContent(),
                    current.getCreatedAt()
            );

            // Check current_hash matches
            if (!expectedHash.equals(current.getCurrentHash())) {
                log.warn("Hash chain break at entry {}: current_hash mismatch", current.getId());
                return false;
            }

            // Check previous_hash links correctly (skip for first entry)
            if (i > 0) {
                AuditLogEntity previous = logs.get(i - 1);
                if (!current.getPreviousHash().equals(previous.getCurrentHash())) {
                    log.warn("Hash chain break at entry {}: previous_hash does not match entry {}",
                            current.getId(), previous.getId());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Verify chain integrity and return detailed break information.
     *
     * @param logs ordered list of audit log entities (sorted by id ASC)
     * @return ChainVerificationResult with validity flag and list of broken links
     */
    public static ChainVerificationResult verifyChainDetailed(List<AuditLogEntity> logs) {
        ChainVerificationResult result = new ChainVerificationResult();
        result.setTotalChecked(logs != null ? logs.size() : 0);

        if (logs == null || logs.isEmpty()) {
            result.setValid(true);
            return result;
        }

        boolean allValid = true;

        for (int i = 0; i < logs.size(); i++) {
            AuditLogEntity current = logs.get(i);

            // Check current_hash
            String expectedHash = computeHash(
                    current.getId(),
                    current.getPreviousHash(),
                    current.getTargetContent(),
                    current.getCreatedAt()
            );

            if (!expectedHash.equals(current.getCurrentHash())) {
                ChainBreakVO breakVO = new ChainBreakVO();
                breakVO.setLogId(current.getId());
                breakVO.setExpectedHash(expectedHash);
                breakVO.setActualHash(current.getCurrentHash());
                breakVO.setPosition("current_hash mismatch at index " + i);
                result.getBrokenLinks().add(breakVO);
                allValid = false;
            }

            // Check previous_hash link
            if (i > 0) {
                AuditLogEntity previous = logs.get(i - 1);
                if (!current.getPreviousHash().equals(previous.getCurrentHash())) {
                    ChainBreakVO breakVO = new ChainBreakVO();
                    breakVO.setLogId(current.getId());
                    breakVO.setExpectedHash(previous.getCurrentHash());
                    breakVO.setActualHash(current.getPreviousHash());
                    breakVO.setPosition("previous_hash link broken between entry "
                            + previous.getId() + " and " + current.getId());
                    result.getBrokenLinks().add(breakVO);
                    allValid = false;
                }
            }
        }

        result.setValid(allValid);
        return result;
    }

    /**
     * Get the current_hash of the last log entry in the list.
     * Used when computing the previous_hash for a new entry.
     *
     * @param logs ordered list of audit log entities (sorted by id DESC, or any order)
     * @return the current_hash of the last entry, or empty string if list is empty
     */
    public static String computePreviousHash(List<AuditLogEntity> logs) {
        if (logs == null || logs.isEmpty()) {
            return "";
        }
        // Get the last entry's current_hash
        return logs.get(logs.size() - 1).getCurrentHash();
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_DIGITS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
