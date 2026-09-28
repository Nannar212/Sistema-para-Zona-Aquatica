import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Waves, User, Lock } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'

export default function LoginPage() {
  const { login, isAuthenticated, rutaInicio } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [cargando, setCargando] = useState(false)

  if (isAuthenticated) return <Navigate to={rutaInicio} replace />

  const onSubmit = async (e) => {
    e.preventDefault()
    setCargando(true)
    try {
      await login(username.trim(), password)
    } catch {
      /* el toast lo maneja el contexto */
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-aqua-600 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-aqua-500 text-white">
            <Waves size={28} />
          </div>
          <h1 className="text-xl font-semibold text-slate-800">Zona Aquática</h1>
          <p className="text-sm text-slate-500">Ingresa con tu usuario y contraseña</p>
        </div>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="label">Usuario</label>
            <div className="relative">
              <User size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                className="input pl-10"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="tu.usuario"
                autoFocus
              />
            </div>
          </div>
          <div>
            <label className="label">Contraseña</label>
            <div className="relative">
              <Lock size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="password"
                className="input pl-10"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>
          </div>
          <button type="submit" className="btn-primary w-full" disabled={cargando}>
            {cargando ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>
      </div>
    </div>
  )
}
