CREATE TABLE `artifact_task_owner_guard` (
  `owner_key` varchar(128) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT `pk_artifact_task_owner_guard` PRIMARY KEY (`owner_key`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE `artifact_task` (
  `id` varchar(36) NOT NULL,
  `owner_key` varchar(128) NOT NULL,
  `display_name` varchar(255) NOT NULL,
  `media_type` varchar(128) NOT NULL,
  `file_suffix` varchar(32) NOT NULL,
  `state` varchar(16) NOT NULL,
  `accepted_at` timestamp(3) NOT NULL,
  `deadline_at` timestamp(3) NOT NULL,
  `completed_at` timestamp(3) NULL,
  `blob_key` varchar(512) NULL,
  `failure_code` varchar(64) NULL,
  `failure_hint` varchar(255) NULL,
  `failure_trace_id` varchar(128) NULL,
  `trace_id` varchar(128) NOT NULL,
  CONSTRAINT `pk_artifact_task` PRIMARY KEY (`id`),
  CONSTRAINT `fk_artifact_task_owner`
    FOREIGN KEY (`owner_key`) REFERENCES `artifact_task_owner_guard` (`owner_key`),
  INDEX `idx_artifact_task_owner_state` (`owner_key`, `state`),
  INDEX `idx_artifact_task_deadline` (`state`, `deadline_at`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;
