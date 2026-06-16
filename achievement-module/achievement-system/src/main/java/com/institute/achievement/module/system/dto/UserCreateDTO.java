package com.institute.achievement.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * User creation DTO.
 * Password validated in service: min 8 chars, must contain letter + number (D-12).
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50, message = "Username must be 4-50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String realName;

    private String email;

    private String phone;

    private Long deptId;

    /** List of role IDs for multi-role assignment (D-11) */
    private List<Long> roleIds;
}
