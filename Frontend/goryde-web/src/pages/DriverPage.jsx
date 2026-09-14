import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import {
  acceptRide,
  completeRide,
  getCurrentDriver,
  getDriverRides,
  isForbidden,
  isUnauthorized,
  markArrived,
  markArriving,
  rejectRide,
  startRide,
  updateAvailability,
  updateLocation,
} from '../services/driverService'

const availabilityOptions = ['OFFLINE', 'AVAILABLE', 'BUSY']
const rideActions = {
  DRIVER_ASSIGNED: [{ label: 'Accept ride', action: 'accept' }, { label: 'Reject ride', action: 'reject' }],
  DRIVER_ACCEPTED: [{ label: 'Mark arriving', action: 'arriving' }],
  DRIVER_ARRIVING: [{ label: 'Mark arrived', action: 'arrived' }],
  DRIVER_ARRIVED: [{ label: 'Start ride', action: 'start' }],
  RIDE_STARTED: [{ label: 'Complete ride', action: 'complete' }],
}

function DriverPage() {
  const { user, logout } = useAuth()
  const [driver, setDriver] = useState(null)
  const [rides, setRides] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [location, setLocation] = useState({ latitude: '', longitude: '' })

  useEffect(() => {
    loadDashboard()
  }, [])

  async function loadDashboard(showLoading = true) {
    if (showLoading) setIsLoading(true)
    else setIsRefreshing(true)
    setError('')

    try {
      const [currentDriver, driverRides] = await Promise.all([getCurrentDriver(), getDriverRides()])
      setDriver(currentDriver)
      setRides(driverRides)
      setLocation({ latitude: currentDriver.latitude ?? '', longitude: currentDriver.longitude ?? '' })
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }

  async function handleAvailabilityChange(event) {
    setIsSaving(true)
    try {
      setDriver(await updateAvailability(driver.id, event.target.value))
      toast.success('Availability updated.')
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleLocationSubmit(event) {
    event.preventDefault()
    setIsSaving(true)
    try {
      const updatedDriver = await updateLocation(driver.id, location)
      setDriver(updatedDriver)
      setLocation({ latitude: updatedDriver.latitude ?? '', longitude: updatedDriver.longitude ?? '' })
      toast.success('Location updated.')
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleRideAction(action, rideId) {
    setIsSaving(true)
    try {
      const actionHandlers = { accept: acceptRide, reject: rejectRide, arriving: markArriving, arrived: markArrived }
      if (actionHandlers[action]) await actionHandlers[action](driver.id, rideId)
      if (action === 'start') await startRide(rideId)
      if (action === 'complete') await completeRide(rideId)
      toast.success('Ride updated.')
      await loadDashboard(false)
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setIsSaving(false)
    }
  }

  function handleRequestError(requestError) {
    let message = requestError.message
    if (isUnauthorized(requestError)) {
      message = 'Your session has expired. Please log in again.'
    } else if (isForbidden(requestError)) {
      message = 'You are not allowed to access this driver resource.'
    } else if (requestError.status === 404) {
      message = 'No driver profile exists for this account.'
    } else if (requestError.status === 503) {
      message = 'The assigned rides service is temporarily unavailable. Please try again shortly.'
    }
    setError(message)
    toast.error(message)
    if (isUnauthorized(requestError)) logout()
  }

  if (isLoading) return <DashboardMessage message="Loading your driver dashboard..." />
  if (!driver) return <DashboardMessage message={error || 'Your driver profile could not be loaded.'} isError />

  const currentRide = rides.find((ride) => ride.driverId === driver.id && !['RIDE_COMPLETED', 'PAYMENT_PENDING'].includes(ride.status))

  return (
    <div className="space-y-8">
      <section className="flex flex-col justify-between gap-4 rounded-2xl bg-slate-900 p-8 text-white sm:flex-row sm:items-center">
        <div>
          <p className="text-sm font-medium text-sky-300">Driver dashboard</p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight">Welcome, {driver.name || user?.name || user?.email}</h1>
          <p className="mt-2 text-slate-300">Manage your availability, location, and assigned rides.</p>
        </div>
        <div className="flex gap-3">
          <button className="rounded-lg border border-slate-600 px-4 py-2 text-sm font-semibold text-slate-200 hover:border-slate-400 disabled:opacity-60" disabled={isRefreshing} onClick={() => loadDashboard(false)} type="button">{isRefreshing ? 'Refreshing...' : 'Refresh'}</button>
          <button className="rounded-lg border border-slate-600 px-4 py-2 text-sm font-semibold text-slate-200 hover:border-slate-400" onClick={logout} type="button">Log out</button>
        </div>
      </section>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-700">{error}</div>}

      <section className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <h2 className="text-2xl font-bold tracking-tight text-slate-950">Driver profile</h2>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <ProfileField label="Name" value={driver.name} />
            <ProfileField label="Email" value={driver.email} />
            <ProfileField label="Phone" value={driver.phone} />
            <ProfileField label="Vehicle" value={driver.vehicleDetails} />
            <ProfileField label="Latitude" value={driver.latitude ?? 'Not set'} />
            <ProfileField label="Longitude" value={driver.longitude ?? 'Not set'} />
          </div>
          <label className="mt-6 block text-sm font-semibold text-slate-700">Availability
            <select className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 font-normal text-slate-900" disabled={isSaving} onChange={handleAvailabilityChange} value={driver.availability}>
              {availabilityOptions.map((option) => <option key={option} value={option}>{option}</option>)}
            </select>
          </label>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <h2 className="text-2xl font-bold tracking-tight text-slate-950">Current location</h2>
          <p className="mt-1 text-slate-600">Update the coordinates used for ride matching.</p>
          <form className="mt-6 space-y-4" onSubmit={handleLocationSubmit}>
            <CoordinateInput label="Latitude" name="latitude" value={location.latitude} onChange={setLocation} />
            <CoordinateInput label="Longitude" name="longitude" value={location.longitude} onChange={setLocation} />
            <button className="w-full rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-sky-700 disabled:opacity-60" disabled={isSaving} type="submit">{isSaving ? 'Saving...' : 'Update location'}</button>
          </form>
        </section>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div><h2 className="text-2xl font-bold tracking-tight text-slate-950">Assigned ride</h2><p className="mt-1 text-slate-600">Only rides returned by the backend appear here.</p></div>
          <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-700">{currentRide ? currentRide.status : 'No active ride'}</span>
        </div>
        <div className="mt-6">{currentRide ? <RideCard ride={currentRide} isSaving={isSaving} onAction={handleRideAction} /> : <p className="text-slate-600">No assigned ride was returned.</p>}</div>
      </section>
    </div>
  )
}

function ProfileField({ label, value }) {
  return <div><dt className="text-sm font-medium text-slate-500">{label}</dt><dd className="mt-1 font-semibold text-slate-950">{value || 'Not provided'}</dd></div>
}

function CoordinateInput({ label, name, value, onChange }) {
  return <label className="block text-sm font-semibold text-slate-700">{label}<input className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 font-normal text-slate-900" max={name === 'latitude' ? 90 : 180} min={name === 'latitude' ? -90 : -180} name={name} onChange={(event) => onChange((current) => ({ ...current, [name]: event.target.value }))} required step="any" type="number" value={value} /></label>
}

function RideCard({ ride, isSaving, onAction }) {
  const actions = rideActions[ride.status] ?? []
  return <article className="space-y-5"><div className="grid gap-4 sm:grid-cols-2"><ProfileField label="Ride ID" value={ride.id} /><ProfileField label="Passenger ID" value={ride.passengerId ?? 'Not returned'} /><ProfileField label="Pickup" value={ride.pickupLocation} /><ProfileField label="Drop" value={ride.dropLocation} /><ProfileField label="Distance" value={ride.distanceKm == null ? 'Not returned' : `${ride.distanceKm} km`} /><ProfileField label="Estimated duration" value={ride.estimatedDurationMinutes == null ? 'Not returned' : `${ride.estimatedDurationMinutes} minutes`} /><ProfileField label="Fare" value={ride.fare == null ? 'Not returned' : ride.fare} /><ProfileField label="Ride status" value={ride.status} /></div><div className="flex flex-wrap gap-3">{actions.map((rideAction) => <button className="rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-sky-700 disabled:opacity-60" disabled={isSaving} key={rideAction.action} onClick={() => onAction(rideAction.action, ride.id)} type="button">{isSaving ? 'Updating...' : rideAction.label}</button>)}</div></article>
}

function DashboardMessage({ message, isError = false }) {
  return <section className={`rounded-2xl border p-10 shadow-sm ${isError ? 'border-red-200 bg-red-50 text-red-700' : 'border-slate-200 bg-white text-slate-600'}`}><p>{message}</p></section>
}

export default DriverPage