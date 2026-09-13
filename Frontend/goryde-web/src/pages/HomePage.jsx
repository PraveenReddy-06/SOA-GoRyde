import { Link } from 'react-router-dom'

function HomePage() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-20">
      <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-sky-600">Welcome to GoRyde</p>
      <h1 className="max-w-2xl text-4xl font-bold tracking-tight text-slate-950 sm:text-6xl">Move through your day with confidence.</h1>
      <p className="mt-6 max-w-xl text-lg leading-8 text-slate-600">A simple foundation for reliable passenger and driver experiences.</p>
      <div className="mt-8 flex flex-wrap gap-4">
        <Link className="rounded-lg bg-sky-600 px-5 py-3 font-semibold text-white hover:bg-sky-700" to="/register">Get started</Link>
        <Link className="rounded-lg border border-slate-300 bg-white px-5 py-3 font-semibold text-slate-700 hover:border-sky-400" to="/login">Sign in</Link>
      </div>
    </section>
  )
}

export default HomePage