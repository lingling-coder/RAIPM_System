package com.institute.achievement.fee.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Fee statistics view object — output for fee stats API responses.
 * <p>
 * Serves dual purpose:
 * <ul>
 *   <li><b>Overview:</b> Only aggregate fields are populated (totalAmount, totalPaid, totalPending, totalOverdue, recordCount)</li>
 *   <li><b>Dimension row:</b> All fields populated including dimensionValue, dimensionCode, plus optional cross-filter fields</li>
 * </ul>
 * All BigDecimal fields default to {@link BigDecimal#ZERO} to avoid null in JSON responses.
 */
@Data
public class FeeStatsVO {

    // ── Aggregate fields ────────────────────────────────────────────

    /** Total amount of all fee records */
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Sum of paid_amount for records with status = 'paid' */
    private BigDecimal totalPaid = BigDecimal.ZERO;

    /** Sum of amount for records with status = 'pending' */
    private BigDecimal totalPending = BigDecimal.ZERO;

    /** Sum of amount for overdue pending records (due_date &lt; CURDATE() AND status = 'pending') */
    private BigDecimal totalOverdue = BigDecimal.ZERO;

    /** Count of fee records */
    private Long recordCount = 0L;

    // ── Dimension fields ────────────────────────────────────────────

    /** The group-by dimension value (e.g., department name, year, patent type label, funding source label) */
    private String dimensionValue;

    /** The raw code of the dimension value (e.g., dept_id, patent_type code, funding_source code) */
    private String dimensionCode;

    // ── Cross-filter fields (used in detail/export queries) ──────────

    /** Department ID */
    private Long deptId;

    /** Fee due date year */
    private Integer year;

    /** Fee type code (e.g., annual_fee, registration_fee) */
    private String feeType;

    /** Funding source code from data dictionary */
    private String fundingSource;
}
