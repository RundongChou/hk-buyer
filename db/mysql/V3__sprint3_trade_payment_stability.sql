CREATE TABLE IF NOT EXISTS user_cart_item (
  cart_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL,
  selected TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_cart_sku FOREIGN KEY (sku_id) REFERENCES sku_item(sku_id),
  UNIQUE KEY uk_user_cart (user_id, sku_id),
  INDEX idx_user_cart_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coupon_template (
  coupon_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  coupon_code VARCHAR(40) NOT NULL,
  coupon_name VARCHAR(100) NOT NULL,
  discount_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(10,2) NOT NULL,
  min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  valid_from DATETIME NOT NULL,
  valid_to DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_coupon_code (coupon_code),
  INDEX idx_coupon_status_valid (status, valid_from, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_compensation (
  compensation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  payment_channel VARCHAR(30) NOT NULL,
  failure_reason VARCHAR(120) NOT NULL,
  compensation_token VARCHAR(80) NOT NULL,
  compensation_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  compensated_at DATETIME NULL,
  CONSTRAINT fk_payment_compensation_order FOREIGN KEY (order_id) REFERENCES order_main(order_id),
  UNIQUE KEY uk_payment_comp_token (compensation_token),
  INDEX idx_payment_comp_order_status (order_id, compensation_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO coupon_template (
  coupon_code,
  coupon_name,
  discount_type,
  discount_value,
  min_order_amount,
  status,
  valid_from,
  valid_to
)
VALUES (
  'SPRINT3OFF20',
  '新客立减20',
  'FIXED',
  20.00,
  199.00,
  'ACTIVE',
  '2026-01-01 00:00:00',
  '2027-01-01 00:00:00'
)
ON DUPLICATE KEY UPDATE
  coupon_name = VALUES(coupon_name),
  discount_type = VALUES(discount_type),
  discount_value = VALUES(discount_value),
  min_order_amount = VALUES(min_order_amount),
  status = VALUES(status),
  valid_from = VALUES(valid_from),
  valid_to = VALUES(valid_to),
  updated_at = NOW();
