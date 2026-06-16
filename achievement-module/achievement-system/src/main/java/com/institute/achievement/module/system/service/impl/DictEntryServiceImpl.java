package com.institute.achievement.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.institute.achievement.common.exception.BusinessException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.module.system.dto.DictEntryDTO;
import com.institute.achievement.module.system.entity.SysDictCategory;
import com.institute.achievement.module.system.entity.SysDictEntry;
import com.institute.achievement.module.system.mapper.SysDictCategoryMapper;
import com.institute.achievement.module.system.mapper.SysDictEntryMapper;
import com.institute.achievement.module.system.service.DictEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dictionary entry service implementation.
 */
@Service
@RequiredArgsConstructor
public class DictEntryServiceImpl extends ServiceImpl<SysDictEntryMapper, SysDictEntry> implements DictEntryService {

    private final SysDictCategoryMapper dictCategoryMapper;

    @Override
    public PageResult<DictEntryDTO> page(Long categoryId, String keyword, PageQuery dto) {
        LambdaQueryWrapper<SysDictEntry> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(SysDictEntry::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(SysDictEntry::getDictKey, keyword)
                    .or()
                    .like(SysDictEntry::getDictValue, keyword)
            );
        }
        wrapper.orderByAsc(SysDictEntry::getSortOrder);

        IPage<SysDictEntry> page = this.baseMapper.selectPage(
                new Page<>(dto.getPage(), dto.getPageSize()), wrapper
        );

        List<DictEntryDTO> voList = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DictEntryDTO dto) {
        // Check unique key within category
        Long count = this.baseMapper.selectCount(
                new LambdaQueryWrapper<SysDictEntry>()
                        .eq(SysDictEntry::getCategoryId, dto.getCategoryId())
                        .eq(SysDictEntry::getDictKey, dto.getDictKey())
        );
        if (count > 0) {
            throw new BusinessException("Dictionary key already exists in this category: " + dto.getDictKey());
        }

        SysDictEntry entry = new SysDictEntry();
        entry.setCategoryId(dto.getCategoryId());
        entry.setDictKey(dto.getDictKey());
        entry.setDictValue(dto.getDictValue());
        entry.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entry.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        this.baseMapper.insert(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DictEntryDTO dto) {
        SysDictEntry entry = this.baseMapper.selectById(dto.getId());
        if (entry == null) {
            throw new EntityNotFoundException("DictEntry", dto.getId());
        }

        // If key changed, check uniqueness
        if (dto.getDictKey() != null && !dto.getDictKey().equals(entry.getDictKey())) {
            Long count = this.baseMapper.selectCount(
                    new LambdaQueryWrapper<SysDictEntry>()
                            .eq(SysDictEntry::getCategoryId, entry.getCategoryId())
                            .eq(SysDictEntry::getDictKey, dto.getDictKey())
                            .ne(SysDictEntry::getId, dto.getId())
            );
            if (count > 0) {
                throw new BusinessException("Dictionary key already exists in this category: " + dto.getDictKey());
            }
        }

        entry.setDictKey(dto.getDictKey());
        entry.setDictValue(dto.getDictValue());
        entry.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) {
            entry.setStatus(dto.getStatus());
        }
        this.baseMapper.updateById(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDictEntry entry = this.baseMapper.selectById(id);
        if (entry == null) {
            throw new EntityNotFoundException("DictEntry", id);
        }
        this.baseMapper.deleteById(id);
    }

    private DictEntryDTO toDTO(SysDictEntry entity) {
        DictEntryDTO dto = new DictEntryDTO();
        dto.setId(entity.getId());
        dto.setCategoryId(entity.getCategoryId());
        dto.setDictKey(entity.getDictKey());
        dto.setDictValue(entity.getDictValue());
        dto.setSortOrder(entity.getSortOrder());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());

        // Resolve category name
        SysDictCategory category = dictCategoryMapper.selectById(entity.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getCategoryName());
        }
        return dto;
    }
}
