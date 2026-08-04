ALTER TABLE `artifact_task`
  ADD COLUMN `source_module` varchar(64) NOT NULL DEFAULT 'OTHER' AFTER `file_suffix`;

ALTER TABLE `artifact_task`
  ADD COLUMN `expires_at` timestamp(3) NULL AFTER `completed_at`;

CREATE INDEX `idx_artifact_task_expiry`
  ON `artifact_task` (`state`, `expires_at`);
