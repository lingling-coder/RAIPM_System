package com.institute.achievement.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.patent.entity.Patent;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for Patent entity.
 * <p>
 * Inherits standard CRUD operations from BaseMapper.
 * The SQL-layer permission interceptor automatically injects dept_id
 * filtering conditions (Phase 0 infrastructure).
 */
@Mapper
public interface PatentMapper extends BaseMapper<Patent> {

}
