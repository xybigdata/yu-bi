DROP INDEX `idx_artifact_task_expiry` ON `artifact_task`;

ALTER TABLE `artifact_task`
  DROP COLUMN `expires_at`;

ALTER TABLE `artifact_task`
  DROP COLUMN `source_module`;
