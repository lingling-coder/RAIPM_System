package com.institute.achievement.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Generic paginated result wrapper.
 *
 * @param <T> the type of records in this page
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** List of records for the current page */
    private List<T> records = Collections.emptyList();

    /** Total number of records */
    private long total = 0;

    /** Current page number */
    private int page = 1;

    /** Current page size */
    private int pageSize = 20;

    /**
     * Total number of pages.
     */
    public long getPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * Create an empty page result.
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0, 1, 20);
    }

    /**
     * Create a page result with records and total.
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int pageSize) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
