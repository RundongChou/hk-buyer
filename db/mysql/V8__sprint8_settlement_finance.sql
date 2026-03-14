ALTER TABLE buyer_profile
  ADD COLUMN IF NOT EXISTS settlement_account VARCHAR(120) NULL AFTER specialty_category;

UPDATE buyer_profile bp
LEFT JOIN (
  SELECT boa.buyer_id, boa.settlement_account
  FROM buyer_onboarding_application boa
  INNER JOIN (
    SELECT buyer_id, MAX(application_id) AS max_application_id
    FROM buyer_onboarding_application
    WHERE application_status = 'APPROVED'
    GROUP BY buyer_id
  ) latest ON latest.max_application_id = boa.application_id
) approved_account ON approved_account.buyer_id = bp.buyer_id
SET bp.settlement_account = COALESCE(bp.settlement_account, approved_account.settlement_account);

CREATE TABLE IF NOT EXISTS settlement_ledger (
  ledger_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  buyer_settlement_account VARCHAR(120) NULL,
  order_amount DECIMAL(10,2) NOT NULL,
  goods_cost_amount DECIMAL(10,2) NOT NULL,
  buyer_income_amount DECIMAL(10,2) NOT NULL,
  logistics_cost_amount DECIMAL(10,2) NOT NULL,
  platform_service_amount DECIMAL(10,2) NOT NULL,
  settlement_status VARCHAR(30) NOT NULL,
  reconciliation_status VARCHAR(30) NOT NULL,
  exception_reason VARCHAR(255) NULL,
  signed_at DATETIME NOT NULL,
  payout_requested_at DATETIME NULL,
  settled_at DATETIME NULL,
  reconciled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_settlement_order FOREIGN KEY (order_id) REFERENCES order_main(order_id),
  CONSTRAINT fk_settlement_task FOREIGN KEY (task_id) REFERENCES procurement_task(task_id),
  CONSTRAINT fk_settlement_buyer FOREIGN KEY (buyer_id) REFERENCES buyer_profile(buyer_id),
  UNIQUE KEY uk_settlement_order (order_id),
  INDEX idx_settlement_buyer_status (buyer_id, settlement_status, created_at),
  INDEX idx_settlement_status_recon (settlement_status, reconciliation_status, updated_at),
  INDEX idx_settlement_signed_at (signed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
