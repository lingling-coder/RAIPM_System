package com.institute.achievement.reminder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * DTO for SMTP email configuration (D-14, D-15).
 * <p>
 * Used by EmailConfigController for both GET (read) and PUT (save) operations.
 * The password field is write-only: it is never returned in GET responses,
 * only accepted from the frontend when updating the config.
 * <p>
 * Per D-14: SMTP config is stored in sys_config table, runtime-dynamic.
 * Per T-4-03: Password is AES-256 encrypted at rest.
 */
@Data
public class EmailConfigDTO {

    /** SMTP server hostname (e.g., smtp.example.com) */
    private String host;

    /** SMTP server port (default 587 for STARTTLS) */
    @Min(1)
    @Max(65535)
    private Integer port = 587;

    /** SMTP authentication username */
    private String username;

    /**
     * SMTP password (sensitive field, write-only).
     * <p>
     * When sending from frontend to backend: contains the plaintext password
     * (or new password on update). When returning in GET response: always null/masked.
     * When empty/null in update request: keep existing password unchanged.
     */
    private String password;

    /** Display name for the "From" field in emails */
    private String senderName;

    /** Whether to enable STARTTLS (default true) */
    private Boolean tls = true;

    /** Test email address for the test connection feature (D-15) */
    private String testEmail;
}
