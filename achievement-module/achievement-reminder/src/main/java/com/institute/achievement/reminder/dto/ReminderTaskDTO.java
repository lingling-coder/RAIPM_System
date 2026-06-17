package com.institute.achievement.reminder.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Query DTO for reminder task listing.
 * <p>
 * Supports filtering by config ID, user ID (admin query),
 * type code, status, and deadline date range.
 */
@Data
public class ReminderTaskDTO {

    /** Filter by config ID */
    private Long configId;

    /** Filter by user ID (admin use only) */
    private Long userId;

    /** Filter by reminder type code */
    private String typeCode;

    /** Filter by urgency: HIGH / MEDIUM / LOW */
    private String urgency;

    /** Filter by status: unconfirmed / confirmed */
    private String status;

    /** Filter: deadline range start (inclusive) */
    private LocalDate startDate;

    /** Filter: deadline range end (inclusive) */
    private LocalDate endDate;
}
