CREATE TABLE IF NOT EXISTS warehouse_inbound (
  inbound_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  warehouse_code VARCHAR(40) NOT NULL,
  handover_at DATETIME NOT NULL,
  warehouse_status VARCHAR(30) NOT NULL,
  qc_status VARCHAR(30) NOT NULL,
  qc_note VARCHAR(255) NULL,
  scanned_at DATETIME NULL,
  inspected_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_inbound_order FOREIGN KEY (order_id) REFERENCES order_main(order_id),
  CONSTRAINT fk_inbound_task FOREIGN KEY (task_id) REFERENCES procurement_task(task_id),
  UNIQUE KEY uk_inbound_order (order_id),
  INDEX idx_inbound_status_updated (warehouse_status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customs_clearance_record (
  clearance_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  declaration_no VARCHAR(64) NOT NULL,
  clearance_status VARCHAR(30) NOT NULL,
  compliance_channel VARCHAR(40) NOT NULL,
  review_comment VARCHAR(255) NULL,
  submitted_at DATETIME NOT NULL,
  reviewed_at DATETIME NULL,
  released_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_customs_order FOREIGN KEY (order_id) REFERENCES order_main(order_id),
  UNIQUE KEY uk_customs_order (order_id),
  INDEX idx_customs_status_updated (clearance_status, updated_at),
  INDEX idx_customs_decl_no (declaration_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shipment_tracking (
  shipment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  carrier VARCHAR(60) NOT NULL,
  tracking_no VARCHAR(80) NOT NULL,
  shipment_status VARCHAR(30) NOT NULL,
  latest_node VARCHAR(120) NOT NULL,
  latest_node_at DATETIME NOT NULL,
  signed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES order_main(order_id),
  UNIQUE KEY uk_shipment_order (order_id),
  INDEX idx_shipment_status_updated (shipment_status, updated_at),
  INDEX idx_shipment_tracking_no (tracking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
