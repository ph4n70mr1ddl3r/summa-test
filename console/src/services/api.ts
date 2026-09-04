const API_BASE = import.meta.env.VITE_API_URL || '/api';
const TOKEN_KEY = 'summa_auth_token';
const USER_KEY = 'summa_user';

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

function loadToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function loadUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

let authToken: string | null = loadToken();
let currentUser: { userId: string; rbac: string; name: string } | null = loadUser();

export function setAuthToken(token: string | null, user?: { userId: string; rbac: string; name: string } | null) {
  authToken = token;
  currentUser = user ?? null;
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(USER_KEY);
    }
  } catch {
    // storage unavailable — values still work in memory for this session
  }
}

export function getAuthToken(): string | null {
  return authToken;
}

export function getCurrentUser(): { userId: string; rbac: string; name: string } | null {
  return currentUser;
}

export function isAuthenticated(): boolean {
  if (!authToken) return false;
  try {
    const parts = authToken.split('.');
    if (parts.length !== 3) return false;
    const payload = JSON.parse(atob(parts[1])) as { exp?: number };
    if (payload.exp === undefined) return false;
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
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
    let message: string;
    try {
      const err = await res.json() as { code?: string; message?: string };
      message = err.message || `HTTP ${res.status}`;
    } catch {
      const text = await res.text();
      message = text || `HTTP ${res.status}`;
    }
    const err = new ApiError(message, res.status);
    throw err;
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return res.json() as Promise<T>;
}

export interface Human {
  id: string;
  name: string;
  email: string;
  rbac: string;
  active: boolean;
  kind?: 'human';
  createdAt?: number;
  auth?: string;
  timezone?: string;
  workingHours?: string;
  updatedAt?: number;
  deactivatedAt?: number;
}

export interface Agent {
  id: string;
  name: string;
  ownerHumanId: string;
  class: string;
  status: AgentStatus;
  kind?: 'agent';
  templateId?: string;
  lineageDepth?: number;
  createdAt?: number;
  spawnedBy?: string;
  ttlAt?: number;
  budgetCap?: number;
  templateVersion?: string;
  suspendedAt?: number;
  retiredAt?: number;
  archivedAt?: number;
  updatedAt?: number;
}

export type Member = Human & { kind: 'human' } | Agent & { kind: 'agent' };

export type AgentStatus = 'active' | 'suspended' | 'retiring' | 'archived' | 'requested';

export interface Ask {
  id: string;
  kind: AskKind;
  from: string;
  to: string;
  payload: string;
  slaTier: AskTier;
  status: AskStatus;
  deadline: number;
  quorumRequired?: number;
  collapsedCount?: number;
  initiativeId?: string;
  workspaceId?: string;
  updatedAt?: number;
  escalation?: string;
  expiryBehavior?: string;
  respondedAt?: number;
}

export type AskStatus = 'pending' | 'answered' | 'expired' | 'withdrawn';
export type AskKind = 'approval' | 'question' | 'assignment' | 'spawn_request' | 'promotion';
export type AskTier = 'critical' | 'standard' | 'bulk';

export interface SpawnRequest {
  id: string;
  requesterId: string;
  templateId?: string;
  customRole?: string;
  class: string;
  purpose: string;
  status: SpawnStatus;
  requestedByHumanId?: string;
  agentId?: string;
  approvedAt?: number;
  workspaceBindings?: string;
  scopeCeiling?: string;
  budgetCap?: number;
  ttlHours?: number;
  approvedBy?: string;
  createdAt?: number;
}

export type SpawnStatus = 'requested' | 'approved' | 'denied' | 'expired' | 'archived';
export interface SpawnStats {
  requested: number;
  approved: number;
  archived: number;
}

export interface Initiative {
  id: string;
  title: string;
  sponsor: string;
  lead: string;
  status: InitiativeStatus;
  deadline?: number;
  goalRef?: string;
  decisionRef?: string;
  businessBudget?: string;
  closedAt?: number;
  dependsOn?: string[];
  updatedAt?: number;
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
  createdAt?: number;
  prompt?: string;
  artifacts?: string;
  errorMessage?: string;
  startedAt?: number;
  completedAt?: number;
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
  createdAt?: number;
  updatedAt?: number;
}

export type DnaCardStatus = 'draft' | 'active' | 'retired';

export interface DnaRule {
  id: string;
  domainId: string;
  statementMd: string;
  machineHint?: string;
  effectiveFrom: number;
  effectiveTo?: number;
  supersedesId?: string;
  status: DnaRuleStatus;
  createdAt?: number;
  updatedAt?: number;
}

export type DnaRuleStatus = 'active' | 'superseded' | 'lapsed';

export interface DnaDecision {
  id: string;
  domainId: string;
  contextMd: string;
  outcomeMd: string;
  decidedBy: string;
  decidedAt: number;
  refs?: string;
  provenance?: string;
}

export interface DnaProposal {
  id: string;
  domainId?: string;
  kind: DnaProposalKind;
  payload: string;
  revision: number;
  proposedBy: string;
  provenance: string;
  status: DnaProposalStatus;
  reviewedBy?: string;
  createdAt?: number;
  reviewedAt?: number;
  updatedAt?: number;
}

export type DnaProposalStatus = 'open' | 'published' | 'rejected' | 'withdrawn';
export type DnaProposalKind = 'card' | 'rule' | 'decision' | 'goal' | 'glossary' | 'edit';

export interface DnaGoal {
  id: string;
  domainId?: string;
  quarter?: string;
  statementMd: string;
  owner: string;
  status: DnaGoalStatus;
  inject: InjectMode;
  effectiveFrom: number;
  effectiveTo?: number;
  createdAt?: number;
  updatedAt?: number;
}

export type DnaGoalStatus = 'active' | 'met' | 'missed' | 'retired';
export type InjectMode = 'always' | 'linked';

export interface DnaGlossary {
  id: string;
  domainId?: string;
  term: string;
  definition: string;
  aliases: string;
  status: DnaGlossaryStatus;
  createdAt?: number;
  updatedAt?: number;
}

export type DnaGlossaryStatus = 'draft' | 'active' | 'retired';

export interface DnaDomain {
  id: string;
  name: string;
  ownerHumanId: string;
  access: DomainAccess;
  status: DnaDomainStatus;
}

export type DomainAccess = 'public' | 'domain' | 'named';
export type DnaDomainStatus = 'active' | 'archived';

export interface BoardTask {
  id: string;
  title: string;
  description: string;
  assigneeMemberId?: string;
  initiativeId?: string;
  status: BoardTaskStatus;
  priority: number;
  dueAt?: number;
  createdBy: string;
  createdAt?: number;
  completedAt?: number;
}

export type BoardTaskStatus = 'open' | 'in_progress' | 'done' | 'cancelled';

export interface Trigger {
  id: string;
  name: string;
  kind: TriggerKind;
  expression: string;
  agentId: string;
  workspaceId?: string;
  criticality: TriggerCriticality;
  status: TriggerStatus;
  config?: string;
  lastFiredAt?: number;
  createdAt?: number;
  updatedAt?: number;
}

export type TriggerCriticality = 'critical' | 'standard';
export type TriggerStatus = 'active' | 'paused' | 'archived';
export type TriggerKind = 'schedule' | 'api' | 'event';

export interface Workspace {
  id: string;
  name: string;
  kind: WorkspaceKind;
  initiativeIds: string;
  domainIds: string;
  nodeId?: string;
  claimEpoch: number;
  leaseExpiresAt?: number;
  participants: string;
  archivedAt?: number;
  createdAt?: number;
  updatedAt?: number;
}

export type WorkspaceKind = 'project' | 'personal' | 'system';

export interface Node {
  id: string;
  name: string;
  kind: NodeKind;
  capabilities: string;
  region?: string;
  pubkey: string;
  enrolledAt: number;
  status: NodeStatus;
  revokedAt?: number;
  updatedAt?: number;
  claim?: string;
  lastHeartbeat?: number;
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
  createdAt?: number;
  updatedAt?: number;
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
  reviewedAt?: number;
  createdAt?: number;
}

export type MemoryTier = 'personal' | 'project' | 'proposal';

export interface Pat {
  id: string;
  memberId: string;
  name: string;
  scopes: string;
  createdAt: number;
  expiresAt: number;
  revokedAt?: number;
  lastUsedAt?: number;
  updatedAt?: number;
}

export interface Group {
  id: string;
  name: string;
  leaderMemberId?: string;
  status: GroupStatus;
  createdAt: number;
  updatedAt?: number;
}

export type GroupStatus = 'active' | 'archived';

export interface SpendSnapshot {
  reserved: number;
  settled: number;
  ceiling: number;
  utilization: string;
  halted: boolean;
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
    suspend: (id: string) =>
      request<Agent>(`/agents/${id}/suspend`, {
        method: 'POST',
      }),
    resume: (id: string) =>
      request<Agent>(`/agents/${id}/resume`, {
        method: 'POST',
      }),
    retire: (id: string) =>
      request<Agent>(`/agents/${id}/retire`, {
        method: 'POST',
      }),
    archive: (id: string) =>
      request<Agent>(`/agents/${id}/archive`, {
        method: 'POST',
      }),
    promote: (id: string, placement: string) =>
      request<Record<string, unknown>>(`/agents/${id}/promote`, {
        method: 'POST',
        body: JSON.stringify({ placement }),
      }),
  },
  dna: {
    cards: (domainId?: string) =>
      request<DnaCard[]>(`/dna/cards${buildQuery(domainId ? { domainId } : undefined)}`),
    createDraft: (body: Record<string, string>) =>
      request<DnaCard>('/dna/cards/drafts', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    rules: (domainId?: string) =>
      request<DnaRule[]>(`/dna/rules${buildQuery(domainId ? { domainId } : undefined)}`),
    supersedeRule: (id: string, supersedesId: string) =>
      request<DnaRule>(`/dna/rules/${id}/supersede/${supersedesId}`, {
        method: 'POST',
      }),
    decisions: (domainId?: string) =>
      request<DnaDecision[]>(`/dna/decisions${buildQuery(domainId ? { domainId } : undefined)}`),
    search: async (query: string) => {
      const res = await request<{ results: Array<Record<string, unknown>>; count: number }>(`/dna/search${buildQuery({ q: query })}`);
      return res.results;
    },
    domains: () => request<DnaDomain[]>('/dna/domains'),
    archiveDomain: (id: string) =>
      request<DnaDomain>(`/dna/domains/${id}/archive`, {
        method: 'POST',
      }),
    renameDomain: (id: string, name: string) =>
      request<DnaDomain>(`/dna/domains/${id}/rename`, {
        method: 'POST',
        body: JSON.stringify({ name }),
      }),
    updateDomainOwner: (id: string, ownerHumanId: string) =>
      request<DnaDomain>(`/dna/domains/${id}/owner`, {
        method: 'PATCH',
        body: JSON.stringify({ ownerHumanId }),
      }),
    updateDomainAccess: (id: string, access: string) =>
      request<DnaDomain>(`/dna/domains/${id}/access`, {
        method: 'PATCH',
        body: JSON.stringify({ access }),
      }),
    goals: (params?: { domainId?: string; inject?: string }) =>
      request<DnaGoal[]>(`/dna/goals${buildQuery(params)}`),
    updateGoalStatus: (id: string, status: string) =>
      request<DnaGoal>(`/dna/goals/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      }),
    updateGoalWindow: (id: string, effectiveFrom?: number, effectiveTo?: number) => {
      const body: Record<string, string> = {};
      if (effectiveFrom !== undefined) body.effectiveFrom = new Date(effectiveFrom * 1000).toISOString();
      if (effectiveTo !== undefined) body.effectiveTo = new Date(effectiveTo * 1000).toISOString();
      return request<DnaGoal>(`/dna/goals/${id}/window`, {
        method: 'PATCH',
        body: Object.keys(body).length ? JSON.stringify(body) : undefined,
      });
    },
    glossary: (params?: { domainId?: string; scope?: string }) =>
      request<DnaGlossary[]>(`/dna/glossary${buildQuery(params)}`),
    proposals: (status?: string) =>
      request<DnaProposal[]>(`/dna/proposals${buildQuery(status ? { status } : undefined)}`),
    publishProposal: (id: string) =>
      request<DnaProposal>(`/dna/proposals/${id}/review`, {
        method: 'POST',
        body: JSON.stringify({ action: 'publish' }),
      }),
    reviewProposal: (id: string, action: string) =>
      request<DnaProposal>(`/dna/proposals/${id}/review`, {
        method: 'POST',
        body: JSON.stringify({ action }),
      }),
    withdrawProposal: (id: string) =>
      request<DnaProposal>(`/dna/proposals/${id}/withdraw`, {
        method: 'POST',
      }),
    amendProposal: (id: string, payload: string) =>
      request<DnaProposal>(`/dna/proposals/${id}/amend`, {
        method: 'POST',
        body: JSON.stringify({ payload }),
      }),
    reviewQueue: (domainId?: string) =>
      request<DnaProposal[]>(`/dna/review-queue${buildQuery(domainId ? { domainId } : undefined)}`),
  },
  asks: {
    list: () =>
      request<Ask[]>('/asks'),
    listByStatus: (status: string) =>
      request<Ask[]>(`/asks${buildQuery({ status })}`),
    respond: (id: string, response: string) =>
      request<Ask>(`/asks/${id}/respond`, {
        method: 'POST',
        body: JSON.stringify({ response }),
      }),
    withdraw: (id: string) =>
      request<Ask>(`/asks/${id}/withdraw`, {
        method: 'POST',
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
    updateRbac: (id: string, rbac: string) =>
      request<Human>(`/org/humans/${id}/rbac`, {
        method: 'PUT',
        body: JSON.stringify({ rbac }),
      }),
    demote: (id: string, rbac: string) =>
      request<Human>(`/org/humans/${id}/demote`, {
        method: 'PUT',
        body: JSON.stringify({ rbac }),
      }),
    setDeputy: (id: string, deputyId: string) =>
      request<Human>(`/org/humans/${id}/deputy`, {
        method: 'PUT',
        body: JSON.stringify({ deputyMemberId: deputyId }),
      }),
    offboard: (id: string) =>
      request<Human>(`/org/humans/${id}/offboard`, {
        method: 'POST',
      }),
    erasure: (id: string) =>
      request(`/org/humans/${id}/erasure`, {
        method: 'POST',
      }),
    members: () => request<{ members: (Human | Agent)[]; total: number }>('/org/members'),
    lineage: async (memberId: string) => {
      const res = await request<{ memberId: string; lineage: string[] }>(`/org/lineage${buildQuery({ memberId })}`);
      return res.lineage;
    },
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
    create: (body: Record<string, string>) =>
      request<SpawnRequest>('/spawn', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    approve: (id: string) =>
      request<SpawnRequest>(`/spawn/${id}/approve`, {
        method: 'POST',
      }),
    deny: (id: string) =>
      request<SpawnRequest>(`/spawn/${id}/deny`, {
        method: 'POST',
      }),
    stats: () => request<SpawnStats>('/spawn/stats'),
  },
  initiatives: {
    list: (status?: string) =>
      request<Initiative[]>(`/initiatives${buildQuery(status ? { status } : undefined)}`),
    create: (body: Record<string, string>) =>
      request<Initiative>('/initiatives', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    activate: (id: string) =>
      request<Initiative>(`/initiatives/${id}/activate`, {
        method: 'POST',
      }),
    pause: (id: string) =>
      request<Initiative>(`/initiatives/${id}/pause`, {
        method: 'POST',
      }),
    resume: (id: string) =>
      request<Initiative>(`/initiatives/${id}/resume`, {
        method: 'POST',
      }),
    close: (id: string) =>
      request<Initiative>(`/initiatives/${id}/close`, {
        method: 'POST',
      }),
  },
  runs: {
    list: (params?: { agentId?: string; workspaceId?: string; status?: string; limit?: number }) =>
      request<Run[]>(`/runs${buildQuery(params)}`),
    create: (body: Record<string, string>) =>
      request<Run>('/runs', {
        method: 'POST',
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
    stats: () => request<Record<string, number>>('/runs/stats'),
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
    create: (body: Record<string, string>) =>
      request('/board-tasks', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    assign: (id: string, memberId: string) =>
      request(`/board-tasks/${id}/assign`, {
        method: 'POST',
        body: JSON.stringify({ assigneeMemberId: memberId }),
      }),
    complete: (id: string) =>
      request(`/board-tasks/${id}/complete`, {
        method: 'POST',
      }),
    unassign: (id: string) =>
      request(`/board-tasks/${id}/unassign`, {
        method: 'POST',
      }),
  },
  triggers: {
    list: (agentId?: string) =>
      request<Trigger[]>(`/triggers${buildQuery(agentId ? { agentId } : undefined)}`),
    create: (body: Record<string, string>) =>
      request('/triggers', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    pause: (id: string) =>
      request(`/triggers/${id}/pause`, {
        method: 'POST',
      }),
    resume: (id: string) =>
      request(`/triggers/${id}/resume`, {
        method: 'POST',
      }),
    archive: (id: string) =>
      request(`/triggers/${id}/archive`, {
        method: 'POST',
      }),
    stats: () => request('/triggers/stats'),
  },
  workspaces: {
    list: () => request<Workspace[]>('/workspaces'),
    create: (body: Record<string, string>) =>
      request('/workspaces', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    rebind: (id: string, targetNodeId: string) =>
      request(`/workspaces/${id}/rebind`, {
        method: 'POST',
        body: JSON.stringify({ targetNodeId }),
      }),
    archive: (id: string) =>
      request(`/workspaces/${id}/archive`, {
        method: 'POST',
      }),
  },
  nodes: {
    list: () => request<Node[]>('/nodes'),
    enroll: (body: Record<string, string>) =>
      request('/nodes/enroll', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    revoke: (id: string) =>
      request(`/nodes/${id}/revoke`, {
        method: 'POST',
      }),
  },
  governance: {
    policies: () => request<Record<string, unknown>>('/governance/policies'),
    quotas: () => request<Record<string, unknown>>('/governance/quotas'),
    spend: () => request<SpendSnapshot>('/governance/spend'),
    updatePolicies: (body: Record<string, unknown>) =>
      request('/governance/policies', {
        method: 'PUT',
        body: JSON.stringify(body),
      }),
    updateQuotas: (body: Record<string, unknown>) =>
      request('/governance/quotas', {
        method: 'PUT',
        body: JSON.stringify(body),
      }),
    createHold: (body: Record<string, string>) =>
      request('/governance/holds', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    releaseHold: (id: string) =>
      request(`/governance/holds/${id}/release`, {
        method: 'POST',
      }),
    ackSpendOverrun: (id: string) =>
      request(`/governance/spend/overruns/${id}/ack`, {
        method: 'POST',
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
    scanSecrets: (content: string) =>
      request('/admin/secrets/scan', {
        method: 'POST',
        body: JSON.stringify({ content }),
      }),
  },
  roleTemplates: {
    list: () => request<RoleTemplate[]>('/role-templates'),
    create: (body: Record<string, string>) =>
      request('/role-templates', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    publish: (id: string) =>
      request(`/role-templates/${id}/publish`, {
        method: 'POST',
      }),
    retire: (id: string) =>
      request(`/role-templates/${id}/retire`, {
        method: 'POST',
      }),
  },
  memory: {
    list: (params?: { memberId?: string; workspaceId?: string; tainted?: string }) =>
      request<MemoryItem[]>(`/memory${buildQuery(params)}`),
    create: (body: Record<string, string>) =>
      request('/memory', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    review: (id: string) =>
      request(`/memory/${id}/review`, {
        method: 'POST',
      }),
    findTainted: () =>
      request<MemoryItem[]>('/memory?tainted=true'),
  },
  authPats: {
    list: (memberId: string) =>
      request<Pat[]>(`/auth/pats${buildQuery({ memberId })}`),
    create: (body: Record<string, string>) =>
      request('/auth/pats', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    revoke: (id: string) =>
      request(`/auth/pats/${id}/revoke`, {
        method: 'POST',
      }),
  },
  groups: {
    list: () => request<Group[]>('/org/groups'),
    create: (body: Record<string, string>) =>
      request('/org/groups', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    archive: (id: string) =>
      request(`/org/groups/${id}/archive`, {
        method: 'POST',
      }),
    setLeader: (id: string, memberId: string) =>
      request(`/org/groups/${id}/leader`, {
        method: 'PUT',
        body: JSON.stringify({ leaderMemberId: memberId }),
      }),
  },
};
