const API_BASE = import.meta.env.VITE_API_URL || '/api';
const TOKEN_KEY = 'summa_auth_token';
const USER_KEY = 'summa_user';
const REQUEST_TIMEOUT_MS = 30000;
declare global {
  interface Window {
    __summaRedirecting?: boolean;
  }
}

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

let authToken: string | null = loadToken();
let navigateRef: ((path: string, options?: { replace?: boolean }) => void) | null = null;

export function setNavigate(fn: ((path: string, options?: { replace?: boolean }) => void) | null) {
  navigateRef = fn;
}

export function setAuthToken(token: string | null, user?: { userId: string; rbac: RbacRole; name?: string } | null) {
  authToken = token;
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
  window.dispatchEvent(new CustomEvent('summa-auth-change', { detail: user }));
}

export function getAuthToken(): string | null {
  return authToken;
}

export function getUser(): { userId: string; rbac: RbacRole; name?: string } | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    const user = JSON.parse(raw);
    if (!user?.userId || !user?.rbac) return null;
    const validRoles = ['admin', 'owner', 'member', 'viewer'] satisfies RbacRole[];
    if (!validRoles.includes(user.rbac)) return null;
    return { userId: user.userId, rbac: user.rbac, name: user.name };
  } catch {
    return null;
  }
}

export function isAuthenticated(): boolean {
  if (!authToken) return false;
  try {
    const parts = authToken.split('.');
    if (parts.length !== 3) return false;
    // Reject alg=none and any non-HS256 algorithm (security: frontend validates token structure)
    const headerB64 = parts[0].replace(/-/g, '+').replace(/_/g, '/');
    const headerJson = headerB64 + '='.repeat((4 - headerB64.length % 4) % 4);
    const header = JSON.parse(atob(headerJson)) as { alg?: string };
    if (header.alg !== 'HS256') return false;
    const payloadB64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const payloadJson = payloadB64 + '='.repeat((4 - payloadB64.length % 4) % 4);
    const payload = JSON.parse(atob(payloadJson)) as { exp?: number };
    if (payload.exp === undefined) return false;
    const nowSeconds = Math.floor(Date.now() / 1000);
    return payload.exp > nowSeconds;
  } catch {
    return false;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T | undefined> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    const headers = new Headers(init?.headers);
    const method = (init?.method ?? '').toUpperCase();
    if (method !== 'GET' && method !== 'HEAD') {
      headers.set('Content-Type', 'application/json');
    }
    if (authToken) {
      headers.set('Authorization', `Bearer ${authToken}`);
    }
    const res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers,
      signal: controller.signal,
    });
    if (!res.ok) {
      let message: string;
      try {
        const text = await res.text();
        try {
          const err = JSON.parse(text) as { code?: string; message?: string };
          message = err.message || `HTTP ${res.status}`;
        } catch {
          message = text || `HTTP ${res.status}`;
        }
      } catch {
        message = `Network error (connection failed)`;
      }
      const err = new ApiError(message, res.status);
      if (res.status === 401) {
        setAuthToken(null, null);
        // Prevent duplicate redirects if multiple requests fail simultaneously.
        // Use a closure-guarded flag so a second 401 during the navigation window
        // does not trigger another redirect.
        if (!window.__summaRedirecting) {
          window.__summaRedirecting = true;
          const cleanup = () => { window.__summaRedirecting = false; };
          if (navigateRef) {
            navigateRef('/login', { replace: true });
          } else {
            window.location.href = '/login';
          }
          // Extend the guard to cover the full navigation cycle, not just 1s
          setTimeout(cleanup, 3000);
          window.addEventListener('focus', cleanup, { once: true });
          // Also clear on auth-change event so re-login resets the guard immediately
          const onAuthChange = () => cleanup();
          window.addEventListener('summa-auth-change', onAuthChange);
          // Clean up listener after timeout to avoid memory leak
          setTimeout(() => window.removeEventListener('summa-auth-change', onAuthChange), 5000);
        }
        throw err;
      }
    }
     if (res.status === 204) {
      return undefined;
    }
    let json: T;
    try {
      json = await res.json();
    } catch (parseErr) {
      throw new ApiError(`Failed to parse JSON response: ${parseErr instanceof Error ? parseErr.message : String(parseErr)}`, res.status);
    }
    return json;
  } catch (err) {
    // Preserve the original HTTP status for server-origin errors (401, 429, 500, etc.).
    // Only wrap true network/abort failures with status 0 so callers can still
    // distinguish between a network error and an actual HTTP error response.
    const status = err instanceof ApiError ? err.status : 0;
    throw new ApiError(err instanceof Error ? err.message : String(err), status);
  } finally {
    clearTimeout(timeoutId);
  }
}

/**
 * Tries parallel fetches; on any failure, falls back to individual settle calls
 * so that successfully-loaded data is still displayed. Returns an error string
 * when at least one call failed, or null when all succeeded.
 * The returned data array preserves index alignment with the fetch order,
 * with null entries for any individual calls that failed.
 */
export async function loadWithFallback<T>(
  fetchAll: () => Promise<(T | undefined)[]>,
  fetchIndividual: () => Promise<(T | null | undefined)[]>,
): Promise<{ data: (T | null | undefined)[]; error: string | null }> {
  try {
    const data = await fetchAll();
    return { data, error: null };
  } catch (e) {
    const results = await fetchIndividual();
    const hasError = results.some(r => r === null);
    const error = hasError
      ? 'Some data could not be loaded: ' + (e instanceof Error ? e.message : (typeof e === 'string' ? e : ''))
      : null;
    return { data: results, error };
  }
}

export interface Human {
  id: string;
  name: string;
  email: string;
  rbac: RbacRole;
  active: boolean;
  kind: 'human';
  createdAt?: number;
  auth?: string;
  timezone?: string;
  workingHours?: string;
  updatedAt?: number;
  deactivatedAt?: number;
  deputyMemberId?: string;
}

export type AgentClass = 'persistent' | 'ephemeral' | 'ephemeral-subagent';

export interface Agent {
  id: string;
  name: string;
  ownerHumanId: string;
  class: AgentClass;
  status: AgentStatus;
  kind: 'agent';
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

export type Member = Human | Agent;

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
  createdAt?: number;
  updatedAt?: number;
  escalation?: string;
  expiryBehavior?: string;
  respondedAt?: number;
  responses?: string;
}

export type AskStatus = 'pending' | 'answered' | 'expired' | 'withdrawn';
export type AskKind = 'approval' | 'question' | 'assignment' | 'spawn_request' | 'promotion';
export type AskTier = 'critical' | 'standard' | 'bulk';

export interface SpawnRequest {
  id: string;
  requesterId: string;
  templateId?: string;
  customRole?: string;
  class: AgentClass;
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
  gateTarget?: string;
  createdAt?: number;
}

export type SpawnStatus = 'requested' | 'approved' | 'halted' | 'archived';
export interface SpawnStats {
  requested: number;
  approved: number;
  archived: number;
  halted?: number;
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
  dependsOn?: string;
  createdAt?: number;
  updatedAt?: number;
}

export type InitiativeStatus = 'proposed' | 'active' | 'paused' | 'closed';

export interface Run {
  id: string;
  agentId: string;
  workspaceId?: string;
  status: RunStatus;
  result?: string;
  costTokens: number;
  costUsd: number;
  createdAt?: number;
  updatedAt?: number;
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

export interface RunListParams {
  [key: string]: string | number | boolean | undefined;
  agentId?: string
  workspaceId?: string
  status?: RunStatus
  limit?: number
}

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
  createdAt?: number;
}

export interface DnaProposal {
  id: string;
  domainId?: string;
  kind: DnaProposalKind;
  payload: string;
  revision: number;
  proposedBy: string;
  reviewBy?: string;
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

export interface HealthStatus {
  status: 'UP' | 'DEGRADED';
  service: string;
  mode: string;
  checks: { database: string; git_store: string };
}

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
  store?: string;
  reviewSlaDays?: number;
  residency?: string;
  namedReaders?: string;
  sod?: string;
  createdAt?: number;
  updatedAt?: number;
}

export type DomainAccess = 'public' | 'domain' | 'named';
export type RbacRole = 'admin' | 'owner' | 'member' | 'viewer';
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
  updatedAt?: number;
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
  class: AgentClass;
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

export interface DataHold {
  id: string;
  kind: string;
  subjectId: string;
  reasonMd: string;
  createdBy: string;
  createdAt: number;
  releasedAt?: number;
}

export function buildQuery(params?: Record<string, string | number | boolean | undefined>): string {
  const entries = Object.entries(params ?? {}).filter(([, v]) => v !== undefined && v !== null && v !== '');
  if (entries.length === 0) return '';
  const qs = new URLSearchParams(entries.map(([k, v]) => [k, String(v)])).toString();
  return qs ? `?${qs}` : '';
}

export const api = {
  agents: {
    list: (params?: { status?: AgentStatus; ownerId?: string }) =>
      request<Agent[]>(`/agents${buildQuery(params)}`),
    get: (id: string) => request<Agent>(`/agents/${id}`),
    suspend: (id: string) =>
      request<Agent>(`/agents/${id}/suspend`, { method: 'POST' }),
    resume: (id: string) =>
      request<Agent>(`/agents/${id}/resume`, { method: 'POST' }),
    retire: (id: string) =>
      request<Agent>(`/agents/${id}/retire`, { method: 'POST' }),
    archive: (id: string) =>
      request<Agent>(`/agents/${id}/archive`, { method: 'POST' }),
    deny: (id: string) =>
      request<Agent>(`/agents/${id}/deny`, { method: 'POST' }),
    promote: (id: string, placement: RbacRole) =>
      request<Record<string, unknown>>(`/agents/${id}/promote`, {
        method: 'POST',
        body: JSON.stringify({ placement }),
      }),
    lineage: (id: string) => request<string[]>(`/agents/${id}/lineage`),
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
    updateDomainAccess: (id: string, access: DomainAccess) =>
      request<DnaDomain>(`/dna/domains/${id}/access`, {
        method: 'PATCH',
        body: JSON.stringify({ access }),
      }),
    mergeDomain: (id: string, sourceId: string, access?: DomainAccess, namedReaders?: string) =>
      request<DnaDomain>(`/dna/domains/${id}/merge`, {
        method: 'POST',
        body: JSON.stringify({ sourceId, ...(access ? { access } : {}), ...(namedReaders ? { namedReaders } : {}) }),
      }),
    goals: (params?: { domainId?: string; inject?: InjectMode }) =>
      request<DnaGoal[]>(`/dna/goals${buildQuery(params)}`),
    updateGoalStatus: (id: string, status: DnaGoalStatus) =>
      request<DnaGoal>(`/dna/goals/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      }),
    updateGoalWindow: async (id: string, effectiveFrom?: number, effectiveTo?: number) => {
        if (effectiveFrom === undefined && effectiveTo === undefined) {
            throw new ApiError('At least one of effectiveFrom or effectiveTo must be provided', 400);
        }
        const body: Record<string, number> = {};
        if (effectiveFrom !== undefined) body.effectiveFrom = effectiveFrom;
        if (effectiveTo !== undefined) body.effectiveTo = effectiveTo;
        return request<DnaGoal>(`/dna/goals/${id}/window`, {
          method: 'PATCH',
          body: Object.keys(body).length ? JSON.stringify(body) : undefined,
        });
    },
    glossary: (params?: { domainId?: string; scope?: string }) =>
      request<DnaGlossary[]>(`/dna/glossary${buildQuery(params)}`),
    createGlossary: (body: Record<string, string>) =>
      request<DnaGlossary>('/dna/glossary', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    updateGlossary: (id: string, body: Record<string, string>) =>
      request<DnaGlossary>(`/dna/glossary/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(body),
      }),
    retireGlossary: (id: string) =>
      request<DnaGlossary>(`/dna/glossary/${id}/retire`, {
        method: 'POST',
      }),
    proposals: (status?: DnaProposalStatus) =>
      request<DnaProposal[]>(`/dna/proposals${buildQuery(status ? { status } : undefined)}`),
    createProposal: (body: Record<string, string>) =>
      request<DnaProposal>('/dna/proposals', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    publishProposal: (id: string) =>
      request<DnaProposal>(`/dna/proposals/${id}/review`, {
        method: 'POST',
        body: JSON.stringify({ action: 'publish' }),
      }),
    reviewProposal: (id: string, action: 'publish' | 'reject') =>
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
      request<DnaProposal[]>(`/dna/proposals/review-queue${buildQuery(domainId ? { domainId } : undefined)}`),
  },
  asks: {
    list: () =>
      request<Ask[]>('/asks'),
    listByStatus: (status: AskStatus) =>
      request<Ask[]>(`/asks${buildQuery({ status })}`),
    create: (body: Record<string, string>) =>
      request<Ask>('/asks', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
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
    humans: (active?: boolean) =>
      request<Human[]>(`/org/humans${buildQuery(active !== undefined ? { active: String(active) } : undefined)}`),
    updateRbac: (id: string, rbac: RbacRole) =>
      request<Human>(`/org/humans/${id}/rbac`, {
        method: 'PUT',
        body: JSON.stringify({ rbac }),
      }),
    demote: (id: string, rbac: RbacRole) =>
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
      request<Record<string, unknown>>(`/org/humans/${id}/erasure`, {
        method: 'POST',
      }),
    members: () => request<{ members: Member[]; total: number }>('/org/members'),
    lineage: async (memberId: string) => {
      const res = await request<{ memberId: string; lineage: string[] }>(`/org/lineage${buildQuery({ memberId })}`);
      return res?.lineage ?? [];
    },
  },
  spawn: {
    list: (status?: SpawnStatus, requesterId?: string) =>
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
    list: (status?: InitiativeStatus) =>
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
    list: (params?: RunListParams) =>
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
    resume: (id: string) =>
      request<Run>(`/runs/${id}/resume`, { method: 'POST' }),
    stats: () => request<Record<string, number>>('/runs/stats'),
  },
  auth: {
    login: (email: string, password: string) =>
      request<{ token: string; userId: string; rbac: RbacRole; name: string }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
    changePassword: (currentPassword: string, newPassword: string) =>
      request<{ message: string }>('/auth/change-password', {
        method: 'PUT',
        body: JSON.stringify({ currentPassword, newPassword }),
      }),
  },
  health: () => request<HealthStatus>('/health'),
  boardTasks: {
    list: (params?: { status?: BoardTaskStatus; assigneeId?: string; initiativeId?: string }) =>
      request<BoardTask[]>(`/board-tasks${buildQuery(params)}`),
    create: (body: Record<string, string>) =>
      request<BoardTask>('/board-tasks', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    assign: (id: string, memberId: string) =>
      request<BoardTask>(`/board-tasks/${id}/assign`, {
        method: 'POST',
        body: JSON.stringify({ assigneeMemberId: memberId }),
      }),
    complete: (id: string) =>
      request<BoardTask>(`/board-tasks/${id}/complete`, {
        method: 'POST',
      }),
    unassign: (id: string) =>
      request<BoardTask>(`/board-tasks/${id}/unassign`, {
        method: 'POST',
      }),
  },
  triggers: {
    list: (agentId?: string) =>
      request<Trigger[]>(`/triggers${buildQuery(agentId ? { agentId } : undefined)}`),
    create: (body: Record<string, string>) =>
      request<Trigger>('/triggers', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    pause: (id: string) =>
      request<Trigger>(`/triggers/${id}/pause`, {
        method: 'POST',
      }),
    resume: (id: string) =>
      request<Trigger>(`/triggers/${id}/resume`, {
        method: 'POST',
      }),
    archive: (id: string) =>
      request<Trigger>(`/triggers/${id}/archive`, {
        method: 'POST',
      }),
    stats: () => request<Record<string, number>>('/triggers/stats'),
  },
  workspaces: {
    list: () => request<Workspace[]>('/workspaces'),
    create: (body: Record<string, string>) =>
      request<Workspace>('/workspaces', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    rebind: (id: string, targetNodeId: string) =>
      request<Workspace>(`/workspaces/${id}/rebind`, {
        method: 'POST',
        body: JSON.stringify({ targetNodeId }),
      }),
    archive: (id: string) =>
      request<Record<string, unknown>>(`/workspaces/${id}/archive`, {
        method: 'POST',
      }),
  },
  nodes: {
    list: () => request<Node[]>('/nodes'),
    enroll: (body: Record<string, string>) =>
      request<Record<string, unknown>>('/nodes/enroll', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    revoke: (id: string) =>
      request<Record<string, unknown>>(`/nodes/${id}/revoke`, {
        method: 'POST',
      }),
    update: (id: string, body: Record<string, string>) =>
      request<Node>(`/nodes/${id}`, {
        method: 'PUT',
        body: JSON.stringify(body),
      }),
  },
  governance: {
    policies: () => request<Record<string, unknown>>('/governance/policies'),
    quotas: () => request<Record<string, unknown>>('/governance/quotas'),
    spend: () => request<SpendSnapshot>('/governance/spend'),
    updatePolicies: (body: Record<string, unknown>) =>
      request<Record<string, unknown>>('/governance/policies', {
        method: 'PUT',
        body: JSON.stringify(body),
      }),
    updateQuotas: (body: Record<string, unknown>) =>
      request<Record<string, unknown>>('/governance/quotas', {
        method: 'PUT',
        body: JSON.stringify(body),
      }),
    createHold: (body: Record<string, string>) =>
      request<Record<string, unknown>>('/governance/holds', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    listHolds: () => request<DataHold[]>('/governance/holds'),
    releaseHold: (id: string) =>
      request<Record<string, unknown>>(`/governance/holds/${id}/release`, {
        method: 'POST',
      }),
    ackSpendOverrun: (id: string) =>
      request<Record<string, unknown>>(`/governance/spend/overruns/${id}/ack`, {
        method: 'POST',
      }),
  },
  roleTemplates: {
    list: () => request<RoleTemplate[]>('/role-templates'),
    create: (body: Record<string, string>) =>
      request<RoleTemplate>('/role-templates', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    publish: (id: string) =>
      request<Record<string, unknown>>(`/role-templates/${id}/publish`, {
        method: 'POST',
      }),
    retire: (id: string) =>
      request<Record<string, unknown>>(`/role-templates/${id}/retire`, {
        method: 'POST',
      }),
  },
  memory: {
    list: (params?: { memberId?: string; workspaceId?: string; tainted?: string }) =>
      request<MemoryItem[]>(`/memory${buildQuery(params)}`),
    create: (body: Record<string, string>) =>
      request<MemoryItem>('/memory', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    review: (id: string) =>
      request<MemoryItem>(`/memory/${id}/review`, {
        method: 'POST',
      }),
  },
  groups: {
    list: () => request<Group[]>('/org/groups'),
    create: (body: Record<string, string>) =>
      request<Group>('/org/groups', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    archive: (id: string) =>
      request<Group>(`/org/groups/${id}/archive`, {
        method: 'POST',
      }),
    setLeader: (id: string, memberId: string) =>
      request<Record<string, unknown>>(`/org/groups/${id}/leader`, {
        method: 'PUT',
        body: JSON.stringify({ leaderMemberId: memberId }),
      }),
  },
};
