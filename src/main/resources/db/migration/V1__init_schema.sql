CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	name VARCHAR(140) NOT NULL,
	email VARCHAR(180) NOT NULL UNIQUE,
	password VARCHAR(200) NOT NULL,
	role VARCHAR(32) NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pipelines (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	name VARCHAR(140) NOT NULL,
	status VARCHAR(32) NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS repositories (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	repo_name VARCHAR(140) NOT NULL,
	github_url VARCHAR(320) NOT NULL,
	branch VARCHAR(120) NOT NULL,
	encrypted_access_token VARCHAR(2048) NOT NULL,
	created_by VARCHAR(180) NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	CONSTRAINT uk_repo_github_branch UNIQUE (github_url, branch)
);

CREATE TABLE IF NOT EXISTS repository_activity (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	repository_id UUID NOT NULL,
	repo_name VARCHAR(140) NOT NULL,
	action VARCHAR(40) NOT NULL,
	details VARCHAR(2000),
	actor VARCHAR(180) NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
	CONSTRAINT fk_repo_activity_repo FOREIGN KEY (repository_id) REFERENCES repositories(id)
);

CREATE INDEX IF NOT EXISTS idx_repo_activity_repo_id ON repository_activity(repository_id);
CREATE INDEX IF NOT EXISTS idx_repo_activity_action ON repository_activity(action);
CREATE INDEX IF NOT EXISTS idx_repo_activity_actor ON repository_activity(actor);
