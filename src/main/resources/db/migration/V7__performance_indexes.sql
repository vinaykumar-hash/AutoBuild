-- Performance indexes for production-grade query optimization

CREATE INDEX IF NOT EXISTS idx_deployments_pipeline_started_at 
	ON deployments(pipeline_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_deployment_logs_dep_id_timestamp 
	ON deployment_logs(deployment_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_deployment_rollbacks_pipe_created_at 
	ON deployment_rollbacks(pipeline_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_deployment_rollback_logs_rb_id_timestamp 
	ON deployment_rollback_logs(rollback_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_repo_activity_repo_created_at 
	ON repository_activity(repository_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_deployment_job_executions_dep_id 
	ON deployment_job_executions(deployment_id);
