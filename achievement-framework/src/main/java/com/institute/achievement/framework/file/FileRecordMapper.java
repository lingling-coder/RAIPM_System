package com.institute.achievement.framework.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis-Plus mapper for the file_record table.
 * <p>
 * Provides standard CRUD from BaseMapper plus a custom lookup by UUID.
 * No delete method is exposed -- file records are retained for audit purposes.
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecordEntity> {

    /**
     * Find a file record by its UUID stored name.
     *
     * @param storedName the UUID filename (without extension)
     * @return the file record, or null if not found
     */
    @Select("SELECT * FROM file_record WHERE stored_name = #{storedName}")
    FileRecordEntity findByStoredName(@Param("storedName") String storedName);
}
