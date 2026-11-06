import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
} from 'recharts'
import { Users, Ticket, Wallet, CalendarClock, AlertTriangle } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../../services/api'
import { StatsCard, LoadingSpinner } from '../../components/ui'

const COLORES = ['#185fa5', '#ef9f27', '#57a0e0', '#f4b53f', '#0c447c', '#85b7eb']

export default function DashboardPage() {
  const [data, setData] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    api.get('/dashboard/resumen')
      .then(({ data }) => setData(data))
      .catch(() => toast.error('No se pudo cargar el panel'))
      .finally(() => setCargando(false))
  }, [])

  if (cargando) return <LoadingSpinner label="Cargando panel..." />
  if (!data) return null

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-slate-800">Panel administrativo</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatsCard titulo="Clientes activos" valor={data.clientes_activos} icono={Users} color="aqua" />
        <StatsCard titulo="Membresías vigentes" valor={data.membresias_vigentes} icono={Ticket} color="verde" />
        <StatsCard titulo="Ingresos de hoy" valor={`Bs ${data.ingresos_hoy}`} icono={Wallet} color="sol" />
        <StatsCard titulo="Ingresos del mes" valor={`Bs ${data.ingresos_mes}`} icono={CalendarClock} color="aqua" />
      </div>

      {data.membresias_por_vencer > 0 && (
        <div className="flex items-center gap-2 rounded-xl border border-sol-200 bg-sol-50 px-4 py-3 text-sm text-sol-800">
          <AlertTriangle size={18} />
          {data.membresias_por_vencer} membresía(s) vencen en los próximos 3 días.
          <Link to="/app/clientes" className="ml-auto font-medium underline">Ver clientes</Link>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="card">
          <h3 className="mb-3 font-medium text-slate-700">Clientes por membresía</h3>
          {data.distribucion_membresias.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">Sin datos todavía.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={data.distribucion_membresias} dataKey="cantidad" nameKey="membresia" cx="50%" cy="50%" outerRadius={90} label>
                  {data.distribucion_membresias.map((_, i) => <Cell key={i} fill={COLORES[i % COLORES.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="card">
          <h3 className="mb-3 font-medium text-slate-700">Ocupación por horario</h3>
          {data.ocupacion_horarios.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">Sin horarios todavía.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data.ocupacion_horarios}>
                <XAxis dataKey="horario" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="ocupados" fill="#185fa5" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  )
}
