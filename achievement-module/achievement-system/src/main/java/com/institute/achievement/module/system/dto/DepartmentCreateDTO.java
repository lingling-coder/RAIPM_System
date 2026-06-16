package com.institute.achievement.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Department creation DTO.
 */
@Data
public class DepartmentCreateDTO {

    @NotBlank(message = "Department name is required")
    private String deptName;

    @NotBlank(message = "Department code is required")
    private String deptCode;

    private String leader;

    private String phone;

    /** 1=normal, 0=disabled */
    private Integer status;
}
