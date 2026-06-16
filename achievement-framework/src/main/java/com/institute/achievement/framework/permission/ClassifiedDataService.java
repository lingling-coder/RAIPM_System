package com.institute.achievement.framework.permission;

/**
 * Interface for classified data operations.
 * The classified schema (achievement_classified) stores sensitive achievement data
 * with schema-level isolation per D-39.
 *
 * Implementation details and concrete operations will be added in Phase 1
 * when classified achievement management features are implemented.
 */
public interface ClassifiedDataService {

    /**
     * Check if the classified schema connection is available.
     */
    boolean isSchemaAvailable();

    /**
     * Get the count of classified records (for monitoring/dashboard).
     */
    long countClassifiedRecords();
}
