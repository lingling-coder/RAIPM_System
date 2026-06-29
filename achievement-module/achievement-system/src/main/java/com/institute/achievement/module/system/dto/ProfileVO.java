package com.institute.achievement.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Profile view object returned to the frontend.
 * Contains personal information and role/department details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileVO {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Long deptId;
    private String deptName;
    private List<String> roleNames;
    private List<String> roles;
    private List<String> permissions;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private boolean passwordChangeRequired;
}
