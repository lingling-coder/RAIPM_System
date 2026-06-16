package com.institute.achievement.framework.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * View object for file record metadata returned to the frontend.
 * <p>
 * Contains the proxy URL ({@code /api/files/{uuid}}) instead of the
 * real storage path, ensuring direct path exposure is prevented (D-30).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileRecordVO {

    /** Database record ID */
    private Long id;

    /** Original filename from upload */
    private String originalName;

    /** UUID stored name */
    private String storedName;

    /** File size in bytes */
    private Long fileSize;

    /** MIME type */
    private String mimeType;

    /** Business type identifier */
    private String fileType;

    /** Proxy URL for accessing the file (/api/files/{uuid}) */
    private String proxyUrl;

    /** Upload timestamp */
    private String createdAt;
}
