package com.institute.achievement.module.system.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * Profile update DTO.
 * Users can only update their name, email, and phone (D-16).
 * Username, department, and roles are read-only (admin-only modification).
 */
@Data
public class ProfileUpdateDTO {

    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;
}
