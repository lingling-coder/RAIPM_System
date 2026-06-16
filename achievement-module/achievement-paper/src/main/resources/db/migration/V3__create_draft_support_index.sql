-- V3: Draft query support index
-- Since drafts are stored in the paper table itself with status=DRAFT,
-- this migration adds the draft-user query index.
-- The idx_draft_user index was already created in V1 as part of the paper table,
-- so this migration is a no-op for the paper table.
--
-- If draft tables for patent/copyright are added later, they will get their
-- own draft-user indexes here.

-- Draft-user index was already created in V1:
-- INDEX `idx_draft_user` (`created_by`, `status`)
--
-- This migration exists as a placeholder for future draft-related additions
-- and to maintain the migration sequence V1->V2->V3->V4.

-- No DDL needed — index exists from V1.
SELECT 1;
