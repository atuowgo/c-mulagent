-- c-mulagent-server Schema DDL

-- 1. task_plan
CREATE TABLE IF NOT EXISTS task_plan (
    id            TEXT PRIMARY KEY,
    name          TEXT    NOT NULL,
    description   TEXT,
    status        TEXT    NOT NULL DEFAULT 'PENDING',
    priority      INTEGER NOT NULL DEFAULT 0,
    parent_id     TEXT,
    context       TEXT,
    metadata      TEXT,
    created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    completed_at  TEXT,
    FOREIGN KEY (parent_id) REFERENCES task_plan(id)
);

-- 2. subtask
CREATE TABLE IF NOT EXISTS subtask (
    id             TEXT PRIMARY KEY,
    task_plan_id   TEXT    NOT NULL,
    name           TEXT    NOT NULL,
    description    TEXT,
    status         TEXT    NOT NULL DEFAULT 'PENDING',
    assigned_agent TEXT,
    input_data     TEXT,
    output_data    TEXT,
    priority       INTEGER NOT NULL DEFAULT 0,
    dependencies   TEXT,
    retry_count    INTEGER NOT NULL DEFAULT 0,
    max_retries    INTEGER NOT NULL DEFAULT 3,
    created_at     TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at     TEXT    NOT NULL DEFAULT (datetime('now')),
    started_at     TEXT,
    completed_at   TEXT,
    FOREIGN KEY (task_plan_id) REFERENCES task_plan(id)
);

-- 3. agent_execution
CREATE TABLE IF NOT EXISTS agent_execution (
    id            TEXT PRIMARY KEY,
    subtask_id    TEXT    NOT NULL,
    agent_spec_id TEXT    NOT NULL,
    status        TEXT    NOT NULL DEFAULT 'PENDING',
    start_time    TEXT,
    end_time      TEXT,
    duration_ms   INTEGER,
    total_tokens  INTEGER,
    error_message TEXT,
    metadata      TEXT,
    created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (subtask_id) REFERENCES subtask(id)
);

-- 4. message_record
CREATE TABLE IF NOT EXISTS message_record (
    id              TEXT PRIMARY KEY,
    agent_execution_id TEXT,
    role            TEXT    NOT NULL,
    content         TEXT    NOT NULL,
    model           TEXT,
    token_count     INTEGER,
    metadata        TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (agent_execution_id) REFERENCES agent_execution(id)
);

-- 5. tool_invocation
CREATE TABLE IF NOT EXISTS tool_invocation (
    id              TEXT PRIMARY KEY,
    agent_execution_id TEXT,
    tool_name       TEXT    NOT NULL,
    input_params    TEXT,
    output_result   TEXT,
    status          TEXT    NOT NULL DEFAULT 'PENDING',
    duration_ms     INTEGER,
    error_message   TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    completed_at    TEXT,
    FOREIGN KEY (agent_execution_id) REFERENCES agent_execution(id)
);

-- 6. agent_spec
CREATE TABLE IF NOT EXISTS agent_spec (
    id              TEXT PRIMARY KEY,
    name            TEXT    NOT NULL UNIQUE,
    role            TEXT,
    base_url        TEXT,
    model           TEXT,
    api_key         TEXT,
    tools           TEXT,
    max_steps       INTEGER DEFAULT 10,
    output_format   TEXT,
    enabled         INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- 7. skill_template
CREATE TABLE IF NOT EXISTS skill_template (
    id              TEXT PRIMARY KEY,
    name            TEXT    NOT NULL UNIQUE,
    description     TEXT,
    category        TEXT,
    prompt_template TEXT,
    tool_bindings   TEXT,
    input_schema    TEXT,
    output_schema   TEXT,
    version         TEXT    NOT NULL DEFAULT '1.0.0',
    enabled         INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- 8. task_template
CREATE TABLE IF NOT EXISTS task_template (
    id              TEXT PRIMARY KEY,
    name            TEXT    NOT NULL UNIQUE,
    description     TEXT,
    category        TEXT,
    plan_template   TEXT,
    agent_bindings  TEXT,
    skill_bindings  TEXT,
    tool_bindings   TEXT,
    version         TEXT    NOT NULL DEFAULT '1.0.0',
    enabled         INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_subtask_task_plan ON subtask(task_plan_id);
CREATE INDEX IF NOT EXISTS idx_agent_execution_subtask ON agent_execution(subtask_id);
CREATE INDEX IF NOT EXISTS idx_agent_execution_agent ON agent_execution(agent_spec_id);
CREATE INDEX IF NOT EXISTS idx_message_record_execution ON message_record(agent_execution_id);
CREATE INDEX IF NOT EXISTS idx_tool_invocation_execution ON tool_invocation(agent_execution_id);
CREATE INDEX IF NOT EXISTS idx_agent_spec_name ON agent_spec(name);
CREATE INDEX IF NOT EXISTS idx_skill_template_name ON skill_template(name);
CREATE INDEX IF NOT EXISTS idx_task_template_name ON task_template(name);