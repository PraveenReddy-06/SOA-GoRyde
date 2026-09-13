import { useState } from 'react'
import toast from 'react-hot-toast'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getHomePath } from '../routes/RouteGuards'
import { useAuth } from '../context/AuthContext'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  function validate() {
    const nextErrors = {}
    if (!form.email.trim()) nextErrors.email = 'Email is required.'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) nextErrors.email = 'Enter a valid email address.'
    if (!form.password) nextErrors.password = 'Password is required.'
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setIsSubmitting(true)
    try {
      const authenticatedUser = await login(form)
      toast.success('Welcome back to GoRyde.')
      navigate(location.state?.from?.pathname || getHomePath(authenticatedUser), { replace: true })
    } catch (error) {
      toast.error(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthForm title="Welcome back" description="Sign in to continue to GoRyde." onSubmit={handleSubmit} submitLabel="Log in" isSubmitting={isSubmitting}>
      <Field label="Email" name="email" type="email" value={form.email} onChange={updateField} error={errors.email} autoComplete="email" />
      <Field label="Password" name="password" type="password" value={form.password} onChange={updateField} error={errors.password} autoComplete="current-password" />
      <p className="text-center text-sm text-slate-600">New to GoRyde? <Link className="font-semibold text-sky-700 hover:text-sky-800" to="/register">Create an account</Link></p>
    </AuthForm>
  )
}

export function AuthForm({ title, description, children, onSubmit, submitLabel, isSubmitting }) {
  return (
    <section className="mx-auto max-w-md px-6 py-16">
      <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-3xl font-bold tracking-tight text-slate-950">{title}</h1>
        <p className="mt-2 text-slate-600">{description}</p>
        <form className="mt-8 space-y-5" onSubmit={onSubmit} noValidate>
          {children}
          <button className="w-full rounded-lg bg-sky-600 px-4 py-3 font-semibold text-white hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Working...' : submitLabel}
          </button>
        </form>
      </div>
    </section>
  )
}

export function Field({ label, name, error, ...props }) {
  return (
    <label className="block text-left text-sm font-medium text-slate-700" htmlFor={name}>
      {label}
      <input className={`mt-2 w-full rounded-lg border px-3 py-3 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 ${error ? 'border-red-400' : 'border-slate-300'}`} id={name} name={name} {...props} />
      {error && <span className="mt-1 block text-sm font-normal text-red-600">{error}</span>}
    </label>
  )
}

export default LoginPage