package com.institute.achievement.paper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.paper.entity.Paper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for Paper entity.
 * <p>
 * Inherits standard CRUD operations from BaseMapper.
 * Custom queries (e.g., findByOwner) can be added as needed.
 * The SQL-layer permission interceptor automatically injects dept_id
 * filtering conditions (Phase 0 infrastructure).
 */
@Mapper
public interface PaperMapper extends BaseMapper<Paper> {

}
