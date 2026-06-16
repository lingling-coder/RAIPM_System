package com.institute.achievement.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.institute.achievement.common.exception.BusinessException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.module.system.dto.*;
import com.institute.achievement.module.system.entity.SysDepartment;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.mapper.SysDepartmentMapper;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import com.institute.achievement.module.system.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Department service implementation.
 * Flat structure per D-08. Blocks delete if members exist.
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<SysDepartmentMapper, SysDepartment> implements DepartmentService {

    private final SysUserMapper userMapper;

    @Override
    public PageResult<DepartmentVO> page(PageQuery dto) {
        IPage<SysDepartment> page = this.baseMapper.selectPage(
                new Page<>(dto.getPage(), dto.getPageSize()),
                new LambdaQueryWrapper<SysDepartment>().eq(SysDepartment::getDeleted, 0)
        );

        List<DepartmentVO> voList = page.getRecords().stream()
                .map(this::toDepartmentVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public DepartmentVO getById(Long id) {
        SysDepartment dept = this.baseMapper.selectById(id);
        if (dept == null || dept.getDeleted() == 1) {
            throw new EntityNotFoundException("Department", id);
        }
        return toDepartmentVO(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DepartmentCreateDTO dto) {
        // Check dept code uniqueness
        Long count = this.baseMapper.selectCount(
                new LambdaQueryWrapper<SysDepartment>().eq(SysDepartment::getDeptCode, dto.getDeptCode())
        );
        if (count > 0) {
            throw new BusinessException("Department code already exists: " + dto.getDeptCode());
        }

        SysDepartment dept = new SysDepartment();
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        this.baseMapper.insert(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DepartmentUpdateDTO dto) {
        SysDepartment dept = this.baseMapper.selectById(dto.getId());
        if (dept == null || dept.getDeleted() == 1) {
            throw new EntityNotFoundException("Department", dto.getId());
        }

        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            dept.setStatus(dto.getStatus());
        }
        this.baseMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDepartment dept = this.baseMapper.selectById(id);
        if (dept == null || dept.getDeleted() == 1) {
            throw new EntityNotFoundException("Department", id);
        }

        // Check if users are assigned to this department
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id)
        );
        if (userCount > 0) {
            throw new BusinessException("Cannot delete department '" + dept.getDeptName()
                    + "': " + userCount + " member(s) are assigned");
        }

        this.baseMapper.deleteById(id);
    }

    @Override
    public List<DepartmentVO> listAll() {
        List<SysDepartment> depts = this.baseMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getDeleted, 0)
                        .eq(SysDepartment::getStatus, 1)
        );
        return depts.stream().map(this::toDepartmentVO).collect(Collectors.toList());
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private DepartmentVO toDepartmentVO(SysDepartment dept) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(dept.getId());
        vo.setDeptName(dept.getDeptName());
        vo.setDeptCode(dept.getDeptCode());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setStatus(dept.getStatus());
        vo.setCreatedAt(dept.getCreatedAt());

        // Count members
        Long memberCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, dept.getId())
        );
        vo.setMemberCount(memberCount.intValue());

        return vo;
    }
}
