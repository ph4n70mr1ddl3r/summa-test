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

/**
 * A page load in flight.
 *
 * The handle is two things at once: the effect's cleanup (call it to abandon
 * an in-flight load) and the load's promise (await `handle.promise` to learn
 * how the load ended). A loader that returned only the cleanup made
 * `await loadX()` a no-op — the await resolved on the function object, so the
 * catch around a post-action reload could never fire and a failed reload went
 * unreported while the page silently kept a stale list.
 */
export interface CancellableLoad {
  (): void
  promise: Promise<void>
}

/**
 * Wrap a load's promise and its cancellation in the handle a page returns.
 *
 * @param promise rejects when the load failed; the page records the failure in
 *                its own error state as well
 * @param cancel  abandons the in-flight load (the effect's cleanup)
 */
export function cancellableLoad(promise: Promise<void>, cancel: () => void): CancellableLoad {
  const handle = (() => cancel()) as CancellableLoad
  handle.promise = promise
  return handle
}

/**
 * Start a page load from an effect.
 *
 * The failure is already recorded in the page's own error state, so the
 * rejection is swallowed here — an unhandled rejection must never escape the
 * effect — while the promise stays available to a caller that wants to know
 * how the load ended.
 */
export function startLoad(load: () => CancellableLoad): () => void {
  const handle = load()
  handle.promise.catch(() => undefined)
  return handle
}
