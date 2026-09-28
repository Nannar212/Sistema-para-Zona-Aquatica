import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import ProtectedRoute from './components/auth/ProtectedRoute'
import MainLayout from './components/layout/MainLayout'
import { LoadingSpinner } from './components/ui'

import LoginPage from './pages/LoginPage'
import CambiarPasswordPage from './pages/CambiarPasswordPage'
import NoAutorizadoPage from './pages/NoAutorizadoPage'
import NotFoundPage from './pages/NotFoundPage'

import DashboardPage from './pages/admin/DashboardPage'
import PersonalPage from './pages/admin/PersonalPage'
import MembresiasPage from './pages/admin/MembresiasPage'
import GastosPage from './pages/admin/GastosPage'
import FinanzasPage from './pages/admin/FinanzasPage'
import RegistroActividadPage from './pages/admin/RegistroActividadPage'
import ContenidoPublicoPage from './pages/admin/ContenidoPublicoPage'
import HorariosPage from './pages/HorariosPage'
import ClientesPage from './pages/ClientesPage'
import AsistenciaPage from './pages/AsistenciaPage'
import PublicPage from './pages/public/PublicPage'

function InicioRedirect() {
  const { rutaInicio } = useAuth()
  return <Navigate to={rutaInicio} replace />
}

export default function App() {
  const { loading } = useAuth()

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-aqua-50">
        <LoadingSpinner label="Cargando Zona Aquática..." />
      </div>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/no-autorizado" element={<NoAutorizadoPage />} />
      <Route
        path="/cambiar-password"
        element={
          <ProtectedRoute>
            <CambiarPasswordPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/app"
        element={
          <ProtectedRoute roles={['admin', 'recepcionista']}>
            <MainLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<InicioRedirect />} />
        <Route path="clientes" element={<ClientesPage />} />
        <Route path="asistencia" element={<AsistenciaPage />} />
        <Route path="horarios" element={<HorariosPage />} />
        <Route
          path="dashboard"
          element={<ProtectedRoute roles={['admin']}><DashboardPage /></ProtectedRoute>}
        />
        <Route
          path="membresias"
          element={<ProtectedRoute roles={['admin']}><MembresiasPage /></ProtectedRoute>}
        />
        <Route
          path="personal"
          element={<ProtectedRoute roles={['admin']}><PersonalPage /></ProtectedRoute>}
        />
        <Route
          path="finanzas"
          element={<ProtectedRoute roles={['admin']}><FinanzasPage /></ProtectedRoute>}
        />
        <Route
          path="gastos"
          element={<ProtectedRoute roles={['admin']}><GastosPage /></ProtectedRoute>}
        />
        <Route
          path="actividad"
          element={<ProtectedRoute roles={['admin']}><RegistroActividadPage /></ProtectedRoute>}
        />
        <Route
          path="contenido"
          element={<ProtectedRoute roles={['admin']}><ContenidoPublicoPage /></ProtectedRoute>}
        />
      </Route>

      <Route path="/" element={<PublicPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
