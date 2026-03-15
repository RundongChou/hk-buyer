CREATE TABLE IF NOT EXISTS ops_experiment (
  experiment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  experiment_key VARCHAR(64) NOT NULL,
  experiment_name VARCHAR(120) NOT NULL,
  experiment_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  control_ratio DECIMAL(6,4) NOT NULL DEFAULT 0.5000,
  treatment_ratio DECIMAL(6,4) NOT NULL DEFAULT 0.5000,
  treatment_markup_delta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  treatment_sla_hours INT NOT NULL DEFAULT 48,
  created_by BIGINT NOT NULL,
  activated_by BIGINT NULL,
  activated_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ops_experiment_key (experiment_key),
  INDEX idx_ops_experiment_status (experiment_status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_experiment_assignment (
  assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  experiment_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  variant VARCHAR(20) NOT NULL,
  base_sla_hours INT NOT NULL,
  final_sla_hours INT NOT NULL,
  base_markup DECIMAL(10,2) NOT NULL,
  final_markup DECIMAL(10,2) NOT NULL,
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ops_assignment_experiment FOREIGN KEY (experiment_id) REFERENCES ops_experiment(experiment_id),
  UNIQUE KEY uk_ops_assignment_experiment_order (experiment_id, order_id),
  INDEX idx_ops_assignment_variant_assigned (variant, assigned_at),
  INDEX idx_ops_assignment_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_alert_event (
  alert_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_type VARCHAR(40) NOT NULL,
  metric_key VARCHAR(80) NOT NULL,
  metric_value DECIMAL(12,4) NOT NULL,
  threshold_value DECIMAL(12,4) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  alert_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  detail VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at DATETIME NULL,
  INDEX idx_ops_alert_status_created (alert_status, created_at),
  INDEX idx_ops_alert_type_status (alert_type, alert_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
