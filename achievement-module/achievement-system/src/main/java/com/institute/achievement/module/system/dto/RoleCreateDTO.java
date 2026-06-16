package com.institute.achievement.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Role creation DTO.
 */
@Data
public class RoleCreateDTO {

    @NotBlank(message = "Role name is required")
    private String roleName;

    @NotBlank(message = "Role code is required")
    private String roleCode;

    private String description;
}
