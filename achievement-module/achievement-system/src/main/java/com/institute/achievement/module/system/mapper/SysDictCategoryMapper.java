package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.SysDictCategory;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Dictionary category mapper.
 */
public interface SysDictCategoryMapper extends BaseMapper<SysDictCategory> {

    /**
     * Select all categories ordered by sort_order.
     */
    @Select("SELECT * FROM sys_dict_category ORDER BY sort_order")
    List<SysDictCategory> selectAllOrderBySort();
}
