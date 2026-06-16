package com.institute.achievement.framework.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation that carries JWT claims.
 * Used both by UserDetailsService (during login) and JwtAuthenticationFilter (from token).
 */
@Getter
public class JwtUser implements UserDetails {

    private final Long userId;
    private final String password;
    private final Long deptId;
    private final List<String> roles;
    private final List<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtUser(Long userId, String password, Long deptId,
                   List<String> roles, List<String> permissions,
                   Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.password = password;
        this.deptId = deptId;
        this.roles = roles;
        this.permissions = permissions;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
