package com.institute.achievement.framework.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single break in the hash chain.
 * Contains the log entry ID, the expected vs actual hash values,
 * and a description of where the break occurred.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChainBreakVO {

    /** ID of the log entry where the break was detected */
    private Long logId;

    /** The expected hash value */
    private String expectedHash;

    /** The actual hash value stored in the entry */
    private String actualHash;

    /** Description of the break position and type */
    private String position;
}
