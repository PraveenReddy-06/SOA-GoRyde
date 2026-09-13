import { Route, Routes } from 'react-router-dom'
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
      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<AuthenticatedLayout />}>
        <Route path="/passenger" element={<PassengerPage />} />
        <Route path="/driver" element={<DriverPage />} />
      </Route>
    </Routes>
  )
}

export default AppRoutes