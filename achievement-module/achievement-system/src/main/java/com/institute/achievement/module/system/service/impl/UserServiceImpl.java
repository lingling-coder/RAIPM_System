package com.institute.achievement.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.institute.achievement.common.exception.BadRequestException;
import com.institute.achievement.common.exception.BusinessException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.module.system.dto.*;
import com.institute.achievement.module.system.entity.SysDepartment;
import com.institute.achievement.module.system.entity.SysRole;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.entity.SysUserRole;
import com.institute.achievement.module.system.mapper.SysDepartmentMapper;
import com.institute.achievement.module.system.mapper.SysRoleMapper;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import com.institute.achievement.module.system.mapper.SysUserRoleMapper;
import com.institute.achievement.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * User service implementation.
 * Handles CRUD, multi-role assignment (D-17), password policy (D-12),
 * enable/disable (D-19), soft delete (D-20), and CSV import (D-09).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private static final Pattern LETTER_NUMBER_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).+$");
    private static final String DEFAULT_RESET_PASSWORD = "password123";
    private static final int MAX_IMPORT_ROWS = 1000;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDepartmentMapper departmentMapper;

    @Override
    public PageResult<UserVO> page(UserPageDTO dto) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeleted, 0);

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, dto.getKeyword())
                    .or()
                    .like(SysUser::getRealName, dto.getKeyword())
            );
        }
        if (dto.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, dto.getDeptId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, dto.getStatus());
        }

        IPage<SysUser> page = this.baseMapper.selectPage(
                new Page<>(dto.getPage(), dto.getPageSize()),
                wrapper
        );

        List<UserVO> voList = page.getRecords().stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public UserVO getById(Long id) {
        SysUser user = this.baseMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new EntityNotFoundException("User", id);
        }
        return toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(UserCreateDTO dto) {
        // Check username uniqueness
        Long count = this.baseMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("Username already exists: " + dto.getUsername());
        }

        // Validate password policy (D-12)
        if (!isValidPassword(dto.getPassword())) {
            throw new BadRequestException("Password must be at least 8 characters with both letters and numbers");
        }

        // Create user
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setStatus(1);
        user.setPasswordChangeRequired(0);

        this.baseMapper.insert(user);

        // Insert role associations (D-11/D-17)
        if (CollUtil.isNotEmpty(dto.getRoleIds())) {
            insertUserRoles(user.getId(), dto.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateDTO dto) {
        SysUser user = this.baseMapper.selectById(dto.getId());
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new EntityNotFoundException("User", dto.getId());
        }

        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }

        this.baseMapper.updateById(user);

        // Reassign roles
        if (dto.getRoleIds() != null) {
            // Remove existing role associations
            userRoleMapper.delete(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, dto.getId())
            );
            // Insert new role associations
            if (CollUtil.isNotEmpty(dto.getRoleIds())) {
                insertUserRoles(dto.getId(), dto.getRoleIds());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = this.baseMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new EntityNotFoundException("User", id);
        }
        // MyBatis-Plus @TableLogic will convert to UPDATE deleted=1
        this.baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long id, Integer status) {
        SysUser user = this.baseMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new EntityNotFoundException("User", id);
        }
        if (status != 0 && status != 1) {
            throw new BadRequestException("Status must be 0 (disabled) or 1 (enabled)");
        }
        user.setStatus(status);
        this.baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id) {
        SysUser user = this.baseMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new EntityNotFoundException("User", id);
        }
        user.setPassword(passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
        user.setPasswordChangeRequired(1);
        this.baseMapper.updateById(user);
        log.info("Password reset for user: {} by admin", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int inserted = 0;
        int updated = 0;
        int failed = 0;
        int rowCount = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Read header line
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new BadRequestException("CSV file is empty");
            }

            String line;
            while ((line = br.readLine()) != null && rowCount < MAX_IMPORT_ROWS) {
                rowCount++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    String[] fields = parseCsvLine(line);
                    // Expected: username, realName, email, phone, deptCode
                    if (fields.length < 1 || fields[0].isEmpty()) {
                        failed++;
                        errors.add("Row " + (rowCount + 1) + ": missing username");
                        continue;
                    }

                    String username = fields[0].trim();
                    String realName = fields.length > 1 ? fields[1].trim() : null;
                    String email = fields.length > 2 ? fields[2].trim() : null;
                    String phone = fields.length > 3 ? fields[3].trim() : null;
                    String deptCode = fields.length > 4 ? fields[4].trim() : null;

                    // Check if user exists by username (D-09: match by username)
                    SysUser existingUser = this.baseMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
                    );

                    Long deptId = null;
                    if (deptCode != null && !deptCode.isEmpty()) {
                        SysDepartment dept = departmentMapper.selectOne(
                                new LambdaQueryWrapper<SysDepartment>()
                                        .eq(SysDepartment::getDeptCode, deptCode)
                                        .eq(SysDepartment::getDeleted, 0)
                        );
                        if (dept != null) {
                            deptId = dept.getId();
                        }
                    }

                    if (existingUser != null) {
                        // Update existing (D-09: overwrite)
                        existingUser.setRealName(realName);
                        existingUser.setEmail(email);
                        existingUser.setPhone(phone);
                        existingUser.setDeptId(deptId);
                        this.baseMapper.updateById(existingUser);
                        updated++;
                    } else {
                        // Insert new
                        SysUser newUser = new SysUser();
                        newUser.setUsername(username);
                        newUser.setPassword(passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
                        newUser.setRealName(realName);
                        newUser.setEmail(email);
                        newUser.setPhone(phone);
                        newUser.setDeptId(deptId);
                        newUser.setStatus(1);
                        newUser.setPasswordChangeRequired(1);
                        this.baseMapper.insert(newUser);
                        inserted++;
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + (rowCount + 1) + ": " + e.getMessage());
                }
            }

            if (rowCount >= MAX_IMPORT_ROWS) {
                errors.add("Maximum " + MAX_IMPORT_ROWS + " rows per import, some rows may be skipped");
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Failed to parse CSV file: " + e.getMessage());
        }

        return ImportResult.builder()
                .inserted(inserted)
                .updated(updated)
                .failed(failed)
                .errors(errors)
                .build();
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setDeptId(user.getDeptId());
        vo.setStatus(user.getStatus());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setPasswordChangeRequired(user.getPasswordChangeRequired());
        vo.setCreatedAt(user.getCreatedAt());

        // Resolve department name
        if (user.getDeptId() != null) {
            SysDepartment dept = departmentMapper.selectById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }

        // Resolve roles
        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        if (CollUtil.isNotEmpty(roles)) {
            vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        }

        return vo;
    }

    private void insertUserRoles(Long userId, List<Long> roleIds) {
        List<SysUserRole> list = roleIds.stream()
                .map(roleId -> {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    return ur;
                })
                .collect(Collectors.toList());
        userRoleMapper.insert(list);
    }

    /**
     * Validate password per D-12: minimum 8 characters, must contain letter + number.
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return LETTER_NUMBER_PATTERN.matcher(password).matches();
    }

    /**
     * Parse a CSV line respecting basic quoting.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
