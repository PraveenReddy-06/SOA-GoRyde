import { useState } from 'react'

const initialForm = {
  pickupLocation: '',
  dropLocation: '',
  pickupLatitude: '',
  pickupLongitude: '',
  dropLatitude: '',
  dropLongitude: '',
}

function BookRide({ onSubmit, isSubmitting }) {
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  function validate() {
    const nextErrors = {}
    const coordinateFields = ['pickupLatitude', 'pickupLongitude', 'dropLatitude', 'dropLongitude']

    if (!form.pickupLocation.trim()) nextErrors.pickupLocation = 'Pickup location is required.'
    if (!form.dropLocation.trim()) nextErrors.dropLocation = 'Drop location is required.'

    coordinateFields.forEach((field) => {
      if (form[field] === '') nextErrors[field] = 'This coordinate is required.'
      else if (!Number.isFinite(Number(form[field]))) nextErrors[field] = 'Enter a valid number.'
    })

    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    onSubmit({
      pickupLocation: form.pickupLocation.trim(),
      dropLocation: form.dropLocation.trim(),
      pickupLatitude: Number(form.pickupLatitude),
      pickupLongitude: Number(form.pickupLongitude),
      dropLatitude: Number(form.dropLatitude),
      dropLongitude: Number(form.dropLongitude),
    })
  }

  return (
    <form className="space-y-5" onSubmit={handleSubmit} noValidate>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Pickup location" name="pickupLocation" value={form.pickupLocation} onChange={updateField} error={errors.pickupLocation} placeholder="MG Road, Vijayawada" />
        <Field label="Drop location" name="dropLocation" value={form.dropLocation} onChange={updateField} error={errors.dropLocation} placeholder="Benz Circle, Vijayawada" />
      </div>
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <Field label="Pickup latitude" name="pickupLatitude" type="number" step="any" value={form.pickupLatitude} onChange={updateField} error={errors.pickupLatitude} />
        <Field label="Pickup longitude" name="pickupLongitude" type="number" step="any" value={form.pickupLongitude} onChange={updateField} error={errors.pickupLongitude} />
        <Field label="Drop latitude" name="dropLatitude" type="number" step="any" value={form.dropLatitude} onChange={updateField} error={errors.dropLatitude} />
        <Field label="Drop longitude" name="dropLongitude" type="number" step="any" value={form.dropLongitude} onChange={updateField} error={errors.dropLongitude} />
      </div>
      <button className="rounded-lg bg-sky-600 px-5 py-3 font-semibold text-white hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60" disabled={isSubmitting} type="submit">
        {isSubmitting ? 'Booking ride...' : 'Book a ride'}
      </button>
    </form>
  )
}

function Field({ label, name, error, ...props }) {
  return (
    <label className="block text-left text-sm font-medium text-slate-700" htmlFor={name}>
      {label}
      <input className={`mt-2 w-full rounded-lg border px-3 py-3 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 ${error ? 'border-red-400' : 'border-slate-300'}`} id={name} name={name} {...props} />
      {error && <span className="mt-1 block text-sm font-normal text-red-600">{error}</span>}
    </label>
  )
}

export default BookRide