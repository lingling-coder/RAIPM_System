package com.institute.achievement.module.system.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

/**
 * User update DTO.
 * Password is not updated here - use resetPassword endpoint (D-14).
 */
@Data
public class UserUpdateDTO {

    private Long id;

    private String realName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private Long deptId;

    /** List of role IDs for multi-role assignment (D-11) */
    private List<Long> roleIds;

    /** 1=normal, 0=disabled (D-19) */
    private Integer status;
}
