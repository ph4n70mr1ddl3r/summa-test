import { useNavigate } from 'react-router-dom'

export default function NotFound() {
  const navigate = useNavigate()
  return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-gray-600">404</h1>
        <p className="text-xl text-gray-400 mt-4">Page not found</p>
        <button
          onClick={() => navigate('/')}
          className="mt-6 inline-block text-blue-400 hover:text-blue-300"
        >
          Return home
        </button>
      </div>
    </div>
  )
}
