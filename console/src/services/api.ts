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
    publishProposal: (id: string, reviewedBy: string, actor: string) =>
      request(`/dna/proposals/${id}/review/publish`, {
        method: 'POST',
        headers: { 'X-Actor': actor },
        body: JSON.stringify({ reviewedBy }),
      }),
  },
  asks: {
    list: (status?: string) =>
      request<unknown[]>(withQuery('/asks', status ? { status } : undefined)),
  },
  org: {
    health: () => request<{ status: string; service: string }>('/auth/health'),
    bootstrap: () => request('/org/bootstrap', { method: 'POST' }),
  },
  health: () => request<{ status: string }>('/health'),
};
