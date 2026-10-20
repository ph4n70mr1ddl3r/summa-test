/**
 * True when at least one sub-request returned data.
 *
 * A page that loads several resources in parallel must keep the ones that
 * landed when a sibling fails — `loadWithFallback` already returns per-item
 * nulls, so this distinguishes a partial outage (render the data plus a
 * banner) from a total one (the page is just the error).
 */
export function hasAnyData(data: readonly (unknown | null | undefined)[]): boolean {
  return data.some((d) => d != null)
}
