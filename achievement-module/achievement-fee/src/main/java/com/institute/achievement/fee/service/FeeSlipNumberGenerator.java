package com.institute.achievement.fee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis INCR-based fee slip number generator (D-07).
 * <p>
 * Generates unique, sequential slip numbers in the format {@code FEE-YYYYMMDD-XXX}
 * where YYYYMMDD is today's date and XXX is a zero-padded daily sequence number.
 * Uses Redis INCR for atomic per-day counters with 2-day TTL.
 *
 * <h3>Format</h3>
 * <pre>
 *   FEE-20260616-001
 *   FEE-20260616-002
 *   ...
 *   FEE-20260617-001  (next day, counter resets)
 * </pre>
 *
 * <h3>Threat model alignment</h3>
 * <ul>
 *   <li>T-02-04-02: Redis INCR with daily prefix is atomic and unique per day.
 *       3-digit padding supports up to 999 slips/day (more than sufficient).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeSlipNumberGenerator {

    private static final String KEY_PREFIX = "fee:slip:seq:";
    private static final long KEY_TTL_HOURS = 48; // 2 days
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;

    /**
     * Generate a unique slip number for today.
     * <p>
     * Uses Redis INCR with a daily key prefix ({@code fee:slip:seq:YYYYMMDD}).
     * Sets a 2-day TTL on first creation so old keys expire automatically.
     *
     * @return slip number string in format "FEE-YYYYMMDD-XXX"
     */
    public String generateSlipNo() {
        LocalDate today = LocalDate.now();
        String datePart = today.format(DATE_FORMATTER);
        String redisKey = KEY_PREFIX + datePart;

        // Atomically increment the daily counter
        Long seq = redisTemplate.opsForValue().increment(redisKey);

        // On first call (seq == 1), set TTL so the key auto-expires after 2 days
        if (seq != null && seq == 1) {
            redisTemplate.expire(redisKey, Duration.ofHours(KEY_TTL_HOURS));
        }

        String slipNo = formatSlipNo(datePart, seq != null ? seq : 1L);
        log.debug("Generated slipNo: {} (seq={})", slipNo, seq);
        return slipNo;
    }

    /**
     * Format a slip number from date part and sequence number.
     *
     * @param datePart the date part in yyyyMMdd format
     * @param seq      the sequence number (will be zero-padded to 3 digits)
     * @return formatted slip number string
     */
    public String formatSlipNo(String datePart, long seq) {
        return String.format("FEE-%s-%03d", datePart, seq);
    }
}
