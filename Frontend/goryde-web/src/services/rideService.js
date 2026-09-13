import api from './api'

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? {}
}

function unwrapRide(response) {
  const payload = unwrap(response)
  return payload.ride ?? payload
}

function getErrorMessage(error, fallback) {
  if (error.response?.status === 401) {
    return 'Your session has expired. Please log in again.'
  }

  return error.response?.data?.message || error.response?.data?.error || error.message || fallback
}

export function isUnauthorized(error) {
  return error.response?.status === 401
}

export async function createRide(rideDetails) {
  try {
    const response = await api.post('/api/rides', rideDetails)
    return unwrapRide(response)
  } catch (error) {
    throw new Error(getErrorMessage(error, 'Unable to book your ride. Please try again.'))
  }
}

export async function getMyRides() {
  try {
    const response = await api.get('/api/rides/my-rides')
    const payload = unwrap(response)
    return Array.isArray(payload) ? payload : payload.rides ?? payload.content ?? []
  } catch (error) {
    throw new Error(getErrorMessage(error, 'Unable to load your rides. Please try again.'))
  }
}

export async function getRide(rideId) {
  try {
    const response = await api.get(`/api/rides/${rideId}`)
    return unwrapRide(response)
  } catch (error) {
    throw new Error(getErrorMessage(error, 'Unable to load this ride. Please try again.'))
  }
}

export async function cancelRide(rideId) {
  try {
    const response = await api.post(`/api/rides/${rideId}/cancel`)
    return unwrap(response)
  } catch (error) {
    throw new Error(getErrorMessage(error, 'The ride could not be cancelled.'))
  }
}