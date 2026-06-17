-- V16: Add ngram FULLTEXT indexes on paper, patent, copyright for search
-- Uses WITH PARSER ngram for CJK full-text search (SRCH-01, SRCH-02).
--
-- These indexes enable MATCH...AGAINST IN BOOLEAN MODE queries across
-- the three achievement tables for Phase 3 global search.
--
-- Paper: title, authors, abstract_text
-- Patent: patent_name, inventors (no abstract field)
-- Software Copyright: name, copyright_holder (no abstract field)
--
-- Note: No `keywords` column exists in any table, so it is NOT included.
-- The available fields provide adequate search coverage.
--
-- Expected performance: FULLTEXT queries on <50K rows per table complete in <200ms
-- per RESEARCH.md analysis. For larger datasets, Phase 2 should migrate to ES.

ALTER TABLE paper
    ADD FULLTEXT INDEX ft_paper_search (title, authors, abstract_text)
    WITH PARSER ngram;

ALTER TABLE patent
    ADD FULLTEXT INDEX ft_patent_search (patent_name, inventors)
    WITH PARSER ngram;

ALTER TABLE software_copyright
    ADD FULLTEXT INDEX ft_copyright_search (name, copyright_holder)
    WITH PARSER ngram;
