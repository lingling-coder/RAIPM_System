package com.institute.achievement.common.constant;

/**
 * Field length and value constants shared across the achievement domain.
 * Centralized to avoid magic numbers and ensure consistency between
 * entity definitions, DTO validation, and database schema.
 */
public final class AchievementConstant {

    private AchievementConstant() {
        // Utility class
    }

    // ── Field Length Limits ──────────────────────────────────────────

    /** Paper title / Patent name / Copyright name max length */
    public static final int TITLE_MAX_LENGTH = 500;

    /** Authors / Inventors field max length (semicolon-separated) */
    public static final int AUTHORS_MAX_LENGTH = 1000;

    /** Journal / Source name max length */
    public static final int JOURNAL_MAX_LENGTH = 500;

    /** DOI field max length */
    public static final int DOI_MAX_LENGTH = 255;

    /** ISSN / CN field max length */
    public static final int ISSN_MAX_LENGTH = 50;

    /** Abstract field max length (stored as TEXT, but validated at this limit) */
    public static final int ABSTRACT_MAX_LENGTH = 2000;

    /** Project reference free text max length */
    public static final int PROJECT_REF_MAX_LENGTH = 500;

    /** Original file name max length */
    public static final int FILE_NAME_MAX_LENGTH = 500;

    /** Archive number max length */
    public static final int ARCHIVE_NO_MAX_LENGTH = 100;

    // ── Status Values for Select Options ─────────────────────────────

    public static final String[] INDEX_STATUS_OPTIONS = {
            "SCI", "SSCI", "EI", "CPCI", "CSCD", "CSSCI", "北大核心", "其他"
    };

    public static final String[] ZONE_OPTIONS = {
            "一区", "二区", "三区", "四区", "无"
    };

    public static final String[] CLASSIFIED_LEVEL_OPTIONS = {
            "秘密", "机密"
    };

    // ── File Upload Limits ──────────────────────────────────────────

    /** Maximum single file size in bytes (50MB) */
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;

    /** Maximum HTTP request size including multipart overhead (55MB) */
    public static final long MAX_REQUEST_SIZE = 55 * 1024 * 1024L;

    /** Allowed MIME types for attachment upload */
    public static final String[] ALLOWED_CONTENT_TYPES = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/png", "image/jpeg", "image/jpg",
            "application/x-zip-compressed",
            "application/zip",
            "application/x-rar-compressed",
            "application/vnd.rar"
    };

    /** Allowed file extensions */
    public static final String[] ALLOWED_EXTENSIONS = {
            ".pdf", ".doc", ".docx", ".xls", ".xlsx",
            ".png", ".jpg", ".jpeg",
            ".zip", ".rar"
    };
}
