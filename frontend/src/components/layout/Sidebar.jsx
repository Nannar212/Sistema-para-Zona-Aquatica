import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Users, ClipboardCheck, CalendarDays,
  Ticket, UserCog, Waves, Wallet, TrendingUp, ScrollText, Globe,
} from 'lucide-react'
import { useAuth } from '../../contexts/AuthContext'

const ITEMS = [
  { to: '/app/dashboard', label: 'Panel', icon: LayoutDashboard, roles: ['admin'] },
  { to: '/app/clientes', label: 'Clientes', icon: Users, roles: ['admin', 'recepcionista'] },
  { to: '/app/asistencia', label: 'Asistencia', icon: ClipboardCheck, roles: ['admin', 'recepcionista'] },
  { to: '/app/horarios', label: 'Horarios', icon: CalendarDays, roles: ['admin', 'recepcionista'] },
  { to: '/app/membresias', label: 'Membresías', icon: Ticket, roles: ['admin'] },
  { to: '/app/finanzas', label: 'Ingresos y egresos', icon: TrendingUp, roles: ['admin'] },
  { to: '/app/gastos', label: 'Gastos', icon: Wallet, roles: ['admin'] },
  { to: '/app/personal', label: 'Personal', icon: UserCog, roles: ['admin'] },
  { to: '/app/actividad', label: 'Actividad', icon: ScrollText, roles: ['admin'] },
  { to: '/app/contenido', label: 'Página pública', icon: Globe, roles: ['admin'] },
]

export default function Sidebar({ onNavigate, logo }) {
  const { user } = useAuth()
  const items = ITEMS.filter((i) => i.roles.includes(user?.rol))

  return (
    <aside className="flex h-full w-64 flex-col bg-aqua-600 text-white">
      <div className="flex items-center gap-2 px-5 py-5">
        {logo
          ? <img src={logo} alt="logo" className="h-9 w-9 rounded-full object-cover" />
          : <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/15"><Waves size={20} /></div>}
        <div className="leading-tight">
          <p className="font-semibold">Zona Aquática</p>
          <p className="text-xs text-aqua-100">Sistema de gestión</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 px-3 py-2">
        {items.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                isActive ? 'bg-white text-aqua-700' : 'text-aqua-50 hover:bg-white/10'
              }`
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="px-5 py-4 text-xs text-aqua-100">
        Lunes a viernes · Quillacollo
      </div>
    </aside>
  )
}
