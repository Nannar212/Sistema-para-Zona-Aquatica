import { useEffect, useMemo, useState } from 'react'
import { CalendarDays, Plus, Pencil, Trash2, Users } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { Modal, ConfirmDialog, LoadingSpinner, EmptyState, Badge } from '../components/ui'

const DIAS = [
  { v: 0, l: 'Lunes', c: 'Lun' }, { v: 1, l: 'Martes', c: 'Mar' }, { v: 2, l: 'Miércoles', c: 'Mié' },
  { v: 3, l: 'Jueves', c: 'Jue' }, { v: 4, l: 'Viernes', c: 'Vie' },
  { v: 5, l: 'Sábado', c: 'Sáb' }, { v: 6, l: 'Domingo', c: 'Dom' },
]
const VACIO = { nombre: '', hora_inicio: '07:00', hora_fin: '09:00', cupo_maximo: 30, dias_semana: [0, 1, 2, 3, 4], color: '#185fa5' }
const ROW_H = 52

const aMin = (hhmm) => {
  const [h, m] = (hhmm || '0:0').split(':').map(Number)
  return h * 60 + m
}

export default function HorariosPage() {
  const { isAdmin } = useAuth()
  const [horarios, setHorarios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modal, setModal] = useState(false)
  const [form, setForm] = useState(VACIO)
  const [editId, setEditId] = useState(null)
  const [confirmar, setConfirmar] = useState(null)
  const [personas, setPersonas] = useState(null) // { horario, personas }

  const cargar = async () => {
    setCargando(true)
    try {
      const { data } = await api.get('/horarios')
      setHorarios(data)
    } catch { toast.error('No se pudieron cargar los horarios') } finally { setCargando(false) }
  }
  useEffect(() => { cargar() }, [])

  const diasVisibles = useMemo(() => {
    const usados = new Set()
    horarios.forEach((h) => (h.dias_semana || []).forEach((d) => usados.add(d)))
    const base = [0, 1, 2, 3, 4]
    const todos = [...new Set([...base, ...usados])].sort()
    return DIAS.filter((d) => todos.includes(d.v))
  }, [horarios])

  const [hInicio, hFin] = useMemo(() => {
    if (horarios.length === 0) return [6, 20]
    let min = 24 * 60, max = 0
    horarios.forEach((h) => {
      min = Math.min(min, aMin(h.hora_inicio))
      max = Math.max(max, aMin(h.hora_fin))
    })
    return [Math.floor(min / 60), Math.ceil(max / 60)]
  }, [horarios])

  const horas = []
  for (let h = hInicio; h <= hFin; h++) horas.push(h)

  const abrirNuevo = () => { setForm(VACIO); setEditId(null); setModal(true) }
  const abrirEditar = (h) => {
    setForm({ nombre: h.nombre, hora_inicio: h.hora_inicio, hora_fin: h.hora_fin, cupo_maximo: h.cupo_maximo, dias_semana: h.dias_semana, color: h.color })
    setEditId(h.id); setModal(true)
  }

  const toggleDia = (v) => setForm((f) => ({
    ...f, dias_semana: f.dias_semana.includes(v) ? f.dias_semana.filter((d) => d !== v) : [...f.dias_semana, v].sort(),
  }))

  const guardar = async (e) => {
    e.preventDefault()
    try {
      if (editId) { await api.put(`/horarios/${editId}`, form); toast.success('Horario actualizado') }
      else { await api.post('/horarios', form); toast.success('Horario creado') }
      setModal(false); cargar()
    } catch (error) { toast.error(error.response?.data?.error || 'Error al guardar') }
  }

  const eliminar = async (h) => {
    try { await api.delete(`/horarios/${h.id}`); toast.success('Horario eliminado'); cargar() }
    catch (error) { toast.error(error.response?.data?.error || 'Error al eliminar') }
  }

  const verPersonas = async (h) => {
    try { const { data } = await api.get(`/horarios/${h.id}/personas`); setPersonas(data) }
    catch { toast.error('No se pudo cargar la lista') }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <CalendarDays className="text-aqua-500" />
          <h1 className="text-xl font-semibold text-slate-800">Horarios</h1>
        </div>
        {isAdmin && <button className="btn-primary" onClick={abrirNuevo}><Plus size={18} /> Nuevo horario</button>}
      </div>

      {cargando ? <LoadingSpinner /> : horarios.length === 0 ? (
        <EmptyState titulo="Sin horarios" mensaje={isAdmin ? 'Crea el primer horario para el calendario.' : 'Aún no hay horarios cargados.'} />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white p-3">
          <div className="min-w-[640px]">
            {/* Encabezado de días */}
            <div className="grid" style={{ gridTemplateColumns: `56px repeat(${diasVisibles.length}, 1fr)` }}>
              <div />
              {diasVisibles.map((d) => (
                <div key={d.v} className="pb-2 text-center text-sm font-medium text-slate-600">{d.c}</div>
              ))}
            </div>
            {/* Cuerpo del calendario */}
            <div className="grid" style={{ gridTemplateColumns: `56px repeat(${diasVisibles.length}, 1fr)` }}>
              {/* Columna de horas */}
              <div className="relative" style={{ height: (horas.length - 1) * ROW_H }}>
                {horas.map((h, i) => (
                  <div key={h} className="absolute -translate-y-1/2 text-xs text-slate-400" style={{ top: i * ROW_H }}>
                    {String(h).padStart(2, '0')}:00
                  </div>
                ))}
              </div>
              {/* Columnas por día */}
              {diasVisibles.map((d) => (
                <div key={d.v} className="relative border-l border-slate-100" style={{ height: (horas.length - 1) * ROW_H }}>
                  {horas.map((h, i) => (
                    <div key={h} className="absolute w-full border-t border-slate-100" style={{ top: i * ROW_H }} />
                  ))}
                  {horarios.filter((hr) => (hr.dias_semana || []).includes(d.v)).map((hr) => {
                    const top = (aMin(hr.hora_inicio) - hInicio * 60) / 60 * ROW_H
                    const alto = Math.max(28, (aMin(hr.hora_fin) - aMin(hr.hora_inicio)) / 60 * ROW_H - 4)
                    const lleno = hr.cupos_ocupados >= hr.cupo_maximo
                    return (
                      <button key={hr.id} onClick={() => verPersonas(hr)}
                        className="absolute left-1 right-1 overflow-hidden rounded-lg px-2 py-1 text-left text-white transition hover:opacity-90"
                        style={{ top, height: alto, backgroundColor: hr.activo ? hr.color : '#94a3b8' }}>
                        <p className="truncate text-xs font-semibold">{hr.nombre}</p>
                        <p className="truncate text-[11px] opacity-90">{hr.hora_inicio}–{hr.hora_fin}</p>
                        <p className="text-[11px] opacity-90">{lleno ? 'Lleno' : `${hr.cupos_ocupados}/${hr.cupo_maximo}`}</p>
                      </button>
                    )
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Lista resumida con acciones */}
      {!cargando && horarios.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {horarios.map((h) => (
            <div key={h.id} className="card flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <span className="h-3 w-3 rounded-full" style={{ backgroundColor: h.color }} />
                  <p className="font-medium text-slate-800">{h.nombre}</p>
                </div>
                <p className="text-xs text-slate-400">{h.hora_inicio}–{h.hora_fin} · {(h.dias_semana || []).map((v) => DIAS[v]?.c).join(', ')}</p>
                <div className="mt-1"><Badge color={h.cupos_ocupados >= h.cupo_maximo ? 'rojo' : 'verde'}>{h.cupos_ocupados}/{h.cupo_maximo}</Badge></div>
              </div>
              <div className="flex flex-col gap-1">
                <button className="rounded p-2 text-slate-500 hover:bg-aqua-50 hover:text-aqua-600" title="Ver personas" onClick={() => verPersonas(h)}><Users size={16} /></button>
                {isAdmin && <>
                  <button className="rounded p-2 text-slate-500 hover:bg-aqua-50 hover:text-aqua-600" title="Editar" onClick={() => abrirEditar(h)}><Pencil size={16} /></button>
                  <button className="rounded p-2 text-slate-500 hover:bg-red-50 hover:text-red-600" title="Eliminar"
                    onClick={() => setConfirmar({ mensaje: `¿Eliminar el horario "${h.nombre}"?`, accion: () => eliminar(h) })}><Trash2 size={16} /></button>
                </>}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal crear/editar */}
      <Modal open={modal} onClose={() => setModal(false)} title={editId ? 'Editar horario' : 'Nuevo horario'}>
        <form onSubmit={guardar} className="space-y-3">
          <div><label className="label">Nombre</label><input className="input" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} required placeholder="Ej: Mañana" /></div>
          <div className="grid grid-cols-3 gap-3">
            <div><label className="label">Inicio</label><input type="time" className="input" value={form.hora_inicio} onChange={(e) => setForm({ ...form, hora_inicio: e.target.value })} required /></div>
            <div><label className="label">Fin</label><input type="time" className="input" value={form.hora_fin} onChange={(e) => setForm({ ...form, hora_fin: e.target.value })} required /></div>
            <div><label className="label">Cupo</label><input type="number" min="1" className="input" value={form.cupo_maximo} onChange={(e) => setForm({ ...form, cupo_maximo: Number(e.target.value) })} required /></div>
          </div>
          <div>
            <label className="label">Días</label>
            <div className="flex flex-wrap gap-2">
              {DIAS.map((d) => (
                <button type="button" key={d.v} onClick={() => toggleDia(d.v)}
                  className={`rounded-lg px-3 py-1.5 text-sm ${form.dias_semana.includes(d.v) ? 'bg-aqua-500 text-white' : 'bg-slate-100 text-slate-500'}`}>{d.c}</button>
              ))}
            </div>
          </div>
          <div><label className="label">Color</label><input type="color" className="h-10 w-16 cursor-pointer rounded border border-slate-300" value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} /></div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-outline" onClick={() => setModal(false)}>Cancelar</button>
            <button type="submit" className="btn-primary">{editId ? 'Guardar' : 'Crear'}</button>
          </div>
        </form>
      </Modal>

      {/* Modal personas */}
      <Modal open={!!personas} onClose={() => setPersonas(null)} title={personas ? `Personas en ${personas.horario.nombre}` : ''}>
        {personas && (
          <div>
            <div className="mb-3 flex items-center justify-between text-sm text-slate-500">
              <span>{personas.horario.hora_inicio}–{personas.horario.hora_fin}</span>
              <Badge color="azul">{personas.personas.length}/{personas.horario.cupo_maximo} cupos</Badge>
            </div>
            {personas.personas.length === 0 ? (
              <EmptyState titulo="Sin personas asignadas" />
            ) : (
              <ul className="divide-y divide-slate-100">
                {personas.personas.map((p) => (
                  <li key={p.id} className="flex items-center justify-between py-2">
                    <span className="text-slate-700">{p.nombre_completo}</span>
                    <span className="text-xs text-slate-400">{p.celular}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </Modal>

      <ConfirmDialog open={!!confirmar} onClose={() => setConfirmar(null)} onConfirm={() => confirmar?.accion()} mensaje={confirmar?.mensaje} textoConfirmar="Eliminar" peligro />
    </div>
  )
}
