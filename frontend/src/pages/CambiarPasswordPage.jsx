import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { KeyRound } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '../contexts/AuthContext'
import authService from '../services/authService'

export default function CambiarPasswordPage() {
  const { user, refreshUser, rutaInicio } = useAuth()
  const navigate = useNavigate()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [cargando, setCargando] = useState(false)

  const esTemporal = user?.es_password_temporal

  const onSubmit = async (e) => {
    e.preventDefault()
    if (nueva !== confirmar) {
      toast.error('Las contraseñas nuevas no coinciden')
      return
    }
    setCargando(true)
    try {
      await authService.cambiarPassword(actual, nueva)
      await refreshUser()
      toast.success('Contraseña actualizada')
      navigate(rutaInicio)
    } catch (error) {
      toast.error(error.response?.data?.error || 'No se pudo cambiar la contraseña')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-aqua-600 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-sol-200 text-sol-800">
            <KeyRound size={26} />
          </div>
          <h1 className="text-xl font-semibold text-slate-800">
            {esTemporal ? 'Crea tu contraseña' : 'Cambiar contraseña'}
          </h1>
          <p className="text-sm text-slate-500">
            {esTemporal
              ? 'Es tu primer ingreso. Define una contraseña personal para continuar.'
              : 'Ingresa tu contraseña actual y la nueva.'}
          </p>
        </div>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="label">{esTemporal ? 'Contraseña temporal' : 'Contraseña actual'}</label>
            <input type="password" className="input" value={actual} onChange={(e) => setActual(e.target.value)} />
          </div>
          <div>
            <label className="label">Nueva contraseña</label>
            <input type="password" className="input" value={nueva} onChange={(e) => setNueva(e.target.value)} />
            <p className="mt-1 text-xs text-slate-400">Mínimo 6 caracteres, con al menos una letra y un número.</p>
          </div>
          <div>
            <label className="label">Repetir nueva contraseña</label>
            <input type="password" className="input" value={confirmar} onChange={(e) => setConfirmar(e.target.value)} />
          </div>
          <button type="submit" className="btn-primary w-full" disabled={cargando}>
            {cargando ? 'Guardando...' : 'Guardar contraseña'}
          </button>
        </form>
      </div>
    </div>
  )
}
