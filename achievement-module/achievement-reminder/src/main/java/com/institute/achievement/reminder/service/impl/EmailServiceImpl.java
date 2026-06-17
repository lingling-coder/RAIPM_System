package com.institute.achievement.reminder.service.impl;

import com.institute.achievement.common.util.EncryptUtil;
import com.institute.achievement.reminder.dto.EmailConfigDTO;
import com.institute.achievement.reminder.entity.SysConfig;
import com.institute.achievement.reminder.mapper.SysConfigMapper;
import com.institute.achievement.reminder.service.EmailService;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

/**
 * Implementation of EmailService with dynamic SMTP config from sys_config (D-14).
 * <p>
 * Key design decisions:
 * <ul>
 *   <li>JavaMailSenderImpl built at runtime from DB values — no static YAML config (D-14)</li>
 *   <li>Async sending via @Async("emailTaskExecutor") — non-blocking (D-11)</li>
 *   <li>Resilience4j retry 3x with exponential backoff 1min/5min/15min (D-16)</li>
 *   <li>Password AES-256 encrypted at rest (T-4-03)</li>
 *   <li>New JavaMailSenderImpl instance on every config change (Pitfall 1)</li>
 *   <li>Explicit SMTP timeouts to prevent thread pool starvation (Pitfall 5)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final SysConfigMapper sysConfigMapper;
    private final TemplateEngine templateEngine;

    /**
     * The current JavaMailSenderImpl instance.
     * Marked volatile so config changes (refreshConfig) are visible across threads.
     * Rebuilt entirely (new instance) on each config change per Pitfall 1.
     */
    private volatile JavaMailSenderImpl mailSender;

    /**
     * Cached sender name for composing from-address.
     */
    private volatile String cachedSenderName;

    // ── Initialization ──────────────────────────────────────────────────────

    /**
     * Initialize the mail sender on startup from sys_config values.
     * Called by Spring after constructor injection.
     */
    @jakarta.annotation.PostConstruct
    public void initMailSender() {
        this.mailSender = buildMailSender();
    }

    // ── EmailService Interface Implementation ───────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    @Retry(name = "emailSend", fallbackMethod = "sendEmailFallback")
    public void sendEmail(String to, String subject, String htmlBody) {
        JavaMailSenderImpl sender = this.mailSender;
        if (sender == null) {
            log.warn("Email not sent: SMTP not configured (to={}, subject={})", to, subject);
            return;
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = sender.getUsername();
            if (cachedSenderName != null && !cachedSenderName.isBlank()) {
                helper.setFrom(fromAddress, cachedSenderName);
            } else {
                helper.setFrom(fromAddress);
            }

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            sender.send(message);
            log.info("Email sent successfully to {}: subject={}", to, subject);
        } catch (MessagingException e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email to " + to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Unexpected error sending email to " + to, e);
        }
    }

    /**
     * Fallback method called when all email send retry attempts are exhausted (D-16).
     *
     * @param to       recipient email address
     * @param subject  email subject
     * @param htmlBody HTML email body
     * @param ex       the exception that caused the final failure
     */
    @SuppressWarnings("unused")
    private void sendEmailFallback(String to, String subject, String htmlBody, Exception ex) {
        log.error("Email send failed after all retries (to={}, subject={}): {}",
                to, subject, ex.getMessage());
    }

    @Override
    public EmailTestResult testConnection(String testEmail) {
        // Build a fresh sender from current config for the test
        JavaMailSenderImpl testSender = buildMailSender();
        if (testSender == null || testSender.getHost() == null || testSender.getHost().isBlank()) {
            return new EmailTestResult(false, "SMTP未配置，请先保存SMTP服务器配置", 0);
        }

        Instant start = Instant.now();
        try {
            MimeMessage message = testSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = testSender.getUsername();
            String senderName = getConfigValue("smtp.senderName");
            if (senderName != null && !senderName.isBlank()) {
                helper.setFrom(fromAddress, senderName);
            } else {
                helper.setFrom(fromAddress);
            }

            helper.setTo(testEmail);
            helper.setSubject("科研成果管理系统 - SMTP测试邮件");
            helper.setText(buildTestEmailBody(), true);

            testSender.send(message);

            long elapsed = Duration.between(start, Instant.now()).toMillis();
            log.info("SMTP test connection successful: to={}, responseTime={}ms", testEmail, elapsed);
            return new EmailTestResult(true, "连接成功", elapsed);
        } catch (Exception e) {
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            log.warn("SMTP test connection failed: to={}, error={}", testEmail, e.getMessage());
            return new EmailTestResult(false, "连接失败: " + e.getMessage(), elapsed);
        }
    }

    @Override
    public void refreshConfig() {
        JavaMailSenderImpl newSender = buildMailSender();
        this.mailSender = newSender;
        // Also update cached sender name
        this.cachedSenderName = getConfigValue("smtp.senderName");
        log.info("SMTP mail sender refreshed from database config");
    }

    @Override
    public boolean isConfigured() {
        JavaMailSenderImpl sender = this.mailSender;
        return sender != null && sender.getHost() != null && !sender.getHost().isBlank();
    }

    @Override
    public EmailConfigDTO getConfig() {
        EmailConfigDTO dto = new EmailConfigDTO();
        dto.setHost(getConfigValue("smtp.host"));
        dto.setPort(getConfigValueAsInt("smtp.port", 587));
        dto.setUsername(getConfigValue("smtp.username"));
        dto.setPassword(null); // Never return password in GET response
        dto.setSenderName(getConfigValue("smtp.senderName"));
        dto.setTls(getConfigValueAsBoolean("smtp.tls", true));
        return dto;
    }

    @Override
    public void saveConfig(EmailConfigDTO config) {
        // Save each field to sys_config table (upsert pattern)
        upsertConfig("smtp.host", config.getHost(), "SMTP服务器地址");
        upsertConfig("smtp.port", config.getPort() != null ? config.getPort().toString() : "587", "SMTP服务器端口");
        upsertConfig("smtp.username", config.getUsername(), "SMTP账号");
        upsertConfig("smtp.senderName", config.getSenderName(), "发件人名称");
        upsertConfig("smtp.tls", config.getTls() != null ? config.getTls().toString() : "true", "启用TLS");

        // Encrypt password before storing (T-4-03)
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            String encryptedPassword = EncryptUtil.encrypt(config.getPassword());
            upsertConfig("smtp.password", encryptedPassword, "SMTP密码(AES-256加密)");
        }
        // If password is null/empty in update request, keep existing password unchanged

        // Apply changes immediately without restart
        refreshConfig();

        log.info("SMTP configuration saved and applied");
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    /**
     * Build a JavaMailSenderImpl from current sys_config values (D-14).
     * <p>
     * Called on startup (@PostConstruct) and whenever SMTP config is updated.
     * Always creates a NEW instance (Pitfall 1: never modify existing instance).
     *
     * @return configured JavaMailSenderImpl, or null if host is not configured
     */
    private JavaMailSenderImpl buildMailSender() {
        String host = getConfigValue("smtp.host");
        if (host == null || host.isBlank()) {
            log.debug("SMTP host not configured, mail sender not initialized");
            return null;
        }

        int port = getConfigValueAsInt("smtp.port", 587);
        String username = getConfigValue("smtp.username");
        String encryptedPassword = getConfigValue("smtp.password");
        boolean tlsEnabled = getConfigValueAsBoolean("smtp.tls", true);

        // Always create new instance — Pitfall 1: never modify existing instance
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setDefaultEncoding("UTF-8");

        // Decrypt password if encrypted (T-4-03)
        if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
            try {
                String decrypted = EncryptUtil.decrypt(encryptedPassword);
                sender.setPassword(decrypted);
            } catch (Exception e) {
                log.warn("Failed to decrypt SMTP password, using as-is: {}", e.getMessage());
                sender.setPassword(encryptedPassword);
            }
        }

        // Set JavaMail properties (Pitfall 5: explicit timeouts)
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (tlsEnabled) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        props.put("mail.smtp.connectiontimeout", "5000");   // 5s connect timeout
        props.put("mail.smtp.timeout", "3000");              // 3s read timeout
        props.put("mail.smtp.writetimeout", "5000");         // 5s write timeout

        // Cache sender name for email composition
        this.cachedSenderName = getConfigValue("smtp.senderName");

        log.info("SMTP mail sender built: host={}, port={}, tls={}, username={}",
                host, port, tlsEnabled, username);
        return sender;
    }

    /**
     * Build the test email HTML body.
     *
     * @return HTML string for test email
     */
    private String buildTestEmailBody() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: \"Microsoft YaHei\", Arial, sans-serif; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden;'>" +
                "<div style='background-color: #409eff; padding: 20px; text-align: center;'>" +
                "<h1 style='color: #ffffff; font-size: 18px; margin: 0;'>SMTP配置测试</h1>" +
                "</div>" +
                "<div style='padding: 30px;'>" +
                "<p style='color: #333333; font-size: 15px;'>这封邮件是来自科研成果管理系统的SMTP配置测试。</p>" +
                "<p style='color: #333333; font-size: 15px;'>如果您收到此邮件，说明SMTP邮件服务配置正确。</p>" +
                "<p style='color: #999999; font-size: 12px; margin-top: 24px;'>此邮件由系统自动发送，请勿回复。</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Read a config value from sys_config by key.
     *
     * @param key config key
     * @return config value, or null if not found
     */
    private String getConfigValue(String key) {
        try {
            return sysConfigMapper.getValueByKey(key);
        } catch (Exception e) {
            log.debug("Failed to read config key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Read a config value and parse as integer.
     *
     * @param key          config key
     * @param defaultValue fallback if value is null or invalid
     * @return parsed integer value, or defaultValue
     */
    private int getConfigValueAsInt(String key, int defaultValue) {
        String value = getConfigValue(key);
        if (value != null && !value.isBlank()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for config key '{}': {}", key, value);
            }
        }
        return defaultValue;
    }

    /**
     * Read a config value and parse as boolean.
     *
     * @param key          config key
     * @param defaultValue fallback if value is null
     * @return parsed boolean value, or defaultValue
     */
    private boolean getConfigValueAsBoolean(String key, boolean defaultValue) {
        String value = getConfigValue(key);
        if (value != null && !value.isBlank()) {
            return "true".equalsIgnoreCase(value.trim())
                    || "1".equals(value.trim())
                    || "yes".equalsIgnoreCase(value.trim());
        }
        return defaultValue;
    }

    /**
     * Insert or update a sys_config entry.
     * <p>
     * Uses MySQL INSERT ... ON DUPLICATE KEY UPDATE pattern.
     *
     * @param key         config key
     * @param value       config value
     * @param description human-readable description
     */
    private void upsertConfig(String key, String value, String description) {
        try {
            // Check if the key exists
            SysConfig existing = sysConfigMapper.getByKey(key);
            if (existing != null) {
                existing.setConfigValue(value != null ? value : "");
                if (description != null) {
                    existing.setDescription(description);
                }
                sysConfigMapper.updateById(existing);
            } else {
                SysConfig entity = new SysConfig();
                entity.setConfigKey(key);
                entity.setConfigValue(value != null ? value : "");
                entity.setDescription(description);
                sysConfigMapper.insert(entity);
            }
        } catch (Exception e) {
            log.error("Failed to upsert config key '{}': {}", key, e.getMessage());
            throw new RuntimeException("Failed to save configuration: " + key, e);
        }
    }
}
