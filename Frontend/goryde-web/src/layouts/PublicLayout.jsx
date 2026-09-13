import { Link, Outlet } from 'react-router-dom'

function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <Link className="text-xl font-bold tracking-tight text-sky-700" to="/">
            GoRyde
          </Link>
          <div className="flex items-center gap-4 text-sm font-medium">
            <Link className="text-slate-600 hover:text-sky-700" to="/login">
              Login
            </Link>
            <Link className="rounded-lg bg-sky-600 px-4 py-2 text-white hover:bg-sky-700" to="/register">
              Register
            </Link>
          </div>
        </nav>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default PublicLayout