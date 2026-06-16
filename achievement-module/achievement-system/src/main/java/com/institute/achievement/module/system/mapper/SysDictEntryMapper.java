package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.SysDictEntry;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Dictionary entry mapper.
 */
public interface SysDictEntryMapper extends BaseMapper<SysDictEntry> {

    /**
     * Select active entries for a category, ordered by sort.
     */
    @Select("SELECT * FROM sys_dict_entry WHERE category_id = #{categoryId} AND status = 1 ORDER BY sort_order")
    List<SysDictEntry> selectByCategoryId(Long categoryId);
}
