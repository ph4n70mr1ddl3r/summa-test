export function escapeHtml(str: string | null | undefined): string {
  if (str == null) return ''
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/`/g, '&#96;')
    // Normalize CRLF to LF before stripping control chars
    .replace(/\r\n?/g, '\n')
    // Strip remaining control characters except LF (0x0A) which was preserved by CRLF normalization
    // eslint-disable-next-line no-control-regex
    .replace(/[\x00-\x09\x0b\x0c\x0e-\x1f\x7f]/g, '')
}
