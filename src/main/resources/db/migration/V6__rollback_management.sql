ALTER TABLE deployments ADD COLUMN IF NOT EXISTS version_number BIGINT DEFAULT 1 NOT NULL;
ALTER TABLE deployments ADD COLUMN IF NOT EXISTS stable BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX IF NOT EXISTS idx_deployments_version ON deployments(pipeline_id, version_number);
CREATE INDEX IF NOT EXISTS idx_deployments_stable ON deployments(pipeline_id, stable);

CREATE TABLE IF NOT EXISTS deployment_rollbacks (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	pipeline_id UUID NOT NULL,
	from_deployment_id UUID NOT NULL,
	to_deployment_id UUID NOT NULL,
	triggered_by VARCHAR(180) NOT NULL,
	reason VARCHAR(1000),
	status VARCHAR(32) NOT NULL,
	started_at TIMESTAMP WITH TIME ZONE,
	completed_at TIMESTAMP WITH TIME ZONE,
	error_message VARCHAR(2000),
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deployment_rollback_logs (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	rollback_id UUID NOT NULL,
	message VARCHAR(2000) NOT NULL,
	timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
	level VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_deployment_rollbacks_pipeline ON deployment_rollbacks(pipeline_id);
CREATE INDEX IF NOT EXISTS idx_deployment_rollbacks_status ON deployment_rollbacks(status);
CREATE INDEX IF NOT EXISTS idx_deployment_rollback_logs_rollback_id ON deployment_rollback_logs(rollback_id);
