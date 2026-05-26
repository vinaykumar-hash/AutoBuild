ALTER TABLE deployment_job_executions ADD COLUMN IF NOT EXISTS deployment_id UUID;

CREATE TABLE IF NOT EXISTS deployments (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	pipeline_id UUID NOT NULL,
	commit_hash VARCHAR(64) NOT NULL,
	status VARCHAR(32) NOT NULL,
	started_at TIMESTAMP WITH TIME ZONE,
	completed_at TIMESTAMP WITH TIME ZONE,
	duration_ms BIGINT,
	logs VARCHAR(4000),
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deployment_logs (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	deployment_id UUID NOT NULL,
	message VARCHAR(2000) NOT NULL,
	timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
	level VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS deployment_rollback_points (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	deployment_id UUID NOT NULL,
	stage_name VARCHAR(32) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,
	metadata VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS idx_deployments_pipeline_id ON deployments(pipeline_id);
CREATE INDEX IF NOT EXISTS idx_deployments_status ON deployments(status);
CREATE INDEX IF NOT EXISTS idx_deployment_logs_deployment_id ON deployment_logs(deployment_id);
CREATE INDEX IF NOT EXISTS idx_rollback_points_deployment_id ON deployment_rollback_points(deployment_id);
