ALTER TABLE pipelines RENAME COLUMN name TO pipeline_name;

ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS repository_id UUID;
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS created_by VARCHAR(180);
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS last_triggered_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS last_execution_started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS last_execution_finished_at TIMESTAMP WITH TIME ZONE;

UPDATE pipelines SET created_by = COALESCE(created_by, 'system');
ALTER TABLE pipelines ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE pipelines ALTER COLUMN repository_id SET NOT NULL;
ALTER TABLE pipelines ADD CONSTRAINT fk_pipeline_repository FOREIGN KEY (repository_id) REFERENCES repositories(id);

CREATE TABLE IF NOT EXISTS pipeline_stages (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	pipeline_id UUID NOT NULL,
	stage_name VARCHAR(32) NOT NULL,
	execution_order INT NOT NULL,
	status VARCHAR(32) NOT NULL,
	started_at TIMESTAMP WITH TIME ZONE,
	finished_at TIMESTAMP WITH TIME ZONE,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	CONSTRAINT fk_pipeline_stage_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines(id),
	CONSTRAINT uk_pipeline_stage_order UNIQUE (pipeline_id, execution_order)
);

CREATE INDEX IF NOT EXISTS idx_pipeline_repo ON pipelines(repository_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_status ON pipelines(status);
CREATE INDEX IF NOT EXISTS idx_pipeline_created_by ON pipelines(created_by);
