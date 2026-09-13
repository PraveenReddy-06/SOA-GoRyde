import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import BookRide from '../components/BookRide'
import { useAuth } from '../context/AuthContext'
import { cancelRide, createRide, getMyRides, getRide, isUnauthorized } from '../services/rideService'

function PassengerPage() {
  const { user, logout } = useAuth()
  const [rides, setRides] = useState([])
  const [selectedRide, setSelectedRide] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isBooking, setIsBooking] = useState(false)
  const [isLoadingRide, setIsLoadingRide] = useState(false)
  const [cancellingRideId, setCancellingRideId] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    loadRides()
  }, [])

  async function loadRides() {
    setIsLoading(true)
    setError('')
    try {
      setRides(await getMyRides())
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setIsLoading(false)
    }
  }

  async function handleBookRide(rideDetails) {
    setIsBooking(true)
    try {
      const newRide = await createRide(rideDetails)
      setRides((currentRides) => [newRide, ...currentRides])
      setSelectedRide(newRide)
      toast.success('Your ride was booked successfully.')
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setIsBooking(false)
    }
  }

  async function handleViewRide(rideId) {
    setIsLoadingRide(true)
    try {
      setSelectedRide(await getRide(rideId))
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setIsLoadingRide(false)
    }
  }

  async function handleCancelRide(rideId) {
    setCancellingRideId(rideId)
    try {
      const cancelledRide = await cancelRide(rideId)
      setRides((currentRides) => currentRides.map((ride) => getRideId(ride) === rideId ? { ...ride, ...cancelledRide } : ride))
      setSelectedRide((currentRide) => currentRide && getRideId(currentRide) === rideId ? { ...currentRide, ...cancelledRide } : currentRide)
      toast.success('Your ride was cancelled.')
    } catch (requestError) {
      handleRequestError(requestError)
    } finally {
      setCancellingRideId(null)
    }
  }

  function handleRequestError(requestError) {
    setError(requestError.message)
    toast.error(requestError.message)
    if (isUnauthorized(requestError)) logout()
  }

  const recentRide = rides[0]

  return (
    <div className="space-y-8">
      <section className="flex flex-col justify-between gap-4 rounded-2xl bg-slate-900 p-8 text-white sm:flex-row sm:items-center">
        <div>
          <p className="text-sm font-medium text-sky-300">Passenger dashboard</p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight">Welcome, {user?.name || user?.email}</h1>
          <p className="mt-2 text-slate-300">Book a ride and keep track of your journeys.</p>
        </div>
        <button className="self-start rounded-lg border border-slate-600 px-4 py-2 text-sm font-semibold text-slate-200 hover:border-slate-400 sm:self-auto" onClick={logout} type="button">Log out</button>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="mb-6">
          <h2 className="text-2xl font-bold tracking-tight text-slate-950">Book a ride</h2>
          <p className="mt-1 text-slate-600">Enter your locations and coordinates to request a ride.</p>
        </div>
        <BookRide isSubmitting={isBooking} onSubmit={handleBookRide} />
      </section>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-700">{error}</div>}

      <section className="grid gap-6 lg:grid-cols-[1fr_1.4fr]">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-bold text-slate-950">Current or recent ride</h2>
          <div className="mt-5">
            {recentRide ? <RideSummary ride={recentRide} onView={handleViewRide} onCancel={handleCancelRide} cancellingRideId={cancellingRideId} /> : <p className="text-slate-600">No rides yet. Your latest ride will appear here.</p>}
          </div>
        </section>
        <RideDetails ride={selectedRide} isLoading={isLoadingRide} onCancel={handleCancelRide} cancellingRideId={cancellingRideId} />
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-2xl font-bold tracking-tight text-slate-950">My rides</h2>
            <p className="mt-1 text-slate-600">Your ride history from GoRyde.</p>
          </div>
          <button className="text-sm font-semibold text-sky-700 hover:text-sky-800" onClick={loadRides} type="button">Refresh</button>
        </div>
        <div className="mt-6">{isLoading ? <p className="text-slate-600">Loading your rides...</p> : rides.length ? <RideList rides={rides} onView={handleViewRide} onCancel={handleCancelRide} cancellingRideId={cancellingRideId} /> : <p className="text-slate-600">No rides found.</p>}</div>
      </section>
    </div>
  )
}

function RideList({ rides, ...props }) {
  return <div className="divide-y divide-slate-200">{rides.map((ride, index) => <RideSummary key={getRideId(ride) || index} ride={ride} {...props} />)}</div>
}

function RideSummary({ ride, onView, onCancel, cancellingRideId }) {
  const rideId = getRideId(ride)
  const status = getStatus(ride)

  return (
    <article className="space-y-4 py-5 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-slate-950">{ride.pickupLocation || ride.pickup || 'Pickup unavailable'}</p>
          <p className="mt-1 text-sm text-slate-600">to {ride.dropLocation || ride.drop || 'Drop unavailable'}</p>
        </div>
        <StatusBadge status={status} />
      </div>
      <div className="grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
        <Info label="Ride ID" value={rideId || 'Unavailable'} />
        <Info label="Driver" value={getDriverName(ride)} />
        <Info label="Fare" value={formatValue(ride.fare, 'Not assigned')} />
        <Info label="Created" value={formatDate(ride.createdAt || ride.createdTime)} />
      </div>
      <div className="flex flex-wrap gap-3">
        {rideId && <button className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:border-sky-400" onClick={() => onView(rideId)} type="button">View ride</button>}
        {canCancel(ride) && <button className="rounded-lg border border-red-200 px-3 py-2 text-sm font-semibold text-red-700 hover:bg-red-50 disabled:opacity-60" disabled={cancellingRideId === rideId} onClick={() => onCancel(rideId)} type="button">{cancellingRideId === rideId ? 'Cancelling...' : 'Cancel ride'}</button>}
      </div>
    </article>
  )
}

function RideDetails({ ride, isLoading, onCancel, cancellingRideId }) {
  if (isLoading) return <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><p className="text-slate-600">Loading ride details...</p></section>
  if (!ride) return <section className="rounded-2xl border border-dashed border-slate-300 bg-white p-6 shadow-sm"><p className="text-slate-600">Select a ride to view its full details.</p></section>

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div><h2 className="text-xl font-bold text-slate-950">Ride details</h2><p className="mt-1 text-sm text-slate-500">{getRideId(ride) || 'Ride ID unavailable'}</p></div>
        <StatusBadge status={getStatus(ride)} />
      </div>
      <dl className="mt-6 grid gap-5 sm:grid-cols-2">
        <Info label="Pickup" value={ride.pickupLocation || ride.pickup || 'Unavailable'} />
        <Info label="Drop" value={ride.dropLocation || ride.drop || 'Unavailable'} />
        <Info label="Driver" value={getDriverName(ride)} />
        <Info label="Fare" value={formatValue(ride.fare, 'Not assigned')} />
        <Info label="Distance" value={formatValue(ride.distance, 'Not available')} />
        <Info label="Estimated duration" value={formatValue(ride.estimatedDuration || ride.duration, 'Not available')} />
      </dl>
      {canCancel(ride) && <button className="mt-6 rounded-lg border border-red-200 px-3 py-2 text-sm font-semibold text-red-700 hover:bg-red-50 disabled:opacity-60" disabled={cancellingRideId === getRideId(ride)} onClick={() => onCancel(getRideId(ride))} type="button">{cancellingRideId === getRideId(ride) ? 'Cancelling...' : 'Cancel ride'}</button>}
    </section>
  )
}

function Info({ label, value }) {
  return <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</dt><dd className="mt-1 break-words font-medium text-slate-800">{value}</dd></div>
}

function StatusBadge({ status }) {
  return <span className="inline-flex rounded-full bg-slate-100 px-3 py-1 text-xs font-bold uppercase tracking-wide text-slate-700">{status}</span>
}

function getRideId(ride) {
  return ride?.rideId ?? ride?.id
}

function getStatus(ride) {
  return ride?.status || 'Unknown'
}

function getDriverName(ride) {
  return ride?.driver?.name || ride?.driverName || 'Not assigned'
}

function formatValue(value, fallback) {
  return value === null || value === undefined || value === '' ? fallback : String(value)
}

function formatDate(value) {
  if (!value) return 'Unavailable'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function canCancel(ride) {
  return ride?.canCancel === true || ride?.cancellable === true
}

export default PassengerPage