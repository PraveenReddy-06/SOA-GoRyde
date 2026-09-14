import api from './api'

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? {}
}

function getErrorMessage(error, fallback) {
  const message = error.response?.data?.message || error.response?.data?.error || error.message || fallback
  const requestError = new Error(message)
  requestError.status = error.response?.status
  return requestError
}

function unwrapDriver(response) {
  const payload = unwrap(response)
  return payload.driver ?? payload
}

export function isUnauthorized(error) {
  return error.status === 401
}

export function isForbidden(error) {
  return error.status === 403
}

export async function getCurrentDriver() {
  try {
    return unwrapDriver(await api.get('/api/drivers/me'))
  } catch (error) {
    throw getErrorMessage(error, 'Unable to load your driver profile.')
  }
}

export async function updateAvailability(driverId, availability) {
  try {
    return unwrapDriver(await api.patch(`/api/drivers/${driverId}/availability`, { availability }))
  } catch (error) {
    throw getErrorMessage(error, 'Unable to update driver availability.')
  }
}

export async function updateLocation(driverId, location) {
  try {
    return unwrapDriver(await api.patch(`/api/drivers/${driverId}/location`, location))
  } catch (error) {
    throw getErrorMessage(error, 'Unable to update your location.')
  }
}

export async function getDriverRides() {
  try {
    const payload = unwrap(await api.get('/api/rides/driver/my-rides'))
    return Array.isArray(payload) ? payload : payload.rides ?? payload.content ?? []
  } catch (error) {
    throw getErrorMessage(error, 'Unable to load assigned rides.')
  }
}

async function postDriverAction(driverId, action, rideId) {
  try {
    return unwrapDriver(await api.post(`/api/drivers/${driverId}/${action}`, { rideId }))
  } catch (error) {
    throw getErrorMessage(error, `Unable to ${action} this ride.`)
  }
}

export function acceptRide(driverId, rideId) {
  return postDriverAction(driverId, 'accept', rideId)
}

export function rejectRide(driverId, rideId) {
  return postDriverAction(driverId, 'reject', rideId)
}

export function markArriving(driverId, rideId) {
  return postDriverAction(driverId, 'arriving', rideId)
}

export function markArrived(driverId, rideId) {
  return postDriverAction(driverId, 'arrived', rideId)
}

export async function startRide(rideId) {
  try {
    return unwrap(await api.post(`/api/rides/${rideId}/start`))
  } catch (error) {
    throw getErrorMessage(error, 'Unable to start this ride.')
  }
}

export async function completeRide(rideId) {
  try {
    return unwrap(await api.post(`/api/rides/${rideId}/complete`))
  } catch (error) {
    throw getErrorMessage(error, 'Unable to complete this ride.')
  }
}