CREATE TABLE IF NOT EXISTS buyer_onboarding_application (
  application_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  buyer_id BIGINT NOT NULL,
  real_name VARCHAR(60) NOT NULL,
  id_card_suffix VARCHAR(8) NOT NULL,
  service_region VARCHAR(40) NOT NULL,
  specialty_category VARCHAR(60) NOT NULL,
  settlement_account VARCHAR(120) NOT NULL,
  application_status VARCHAR(20) NOT NULL,
  reviewed_by BIGINT NULL,
  review_comment VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_buyer_apply_status_created (application_status, created_at),
  INDEX idx_buyer_apply_buyer_created (buyer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS buyer_profile (
  buyer_id BIGINT PRIMARY KEY,
  display_name VARCHAR(60) NOT NULL,
  audit_status VARCHAR(20) NOT NULL,
  buyer_level VARCHAR(20) NOT NULL,
  credit_score INT NOT NULL,
  reward_points INT NOT NULL DEFAULT 0,
  penalty_points INT NOT NULL DEFAULT 0,
  accepted_task_count INT NOT NULL DEFAULT 0,
  approved_proof_count INT NOT NULL DEFAULT 0,
  rejected_proof_count INT NOT NULL DEFAULT 0,
  service_region VARCHAR(40) NOT NULL,
  specialty_category VARCHAR(60) NOT NULL,
  last_active_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_buyer_profile_status_level (audit_status, buyer_level),
  INDEX idx_buyer_profile_region_category (service_region, specialty_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE procurement_task
  ADD COLUMN task_tier VARCHAR(20) NOT NULL DEFAULT 'OPEN' AFTER suggested_markup,
  ADD COLUMN required_buyer_level VARCHAR(20) NOT NULL DEFAULT 'BRONZE' AFTER task_tier,
  ADD COLUMN target_region VARCHAR(40) NOT NULL DEFAULT 'HK' AFTER required_buyer_level,
  ADD COLUMN target_category VARCHAR(60) NOT NULL DEFAULT 'GENERAL' AFTER target_region,
  ADD COLUMN sla_hours INT NOT NULL DEFAULT 72 AFTER target_category;

CREATE INDEX idx_task_dispatch
  ON procurement_task(task_status, task_tier, required_buyer_level, target_region, target_category);
