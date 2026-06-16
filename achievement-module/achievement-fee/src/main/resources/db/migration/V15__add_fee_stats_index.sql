-- V15: Add composite stats index on fee_record for fee statistics aggregation
-- Covers the most common filter and GROUP BY patterns used by FeeStatsMapper.xml
-- for multi-dimensional fee statistics (Plan 02-05).
--
-- Column order: status (filter), due_date (overdue calc + YEAR GROUP BY),
-- fee_type (filter), funding_source (filter + GROUP BY), dept_id (filter + GROUP BY)
--
-- Expected improvement: GROUP BY aggregation queries on <50K rows complete in <100ms
-- per RESEARCH.md Pitfall 3 analysis.

CREATE INDEX idx_fee_record_stats ON fee_record (status, due_date, fee_type, funding_source, dept_id)
    COMMENT 'Composite index for fee statistics aggregation queries';
