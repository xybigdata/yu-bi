ALTER TABLE `download` ADD COLUMN `owner_type` varchar(24) NULL;
ALTER TABLE `download` ADD COLUMN `owner_id` varchar(64) NULL;
ALTER TABLE `download` ADD COLUMN `share_id` varchar(32) NULL;
ALTER TABLE `download` ADD COLUMN `failure_code` varchar(32) NULL;
ALTER TABLE `download` ADD COLUMN `deleted_at` timestamp(3) NULL DEFAULT NULL;

UPDATE `download`
SET `owner_type` = 'LEGACY_INACCESSIBLE',
    `owner_id` = NULL,
    `share_id` = NULL
WHERE `owner_type` IS NULL;

ALTER TABLE `download` MODIFY COLUMN `owner_type` varchar(24) NOT NULL;
ALTER TABLE `download` MODIFY COLUMN `create_time` timestamp(0) NULL DEFAULT NULL;

ALTER TABLE `download`
  ADD CONSTRAINT `chk_download_owner_scope` CHECK (
    (`owner_type` = 'AUTHENTICATED' AND `owner_id` IS NOT NULL AND `share_id` IS NULL)
    OR (`owner_type` = 'SHARE' AND `owner_id` IS NOT NULL AND `share_id` IS NOT NULL)
    OR (`owner_type` = 'LEGACY_INACCESSIBLE' AND `owner_id` IS NULL AND `share_id` IS NULL)
  ) ENFORCED;

ALTER TABLE `download`
  ADD CONSTRAINT `chk_download_failure_code` CHECK (
    `failure_code` IS NULL OR `failure_code` IN (
      'PERMISSION_DENIED',
      'DEFINITION_INVALID',
      'QUERY_FAILED',
      'FILE_GENERATION_FAILED',
      'TASK_INTERRUPTED',
      'INTERNAL_FAILURE'
    )
  ) ENFORCED;

CREATE INDEX `idx_download_authenticated_scope`
  ON `download` (`owner_type`, `owner_id`, `deleted_at`, `create_time`);

CREATE INDEX `idx_download_share_scope`
  ON `download` (`owner_type`, `share_id`, `owner_id`, `deleted_at`, `create_time`);

CREATE INDEX `idx_download_cleanup`
  ON `download` (`deleted_at`, `create_time`);
