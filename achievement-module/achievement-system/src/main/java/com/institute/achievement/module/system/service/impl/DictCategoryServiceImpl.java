package com.institute.achievement.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.institute.achievement.common.exception.BusinessException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.module.system.dto.DictCategoryDTO;
import com.institute.achievement.module.system.entity.SysDictCategory;
import com.institute.achievement.module.system.entity.SysDictEntry;
import com.institute.achievement.module.system.mapper.SysDictCategoryMapper;
import com.institute.achievement.module.system.mapper.SysDictEntryMapper;
import com.institute.achievement.module.system.service.DictCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dictionary category service implementation.
 */
@Service
@RequiredArgsConstructor
public class DictCategoryServiceImpl extends ServiceImpl<SysDictCategoryMapper, SysDictCategory> implements DictCategoryService {

    private final SysDictEntryMapper dictEntryMapper;

    @Override
    public List<DictCategoryDTO> listAll() {
        List<SysDictCategory> categories = this.baseMapper.selectAllOrderBySort();
        return categories.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DictCategoryDTO dto) {
        // Check code uniqueness
        Long count = this.baseMapper.selectCount(
                new LambdaQueryWrapper<SysDictCategory>()
                        .eq(SysDictCategory::getCategoryCode, dto.getCategoryCode())
        );
        if (count > 0) {
            throw new BusinessException("Category code already exists: " + dto.getCategoryCode());
        }

        SysDictCategory category = new SysDictCategory();
        category.setCategoryName(dto.getCategoryName());
        category.setCategoryCode(dto.getCategoryCode());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        this.baseMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DictCategoryDTO dto) {
        SysDictCategory category = this.baseMapper.selectById(dto.getId());
        if (category == null) {
            throw new EntityNotFoundException("DictCategory", dto.getId());
        }

        category.setCategoryName(dto.getCategoryName());
        category.setCategoryCode(dto.getCategoryCode());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        this.baseMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDictCategory category = this.baseMapper.selectById(id);
        if (category == null) {
            throw new EntityNotFoundException("DictCategory", id);
        }

        // Check if entries exist
        Long entryCount = dictEntryMapper.selectCount(
                new LambdaQueryWrapper<SysDictEntry>().eq(SysDictEntry::getCategoryId, id)
        );
        if (entryCount > 0) {
            throw new BusinessException("Cannot delete category '" + category.getCategoryName()
                    + "': " + entryCount + " entry(s) exist under this category");
        }

        this.baseMapper.deleteById(id);
    }

    private DictCategoryDTO toDTO(SysDictCategory entity) {
        DictCategoryDTO dto = new DictCategoryDTO();
        dto.setId(entity.getId());
        dto.setCategoryName(entity.getCategoryName());
        dto.setCategoryCode(entity.getCategoryCode());
        dto.setDescription(entity.getDescription());
        dto.setSortOrder(entity.getSortOrder());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
