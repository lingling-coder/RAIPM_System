package com.institute.achievement.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a duplicate check performed at submit time.
 * <p>
 * When a duplicate is detected (D-45/D-46), this object carries the
 * existing achievement's basic information for the frontend DuplicateDialog
 * to display, enabling the user to decide whether to view the existing
 * record or continue with submission regardless.
 * <p>
 * Implements UI-SPEC §6.9 (DuplicateDialog data contract).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckResult {

    /** Whether a duplicate was found */
    private boolean duplicate;

    /** ID of the existing duplicate achievement, if found */
    private Long existingId;

    /** Title/name of the existing achievement */
    private String existingTitle;

    /** Chinese type label: 论文/专利/软件著作权 */
    private String existingType;

    /** Chinese status label (e.g. 已归档, 待部门审核) */
    private String existingStatus;

    /** Submission time of the existing achievement */
    private String existingSubmitTime;

    /**
     * Create a "no duplicate" result.
     */
    public static DuplicateCheckResult noDuplicate() {
        return DuplicateCheckResult.builder().duplicate(false).build();
    }
}
