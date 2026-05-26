ALTER TABLE deployments ADD COLUMN IF NOT EXISTS current_stage VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_deployments_started_at ON deployments(started_at);
CREATE INDEX IF NOT EXISTS idx_deployments_current_stage ON deployments(current_stage);
