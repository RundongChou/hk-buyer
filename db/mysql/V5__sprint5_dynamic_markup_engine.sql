ALTER TABLE procurement_task
  ADD COLUMN markup_applied_count INT NOT NULL DEFAULT 0 AFTER sla_hours,
  ADD COLUMN redispatch_count INT NOT NULL DEFAULT 0 AFTER markup_applied_count,
  ADD COLUMN last_markup_at DATETIME NULL AFTER redispatch_count,
  ADD COLUMN next_markup_eligible_at DATETIME NULL AFTER last_markup_at,
  ADD COLUMN terminal_reason VARCHAR(80) NULL AFTER next_markup_eligible_at;

CREATE INDEX idx_task_timeout_scan
  ON procurement_task(task_status, accept_deadline, next_markup_eligible_at);
