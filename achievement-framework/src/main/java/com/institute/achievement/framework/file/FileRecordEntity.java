package com.institute.achievement.framework.file;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus entity for the file_record table.
 * <p>
 * Stores file metadata for proxy-based file access (D-29, D-30).
 * The actual file is stored on disk; the database record provides the
 * mapping from UUID to storage path.
 */
@Data
@TableName("file_record")
public class FileRecordEntity {

    private Long id;

    /** Original file name from the upload */
    private String originalName;

    /** UUID-based stored file name (without extension) */
    private String storedName;

    /** Relative storage path: YYYY/MM/type/ */
    private String storagePath;

    /** File size in bytes */
    private Long fileSize;

    /** MIME type as detected from the upload */
    private String mimeType;

    /** Business type: PAPER/PATENT/SOFTWARE/AVATAR */
    private String fileType;

    /** Uploader IP address */
    private String uploadIp;

    /** Uploader user ID */
    private Long createdBy;

    /** Upload timestamp */
    private LocalDateTime createdAt;
}
