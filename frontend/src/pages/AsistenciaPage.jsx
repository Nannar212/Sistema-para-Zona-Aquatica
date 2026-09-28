import { useEffect, useState, useCallback } from 'react'
import { ClipboardCheck, Check, X, CalendarClock, CalendarDays, Table2 } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { Modal, Badge, EmptyState, LoadingSpinner } from '../components/ui'
import CalendarioAsistencia from '../components/CalendarioAsistencia'

const dosDig = (n) => String(n).padStart(2, '0')
const hoyISO = () => { const d = new Date(); return `${d.getFullYear()}-${dosDig(d.getMonth() + 1)}-${dosDig(d.getDate())}` }
const primerDiaMesISO = () => { const d = new Date(); return `${d.getFullYear()}-${dosDig(d.getMonth() + 1)}-01` }
const ultimoDiaMesISO = () => { const d = new Date(); const u = new Date(d.getFullYear(), d.getMonth() + 1, 0); return `${u.getFullYear()}-${dosDig(u.getMonth() + 1)}-${dosDig(u.getDate())}` }

const ESTADO_BADGE = {
  presente: { color: 'verde', label: 'Presente' },
  falta: { color: 'rojo', label: 'Falta' },
  justificada: { color: 'amarillo', label: 'Justificada' },
}
const COLOR_CELDA = { presente: 'bg-green-500', falta: 'bg-red-500', justificada: 'bg-yellow-500' }
const LETRA_DIA = ['D', 'L', 'M', 'M', 'J', 'V', 'S']

function rangoDias(desde, hasta) {
  const out = []
  let d = new Date(desde + 'T00:00:00')
  const fin = new Date(hasta + 'T00:00:00')
  let guard = 0
  while (d <= fin && guard < 400) {
    out.push({ iso: `${d.getFullYear()}-${dosDig(d.getMonth() + 1)}-${dosDig(d.getDate())}`, dia: d.getDate(), wd: d.getDay() })
    d = new Date(d.getTime() + 86400000)
    guard++
  }
  return out
}

export default function AsistenciaPage() {
  const { isAdmin } = useAuth()
  const [vista, setVista] = useState('lista')
  const [calCliente, setCalCliente] = useState(null) // { id, nombre }

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-2">
        <ClipboardCheck className="text-aqua-500" />
        <h1 className="text-xl font-semibold text-slate-800">Asistencia</h1>
      </div>

      <div className="flex flex-wrap gap-2 border-b border-slate-200">
        <button onClick={() => setVista('lista')}
          className={`-mb-px flex items-center gap-2 border-b-2 px-4 py-2 text-sm font-medium ${vista === 'lista' ? 'border-aqua-500 text-aqua-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
          <ClipboardCheck size={16} /> Pasar lista
        </button>
        <button onClick={() => setVista('general')}
          className={`-mb-px flex items-center gap-2 border-b-2 px-4 py-2 text-sm font-medium ${vista === 'general' ? 'border-aqua-500 text-aqua-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
          <Table2 size={16} /> General
        </button>
      </div>

      {vista === 'lista'
        ? <PasarLista isAdmin={isAdmin} onVerCalendario={(id, nombre) => setCalCliente({ id, nombre })} />
        : <AsistenciaGeneral onVerCalendario={(id, nombre) => setCalCliente({ id, nombre })} />}

      <Modal open={!!calCliente} onClose={() => setCalCliente(null)} title={calCliente ? `Asistencias de ${calCliente.nombre}` : ''} maxWidth="max-w-lg">
        {calCliente && <CalendarioAsistencia clienteId={calCliente.id} />}
      </Modal>
    </div>
  )
}

/* ─── Pestaña: pasar lista ─── */
function PasarLista({ isAdmin, onVerCalendario }) {
  const [fecha, setFecha] = useState(hoyISO())
  const [horarios, setHorarios] = useState([])
  const [horarioId, setHorarioId] = useState('')
  const [filas, setFilas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [justificar, setJustificar] = useState(null)

  useEffect(() => {
    api.get('/horarios', { params: { activos: true } }).then(({ data }) => setHorarios(data)).catch(() => {})
  }, [])

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      const { data } = await api.get('/asistencias/dia', { params: { fecha, horario_id: horarioId || undefined } })
      setFilas(data.clientes)
    } catch { toast.error('No se pudo cargar la asistencia') } finally { setCargando(false) }
  }, [fecha, horarioId])
  useEffect(() => { cargar() }, [cargar])

  const marcar = async (cliente_id, estado, extra = {}) => {
    try {
      await api.post('/asistencias', { cliente_id, fecha, estado, ...extra })
      setFilas((prev) => prev.map((f) => f.cliente_id === cliente_id
        ? { ...f, estado, dias_justificados: extra.dias_justificados ?? 0, observacion: extra.observacion ?? '' } : f))
    } catch (error) { toast.error(error.response?.data?.error || 'Error al marcar') }
  }

  const guardarJustificacion = async (e) => {
    e.preventDefault()
    await marcar(justificar.cliente.cliente_id, 'justificada', { dias_justificados: Number(justificar.dias || 0), observacion: justificar.observacion })
    if (Number(justificar.dias) > 0) toast.success(`Se sumarán ${justificar.dias} día(s) a la membresía`)
    setJustificar(null)
  }

  const conteo = filas.reduce((acc, f) => { if (f.estado) acc[f.estado] = (acc[f.estado] || 0) + 1; return acc }, {})

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end gap-3">
        <div><label className="label">Fecha</label><input type="date" className="input" value={fecha} onChange={(e) => setFecha(e.target.value)} /></div>
        <div><label className="label">Horario</label>
          <select className="input" value={horarioId} onChange={(e) => setHorarioId(e.target.value)}>
            <option value="">Todos</option>
            {horarios.map((h) => <option key={h.id} value={h.id}>{h.nombre}</option>)}
          </select>
        </div>
        <div className="flex gap-2 pb-0.5">
          <Badge color="verde">Presentes: {conteo.presente || 0}</Badge>
          <Badge color="rojo">Faltas: {conteo.falta || 0}</Badge>
          <Badge color="amarillo">Justificadas: {conteo.justificada || 0}</Badge>
        </div>
      </div>

      {cargando ? <LoadingSpinner /> : filas.length === 0 ? (
        <EmptyState titulo="Sin clientes" mensaje="No hay clientes para este filtro." />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-slate-500">Cliente</th>
                <th className="px-4 py-3 text-left font-medium text-slate-500">Horario</th>
                <th className="px-4 py-3 text-left font-medium text-slate-500">Estado</th>
                <th className="px-4 py-3 text-right font-medium text-slate-500">Marcar</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filas.map((f) => (
                <tr key={f.cliente_id} className="hover:bg-aqua-50/40">
                  <td className="px-4 py-3">
                    <button className="flex items-center gap-2 font-medium text-slate-800 hover:text-aqua-600" onClick={() => onVerCalendario(f.cliente_id, f.nombre_completo)} title="Ver asistencias">
                      <CalendarDays size={16} className="text-aqua-500" /> {f.nombre_completo}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{f.horario || '—'}</td>
                  <td className="px-4 py-3">
                    {f.estado
                      ? <Badge color={ESTADO_BADGE[f.estado].color}>{ESTADO_BADGE[f.estado].label}{f.estado === 'justificada' && f.dias_justificados ? ` (+${f.dias_justificados}d)` : ''}</Badge>
                      : <span className="text-slate-300">Sin marcar</span>}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-1">
                      <button title="Presente" onClick={() => marcar(f.cliente_id, 'presente')} className={`rounded-lg p-2 ${f.estado === 'presente' ? 'bg-green-500 text-white' : 'bg-slate-100 text-slate-500 hover:bg-green-100'}`}><Check size={16} /></button>
                      <button title="Falta" onClick={() => marcar(f.cliente_id, 'falta')} className={`rounded-lg p-2 ${f.estado === 'falta' ? 'bg-red-500 text-white' : 'bg-slate-100 text-slate-500 hover:bg-red-100'}`}><X size={16} /></button>
                      <button title="Justificada" onClick={() => setJustificar({ cliente: f, dias: 0, observacion: f.observacion || '' })} className={`rounded-lg p-2 ${f.estado === 'justificada' ? 'bg-yellow-500 text-white' : 'bg-slate-100 text-slate-500 hover:bg-yellow-100'}`}><CalendarClock size={16} /></button>
                      <button title="Ver asistencias" onClick={() => onVerCalendario(f.cliente_id, f.nombre_completo)} className="rounded-lg bg-slate-100 p-2 text-slate-500 hover:bg-aqua-100 hover:text-aqua-600"><CalendarDays size={16} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal open={!!justificar} onClose={() => setJustificar(null)} title="Ausencia justificada" maxWidth="max-w-md">
        {justificar && (
          <form onSubmit={guardarJustificacion} className="space-y-3">
            <p className="text-sm text-slate-500">Cliente: <span className="font-medium text-slate-700">{justificar.cliente.nombre_completo}</span></p>
            <div>
              <label className="label">Días de justificación a sumar a la membresía</label>
              <input type="number" min="0" className="input" value={justificar.dias} onChange={(e) => setJustificar({ ...justificar, dias: e.target.value })} />
              <p className="mt-1 text-xs text-slate-400">{isAdmin ? 'Estos días se agregan al final de la membresía.' : 'Consulta con el administrador antes de extender la membresía.'}</p>
            </div>
            <div><label className="label">Observación (opcional)</label><textarea className="input" rows={2} value={justificar.observacion} onChange={(e) => setJustificar({ ...justificar, observacion: e.target.value })} /></div>
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" className="btn-outline" onClick={() => setJustificar(null)}>Cancelar</button>
              <button type="submit" className="btn-primary">Guardar</button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  )
}

/* ─── Pestaña: asistencia general (tipo Excel) ─── */
function AsistenciaGeneral({ onVerCalendario }) {
  const [desde, setDesde] = useState(primerDiaMesISO())
  const [hasta, setHasta] = useState(ultimoDiaMesISO())
  const [clientes, setClientes] = useState([])
  const [cargando, setCargando] = useState(true)

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      const { data } = await api.get('/asistencias/general', { params: { desde, hasta } })
      setClientes(data.clientes)
    } catch { toast.error('No se pudo cargar la asistencia general') } finally { setCargando(false) }
  }, [desde, hasta])
  useEffect(() => { cargar() }, [cargar])

  const dias = rangoDias(desde, hasta)

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <div><label className="label">Desde</label><input type="date" className="input" value={desde} onChange={(e) => setDesde(e.target.value)} /></div>
        <div><label className="label">Hasta</label><input type="date" className="input" value={hasta} onChange={(e) => setHasta(e.target.value)} /></div>
        <div className="flex flex-wrap gap-3 pb-1 text-xs text-slate-500">
          <span className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-green-500" /> Asistió</span>
          <span className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-red-500" /> Faltó</span>
          <span className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-yellow-500" /> Justificada</span>
        </div>
      </div>

      {cargando ? <LoadingSpinner /> : clientes.length === 0 ? (
        <EmptyState titulo="Sin clientes activos" />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table className="text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="sticky left-0 z-10 border-r border-slate-200 bg-slate-50 px-3 py-2 text-left font-medium text-slate-500">Cliente</th>
                <th className="px-2 py-2 text-center font-medium text-slate-500">Edad</th>
                <th className="whitespace-nowrap px-2 py-2 text-center font-medium text-slate-500">Inicio</th>
                <th className="whitespace-nowrap px-2 py-2 text-center font-medium text-slate-500">Fin</th>
                {dias.map((d) => (
                  <th key={d.iso} className={`px-1 py-2 text-center text-xs font-medium ${d.wd === 0 || d.wd === 6 ? 'bg-slate-100 text-slate-400' : 'text-slate-500'}`} title={d.iso}>
                    <div>{LETRA_DIA[d.wd]}</div><div>{d.dia}</div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {clientes.map((c) => (
                <tr key={c.cliente_id} className="hover:bg-aqua-50/30">
                  <td className="sticky left-0 z-10 border-r border-slate-200 bg-white px-3 py-2">
                    <button className="flex items-center gap-2 font-medium text-slate-800 hover:text-aqua-600" onClick={() => onVerCalendario(c.cliente_id, c.nombre)} title="Ver calendario">
                      <CalendarDays size={15} className="shrink-0 text-aqua-500" /> <span className="whitespace-nowrap">{c.nombre}</span>
                    </button>
                    <span className="ml-6 text-xs text-green-600">{c.totales.presente}✓</span>
                    <span className="ml-1 text-xs text-red-500">{c.totales.falta}✗</span>
                    <span className="ml-1 text-xs text-yellow-600">{c.totales.justificada}!</span>
                  </td>
                  <td className="px-2 py-2 text-center text-slate-500">{c.edad ?? '—'}</td>
                  <td className="whitespace-nowrap px-2 py-2 text-center text-xs text-slate-500">{c.fecha_inicio || '—'}</td>
                  <td className="whitespace-nowrap px-2 py-2 text-center text-xs text-slate-500">{c.fecha_fin || '—'}</td>
                  {dias.map((d) => {
                    const est = c.asistencias[d.iso]
                    return (
                      <td key={d.iso} className={`px-0.5 py-1 ${d.wd === 0 || d.wd === 6 ? 'bg-slate-50/60' : ''}`}>
                        <div className={`mx-auto h-6 w-6 rounded ${est ? COLOR_CELDA[est] : 'bg-slate-100'}`} title={est || ''} />
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="text-xs text-slate-400">Haz clic en el nombre (o el ícono de calendario) para ver el calendario de asistencias de esa persona.</p>
    </div>
  )
}
