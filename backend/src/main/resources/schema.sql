CREATE TABLE IF NOT EXISTS humans (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    rbac TEXT NOT NULL CHECK (rbac IN ('admin', 'owner', 'member', 'viewer')),
    auth TEXT NOT NULL DEFAULT '{}',
    password_hash TEXT,
    deputy_member_id TEXT,
    timezone TEXT,
    working_hours TEXT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    deactivated_at INTEGER,
    FOREIGN KEY (deputy_member_id) REFERENCES humans(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS agents (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    owner_human_id TEXT NOT NULL,
    "class" TEXT NOT NULL CHECK ("class" IN ('persistent', 'ephemeral', 'ephemeral-subagent')),
    spawned_by TEXT,
    ttl_at INTEGER,
    budget_cap REAL,
    lineage_depth INTEGER NOT NULL DEFAULT 0,
    template_id TEXT,
    template_version TEXT,
    status TEXT NOT NULL DEFAULT 'requested' CHECK (status IN ('requested', 'active', 'suspended', 'retiring', 'archived')),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    suspended_at INTEGER,
    retired_at INTEGER,
    archived_at INTEGER,
    FOREIGN KEY (owner_human_id) REFERENCES humans(id) ON DELETE SET NULL,
    FOREIGN KEY (spawned_by) REFERENCES agents(id) ON DELETE SET NULL,
    FOREIGN KEY (template_id) REFERENCES role_templates(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS role_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    class TEXT NOT NULL CHECK (class IN ('persistent', 'ephemeral-subagent')),
    body TEXT NOT NULL DEFAULT '{}',
    default_scopes TEXT NOT NULL DEFAULT '{}',
    status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'retired')),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE(class, name, version)
);

CREATE TABLE IF NOT EXISTS nodes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('local', 'remote')),
    capabilities TEXT NOT NULL DEFAULT '{}',
    region TEXT,
    claim TEXT,
    last_heartbeat INTEGER,
    pubkey TEXT NOT NULL,
    enrolled_at INTEGER NOT NULL DEFAULT (unixepoch()),
    revoked_at INTEGER,
    status TEXT NOT NULL DEFAULT 'trusted' CHECK (status IN ('trusted', 'revoked')),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE TABLE IF NOT EXISTS dna_domains (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    owner_human_id TEXT NOT NULL,
    access TEXT NOT NULL DEFAULT 'public' CHECK (access IN ('public', 'domain', 'named')),
    named_readers TEXT NOT NULL DEFAULT '[]',
    store TEXT NOT NULL DEFAULT 'git' CHECK (store IN ('git', 'db-only')),
    sod TEXT NOT NULL DEFAULT 'off' CHECK (sod IN ('off', 'reviewer-distinct')),
    review_sla_days INTEGER NOT NULL DEFAULT 7 CHECK (review_sla_days >= 1),
    residency TEXT,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'archived')),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (owner_human_id) REFERENCES humans(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dna_domains_name ON dna_domains(name) WHERE status != 'archived';

CREATE TABLE IF NOT EXISTS dna_cards (
    id TEXT PRIMARY KEY,
    domain_id TEXT NOT NULL,
    title TEXT NOT NULL,
    definition_md TEXT NOT NULL DEFAULT '',
    refs TEXT NOT NULL DEFAULT '[]',
    provenance TEXT NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('draft', 'active', 'retired')),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (domain_id) REFERENCES dna_domains(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dna_rules (
    id TEXT PRIMARY KEY,
    domain_id TEXT NOT NULL,
    statement_md TEXT NOT NULL,
    machine_hint TEXT,
    effective_from INTEGER NOT NULL,
    effective_to INTEGER,
    supersedes_id TEXT,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'superseded', 'lapsed')),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (domain_id) REFERENCES dna_domains(id) ON DELETE CASCADE,
    FOREIGN KEY (supersedes_id) REFERENCES dna_rules(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS dna_decisions (
    id TEXT PRIMARY KEY,
    domain_id TEXT NOT NULL,
    context_md TEXT NOT NULL DEFAULT '',
    outcome_md TEXT NOT NULL,
    decided_by TEXT NOT NULL,
    decided_at INTEGER NOT NULL DEFAULT (unixepoch()),
    refs TEXT NOT NULL DEFAULT '[]',
    provenance TEXT NOT NULL DEFAULT '{}',
    FOREIGN KEY (domain_id) REFERENCES dna_domains(id) ON DELETE CASCADE
    -- decided_by is a keyed union per DAT-120: h:<humans.id> or a:<agents.id>
);

CREATE TABLE IF NOT EXISTS dna_glossary (
    id TEXT PRIMARY KEY,
    domain_id TEXT,
    term TEXT NOT NULL,
    definition TEXT NOT NULL DEFAULT '',
    aliases TEXT NOT NULL DEFAULT '[]',
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('draft', 'active', 'retired')),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (domain_id) REFERENCES dna_domains(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dna_glossary_term_domain ON dna_glossary(term, domain_id) WHERE status != 'retired';

CREATE TABLE IF NOT EXISTS dna_goals (
    id TEXT PRIMARY KEY,
    domain_id TEXT,
    quarter TEXT,
    statement_md TEXT NOT NULL,
    owner TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'met', 'missed', 'retired')),
    inject TEXT NOT NULL DEFAULT 'linked' CHECK (inject IN ('always', 'linked')),
    effective_from INTEGER NOT NULL,
    effective_to INTEGER,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (domain_id) REFERENCES dna_domains(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dna_proposals (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('card', 'rule', 'decision', 'goal', 'glossary', 'edit')),
    payload TEXT NOT NULL DEFAULT '{}',
    revision INTEGER NOT NULL DEFAULT 1,
    proposed_by TEXT NOT NULL,
    provenance TEXT NOT NULL DEFAULT '{}',
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'published', 'rejected', 'withdrawn')),
    reviewed_by TEXT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    reviewed_at INTEGER,
    domain_id TEXT,
    FOREIGN KEY (reviewed_by) REFERENCES humans(id) ON DELETE SET NULL,
    FOREIGN KEY (domain_id) REFERENCES dna_domains(id) ON DELETE SET NULL
    -- proposed_by is a keyed union per DAT-120: h:<humans.id> or a:<agents.id>
);

CREATE INDEX IF NOT EXISTS idx_dna_proposals_domain ON dna_proposals(domain_id);
CREATE INDEX IF NOT EXISTS idx_agents_status ON agents(status);
CREATE INDEX IF NOT EXISTS idx_agents_owner ON agents(owner_human_id);
CREATE INDEX IF NOT EXISTS idx_agents_template ON agents(template_id);
CREATE INDEX IF NOT EXISTS idx_dna_goals_domain ON dna_goals(domain_id);
CREATE INDEX IF NOT EXISTS idx_dna_goals_status ON dna_goals(status);
-- NOTE: indexes on initiatives/group_memberships/memory_items/spend_ledger live
-- directly after their CREATE TABLEs below (SQLite requires the table to exist).

CREATE TABLE IF NOT EXISTS asks (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('approval', 'question', 'assignment', 'spawn_request', 'promotion')),
    "from" TEXT NOT NULL,
    "to" TEXT NOT NULL,
    payload TEXT NOT NULL DEFAULT '{}',
    initiative_id TEXT,
    workspace_id TEXT,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'answered', 'expired', 'withdrawn')),
    deadline INTEGER NOT NULL,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    sla_tier TEXT NOT NULL DEFAULT 'standard' CHECK (sla_tier IN ('critical', 'standard', 'bulk')),
    escalation TEXT,
    expiry_behavior TEXT NOT NULL DEFAULT 'deny' CHECK (expiry_behavior IN ('deny', 'escalate', 'reassign')),
    responded_at INTEGER,
    quorum_required INTEGER NOT NULL DEFAULT 1 CHECK (quorum_required >= 1),
    responses TEXT NOT NULL DEFAULT '[]',
    collapsed_count INTEGER NOT NULL DEFAULT 1,
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (initiative_id) REFERENCES initiatives(id) ON DELETE SET NULL,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_asks_deadline ON asks(deadline);
CREATE INDEX IF NOT EXISTS idx_asks_status ON asks(status);

CREATE TABLE IF NOT EXISTS initiatives (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    goal_ref TEXT,
    decision_ref TEXT,
    sponsor TEXT NOT NULL,
    lead TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'proposed' CHECK (status IN ('proposed', 'active', 'paused', 'closed')),
    business_budget TEXT,
    deadline INTEGER,
    closed_at INTEGER,
    depends_on TEXT NOT NULL DEFAULT '[]',
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (goal_ref) REFERENCES dna_goals(id) ON DELETE SET NULL,
    FOREIGN KEY (decision_ref) REFERENCES dna_decisions(id) ON DELETE SET NULL
    -- sponsor and lead are keyed unions per DAT-120: h:<humans.id> or a:<agents.id>
);

CREATE INDEX IF NOT EXISTS idx_initiatives_status ON initiatives(status);

CREATE TABLE IF NOT EXISTS board_tasks (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    assignee_member_id TEXT,
    initiative_id TEXT,
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'in_progress', 'done', 'cancelled')),
    priority INTEGER NOT NULL DEFAULT 0,
    due_at INTEGER,
    created_by TEXT NOT NULL,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    completed_at INTEGER,
    FOREIGN KEY (initiative_id) REFERENCES initiatives(id) ON DELETE SET NULL
    -- assignee_member_id and created_by are keyed unions per DAT-120: h:<humans.id> or a:<agents.id>
);

CREATE INDEX IF NOT EXISTS idx_board_tasks_status ON board_tasks(status);

CREATE TABLE IF NOT EXISTS workspaces (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('project', 'personal', 'system')),
    initiative_ids TEXT NOT NULL DEFAULT '[]',
    domain_ids TEXT NOT NULL DEFAULT '[]',
    node_id TEXT,
    claim_epoch INTEGER NOT NULL DEFAULT 0,
    lease_expires_at INTEGER,
    participants TEXT NOT NULL DEFAULT '[]',
    archived_at INTEGER,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS triggers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('schedule', 'api', 'event')),
    expression TEXT NOT NULL DEFAULT '',
    agent_id TEXT NOT NULL,
    workspace_id TEXT,
    criticality TEXT NOT NULL DEFAULT 'standard' CHECK (criticality IN ('standard', 'critical')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'paused', 'archived')),
    config TEXT NOT NULL DEFAULT '{}',
    last_fired_at INTEGER,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS playbooks (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    body TEXT NOT NULL DEFAULT '{}',
    criticality TEXT NOT NULL DEFAULT 'standard' CHECK (criticality IN ('standard', 'critical')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('draft', 'active', 'retired')),
    created_by TEXT NOT NULL,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (created_by) REFERENCES agents(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS spend_ledger (
    id TEXT PRIMARY KEY,
    member_id TEXT NOT NULL,
    run_id TEXT,
    spawn_id TEXT,
    kind TEXT NOT NULL CHECK (kind IN ('reserve', 'settle', 'release')),
    tokens_in REAL NOT NULL DEFAULT 0,
    tokens_out REAL NOT NULL DEFAULT 0,
    cost REAL NOT NULL DEFAULT 0,
    pricing_version TEXT NOT NULL DEFAULT 'v1',
    acknowledged INTEGER NOT NULL DEFAULT 0 CHECK (acknowledged IN (0, 1)),
    at INTEGER NOT NULL DEFAULT (unixepoch()),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (member_id) REFERENCES agents(id) ON DELETE CASCADE,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE SET NULL,
    FOREIGN KEY (spawn_id) REFERENCES spawn_requests(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_spend_ledger_member ON spend_ledger(member_id);
CREATE INDEX IF NOT EXISTS idx_spend_ledger_kind ON spend_ledger(kind);

CREATE TABLE IF NOT EXISTS trigger_firings (
    id TEXT PRIMARY KEY,
    trigger_id TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    fired_at INTEGER NOT NULL DEFAULT (unixepoch()),
    run_id TEXT,
    UNIQUE(trigger_id, idempotency_key),
    FOREIGN KEY (trigger_id) REFERENCES triggers(id) ON DELETE CASCADE,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS external_writes (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    connector TEXT NOT NULL,
    op TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'prepared' CHECK (status IN ('prepared', 'committed', 'compensated', 'failed')),
    prepared_at INTEGER NOT NULL DEFAULT (unixepoch()),
    resolved_at INTEGER,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS data_holds (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('member', 'domain')),
    subject_id TEXT NOT NULL,
    reason_md TEXT NOT NULL DEFAULT '',
    created_by TEXT NOT NULL,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    released_at INTEGER,
    FOREIGN KEY (created_by) REFERENCES humans(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_data_holds_subject ON data_holds(kind, subject_id);

CREATE TABLE IF NOT EXISTS groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    leader_member_id TEXT,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'archived')),
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch())
    -- leader_member_id is a keyed union per DAT-120: h:<humans.id> or a:<agents.id>
);

-- Partial uniqueness (names may be reused once archived) as a separate partial
-- index: SQLite does not allow WHERE clauses on inline table constraints.
CREATE UNIQUE INDEX IF NOT EXISTS uq_groups_name_active ON groups(name) WHERE status != 'archived';

CREATE TABLE IF NOT EXISTS group_memberships (
    group_id TEXT NOT NULL,
    member_id TEXT NOT NULL,
    added_by TEXT NOT NULL,
    added_at INTEGER NOT NULL DEFAULT (unixepoch()),
    removed_at INTEGER,
    PRIMARY KEY (group_id, member_id),
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
    -- member_id and added_by are keyed unions per DAT-120: h:<humans.id> or a:<agents.id>
);

CREATE INDEX IF NOT EXISTS idx_group_memberships_member ON group_memberships(member_id);

CREATE TABLE IF NOT EXISTS audit_events (
    id TEXT PRIMARY KEY,
    at INTEGER NOT NULL DEFAULT (unixepoch()),
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    object_type TEXT NOT NULL,
    object_id TEXT NOT NULL,
    detail TEXT NOT NULL DEFAULT '{}',
    node_id TEXT,
    origin TEXT NOT NULL DEFAULT 'live' CHECK (origin IN ('live', 'replay')),
    FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_events_at ON audit_events(at);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events(actor);
CREATE INDEX IF NOT EXISTS idx_audit_events_object ON audit_events(object_type, object_id);

CREATE TABLE IF NOT EXISTS pats (
    id TEXT PRIMARY KEY,
    member_id TEXT NOT NULL,
    name TEXT NOT NULL,
    token_hash TEXT NOT NULL,
    scopes TEXT NOT NULL DEFAULT '[]',
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    expires_at INTEGER NOT NULL,
    revoked_at INTEGER,
    last_used_at INTEGER,
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE(token_hash)
    -- member_id is a keyed union per DAT-120: h:<humans.id> or a:<agents.id>
);

CREATE TABLE IF NOT EXISTS governance_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL DEFAULT '{}',
    edited_by TEXT NOT NULL,
    edited_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (edited_by) REFERENCES humans(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS memory_items (
    id TEXT PRIMARY KEY,
    tier TEXT NOT NULL CHECK (tier IN ('personal', 'project', 'proposal')),
    member_id TEXT,
    workspace_id TEXT,
    content_md TEXT NOT NULL DEFAULT '',
    provenance TEXT NOT NULL DEFAULT '{}',
    tainted INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    reviewed_by TEXT,
    reviewed_at INTEGER,
    FOREIGN KEY (member_id) REFERENCES agents(id) ON DELETE SET NULL,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL,
    FOREIGN KEY (reviewed_by) REFERENCES humans(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_items_tier ON memory_items(tier);
CREATE INDEX IF NOT EXISTS idx_memory_items_workspace ON memory_items(workspace_id);

CREATE TABLE IF NOT EXISTS runs (
    id TEXT PRIMARY KEY,
    agent_id TEXT NOT NULL,
    workspace_id TEXT,
    initiative_id TEXT,
    trigger_id TEXT,
    playbook_id TEXT,
    parent_run_id TEXT,
    status TEXT NOT NULL DEFAULT 'queued' CHECK (status IN ('queued', 'running', 'completed', 'failed', 'cancelled', 'suspended')),
    prompt TEXT NOT NULL DEFAULT '',
    result TEXT,
    artifacts TEXT NOT NULL DEFAULT '[]',
    error_message TEXT,
    started_at INTEGER,
    completed_at INTEGER,
    cost_tokens INTEGER NOT NULL DEFAULT 0,
    cost_usd REAL NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL,
    FOREIGN KEY (initiative_id) REFERENCES initiatives(id) ON DELETE SET NULL,
    FOREIGN KEY (trigger_id) REFERENCES triggers(id) ON DELETE SET NULL,
    FOREIGN KEY (playbook_id) REFERENCES playbooks(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_run_id) REFERENCES runs(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_runs_agent ON runs(agent_id);
CREATE INDEX IF NOT EXISTS idx_runs_workspace ON runs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_runs_status ON runs(status);
CREATE INDEX IF NOT EXISTS idx_runs_created ON runs(created_at);

CREATE TABLE IF NOT EXISTS spawn_requests (
    id TEXT PRIMARY KEY,
    requester_id TEXT NOT NULL,
    template_id TEXT,
    custom_role TEXT,
    "class" TEXT NOT NULL CHECK ("class" IN ('persistent', 'ephemeral', 'ephemeral-subagent')),
    purpose TEXT NOT NULL,
    workspace_bindings TEXT NOT NULL DEFAULT '[]',
    scope_ceiling TEXT NOT NULL DEFAULT '{}',
    budget_cap REAL,
    ttl_hours INTEGER,
    requested_by_human_id TEXT,
    gate_target TEXT,
    status TEXT NOT NULL DEFAULT 'requested' CHECK (status IN ('requested', 'approved', 'denied', 'expired', 'archived')),
    approved_by TEXT,
    approved_at INTEGER,
    agent_id TEXT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (requester_id) REFERENCES agents(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES role_templates(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES humans(id) ON DELETE SET NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_spawn_requests_requester ON spawn_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_spawn_requests_status ON spawn_requests(status);
CREATE INDEX IF NOT EXISTS idx_spawn_requests_gate ON spawn_requests(gate_target);

CREATE TABLE IF NOT EXISTS messages (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('system', 'user', 'assistant', 'tool')),
    content TEXT NOT NULL DEFAULT '',
    timestamp INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_run ON messages(run_id, timestamp);

-- FTS5 virtual table for DNA search.
-- NOTE: source tables use TEXT UUID primary keys, which cannot be stored in
-- the implicit FTS5 integer rowid. We therefore keep an explicit UNINDEXED
-- `id` column and join/delete on it instead of rowid.
CREATE VIRTUAL TABLE IF NOT EXISTS dna_search_index USING fts5(
    id UNINDEXED,
    title,
    definition_md,
    statement_md,
    context_md,
    outcome_md,
    content_md,
    term,
    definition,
    content,
    status,
    domain_id,
    kind
);

-- Create triggers to keep FTS index in sync
CREATE TRIGGER IF NOT EXISTS dna_cards_ai AFTER INSERT ON dna_cards BEGIN
    INSERT INTO dna_search_index (id, title, definition_md, domain_id, kind, status)
    VALUES (new.id, new.title, new.definition_md, new.domain_id, 'card', new.status);
END;

CREATE TRIGGER IF NOT EXISTS dna_cards_ad AFTER DELETE ON dna_cards BEGIN
    DELETE FROM dna_search_index WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_cards_au AFTER UPDATE ON dna_cards BEGIN
    UPDATE dna_search_index SET
        title = new.title,
        definition_md = new.definition_md,
        domain_id = new.domain_id,
        kind = 'card',
        status = new.status
    WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_rules_ai AFTER INSERT ON dna_rules BEGIN
    INSERT INTO dna_search_index (id, statement_md, domain_id, kind, status)
    VALUES (new.id, new.statement_md, new.domain_id, 'rule', new.status);
END;

CREATE TRIGGER IF NOT EXISTS dna_rules_ad AFTER DELETE ON dna_rules BEGIN
    DELETE FROM dna_search_index WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_rules_au AFTER UPDATE ON dna_rules BEGIN
    UPDATE dna_search_index SET
        statement_md = new.statement_md,
        domain_id = new.domain_id,
        kind = 'rule',
        status = new.status
    WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_decisions_ai AFTER INSERT ON dna_decisions BEGIN
    INSERT INTO dna_search_index (id, context_md, outcome_md, domain_id, kind, status)
    VALUES (new.id, new.context_md, new.outcome_md, new.domain_id, 'decision', 'active');
END;

CREATE TRIGGER IF NOT EXISTS dna_glossary_ai AFTER INSERT ON dna_glossary BEGIN
    INSERT INTO dna_search_index (id, term, definition, domain_id, kind, status)
    VALUES (new.id, new.term, new.definition, new.domain_id, 'glossary', new.status);
END;

CREATE TRIGGER IF NOT EXISTS dna_goals_ai AFTER INSERT ON dna_goals BEGIN
    INSERT INTO dna_search_index (id, statement_md, domain_id, kind, status)
    VALUES (new.id, new.statement_md, new.domain_id, 'goal', new.status);
END;

CREATE TRIGGER IF NOT EXISTS dna_decisions_ad AFTER DELETE ON dna_decisions BEGIN
    DELETE FROM dna_search_index WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_decisions_au AFTER UPDATE ON dna_decisions BEGIN
    UPDATE dna_search_index SET
        context_md = new.context_md,
        outcome_md = new.outcome_md,
        domain_id = new.domain_id,
        kind = 'decision',
        status = 'active'
    WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_glossary_ad AFTER DELETE ON dna_glossary BEGIN
    DELETE FROM dna_search_index WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_glossary_au AFTER UPDATE ON dna_glossary BEGIN
    UPDATE dna_search_index SET
        term = new.term,
        definition = new.definition,
        domain_id = new.domain_id,
        kind = 'glossary',
        status = new.status
    WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_goals_ad AFTER DELETE ON dna_goals BEGIN
    DELETE FROM dna_search_index WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS dna_goals_au AFTER UPDATE ON dna_goals BEGIN
    UPDATE dna_search_index SET
        statement_md = new.statement_md,
        domain_id = new.domain_id,
        kind = 'goal',
        status = new.status
    WHERE id = old.id;
END;
