package com.institute.achievement.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Login result VO returned on successful authentication.
 * Contains access token and basic user info for frontend state initialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO {

    private String accessToken;
    private String tokenType;

    /** Token expiration in seconds */
    private long expiresIn;

    /** Basic user info */
    private UserInfo userInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private Long deptId;
        private String deptName;
        private List<String> roles;
        private List<String> permissions;
        private boolean passwordChangeRequired;
    }
}
