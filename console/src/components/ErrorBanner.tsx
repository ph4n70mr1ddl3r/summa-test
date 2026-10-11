interface ErrorBannerProps {
  message: string
  onRetry?: () => void
  onDismiss?: () => void
}

export function ErrorBanner({ message, onRetry, onDismiss }: ErrorBannerProps) {
  return (
    <div className="bg-red-900/30 border border-red-700 rounded-lg p-4 text-red-400" role="alert" aria-live="assertive">
      Failed to load: {message}
      <div className="flex items-center gap-3 mt-2">
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="px-3 py-1 bg-red-700 hover:bg-red-600 rounded text-sm text-red-100"
            aria-label="Retry loading"
          >
            Retry
          </button>
        )}
        {onDismiss && (
          <button
            type="button"
            onClick={onDismiss}
            className="ml-auto text-red-300 hover:text-white text-sm"
            aria-label="Dismiss error"
          >
            ×
          </button>
        )}
      </div>
    </div>
  )
}
