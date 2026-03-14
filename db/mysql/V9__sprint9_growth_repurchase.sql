CREATE TABLE IF NOT EXISTS user_membership_profile (
  user_id BIGINT PRIMARY KEY,
  member_level VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
  total_paid_orders INT NOT NULL DEFAULT 0,
  total_paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  member_points BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_membership_level_updated (member_level, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS growth_campaign (
  campaign_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campaign_name VARCHAR(120) NOT NULL,
  target_member_level VARCHAR(20) NOT NULL,
  coupon_code VARCHAR(40) NOT NULL,
  campaign_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_growth_campaign_status_time (campaign_status, start_at, end_at),
  INDEX idx_growth_campaign_coupon (coupon_code, campaign_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS growth_campaign_touch (
  touch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campaign_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  touch_channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
  touch_status VARCHAR(20) NOT NULL DEFAULT 'SENT',
  coupon_code VARCHAR(40) NOT NULL,
  touched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_growth_touch_campaign FOREIGN KEY (campaign_id) REFERENCES growth_campaign(campaign_id),
  UNIQUE KEY uk_growth_touch_campaign_user (campaign_id, user_id),
  INDEX idx_growth_touch_user_time (user_id, touched_at),
  INDEX idx_growth_touch_coupon_time (coupon_code, touched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE order_main
  ADD COLUMN IF NOT EXISTS applied_coupon_code VARCHAR(40) NULL AFTER total_amount,
  ADD COLUMN IF NOT EXISTS applied_campaign_id BIGINT NULL AFTER applied_coupon_code,
  ADD INDEX idx_order_growth_attribution (applied_campaign_id, applied_coupon_code, created_at),
  ADD CONSTRAINT fk_order_applied_campaign FOREIGN KEY (applied_campaign_id) REFERENCES growth_campaign(campaign_id);
