import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import authService from '../services/authService'
import toast from 'react-hot-toast'

const AuthContext = createContext(null)

const RUTA_INICIO = {
  admin: '/app/dashboard',
  recepcionista: '/app/clientes',
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const init = async () => {
      if (authService.getAccessToken()) {
        try {
          const data = await authService.getMe()
          setUser(data)
          localStorage.setItem('user', JSON.stringify(data))
        } catch {
          localStorage.removeItem('access_token')
          localStorage.removeItem('refresh_token')
          localStorage.removeItem('user')
        }
      }
      setLoading(false)
    }
    init()
  }, [])

  const login = useCallback(async (username, password) => {
    try {
      const data = await authService.login(username, password)
      localStorage.setItem('access_token', data.access_token)
      localStorage.setItem('refresh_token', data.refresh_token)
      localStorage.setItem('user', JSON.stringify(data.usuario))
      setUser(data.usuario)

      if (data.usuario.es_password_temporal) {
        toast('Debes crear tu contraseña personal', { icon: '🔑' })
        navigate('/cambiar-password')
        return
      }
      navigate(RUTA_INICIO[data.usuario.rol] || '/app/clientes')
      toast.success(`Bienvenido, ${data.usuario.nombres}`)
    } catch (error) {
      toast.error(error.response?.data?.error || 'Error al iniciar sesión')
      throw error
    }
  }, [navigate])

  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
    navigate('/login')
    toast.success('Sesión cerrada')
  }, [navigate])

  const refreshUser = useCallback(async () => {
    try {
      const data = await authService.getMe()
      setUser(data)
      localStorage.setItem('user', JSON.stringify(data))
    } catch { /* noop */ }
  }, [])

  const value = {
    user,
    loading,
    login,
    logout,
    refreshUser,
    isAuthenticated: !!user,
    isAdmin: user?.rol === 'admin',
    isRecepcionista: user?.rol === 'recepcionista',
    rutaInicio: RUTA_INICIO[user?.rol] || '/app/clientes',
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}

export default AuthContext
