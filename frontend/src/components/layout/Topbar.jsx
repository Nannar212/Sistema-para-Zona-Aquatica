import { LogOut, PanelLeft } from 'lucide-react'
import { useAuth } from '../../contexts/AuthContext'
import Badge from '../ui/Badge'

export default function Topbar({ onMenu }) {
  const { user, logout } = useAuth()
  return (
    <header className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3">
      <button className="rounded-lg p-2 text-slate-500 hover:bg-slate-100" onClick={onMenu} title="Mostrar/ocultar menú" aria-label="Mostrar u ocultar menú">
        <PanelLeft size={20} />
      </button>
      <div className="flex items-center gap-3">
        <div className="text-right leading-tight">
          <p className="text-sm font-medium text-slate-700">{user?.nombre_completo || user?.nombres}</p>
          <Badge color={user?.rol === 'admin' ? 'azul' : 'amarillo'}>
            {user?.rol === 'admin' ? 'Administrador' : 'Recepcionista'}
          </Badge>
        </div>
        <button
          onClick={logout}
          className="flex items-center gap-1 rounded-lg px-3 py-2 text-sm text-slate-500 hover:bg-red-50 hover:text-red-600"
        >
          <LogOut size={18} />
          <span className="hidden sm:inline">Salir</span>
        </button>
      </div>
    </header>
  )
}
