import { useEffect, useState, useCallback } from 'react'
import { ScrollText } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../../services/api'
import { DataTable, Badge, LoadingSpinner } from '../../components/ui'

const ACCION_COLOR = { crear: 'verde', editar: 'azul', eliminar: 'rojo', login: 'gris', logout: 'gris' }
const MODULOS = ['', 'auth', 'personal', 'clientes', 'membresias', 'horarios', 'asistencias', 'pagos', 'gastos', 'contenido']

export default function RegistroActividadPage() {
  const [registros, setRegistros] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modulo, setModulo] = useState('')
  const [accion, setAccion] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      const { data } = await api.get('/auditoria', { params: { modulo: modulo || undefined, accion: accion || undefined } })
      setRegistros(data)
    } catch { toast.error('No se pudo cargar el registro') } finally { setCargando(false) }
  }, [modulo, accion])
  useEffect(() => { cargar() }, [cargar])

  const columnas = [
    { key: 'fecha', titulo: 'Fecha', render: (r) => new Date(r.fecha).toLocaleString() },
    { key: 'nombre_usuario', titulo: 'Usuario', render: (r) => (
      <div><p className="text-slate-700">{r.nombre_usuario || '—'}</p><p className="text-xs text-slate-400">{r.rol_usuario}</p></div>
    ) },
    { key: 'accion', titulo: 'Acción', render: (r) => <Badge color={ACCION_COLOR[r.accion] || 'gris'}>{r.accion}</Badge> },
    { key: 'modulo', titulo: 'Módulo', render: (r) => <span className="capitalize text-slate-500">{r.modulo}</span> },
    { key: 'descripcion', titulo: 'Detalle', render: (r) => <span className="text-slate-600">{r.descripcion}</span> },
  ]

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-2">
        <ScrollText className="text-aqua-500" />
        <h1 className="text-xl font-semibold text-slate-800">Registro de actividad</h1>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div><label className="label">Módulo</label>
          <select className="input" value={modulo} onChange={(e) => setModulo(e.target.value)}>
            {MODULOS.map((m) => <option key={m} value={m}>{m || 'Todos'}</option>)}
          </select>
        </div>
        <div><label className="label">Acción</label>
          <select className="input" value={accion} onChange={(e) => setAccion(e.target.value)}>
            <option value="">Todas</option>
            <option value="crear">Crear</option><option value="editar">Editar</option>
            <option value="eliminar">Eliminar</option><option value="login">Login</option><option value="logout">Logout</option>
          </select>
        </div>
      </div>

      {cargando ? <LoadingSpinner /> : <DataTable columnas={columnas} datos={registros} />}
    </div>
  )
}
