package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a batch import operation.
 * <p>
 * Contains counters for total/success/error/skipped rows, a list of
 * individual ImportError entries, and an optional path to the generated
 * error report Excel file.
 * <p>
 * Implements D-18 (partial import reporting) and D-20 (duplicate skipping).
 */
@Data
public class BatchImportResult {

    /** Total number of rows processed in the uploaded Excel file */
    private int totalRows;

    /** Number of rows successfully imported */
    private int successRows;

    /** Number of rows that failed validation */
    private int errorRows;

    /** Number of rows skipped as duplicates */
    private int skippedRows;

    /** Individual error entries for failed rows */
    private List<ImportError> errors = new ArrayList<>();

    /** Path to the generated error report Excel file (null if no errors) */
    private String errorReportPath;

    /** ID of the import record saved to the database */
    private Long importRecordId;

    /**
     * Convenience method to check if there were any validation or duplicate errors.
     */
    public boolean hasErrors() {
        return errorRows > 0 || skippedRows > 0;
    }

    /**
     * Convenience method to check if any rows were successfully imported.
     */
    public boolean hasSuccess() {
        return successRows > 0;
    }
}
