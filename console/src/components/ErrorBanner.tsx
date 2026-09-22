import { escapeHtml } from '../utils/escapeHtml'

interface ErrorBannerProps {
  message: string
  onRetry?: () => void
}

export function ErrorBanner({ message, onRetry }: ErrorBannerProps) {
  return (
    <div className="bg-red-900/30 border border-red-700 rounded-lg p-4 text-red-400" role="alert" aria-live="assertive">
      Failed to load: {escapeHtml(message)}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="ml-4 px-3 py-1 bg-red-700 hover:bg-red-600 rounded text-sm text-red-100"
          aria-label="Retry loading"
        >
          Retry
        </button>
      )}
    </div>
  )
}
