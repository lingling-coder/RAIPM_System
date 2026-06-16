package com.institute.achievement.common.util;

import com.institute.achievement.common.constant.ApiConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Generic page query parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Page number (1-indexed) */
    private int page = ApiConstants.DEFAULT_PAGE;

    /** Page size */
    private int pageSize = ApiConstants.DEFAULT_PAGE_SIZE;

    /**
     * Calculate the offset for SQL LIMIT clause.
     */
    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }

    /**
     * Clamp page size to maximum allowed value.
     */
    public void setPageSize(int pageSize) {
        this.pageSize = Math.min(pageSize, ApiConstants.MAX_PAGE_SIZE);
    }
}
