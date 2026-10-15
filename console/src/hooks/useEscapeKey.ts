import { useEffect, useRef } from 'react'

/**
 * Attaches a keydown listener to window and cleans up on unmount.
 * Use this instead of duplicating the addEventListener/removeEventListener pattern.
 * The callback is stored in a ref so the listener is only attached once on mount.
 */
export function useEscapeKey(callback: (e: KeyboardEvent) => void) {
  const callbackRef = useRef(callback)
  useEffect(() => { callbackRef.current = callback })

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') callbackRef.current(e)
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [])
}
