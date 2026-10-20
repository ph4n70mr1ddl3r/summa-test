import { describe, it, expect, vi } from 'vitest'
import { cancellableLoad, startLoad, hasAnyData } from './loadState'

describe('hasAnyData', () => {
  it('is false when every item is missing', () => {
    expect(hasAnyData([null, undefined])).toBe(false)
  })

  it('is true when at least one item landed', () => {
    expect(hasAnyData([null, { id: 'a' }])).toBe(true)
  })
})

describe('cancellableLoad', () => {
  it('exposes the promise so a caller can await the load', async () => {
    const promise = Promise.resolve()
    const handle = cancellableLoad(promise, () => {})

    await expect(handle.promise).resolves.toBeUndefined()
  })

  it('propagates the failure so a caller can surface it', async () => {
    const promise = Promise.reject(new Error('boom'))
    const handle = cancellableLoad(promise, () => {})

    await expect(handle.promise).rejects.toThrow('boom')
  })

  it('cancels the in-flight load when the handle is called', () => {
    const cancel = vi.fn()
    const handle = cancellableLoad(Promise.resolve(), cancel)

    handle()

    expect(cancel).toHaveBeenCalledTimes(1)
  })
})

describe('startLoad', () => {
  it('swallows the rejection so no unhandled rejection escapes the effect', async () => {
    const load = vi.fn(() => cancellableLoad(Promise.reject(new Error('boom')), () => {}))

    const cleanup = startLoad(load)

    expect(load).toHaveBeenCalledTimes(1)
    expect(typeof cleanup).toBe('function')
    // Give the rejected promise a tick to settle — an unhandled rejection would
    // fail the test run.
    await new Promise((resolve) => setTimeout(resolve, 0))
  })

  it('returns the load handle as the effect cleanup', () => {
    const cancel = vi.fn()
    const handle = cancellableLoad(Promise.resolve(), cancel)
    const load = vi.fn(() => handle)

    const cleanup = startLoad(load)

    cleanup()
    expect(cancel).toHaveBeenCalledTimes(1)
  })
})
