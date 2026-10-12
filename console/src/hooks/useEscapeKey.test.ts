import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, } from '@testing-library/react'
import { useEscapeKey } from './useEscapeKey'

describe('useEscapeKey', () => {
  let callback: ReturnType<typeof vi.fn>
  let keyDownEvent: KeyboardEvent

  beforeEach(() => {
    callback = vi.fn()
    keyDownEvent = new KeyboardEvent('keydown', { key: 'Escape' })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('attaches a window keydown listener', () => {
    const addEventListenerSpy = vi.spyOn(window, 'addEventListener')
    const { unmount } = renderHook(() => useEscapeKey(callback))
    expect(addEventListenerSpy).toHaveBeenCalledWith('keydown', expect.any(Function))
    unmount()
  })

  it('calls the callback when Escape is pressed', () => {
    renderHook(() => useEscapeKey(callback))
    window.dispatchEvent(keyDownEvent)
    expect(callback).toHaveBeenCalledTimes(1)
  })

  it('does not call the callback for other keys', () => {
    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' })
    renderHook(() => useEscapeKey(callback))
    window.dispatchEvent(enterEvent)
    expect(callback).not.toHaveBeenCalled()
  })

  it('removes the listener on unmount', () => {
    const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener')
    const { unmount } = renderHook(() => useEscapeKey(callback))
    unmount()
    expect(removeEventListenerSpy).toHaveBeenCalledWith('keydown', expect.any(Function))
  })

  it('passes the event to the callback', () => {
    renderHook(() => useEscapeKey(callback))
    window.dispatchEvent(keyDownEvent)
    expect(callback).toHaveBeenCalledWith(keyDownEvent)
  })
})
