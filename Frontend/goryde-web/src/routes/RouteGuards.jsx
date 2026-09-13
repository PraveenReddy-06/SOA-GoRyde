import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute() {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) return <RouteLoading />
  if (!user) return <Navigate replace state={{ from: location }} to="/login" />

  return <Outlet />
}

export function PublicOnlyRoute() {
  const { user, isLoading } = useAuth()

  if (isLoading) return <RouteLoading />
  if (user) return <Navigate replace to={getHomePath(user)} />

  return <Outlet />
}

export function getHomePath(user) {
  const role = user?.role?.toUpperCase()

  if (role === 'DRIVER') return '/driver'
  if (role === 'ADMIN') return '/admin'
  return '/passenger'
}

function RouteLoading() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6 text-slate-600">
      Checking your session...
    </div>
  )
}