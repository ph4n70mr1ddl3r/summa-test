const TS_MS = 1000

export function formatDate(ts: number | undefined): string {
  if (ts == null) return '—'
  return new Date(ts * TS_MS).toLocaleString()
}

export function formatRelative(ts: number | undefined): string {
  if (ts == null) return '—'
  const diff = ts * TS_MS - Date.now()
  const abs = Math.abs(diff)
  const suffix = diff > 0 ? 'in' : ''
  if (abs < 60_000) return `${suffix} just now`
  if (abs < 3_600_000) return `${suffix} ${Math.floor(abs / 60_000)}m`
  if (abs < 86_400_000) return `${suffix} ${Math.floor(abs / 3_600_000)}h`
  return `${suffix} ${Math.floor(abs / 86_400_000)}d`
}
