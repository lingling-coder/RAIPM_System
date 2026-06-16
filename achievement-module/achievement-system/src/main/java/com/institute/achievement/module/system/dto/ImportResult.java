package com.institute.achievement.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CSV import result DTO.
 * Returns counts of inserted, updated, and failed records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /** Number of new records inserted */
    private int inserted;

    /** Number of existing records updated */
    private int updated;

    /** Number of failed records */
    private int failed;

    /** List of error messages for failed records */
    private List<String> errors;

    /** Total records processed */
    public int getTotal() {
        return inserted + updated + failed;
    }
}
