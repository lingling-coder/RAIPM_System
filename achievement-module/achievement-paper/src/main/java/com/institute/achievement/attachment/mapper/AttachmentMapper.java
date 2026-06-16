package com.institute.achievement.attachment.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.attachment.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis-Plus mapper for Attachment entity.
 * <p>
 * Provides standard CRUD plus custom queries for the Exclusive Arc pattern.
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {

    /**
     * Find attachments for a specific achievement type and ID.
     * Implements the Exclusive Arc pattern: maps type to the appropriate column.
     *
     * @param achievementType the achievement type (paper, patent, copyright)
     * @param typeId          the achievement ID
     * @return list of non-deleted attachments
     */
    default List<Attachment> findByOwner(@Param("type") String achievementType,
                                          @Param("typeId") Long typeId) {
        LambdaQueryWrapper<Attachment> wrapper = new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getIsDeleted, 0);

        switch (achievementType.toLowerCase()) {
            case "paper" -> wrapper.eq(Attachment::getPaperId, typeId);
            case "patent" -> wrapper.eq(Attachment::getPatentId, typeId);
            case "copyright" -> wrapper.eq(Attachment::getCopyrightId, typeId);
            default -> throw new IllegalArgumentException("Unknown achievement type: " + achievementType);
        }

        return selectList(wrapper);
    }
}
