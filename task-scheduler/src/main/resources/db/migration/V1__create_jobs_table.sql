CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE jobs (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      name VARCHAR(255) NOT NULL,
                      cron_expression VARCHAR(100) NOT NULL,

    -- changed from job_status enum to VARCHAR
                      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                      payload TEXT,
                      retry_count INTEGER NOT NULL DEFAULT 0,
                      next_run_at TIMESTAMP NOT NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_status_next_run
    ON jobs(status, next_run_at);