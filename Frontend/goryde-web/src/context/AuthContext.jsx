import { createContext, useContext, useEffect, useState } from 'react'
import api, { AUTH_TOKEN_KEY } from '../services/api'

const AuthContext = createContext(null)

function getResponsePayload(response) {
  return response?.data?.data ?? response?.data ?? {}
}

function getToken(payload) {
  return payload.token ?? payload.accessToken ?? payload.jwt
}

function getUser(payload) {
  return payload.user ?? payload.account ?? (payload.id || payload.email ? payload : null)
}

function getErrorMessage(error, fallback) {
  return error.response?.data?.message || error.response?.data?.error || error.message || fallback
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(() => localStorage.getItem(AUTH_TOKEN_KEY))
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let active = true

    async function restoreSession() {
      const storedToken = localStorage.getItem(AUTH_TOKEN_KEY)

      if (!storedToken) {
        if (active) setIsLoading(false)
        return
      }

      try {
        const response = await api.get('/api/auth/me')
        const restoredUser = getUser(getResponsePayload(response))

        if (!restoredUser) {
          throw new Error('The authentication response did not include a user.')
        }

        if (active) {
          setToken(storedToken)
          setUser(restoredUser)
        }
      } catch {
        localStorage.removeItem(AUTH_TOKEN_KEY)
        if (active) {
          setToken(null)
          setUser(null)
        }
      } finally {
        if (active) setIsLoading(false)
      }
    }

    restoreSession()

    return () => {
      active = false
    }
  }, [])

  async function login(credentials) {
    try {
      const response = await api.post('/api/auth/login', credentials)
      const payload = getResponsePayload(response)
      const nextToken = getToken(payload)
      const nextUser = getUser(payload)

      if (!nextToken || !nextUser) {
        throw new Error('Login response did not include valid authentication data.')
      }

      localStorage.setItem(AUTH_TOKEN_KEY, nextToken)
      setToken(nextToken)
      setUser(nextUser)
      return nextUser
    } catch (error) {
      throw new Error(getErrorMessage(error, 'Unable to log in. Please try again.'))
    }
  }

  async function register(details) {
    try {
      const response = await api.post('/api/auth/register', details)
      const payload = getResponsePayload(response)
      const nextToken = getToken(payload)
      const nextUser = getUser(payload)

      if (nextToken && nextUser) {
        localStorage.setItem(AUTH_TOKEN_KEY, nextToken)
        setToken(nextToken)
        setUser(nextUser)
        return nextUser
      }

      return null
    } catch (error) {
      throw new Error(getErrorMessage(error, 'Unable to create your account. Please try again.'))
    }
  }

  function logout() {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }

  return context
}