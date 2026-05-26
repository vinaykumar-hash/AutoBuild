CREATE TABLE IF NOT EXISTS deployment_job_executions (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	message_id VARCHAR(64) NOT NULL,
	pipeline_id UUID NOT NULL,
	status VARCHAR(32) NOT NULL,
	attempts INT NOT NULL DEFAULT 0,
	started_at TIMESTAMP WITH TIME ZONE,
	finished_at TIMESTAMP WITH TIME ZONE,
	error_message VARCHAR(2000),
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	CONSTRAINT uk_deployment_message UNIQUE (message_id)
);

CREATE INDEX IF NOT EXISTS idx_deployment_pipeline_id ON deployment_job_executions(pipeline_id);
CREATE INDEX IF NOT EXISTS idx_deployment_status ON deployment_job_executions(status);
