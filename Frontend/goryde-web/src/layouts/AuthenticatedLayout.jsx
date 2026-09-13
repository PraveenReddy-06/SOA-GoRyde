import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function AuthenticatedLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <Link className="text-xl font-bold tracking-tight text-sky-700" to="/">
            GoRyde
          </Link>
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-500">{user?.name || user?.email}</span>
            <button className="text-sm font-medium text-slate-600 hover:text-sky-700" onClick={logout} type="button">Log out</button>
          </div>
        </nav>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-10">
        <Outlet />
      </main>
    </div>
  )
}

export default AuthenticatedLayout