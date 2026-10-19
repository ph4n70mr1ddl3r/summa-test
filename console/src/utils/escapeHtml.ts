/**
 * Sanitize a string for safe interpolation into React text nodes or attributes.
 * React escapes HTML on its own, so this helper only strips control characters
 * and normalizes CRLF → LF — it does NOT HTML-encode & < > " ' (doing so would
 * render literal entities when the value lands in a JSX text node).
 */
export function escapeHtml(str: string | null | undefined): string {
  if (str == null) return ''
  return str
    .replace(/\r\n?/g, '\n')
    // Strip control characters except LF (0x0A)
    // eslint-disable-next-line no-control-regex
    .replace(/[\x00-\x09\x0b\x0c\x0e-\x1f\x7f]/g, '')
}
