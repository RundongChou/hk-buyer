CREATE TABLE IF NOT EXISTS sku_spu (
  spu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  spu_name VARCHAR(120) NOT NULL,
  brand_name VARCHAR(80) NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_spu_name (spu_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sku_item (
  sku_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  spu_id BIGINT NOT NULL,
  sku_name VARCHAR(120) NOT NULL,
  spec_value VARCHAR(120) NOT NULL,
  brand_name VARCHAR(80) NOT NULL,
  publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_sku_item_spu FOREIGN KEY (spu_id) REFERENCES sku_spu(spu_id),
  INDEX idx_sku_item_publish (publish_status, updated_at),
  INDEX idx_sku_item_keyword (sku_name, brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sku_price_policy (
  price_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku_id BIGINT NOT NULL,
  base_price DECIMAL(10,2) NOT NULL,
  service_fee_rate DECIMAL(8,4) NOT NULL,
  final_price DECIMAL(10,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_price_policy_sku FOREIGN KEY (sku_id) REFERENCES sku_item(sku_id),
  UNIQUE KEY uk_price_policy_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stock_snapshot (
  snapshot_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku_id BIGINT NOT NULL,
  available_qty INT NOT NULL,
  locked_qty INT NOT NULL DEFAULT 0,
  alert_threshold INT NOT NULL DEFAULT 5,
  updated_reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_stock_snapshot_sku FOREIGN KEY (sku_id) REFERENCES sku_item(sku_id),
  UNIQUE KEY uk_stock_snapshot_sku (sku_id),
  INDEX idx_stock_snapshot_qty (available_qty, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sku_publish_audit_log (
  audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku_id BIGINT NOT NULL,
  admin_id BIGINT NOT NULL,
  decision VARCHAR(20) NOT NULL,
  comment VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sku_audit_sku FOREIGN KEY (sku_id) REFERENCES sku_item(sku_id),
  INDEX idx_sku_audit_sku_created (sku_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
