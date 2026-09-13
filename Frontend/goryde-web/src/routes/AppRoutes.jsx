import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute, PublicOnlyRoute } from './RouteGuards'
import AdminPage from '../pages/AdminPage'
import AuthenticatedLayout from '../layouts/AuthenticatedLayout'
import PublicLayout from '../layouts/PublicLayout'
import DriverPage from '../pages/DriverPage'
import HomePage from '../pages/HomePage'
import LoginPage from '../pages/LoginPage'
import PassengerPage from '../pages/PassengerPage'
import RegisterPage from '../pages/RegisterPage'

function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route element={<AuthenticatedLayout />}>
          <Route path="/passenger" element={<PassengerPage />} />
          <Route path="/driver" element={<DriverPage />} />
          <Route path="/admin" element={<AdminPage />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default AppRoutes