package com.institute.achievement.framework.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a hash chain integrity verification operation.
 * Contains the overall validity flag and a list of any broken links.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChainVerificationResult {

    /** Whether the entire checked chain segment is valid */
    private boolean valid;

    /** List of broken links found (empty if chain is intact) */
    private List<ChainBreakVO> brokenLinks = new ArrayList<>();

    /** Total number of entries checked */
    private int totalChecked;
}
