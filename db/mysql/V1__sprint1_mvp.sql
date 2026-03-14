CREATE TABLE IF NOT EXISTS order_main (
  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_status VARCHAR(40) NOT NULL,
  pay_status VARCHAR(20) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_order_status_created_at (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
  order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  line_amount DECIMAL(10,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES order_main(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procurement_task (
  task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  buyer_id BIGINT NULL,
  task_status VARCHAR(40) NOT NULL,
  publish_at DATETIME NOT NULL,
  accept_deadline DATETIME NOT NULL,
  suggested_markup DECIMAL(10,2) NOT NULL,
  accepted_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_procurement_task_order FOREIGN KEY (order_id) REFERENCES order_main(order_id),
  INDEX idx_task_status_deadline (task_status, accept_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS purchase_proof (
  proof_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  store_name VARCHAR(120) NOT NULL,
  receipt_url VARCHAR(255) NOT NULL,
  batch_no VARCHAR(80) NOT NULL,
  expiry_date DATE NOT NULL,
  product_photo_url VARCHAR(255) NOT NULL,
  audit_status VARCHAR(20) NOT NULL,
  audited_by BIGINT NULL,
  audit_comment VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  audited_at DATETIME NULL,
  CONSTRAINT fk_purchase_proof_task FOREIGN KEY (task_id) REFERENCES procurement_task(task_id),
  INDEX idx_proof_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_timeline_event (
  event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  event_type VARCHAR(60) NOT NULL,
  event_description VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_timeline_order FOREIGN KEY (order_id) REFERENCES order_main(order_id),
  INDEX idx_timeline_order_id (order_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
