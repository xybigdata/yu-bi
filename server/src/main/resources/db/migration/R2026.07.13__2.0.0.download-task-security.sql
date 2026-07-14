DROP INDEX `idx_download_cleanup` ON `download`;
DROP INDEX `idx_download_share_scope` ON `download`;
DROP INDEX `idx_download_authenticated_scope` ON `download`;

ALTER TABLE `download`
  DROP CHECK `chk_download_failure_code`;
ALTER TABLE `download`
  DROP CHECK `chk_download_owner_scope`;

ALTER TABLE `download`
  MODIFY COLUMN `create_time` timestamp(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0);
ALTER TABLE `download` DROP COLUMN `deleted_at`;
ALTER TABLE `download` DROP COLUMN `failure_code`;
ALTER TABLE `download` DROP COLUMN `share_id`;
ALTER TABLE `download` DROP COLUMN `owner_id`;
ALTER TABLE `download` DROP COLUMN `owner_type`;
