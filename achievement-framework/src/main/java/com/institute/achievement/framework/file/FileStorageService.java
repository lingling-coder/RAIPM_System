package com.institute.achievement.framework.file;

import org.springframework.core.io.Resource;

/**
 * Service interface for file storage operations (D-29, D-30, D-31).
 * <p>
 * Files are stored in a layered directory structure: uploads/YYYY/MM/type/
 * and accessed exclusively through UUID-based proxy URLs. Direct storage
 * paths are never exposed to clients.
 */
public interface FileStorageService {

    /**
     * Store a file and return its metadata including proxy URL.
     * <p>
     * Validates file size (<= 50MB) and file extension against the
     * allowed types whitelist before storing.
     *
     * @param originalFilename the original uploaded filename
     * @param fileContent      the file content as byte array
     * @param fileSize         the file size in bytes
     * @param contentType      the MIME type of the file
     * @param fileType         the business type identifier (PAPER/PATENT/SOFTWARE/AVATAR)
     * @return file record VO with UUID, proxy URL, and metadata
     * @throws IllegalArgumentException if validation fails
     */
    FileRecordVO store(String originalFilename, byte[] fileContent,
                       long fileSize, String contentType, String fileType);

    /**
     * Load a file as a Spring Resource by its UUID.
     * Used by FileProxyController to serve file downloads.
     *
     * @param uuid the stored UUID filename (without extension)
     * @return the file resource
     * @throws com.institute.achievement.common.exception.EntityNotFoundException
     *         if the file record is not found
     */
    Resource loadAsResource(String uuid);

    /**
     * Get file metadata by UUID without loading the file content.
     *
     * @param uuid the stored UUID filename
     * @return file record VO with metadata
     */
    FileRecordVO getRecord(String uuid);

    /**
     * Delete a file by UUID (physical delete + record removal).
     *
     * @param uuid the stored UUID filename
     */
    void delete(String uuid);
}
