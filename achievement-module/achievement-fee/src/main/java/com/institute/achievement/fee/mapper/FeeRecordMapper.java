package com.institute.achievement.fee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.fee.dto.FeeRecordQueryDTO;
import com.institute.achievement.fee.dto.FeeRecordVO;
import com.institute.achievement.fee.entity.FeeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis-Plus mapper for FeeRecord entity.
 * <p>
 * Provides standard CRUD via BaseMapper plus a custom paginated listing
 * with JOINs to patent/copyright tables for owner name resolution.
 */
@Mapper
public interface FeeRecordMapper extends BaseMapper<FeeRecord> {

    /**
     * Paginated fee record listing with multi-dimensional filtering.
     * <p>
     * JOINs with patent (when owner_type='patent') and copyright (when
     * owner_type='copyright') tables to populate ownerName. Uses SQL-layer
     * data isolation via dept_id injection (MyBatis-Plus interceptor).
     *
     * @param page  MyBatis-Plus pagination object
     * @param query filter parameters
     * @return paginated fee record VO with owner name populated
     */
    @Select("<script>"
            + "SELECT fr.*, "
            + "  CASE "
            + "    WHEN fr.owner_type = 'patent' THEN p.patent_name "
            + "    WHEN fr.owner_type = 'copyright' THEN c.copyright_name "
            + "    ELSE NULL "
            + "  END AS owner_name "
            + "FROM fee_record fr "
            + "LEFT JOIN patent p ON fr.owner_type = 'patent' AND fr.owner_id = p.id "
            + "LEFT JOIN copyright c ON fr.owner_type = 'copyright' AND fr.owner_id = c.id "
            + "WHERE 1=1 "
            + "<if test='query.status != null and query.status != \"\"'>"
            + "  AND fr.status = #{query.status} "
            + "</if>"
            + "<if test='query.feeType != null and query.feeType != \"\"'>"
            + "  AND fr.fee_type = #{query.feeType} "
            + "</if>"
            + "<if test='query.fundingSource != null and query.fundingSource != \"\"'>"
            + "  AND fr.funding_source = #{query.fundingSource} "
            + "</if>"
            + "<if test='query.keyword != null and query.keyword != \"\"'>"
            + "  AND (p.patent_name LIKE CONCAT('%', #{query.keyword}, '%') "
            + "    OR c.copyright_name LIKE CONCAT('%', #{query.keyword}, '%')) "
            + "</if>"
            + "<if test='query.dueDateFrom != null and query.dueDateFrom != \"\"'>"
            + "  AND fr.due_date &gt;= #{query.dueDateFrom} "
            + "</if>"
            + "<if test='query.dueDateTo != null and query.dueDateTo != \"\"'>"
            + "  AND fr.due_date &lt;= #{query.dueDateTo} "
            + "</if>"
            + "<if test='query.ownerType != null and query.ownerType != \"\"'>"
            + "  AND fr.owner_type = #{query.ownerType} "
            + "</if>"
            + "<if test='query.deptId != null'>"
            + "  AND fr.dept_id = #{query.deptId} "
            + "</if>"
            + "ORDER BY fr.due_date ASC, fr.created_time DESC "
            + "</script>")
    Page<FeeRecordVO> selectFeeRecordPage(Page<?> page, @Param("query") FeeRecordQueryDTO query);
}
