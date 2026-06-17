package com.institute.achievement.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * System configuration entity mapped to the sys_config table.
 * <p>
 * Stores key-value configuration entries for system-level settings.
 * Currently used for SMTP email settings (D-14), can be extended for
 * other system-level configurations in future phases.
 * <p>
 * Per D-14: Runtime-dynamic configuration, no restart needed.
 * Per T-4-03: Sensitive values (e.g., SMTP password) are AES-256 encrypted.
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Unique configuration key, e.g. "smtp.host", "smtp.port" */
    private String configKey;

    /** Configuration value (string/text) */
    private String configValue;

    /** Human-readable description of this config entry */
    private String description;

    /** Last update timestamp */
    private LocalDateTime updatedTime;
}
