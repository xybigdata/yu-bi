CREATE TABLE `recycle_operation_token` (
  `token` varchar(36) NOT NULL,
  `org_id` varchar(36) NOT NULL,
  `actor_id` varchar(36) NOT NULL,
  `resource_type` varchar(32) NOT NULL,
  `root_ids_json` text NOT NULL,
  `expires_at` timestamp(3) NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT `pk_recycle_operation_token` PRIMARY KEY (`token`),
  INDEX `idx_recycle_operation_token_expiry` (`expires_at`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE `recycle_batch` (
  `id` varchar(36) NOT NULL,
  `org_id` varchar(36) NOT NULL,
  `actor_id` varchar(36) NOT NULL,
  `resource_type` varchar(32) NOT NULL,
  `operation` varchar(32) NOT NULL,
  `state` varchar(16) NOT NULL,
  `client_request_id` varchar(128) NOT NULL,
  `undo_token` varchar(36) NULL,
  `undo_expires_at` timestamp(3) NULL,
  `result_json` text NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  `completed_at` timestamp(3) NULL,
  CONSTRAINT `pk_recycle_batch` PRIMARY KEY (`id`),
  CONSTRAINT `uk_recycle_batch_request` UNIQUE (`org_id`, `actor_id`, `client_request_id`),
  INDEX `idx_recycle_batch_org_type_created` (`org_id`, `resource_type`, `created_at`),
  INDEX `idx_recycle_batch_undo` (`undo_token`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE `recycle_record` (
  `id` varchar(36) NOT NULL,
  `org_id` varchar(36) NOT NULL,
  `resource_type` varchar(32) NOT NULL,
  `root_id` varchar(64) NOT NULL,
  `original_name` varchar(255) NOT NULL,
  `original_parent_id` varchar(64) NULL,
  `original_index` double NOT NULL,
  `root_kind` varchar(16) NOT NULL,
  `expanded_item_count` int NOT NULL,
  `subtree_json` text NOT NULL,
  `deleted_by` varchar(36) NOT NULL,
  `deleted_at` timestamp(3) NOT NULL,
  `expires_at` timestamp(3) NULL,
  `batch_id` varchar(36) NULL,
  CONSTRAINT `pk_recycle_record` PRIMARY KEY (`id`),
  CONSTRAINT `uk_recycle_record_root` UNIQUE (`org_id`, `resource_type`, `root_id`),
  INDEX `idx_recycle_record_list` (`org_id`, `resource_type`, `deleted_at`),
  INDEX `idx_recycle_record_expiry` (`expires_at`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE `recycle_policy` (
  `org_id` varchar(36) NOT NULL,
  `resource_type` varchar(32) NOT NULL,
  `enabled` boolean NOT NULL DEFAULT TRUE,
  `retention_days` int NOT NULL DEFAULT 30,
  `updated_by` varchar(36) NULL,
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT `pk_recycle_policy` PRIMARY KEY (`org_id`, `resource_type`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE `recycle_audit_event` (
  `id` varchar(36) NOT NULL,
  `batch_id` varchar(36) NULL,
  `record_id` varchar(36) NULL,
  `org_id` varchar(36) NOT NULL,
  `resource_type` varchar(32) NOT NULL,
  `root_id` varchar(64) NULL,
  `action` varchar(32) NOT NULL,
  `result` varchar(16) NOT NULL,
  `reason` varchar(512) NULL,
  `actor_id` varchar(36) NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  CONSTRAINT `pk_recycle_audit_event` PRIMARY KEY (`id`),
  INDEX `idx_recycle_audit_retention` (`created_at`),
  INDEX `idx_recycle_audit_batch` (`batch_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;
