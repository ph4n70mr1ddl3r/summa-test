const API_BASE = import.meta.env.VITE_API_URL || '/api';
const TOKEN_KEY = 'summa_auth_token';

function loadToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

let authToken: string | null = loadToken();

export function setAuthToken(token: string | null) {
  authToken = token;
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    // localStorage unavailable — token still works in memory for this session
  }
}

export function getAuthToken(): string | null {
  return authToken;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');
  if (authToken) {
    headers.set('Authorization', `Bearer ${authToken}`);
  }
  const res = await fetch(`${API_BASE}${path}`, {
    headers,
    ...init,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`HTTP ${res.status}: ${text}`);
  }
  return res.json() as Promise<T>;
}

export interface Human {
  id: string;
  name: string;
  email: string;
  rbac: string;
  active: boolean;
  createdAt?: string;
  auth?: string;
  timezone?: string;
  workingHours?: string;
  updatedAt?: string;
  deactivatedAt?: string;
}

export interface Agent {
  id: string;
  name: string;
  ownerHumanId: string;
  class: string;
  status: AgentStatus;
  templateId?: string;
  lineageDepth?: number;
  createdAt?: string;
  spawnedBy?: string;
  ttlAt?: string;
  budgetCap?: number;
  templateVersion?: string;
  suspendedAt?: string;
  retiredAt?: string;
  archivedAt?: string;
  updatedAt?: string;
}

export type AgentStatus = 'active' | 'suspended' | 'retiring' | 'archived' | 'requested';

export interface Ask {
  id: string;
  kind: AskKind;
  from: string;
  to: string;
  payload: string;
  slaTier: AskTier;
  status: string;
  deadline: number;
  quorumRequired?: number;
  collapsedCount?: number;
  initiativeId?: string;
  workspaceId?: string;
  updatedAt?: string;
  escalation?: string;
  expiryBehavior?: string;
  respondedAt?: string;
}

export type AskKind = 'approval' | 'question' | 'assignment' | 'spawn_request';
export type AskTier = 'critical' | 'standard' | 'bulk';

export interface SpawnRequest {
  id: string;
  requesterId: string;
  templateId?: string;
  customRole?: string;
  spawnClass: string;
  purpose: string;
  status: SpawnStatus;
  requestedByHumanId?: string;
  agentId?: string;
  approvedAt?: string;
  workspaceBindings?: string;
  scopeCeiling?: string;
  budgetCap?: number;
  ttlHours?: number;
  approvedBy?: string;
  createdAt?: string;
}

export type SpawnStatus = 'requested' | 'approved' | 'denied' | 'expired' | 'archived';

export interface Initiative {
  id: string;
  title: string;
  sponsor: string;
  lead: string;
  status: InitiativeStatus;
  deadline?: string;
  goalRef?: string;
  decisionRef?: string;
  businessBudget?: string;
  closedAt?: string;
  dependsOn?: string;
  updatedAt?: string;
}

export type InitiativeStatus = 'proposed' | 'active' | 'paused' | 'closed';

export interface Run {
  id: string;
  agentId: string;
  workspaceId?: string;
  status: RunStatus;
  result?: string;
  costTokens?: number;
  costUsd?: number;
  createdAt?: string;
  prompt?: string;
  artifacts?: string;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  initiativeId?: string;
  triggerId?: string;
  playbookId?: string;
  parentRunId?: string;
}

export type RunStatus = 'queued' | 'running' | 'completed' | 'failed' | 'cancelled' | 'suspended';

export interface DnaCard {
  id: string;
  domainId: string;
  title: string;
  definitionMd: string;
  refs: string;
  provenance: string;
  version: number;
  status: DnaCardStatus;
  createdAt?: string;
  updatedAt?: string;
}

export type DnaCardStatus = 'draft' | 'active' | 'retired';

export interface DnaRule {
  id: string;
  domainId: string;
  statementMd: string;
  machineHint?: string;
  effectiveFrom: string;
  effectiveTo?: string;
  supersedesId?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DnaDecision {
  id: string;
  domainId: string;
  contextMd: string;
  outcomeMd: string;
  decidedBy: string;
  decidedAt: string;
  refs?: string;
  provenance?: string;
}

export interface DnaGoal {
  id: string;
  domainId?: string;
  quarter?: string;
  statementMd: string;
  owner: string;
  status: DnaGoalStatus;
  inject: string;
  effectiveFrom: string;
  effectiveTo?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type DnaGoalStatus = 'active' | 'met' | 'missed' | 'retired';

export interface DnaRule {
  id: string;
  domainId: string;
  statementMd: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface DnaDecision {
  id: string;
  domainId: string;
  contextMd: string;
  outcomeMd: string;
}

export interface DnaDomain {
  id: string;
  name: string;
  ownerHumanId: string;
  access: DomainAccess;
  status: DnaDomainStatus;
}

export type DomainAccess = 'public' | 'domain' | 'named';
export type DnaDomainStatus = 'active' | 'archived';

export interface GovernanceSetting {
  [key: string]: unknown;
}

export interface BoardTask {
  id: string;
  title: string;
  description: string;
  assigneeMemberId?: string;
  initiativeId?: string;
  status: string;
  priority: number;
  dueAt?: string;
  createdBy: string;
  createdAt?: string;
  completedAt?: string;
}

export interface Trigger {
  id: string;
  name: string;
  kind: string;
  expression: string;
  agentId: string;
  workspaceId?: string;
  criticality: TriggerCriticality;
  status: TriggerStatus;
  config?: string;
  lastFiredAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type TriggerCriticality = 'critical' | 'standard';
export type TriggerStatus = 'active' | 'paused' | 'archived';

export interface Workspace {
  id: string;
  name: string;
  kind: WorkspaceKind;
  initiativeIds: string[];
  domainIds: string[];
  nodeId?: string;
  claimEpoch: number;
  leaseExpiresAt?: string;
  participants: string[];
  archivedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type WorkspaceKind = 'project' | 'personal' | 'system';

export interface Node {
  id: string;
  name: string;
  kind: NodeKind;
  capabilities: Record<string, unknown>;
  region?: string;
  pubkey: string;
  enrolledAt: string;
  status: NodeStatus;
  revokedAt?: string;
  updatedAt?: string;
  claim?: string;
  lastHeartbeat?: string;
}

export type NodeKind = 'local' | 'remote';
export type NodeStatus = 'trusted' | 'revoked';

export interface RoleTemplate {
  id: string;
  name: string;
  version: number;
  class: string;
  status: RoleTemplateStatus;
  body?: string;
  defaultScopes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type RoleTemplateStatus = 'draft' | 'active' | 'retired';

export interface MemoryItem {
  id: string;
  tier: MemoryTier;
  memberId?: string;
  workspaceId?: string;
  contentMd: string;
  provenance: string;
  tainted: boolean;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt?: string;
}

export type MemoryTier = 'personal' | 'project' | 'proposal';

export interface Pat {
  id: string;
  memberId: string;
  name: string;
  createdAt: string;
  expiresAt: string;
  revokedAt?: string;
  lastUsedAt?: string;
}

export interface Group {
  id: string;
  name: string;
  leaderMemberId?: string;
  status: GroupStatus;
  createdAt: string;
}

export type GroupStatus = 'active' | 'archived';

export interface SpendSnapshot {
  periodTokensIn: number;
  periodTokensOut: number;
  periodCostUsd: number;
  circuitBreakerTripped: boolean;
}

function buildQuery(params?: Record<string, string | number | undefined>): string {
  const entries = Object.entries(params ?? {}).filter(([, v]) => v !== undefined);
  if (entries.length === 0) return '';
  const qs = new URLSearchParams(entries.map(([k, v]) => [k, String(v)])).toString();
  return qs ? `?${qs}` : '';
}

export const api = {
  agents: {
    list: (params?: { status?: string; ownerId?: string }) =>
      request<Agent[]>(`/agents${buildQuery(params)}`),
    get: (id: string) => request<Agent>(`/agents/${id}`),
    lineage: (id: string) => request<string[]>(`/agents/${id}/lineage`),
    suspend: (id: string, actor: string) =>
      request<Agent>(`/agents/${id}/suspend`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    resume: (id: string, actor: string) =>
      request<Agent>(`/agents/${id}/resume`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    retire: (id: string, actor: string) =>
      request<Agent>(`/agents/${id}/retire`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    archive: (id: string, actor: string) =>
      request<Agent>(`/agents/${id}/archive`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  dna: {
    cards: (domainId?: string) =>
      request<DnaCard[]>(`/dna/cards${buildQuery(domainId ? { domainId } : undefined)}`),
    rules: (domainId?: string) =>
      request<DnaRule[]>(`/dna/rules${buildQuery(domainId ? { domainId } : undefined)}`),
    decisions: (domainId?: string) =>
      request<DnaDecision[]>(`/dna/decisions${buildQuery(domainId ? { domainId } : undefined)}`),
    search: (query: string) =>
      request<unknown[]>(`/dna/search${buildQuery({ q: query })}`),
    domains: () => request<DnaDomain[]>('/dna/domains'),
    proposals: (status?: string) =>
      request<unknown[]>(`/dna/proposals${buildQuery(status ? { status } : undefined)}`),
    publishProposal: (id: string, actor: string) =>
      request(`/dna/proposals/${id}/review`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ action: 'publish' }),
      }),
    reviewProposal: (id: string, action: string, actor: string) =>
      request(`/dna/proposals/${id}/review`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ action }),
      }),
    withdrawProposal: (id: string, actor: string) =>
      request(`/dna/proposals/${id}/withdraw`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    amendProposal: (id: string, payload: string, actor: string) =>
      request(`/dna/proposals/${id}/amend`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ payload }),
      }),
  },
  asks: {
    list: () =>
      request<Ask[]>('/asks'),
    listByStatus: (status: string) =>
      request<Ask[]>(`/asks${buildQuery({ status })}`),
    respond: (id: string, response: string, actor: string) =>
      request<Ask>(`/asks/${id}/respond`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ response }),
      }),
    withdraw: (id: string, actor: string) =>
      request<Ask>(`/asks/${id}/withdraw`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    expire: (id: string) =>
      request<Ask>(`/asks/${id}/expire`, {
        method: 'POST',
      }),
  },
  org: {
    health: () => request<{ status: string; service: string }>('/health'),
    bootstrap: (body?: Record<string, string>) =>
      request('/org/bootstrap', { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
    humans: (active?: boolean) =>
      request<Human[]>(`/org/humans${buildQuery(active !== undefined ? { active: String(active) } : undefined)}`),
    updateRbac: (id: string, rbac: string, actor: string) =>
      request<Human>(`/org/humans/${id}/rbac`, {
        method: 'PUT',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ rbac }),
      }),
    setDeputy: (id: string, deputyId: string, actor: string) =>
      request<Human>(`/org/humans/${id}/deputy`, {
        method: 'PUT',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ deputyMemberId: deputyId }),
      }),
    offboard: (id: string, actor: string) =>
      request<Human>(`/org/humans/${id}/offboard`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    erasure: (id: string, actor: string) =>
      request(`/org/humans/${id}/erasure`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    members: () => request<{ members: (Human | Agent)[]; total: number }>('/org/members'),
    lineage: (memberId: string) => request<string[]>(`/org/lineage${buildQuery({ memberId })}`),
    audit: (limit?: number, objectType?: string, objectId?: string) =>
      request<unknown[]>(`/org/audit${buildQuery({
        ...(limit !== undefined ? { limit: String(limit) } : {}),
        ...(objectType ? { objectType } : {}),
        ...(objectId ? { objectId } : {}),
      })}`),
  },
  spawn: {
    list: (status?: string, requesterId?: string) =>
      request<SpawnRequest[]>(`/spawn${buildQuery({ status, requesterId })}`),
    create: (body: Record<string, string>, actor: string) =>
      request<SpawnRequest>('/spawn', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    approve: (id: string, actor: string) =>
      request<SpawnRequest>(`/spawn/${id}/approve`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    deny: (id: string, actor: string) =>
      request<SpawnRequest>(`/spawn/${id}/deny`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    stats: () => request('/spawn/stats'),
  },
  initiatives: {
    list: (status?: string) =>
      request<Initiative[]>(`/initiatives${buildQuery(status ? { status } : undefined)}`),
    create: (body: Record<string, string>, actor: string) =>
      request<Initiative>('/initiatives', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    activate: (id: string, actor: string) =>
      request<Initiative>(`/initiatives/${id}/activate`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    pause: (id: string, actor: string) =>
      request<Initiative>(`/initiatives/${id}/pause`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    resume: (id: string, actor: string) =>
      request<Initiative>(`/initiatives/${id}/resume`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    close: (id: string, actor: string) =>
      request<Initiative>(`/initiatives/${id}/close`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  runs: {
    list: (params?: { agentId?: string; workspaceId?: string; status?: string; limit?: number }) =>
      request<Run[]>(`/runs${buildQuery(params)}`),
    create: (body: Record<string, string>, actor: string) =>
      request<Run>('/runs', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    start: (id: string) =>
      request<Run>(`/runs/${id}/start`, { method: 'POST' }),
    complete: (id: string, body: Record<string, unknown>) =>
      request<Run>(`/runs/${id}/complete`, {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    fail: (id: string, errorMessage: string) =>
      request<Run>(`/runs/${id}/fail`, {
        method: 'POST',
        body: JSON.stringify({ errorMessage }),
      }),
    cancel: (id: string) =>
      request<Run>(`/runs/${id}/cancel`, { method: 'POST' }),
    stats: () => request('/runs/stats'),
  },
  auth: {
    login: (email: string, password: string) =>
      request<{ token: string; userId: string; rbac: string; name: string }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
    changePassword: (currentPassword: string, newPassword: string) =>
      request<{ message: string }>('/auth/change-password', {
        method: 'PUT',
        body: JSON.stringify({ currentPassword, newPassword }),
      }),
  },
  health: () => request<{ status: string }>('/health'),
  boardTasks: {
    list: (params?: { status?: string; assigneeId?: string; initiativeId?: string }) =>
      request<BoardTask[]>(`/board-tasks${buildQuery(params)}`),
    create: (body: Record<string, string>, actor: string) =>
      request('/board-tasks', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    assign: (id: string, memberId: string, actor: string) =>
      request(`/board-tasks/${id}/assign`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ assigneeMemberId: memberId }),
      }),
    complete: (id: string, actor: string) =>
      request(`/board-tasks/${id}/complete`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    unassign: (id: string, actor: string) =>
      request(`/board-tasks/${id}/unassign`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  triggers: {
    list: (agentId?: string) =>
      request<Trigger[]>(`/triggers${buildQuery(agentId ? { agentId } : undefined)}`),
    create: (body: Record<string, string>, actor: string) =>
      request('/triggers', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    pause: (id: string, actor: string) =>
      request(`/triggers/${id}/pause`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    resume: (id: string, actor: string) =>
      request(`/triggers/${id}/resume`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    archive: (id: string, actor: string) =>
      request(`/triggers/${id}/archive`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    stats: () => request('/triggers/stats'),
  },
  workspaces: {
    list: () => request<Workspace[]>('/workspaces'),
    create: (body: Record<string, string>, actor: string) =>
      request('/workspaces', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    rebind: (id: string, targetNodeId: string, actor: string) =>
      request(`/workspaces/${id}/rebind`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ targetNodeId }),
      }),
    archive: (id: string, actor: string) =>
      request(`/workspaces/${id}/archive`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  nodes: {
    list: () => request<Node[]>('/nodes'),
    enroll: (body: Record<string, string>) =>
      request('/nodes/enroll', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    revoke: (id: string, actor: string) =>
      request(`/nodes/${id}/revoke`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  governance: {
    policies: () => request<GovernanceSetting>('/governance/policies'),
    quotas: () => request<GovernanceSetting>('/governance/quotas'),
    spend: () => request<SpendSnapshot>('/governance/spend'),
    updatePolicies: (body: Record<string, unknown>, actor: string) =>
      request('/governance/policies', {
        method: 'PUT',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    updateQuotas: (body: Record<string, unknown>, actor: string) =>
      request('/governance/quotas', {
        method: 'PUT',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    createHold: (body: Record<string, string>, actor: string) =>
      request('/governance/holds', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    releaseHold: (id: string, actor: string) =>
      request(`/governance/holds/${id}/release`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  admin: {
    backup: (backupDir?: string) =>
      request('/admin/backup', {
        method: 'POST',
        body: JSON.stringify({ backupDir }),
      }),
    restore: (backupPath: string) =>
      request('/admin/backup/restore', {
        method: 'POST',
        body: JSON.stringify({ backupPath }),
      }),
    scanSecrets: (actor: string) =>
      request('/admin/secrets/scan', {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  roleTemplates: {
    list: () => request<RoleTemplate[]>('/role-templates'),
    create: (body: Record<string, string>, actor: string) =>
      request('/role-templates', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    publish: (id: string, actor: string) =>
      request(`/role-templates/${id}/publish`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    retire: (id: string, actor: string) =>
      request(`/role-templates/${id}/retire`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  memory: {
    list: (params?: { memberId?: string; workspaceId?: string; tainted?: string }) =>
      request<MemoryItem[]>(`/memory${buildQuery(params)}`),
    create: (body: Record<string, string>, actor: string) =>
      request('/memory', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    review: (id: string, actor: string) =>
      request(`/memory/${id}/review`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  authPats: {
    list: (memberId: string) =>
      request<Pat[]>(`/auth/pats${buildQuery({ memberId })}`),
    create: (body: Record<string, string>, actor: string) =>
      request('/auth/pats', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    revoke: (id: string, actor: string) =>
      request(`/auth/pats/${id}/revoke`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  groups: {
    list: () => request<Group[]>('/org/groups'),
    create: (body: Record<string, string>, actor: string) =>
      request('/org/groups', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    archive: (id: string, actor: string) =>
      request(`/org/groups/${id}/archive`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    setLeader: (id: string, memberId: string, actor: string) =>
      request(`/org/groups/${id}/leader`, {
        method: 'PUT',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ memberId }),
      }),
  },
};
