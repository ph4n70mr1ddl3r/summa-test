const API_BASE = import.meta.env.VITE_API_URL || '/api';

function withQuery(path: string, params?: Record<string, string>): string {
  if (!params || Object.keys(params).length === 0) return path;
  const qs = new URLSearchParams(params).toString();
  return `${path}?${qs}`;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
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
}

export interface Agent {
  id: string;
  name: string;
  ownerHumanId: string;
  class: string;
  status: string;
  templateId?: string;
  lineageDepth?: number;
  createdAt?: string;
}

export interface Ask {
  id: string;
  kind: string;
  from: string;
  to: string;
  payload: string;
  slaTier: string;
  status: string;
  deadline: string;
  quorumRequired?: number;
}

export interface SpawnRequest {
  id: string;
  requesterId: string;
  templateId?: string;
  customRole?: string;
  spawnClass: string;
  purpose: string;
  status: string;
  requestedByHumanId?: string;
  agentId?: string;
  approvedAt?: string;
}

export interface Initiative {
  id: string;
  title: string;
  sponsor: string;
  lead: string;
  status: string;
  deadline?: string;
  goalRef?: string;
}

export interface Run {
  id: string;
  agentId: string;
  workspaceId?: string;
  status: string;
  result?: string;
  costTokens?: number;
  costUsd?: number;
  createdAt?: string;
}

export interface DnaCard {
  id: string;
  domainId: string;
  title: string;
  definitionMd: string;
  status: string;
}

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
  access: string;
  status: string;
}

export interface GovernanceSetting {
  [key: string]: unknown;
}

export const api = {
  agents: {
    list: (params?: { status?: string; ownerId?: string }) =>
      request<Agent[]>(withQuery('/agents', params)),
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
      request<DnaCard[]>(withQuery('/dna/cards', domainId ? { domainId } : undefined)),
    rules: (domainId?: string) =>
      request<DnaRule[]>(withQuery('/dna/rules', domainId ? { domainId } : undefined)),
    decisions: (domainId?: string) =>
      request<DnaDecision[]>(withQuery('/dna/decisions', domainId ? { domainId } : undefined)),
    search: (query: string) =>
      request<unknown[]>(withQuery('/dna/search', { q: query })),
    domains: () => request<DnaDomain[]>('/dna/domains'),
    proposals: (status?: string) =>
      request<unknown[]>(withQuery('/dna/proposals', status ? { status } : undefined)),
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
      request<Ask[]>(withQuery('/asks', { status })),
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
      request<Human[]>(withQuery('/org/humans', active !== undefined ? { active: String(active) } : undefined)),
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
    members: () => request<{ members: unknown[]; total: number }>('/org/members'),
    lineage: (memberId: string) => request(withQuery('/org/lineage', { memberId })),
    audit: (limit?: number, objectType?: string, objectId?: string) =>
      request<unknown[]>(withQuery('/org/audit', {
        limit: String(limit ?? 100),
        ...(objectType ? { objectType } : {}),
        ...(objectId ? { objectId } : {}),
      })),
  },
  spawn: {
    list: (status?: string, requesterId?: string) =>
      request<SpawnRequest[]>(withQuery('/spawn', {
        ...(status ? { status } : {}),
        ...(requesterId ? { requesterId } : {}),
      })),
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
      request<Initiative[]>(withQuery('/initiatives', status ? { status } : undefined)),
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
      request<Run[]>(withQuery('/runs', params as Record<string, string>)),
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
  },
  health: () => request<{ status: string }>('/health'),
  boardTasks: {
    list: (params?: { status?: string; assigneeId?: string; initiativeId?: string }) =>
      request<unknown[]>(withQuery('/board-tasks', params as Record<string, string>)),
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
      request<unknown[]>(withQuery('/triggers', agentId ? { agentId } : undefined)),
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
    list: () => request<unknown[]>('/workspaces'),
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
    list: () => request<unknown[]>('/nodes'),
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
    spend: () => request<unknown>('/governance/spend'),
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
    list: () => request<unknown[]>('/role-templates'),
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
      request<unknown[]>(withQuery('/memory', params as Record<string, string>)),
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
      request<unknown[]>(withQuery('/auth/pats', { memberId })),
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
    list: () => request<unknown[]>('/org/groups'),
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
