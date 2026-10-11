import { useEffect, useCallback } from 'react'

/**
 * Attaches a keydown listener to window and cleans up on unmount.
 * Use this instead of duplicating the addEventListener/removeEventListener pattern.
 */
export function useEscapeKey(callback: (e: KeyboardEvent) => void) {
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      callback(e)
    }
  }, [callback])

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [handleKeyDown])
}
