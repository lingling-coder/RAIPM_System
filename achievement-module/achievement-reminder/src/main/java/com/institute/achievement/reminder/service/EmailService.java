package com.institute.achievement.reminder.service;

import com.institute.achievement.reminder.dto.EmailConfigDTO;

/**
 * Service interface for email delivery via SMTP (API-02).
 * <p>
 * Provides async email sending with Thymeleaf template rendering,
 * SMTP configuration management (D-14), test connection (D-15),
 * and runtime config refresh.
 * <p>
 * All email sending is async via @Async("emailTaskExecutor") (D-11)
 * with Resilience4j retry (3 attempts, exponential backoff) (D-16).
 */
public interface EmailService {

    /**
     * Send an email asynchronously.
     * <p>
     * Called by sendReminderEmailAsync() after Thymeleaf template rendering.
     * Executes on the dedicated emailTaskExecutor thread pool (D-11).
     * Retries up to 3 times on failure with exponential backoff (D-16).
     *
     * @param to       recipient email address
     * @param subject  email subject
     * @param htmlBody HTML email body (already rendered via Thymeleaf)
     */
    void sendEmail(String to, String subject, String htmlBody);

    /**
     * Test SMTP connection by sending a test email (D-15).
     * <p>
     * Runs synchronously for immediate admin feedback.
     * Builds a temporary JavaMailSenderImpl from current config.
     *
     * @param testEmail recipient email address for the test message
     * @return test result with success/failure, message, and response time
     */
    EmailTestResult testConnection(String testEmail);

    /**
     * Refresh the JavaMailSenderImpl from current sys_config values.
     * <p>
     * Called after SMTP config update to rebuild the mail sender
     * without requiring a service restart (D-14).
     * Always creates a NEW JavaMailSenderImpl instance (Pitfall 1 fix).
     */
    void refreshConfig();

    /**
     * Check if SMTP is configured with a valid host.
     *
     * @return true if SMTP host is configured and mailSender is initialized
     */
    boolean isConfigured();

    /**
     * Get the current SMTP configuration (password masked).
     * <p>
     * Returns the SMTP settings from sys_config table with the password
     * field set to null/masked for security.
     *
     * @return current EmailConfigDTO with masked password
     */
    EmailConfigDTO getConfig();

    /**
     * Save/update SMTP configuration in sys_config table.
     * <p>
     * Encrypts the password via AES-256 (T-4-03) before storage.
     * Calls refreshConfig() after save to apply changes immediately.
     *
     * @param config the SMTP configuration to save
     */
    void saveConfig(EmailConfigDTO config);

    // ── Nested Types ────────────────────────────────────────────────────────

    /**
     * Result of an SMTP test connection operation.
     */
    class EmailTestResult {
        private final boolean success;
        private final String message;
        private final long responseTimeMs;

        public EmailTestResult(boolean success, String message, long responseTimeMs) {
            this.success = success;
            this.message = message;
            this.responseTimeMs = responseTimeMs;
        }

        public boolean getSuccess() { return success; }
        public String getMessage() { return message; }
        public long getResponseTimeMs() { return responseTimeMs; }
    }
}
