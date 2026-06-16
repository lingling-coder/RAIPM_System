package com.institute.achievement.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.copyright.entity.Copyright;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for Copyright entity.
 * <p>
 * Inherits standard CRUD operations from BaseMapper.
 * The SQL-layer permission interceptor automatically injects dept_id
 * filtering conditions (Phase 0 infrastructure).
 */
@Mapper
public interface CopyrightMapper extends BaseMapper<Copyright> {

}
