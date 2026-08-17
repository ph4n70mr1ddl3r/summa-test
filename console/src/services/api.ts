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

export const api = {
  agents: {
    list: (params?: { status?: string; ownerId?: string }) =>
      request<unknown[]>(withQuery('/agents', params)),
    get: (id: string) => request(`/agents/${id}`),
    lineage: (id: string) => request<string[]>(`/agents/${id}/lineage`),
    suspend: (id: string, actor: string) =>
      request(`/agents/${id}/suspend`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    resume: (id: string, actor: string) =>
      request(`/agents/${id}/resume`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    retire: (id: string, actor: string) =>
      request(`/agents/${id}/retire`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    archive: (id: string, actor: string) =>
      request(`/agents/${id}/archive`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  dna: {
    cards: (domainId?: string) =>
      request<unknown[]>(withQuery('/dna/cards', domainId ? { domainId } : undefined)),
    rules: (domainId?: string) =>
      request<unknown[]>(withQuery('/dna/rules', domainId ? { domainId } : undefined)),
    decisions: (domainId?: string) =>
      request<unknown[]>(withQuery('/dna/decisions', domainId ? { domainId } : undefined)),
    search: (query: string) =>
      request<unknown[]>(withQuery('/dna/search', { q: query })),
    domains: () => request<unknown[]>('/dna/domains'),
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
    list: (status?: string) =>
      request<unknown[]>(withQuery('/asks', status ? { status } : undefined)),
    respond: (id: string, response: string, actor: string) =>
      request(`/asks/${id}/respond`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ response }),
      }),
    withdraw: (id: string, actor: string) =>
      request(`/asks/${id}/withdraw`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    expire: (id: string) =>
      request(`/asks/${id}/expire`, {
        method: 'POST',
      }),
  },
  org: {
    health: () => request<{ status: string; service: string }>('/auth/health'),
    bootstrap: () => request('/org/bootstrap', { method: 'POST' }),
    humans: (active?: boolean) =>
      request<unknown[]>(withQuery('/org/humans', active !== undefined ? { active: String(active) } : undefined)),
    updateRbac: (id: string, rbac: string, actor: string) =>
      request(`/org/humans/${id}/rbac`, {
        method: 'PUT',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ rbac }),
      }),
    setDeputy: (id: string, deputyId: string, actor: string) =>
      request(`/org/humans/${id}/deputy`, {
        method: 'PUT',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ deputyMemberId: deputyId }),
      }),
    offboard: (id: string, actor: string) =>
      request(`/org/humans/${id}/offboard`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    erasure: (id: string, actor: string) =>
      request(`/org/humans/${id}/erasure`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    members: () => request('/org/members'),
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
      request<unknown[]>(withQuery('/spawn', {
        ...(status ? { status } : {}),
        ...(requesterId ? { requesterId } : {}),
      })),
    create: (body: Record<string, string>, actor: string) =>
      request('/spawn', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    approve: (id: string, actor: string) =>
      request(`/spawn/${id}/approve`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    deny: (id: string, actor: string) =>
      request(`/spawn/${id}/deny`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    stats: () => request('/spawn/stats'),
  },
  initiatives: {
    list: (status?: string) =>
      request<unknown[]>(withQuery('/initiatives', status ? { status } : undefined)),
    create: (body: Record<string, string>, actor: string) =>
      request('/initiatives', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    activate: (id: string, actor: string) =>
      request(`/initiatives/${id}/activate`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    pause: (id: string, actor: string) =>
      request(`/initiatives/${id}/pause`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    resume: (id: string, actor: string) =>
      request(`/initiatives/${id}/resume`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
    close: (id: string, actor: string) =>
      request(`/initiatives/${id}/close`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
      }),
  },
  runs: {
    list: (params?: { agentId?: string; workspaceId?: string; status?: string; limit?: number }) =>
      request<unknown[]>(withQuery('/runs', params as Record<string, string>)),
    create: (body: Record<string, string>, actor: string) =>
      request('/runs', {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify(body),
      }),
    start: (id: string) =>
      request(`/runs/${id}/start`, { method: 'POST' }),
    complete: (id: string, body: Record<string, unknown>) =>
      request(`/runs/${id}/complete`, {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    fail: (id: string, errorMessage: string) =>
      request(`/runs/${id}/fail`, {
        method: 'POST',
        body: JSON.stringify({ errorMessage }),
      }),
    cancel: (id: string) =>
      request(`/runs/${id}/cancel`, { method: 'POST' }),
    stats: () => request('/runs/stats'),
  },
  auth: {
    login: (email: string, password: string) =>
      request('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
  },
  health: () => request<{ status: string }>('/health'),
};
