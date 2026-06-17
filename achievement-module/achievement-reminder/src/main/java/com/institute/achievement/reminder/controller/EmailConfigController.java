package com.institute.achievement.reminder.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.reminder.dto.EmailConfigDTO;
import com.institute.achievement.reminder.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for SMTP email configuration (API-02).
 * <p>
 * Provides endpoints for managing SMTP settings stored in sys_config table.
 * All endpoints require ROLE_SYSTEM_ADMIN access.
 * <p>
 * Per D-14: SMTP config is runtime-dynamic, changes apply immediately without restart.
 * Per D-15: Test email send feature for verifying SMTP configuration.
 * Per D-22: Config page accessible under 系统管理 menu.
 */
@Slf4j
@RestController
@RequestMapping("/api/system/email-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
public class EmailConfigController {

    private final EmailService emailService;

    /**
     * Get current SMTP configuration.
     * <p>
     * Returns the SMTP settings from sys_config table.
     * Password field is always null/masked in the response for security.
     *
     * @return current EmailConfigDTO with masked password
     */
    @GetMapping
    public Result<EmailConfigDTO> getConfig() {
        EmailConfigDTO config = emailService.getConfig();
        return Result.success(config);
    }

    /**
     * Save/update SMTP configuration.
     * <p>
     * Encrypts the password (AES-256) before storing in sys_config.
     * Applies changes immediately - no restart required (D-14).
     * If password is empty/null in the request body, keeps existing password.
     *
     * @param config the SMTP configuration to save
     * @return success response
     */
    @PutMapping
    public Result<Void> saveConfig(@Valid @RequestBody EmailConfigDTO config) {
        emailService.saveConfig(config);
        log.info("SMTP configuration updated by admin");
        return Result.success();
    }

    /**
     * Test SMTP connection by sending a test email (D-15).
     * <p>
     * Builds a temporary JavaMailSenderImpl from current config,
     * sends a test message to the specified address, and returns
     * the result with response time.
     *
     * @param request containing the test email address
     * @return test result with success/failure, message, and response time
     */
    @PostMapping("/test")
    public Result<EmailService.EmailTestResult> testConnection(@RequestBody @Valid TestEmailRequest request) {
        EmailService.EmailTestResult result = emailService.testConnection(request.getTestEmail());
        log.info("SMTP test connection result: success={}, responseTime={}ms",
                result.getSuccess(), result.getResponseTimeMs());
        return Result.success(result);
    }

    /**
     * Internal DTO for test email request body.
     */
    public static class TestEmailRequest {
        private String testEmail;

        public String getTestEmail() {
            return testEmail;
        }

        public void setTestEmail(String testEmail) {
            this.testEmail = testEmail;
        }
    }
}
