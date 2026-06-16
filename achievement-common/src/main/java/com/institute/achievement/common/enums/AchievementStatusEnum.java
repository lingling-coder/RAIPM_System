package com.institute.achievement.common.enums;

import lombok.Getter;

/**
 * Lifecycle states for all achievement types (paper, patent, copyright).
 * Implements the state machine defined in RESEARCH.md lines 519-543.
 *
 * State transition rules (enforced in ApprovalAction / Service layer):
 * <pre>
 *   DRAFT                     → PENDING_DEPT_REVIEW  (submit)
 *   PENDING_DEPT_REVIEW       → PENDING_ADMIN_ARCHIVE (dept pass)
 *   PENDING_DEPT_REVIEW       → REJECTED              (dept reject)
 *   PENDING_ADMIN_ARCHIVE     → ARCHIVED              (admin archive)
 *   PENDING_ADMIN_ARCHIVE     → REJECTED              (admin reject)
 *   PENDING_DEPT_REVIEW       → WITHDRAWN             (submitter withdraw)
 *   PENDING_ADMIN_ARCHIVE     → WITHDRAWN             (submitter withdraw)
 *   REJECTED                  → PENDING_DEPT_REVIEW   (resubmit)
 *   ARCHIVED                  → INVALIDATED           (invalidate)
 * </pre>
 */
@Getter
public enum AchievementStatusEnum {

    DRAFT("草稿"),
    PENDING_DEPT_REVIEW("待部门审核"),
    PENDING_ADMIN_ARCHIVE("待管理员归档"),
    ARCHIVED("已归档"),
    REJECTED("已退回"),
    INVALIDATED("已作废"),
    WITHDRAWN("已撤回");

    private final String label;

    AchievementStatusEnum(String label) {
        this.label = label;
    }

    /**
     * Resolve status by name.
     */
    public static AchievementStatusEnum fromName(String name) {
        for (AchievementStatusEnum status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown achievement status: " + name);
    }

    // ── Convenience Checks ──────────────────────────────────────────

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isPendingReview() {
        return this == PENDING_DEPT_REVIEW || this == PENDING_ADMIN_ARCHIVE;
    }

    public boolean isArchived() {
        return this == ARCHIVED;
    }

    public boolean isTerminal() {
        return this == ARCHIVED || this == INVALIDATED;
    }
}
