package com.institute.achievement.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single row error during batch import.
 * <p>
 * Captures the row index (1-based, matching Excel row numbers),
 * the achievement type of the row, and one or more validation error messages.
 * <p>
 * Implements D-18 error reporting: failed rows are recorded with reasons
 * for later export to an error report Excel file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {

    /** Excel row number (1-based, header row excluded) */
    private int rowIndex;

    /** Achievement type (paper/patent/copyright) as determined from the row */
    private String type;

    /** Human-readable validation error messages */
    private List<String> reasons;
}
