import { useState } from 'react'
import toast from 'react-hot-toast'
import { Link, useNavigate } from 'react-router-dom'
import { AuthForm, Field } from './LoginPage'
import { getHomePath } from '../routes/RouteGuards'
import { useAuth } from '../context/AuthContext'

function RegisterPage() {
  const { register, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'PASSENGER' })
  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  function validate() {
    const nextErrors = {}
    if (!form.name.trim()) nextErrors.name = 'Name is required.'
    if (!form.email.trim()) nextErrors.email = 'Email is required.'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) nextErrors.email = 'Enter a valid email address.'
    if (form.password.length < 6) nextErrors.password = 'Password must be at least 6 characters.'
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setIsSubmitting(true)
    try {
      const registeredUser = await register(form)
      if (registeredUser) {
        toast.success('Your GoRyde account is ready.')
        navigate(getHomePath(registeredUser), { replace: true })
      } else {
        await login({ email: form.email, password: form.password })
        toast.success('Your GoRyde account is ready.')
        navigate(form.role === 'DRIVER' ? '/driver' : '/passenger', { replace: true })
      }
    } catch (error) {
      toast.error(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthForm title="Create your account" description="Choose how you will use GoRyde." onSubmit={handleSubmit} submitLabel="Create account" isSubmitting={isSubmitting}>
      <Field label="Name" name="name" value={form.name} onChange={updateField} error={errors.name} autoComplete="name" />
      <Field label="Email" name="email" type="email" value={form.email} onChange={updateField} error={errors.email} autoComplete="email" />
      <Field label="Password" name="password" type="password" value={form.password} onChange={updateField} error={errors.password} autoComplete="new-password" />
      <label className="block text-left text-sm font-medium text-slate-700" htmlFor="role">
        Account type
        <select className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3 py-3 outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-100" id="role" name="role" value={form.role} onChange={updateField}>
          <option value="PASSENGER">Passenger</option>
          <option value="DRIVER">Driver</option>
        </select>
      </label>
      <p className="text-center text-sm text-slate-600">Already registered? <Link className="font-semibold text-sky-700 hover:text-sky-800" to="/login">Log in</Link></p>
    </AuthForm>
  )
}

export default RegisterPage