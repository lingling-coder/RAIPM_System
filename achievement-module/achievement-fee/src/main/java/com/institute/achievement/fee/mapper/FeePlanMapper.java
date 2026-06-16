package com.institute.achievement.fee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.fee.dto.FeePlanQueryDTO;
import com.institute.achievement.fee.dto.FeePlanVO;
import com.institute.achievement.fee.entity.FeePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * MyBatis-Plus mapper for FeePlan entity.
 * <p>
 * Provides standard CRUD via BaseMapper plus a custom paginated listing
 * with JOIN to patent table for patent name/type/application number resolution,
 * dedup check, and batch pause operations.
 */
@Mapper
public interface FeePlanMapper extends BaseMapper<FeePlan> {

    /**
     * Paginated fee plan listing with patent JOIN.
     * <p>
     * JOINs with patent table to populate patentName, patentType, applicationNo.
     * Supports filtering by status, feeType, keyword (patent name), and patentId.
     * Uses SQL-layer data isolation via dept_id injection (MyBatis-Plus interceptor).
     *
     * @param page  MyBatis-Plus pagination object
     * @param query filter parameters
     * @return paginated fee plan VO with patent info populated
     */
    @Select("<script>"
            + "SELECT fp.*, "
            + "  p.patent_name AS patent_name, "
            + "  p.patent_type AS patent_type, "
            + "  p.application_no AS application_no "
            + "FROM fee_plan fp "
            + "LEFT JOIN patent p ON fp.patent_id = p.id "
            + "WHERE 1=1 "
            + "<if test='query.status != null and query.status != \"\"'>"
            + "  AND fp.status = #{query.status} "
            + "</if>"
            + "<if test='query.feeType != null and query.feeType != \"\"'>"
            + "  AND fp.fee_type = #{query.feeType} "
            + "</if>"
            + "<if test='query.keyword != null and query.keyword != \"\"'>"
            + "  AND p.patent_name LIKE CONCAT('%', #{query.keyword}, '%') "
            + "</if>"
            + "<if test='query.patentId != null'>"
            + "  AND fp.patent_id = #{query.patentId} "
            + "</if>"
            + "<if test='query.deptId != null'>"
            + "  AND fp.dept_id = #{query.deptId} "
            + "</if>"
            + "ORDER BY fp.due_date ASC, fp.created_time DESC "
            + "</script>")
    Page<FeePlanVO> selectFeePlanPage(Page<?> page, @Param("query") FeePlanQueryDTO query);

    /**
     * Count existing fee plans for dedup check during recurring generation.
     *
     * @param patentId the patent ID
     * @param feeType  the fee type code
     * @param dueDate  the due date to check
     * @return count of matching plans (0 means no duplicate)
     */
    @Select("SELECT COUNT(*) FROM fee_plan "
            + "WHERE patent_id = #{patentId} AND fee_type = #{feeType} AND due_date = #{dueDate}")
    int countByPatentAndType(@Param("patentId") Long patentId,
                              @Param("feeType") String feeType,
                              @Param("dueDate") LocalDate dueDate);

    /**
     * Batch pause all active plans for a given patent.
     * <p>
     * Used by {@link com.institute.achievement.fee.service.PatentInvalidationListener}
     * when a patent is invalidated (D-18).
     *
     * @param patentId the patent ID
     * @return number of plans paused
     */
    @Select("UPDATE fee_plan SET status = 'paused', updated_time = NOW() "
            + "WHERE patent_id = #{patentId} AND status = 'active'")
    int batchPauseByPatentId(@Param("patentId") Long patentId);
}
