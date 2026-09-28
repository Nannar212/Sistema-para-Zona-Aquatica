import { useEffect, useState } from 'react'
import { Ticket, Plus, Pencil, Trash2, Clock, Calendar } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../../services/api'
import { Modal, ConfirmDialog, Badge, EmptyState, LoadingSpinner } from '../../components/ui'

const DIAS = [
  { v: 0, l: 'Lun' }, { v: 1, l: 'Mar' }, { v: 2, l: 'Mié' },
  { v: 3, l: 'Jue' }, { v: 4, l: 'Vie' }, { v: 5, l: 'Sáb' }, { v: 6, l: 'Dom' },
]
const VACIO = { nombre: '', descripcion: '', duracion_dias: 30, numero_clases: '', precio: '', dias_acceso: [0, 1, 2, 3, 4], activa: true }

export default function MembresiasPage() {
  const [items, setItems] = useState([])
  const [cargando, setCargando] = useState(true)
  const [modal, setModal] = useState(false)
  const [form, setForm] = useState(VACIO)
  const [editId, setEditId] = useState(null)
  const [confirmar, setConfirmar] = useState(null)

  const cargar = async () => {
    setCargando(true)
    try {
      const { data } = await api.get('/membresias')
      setItems(data)
    } catch { toast.error('No se pudieron cargar las membresías') } finally { setCargando(false) }
  }
  useEffect(() => { cargar() }, [])

  const abrirNuevo = () => { setForm(VACIO); setEditId(null); setModal(true) }
  const abrirEditar = (m) => {
    setForm({ nombre: m.nombre, descripcion: m.descripcion, duracion_dias: m.duracion_dias, numero_clases: m.numero_clases ?? '', precio: m.precio, dias_acceso: m.dias_acceso, activa: m.activa })
    setEditId(m.id); setModal(true)
  }

  const toggleDia = (v) => {
    setForm((f) => ({
      ...f,
      dias_acceso: f.dias_acceso.includes(v) ? f.dias_acceso.filter((d) => d !== v) : [...f.dias_acceso, v].sort(),
    }))
  }

  const guardar = async (e) => {
    e.preventDefault()
    const payload = {
      ...form,
      duracion_dias: Number(form.duracion_dias),
      numero_clases: form.numero_clases === '' || form.numero_clases === null ? null : Number(form.numero_clases),
      precio: Number(form.precio || 0),
    }
    try {
      if (editId) { await api.put(`/membresias/${editId}`, payload); toast.success('Membresía actualizada') }
      else { await api.post('/membresias', payload); toast.success('Membresía creada') }
      setModal(false); cargar()
    } catch (error) { toast.error(error.response?.data?.error || 'Error al guardar') }
  }

  const eliminar = async (m) => {
    try { await api.delete(`/membresias/${m.id}`); toast.success('Membresía eliminada'); cargar() }
    catch (error) { toast.error(error.response?.data?.error || 'Error al eliminar') }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Ticket className="text-aqua-500" />
          <h1 className="text-xl font-semibold text-slate-800">Membresías</h1>
        </div>
        <button className="btn-primary" onClick={abrirNuevo}><Plus size={18} /> Nueva membresía</button>
      </div>

      {cargando ? <LoadingSpinner /> : items.length === 0 ? (
        <EmptyState titulo="Sin membresías" mensaje="Crea la primera membresía para asignarla a los clientes." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((m) => (
            <div key={m.id} className="card space-y-3">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-slate-800">{m.nombre}</h3>
                  {!m.activa && <Badge color="rojo">Inactiva</Badge>}
                </div>
                <p className="text-xl font-semibold text-aqua-600">Bs {m.precio}</p>
              </div>
              {m.descripcion && <p className="text-sm text-slate-500">{m.descripcion}</p>}
              <div className="flex items-center gap-2 text-sm text-slate-500"><Clock size={15} /> {m.numero_clases ? `${m.numero_clases} clases` : `${m.duracion_dias} días`}</div>
              <div className="flex items-center gap-2 text-sm text-slate-500">
                <Calendar size={15} />
                <div className="flex flex-wrap gap-1">
                  {DIAS.filter((d) => m.dias_acceso.includes(d.v)).map((d) => (
                    <span key={d.v} className="rounded bg-aqua-50 px-1.5 py-0.5 text-xs text-aqua-700">{d.l}</span>
                  ))}
                </div>
              </div>
              <div className="flex justify-end gap-1 border-t border-slate-100 pt-2">
                <button className="rounded p-2 text-slate-500 hover:bg-aqua-50 hover:text-aqua-600" onClick={() => abrirEditar(m)}><Pencil size={16} /></button>
                <button className="rounded p-2 text-slate-500 hover:bg-red-50 hover:text-red-600"
                  onClick={() => setConfirmar({ mensaje: `¿Eliminar la membresía "${m.nombre}"?`, accion: () => eliminar(m) })}><Trash2 size={16} /></button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={modal} onClose={() => setModal(false)} title={editId ? 'Editar membresía' : 'Nueva membresía'}>
        <form onSubmit={guardar} className="space-y-3">
          <div><label className="label">Nombre</label><input className="input" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} required /></div>
          <div><label className="label">Descripción</label><textarea className="input" rows={2} value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} /></div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className="label">Número de clases</label><input type="number" min="0" className="input" value={form.numero_clases} onChange={(e) => setForm({ ...form, numero_clases: e.target.value })} placeholder="Ej: 9 (opcional)" /></div>
            <div><label className="label">Precio (Bs)</label><input type="number" min="0" step="0.01" className="input" value={form.precio} onChange={(e) => setForm({ ...form, precio: e.target.value })} required /></div>
            <div><label className="label">Duración máx. (días)</label><input type="number" min="1" className="input" value={form.duracion_dias} onChange={(e) => setForm({ ...form, duracion_dias: e.target.value })} required /></div>
          </div>
          <p className="rounded-lg bg-aqua-50 px-3 py-2 text-xs text-aqua-700">
            <b>Número de clases</b>: para paquetes (ej: 9 clases). Se descuenta 1 clase por cada día habilitado (según los "Días de acceso") a partir de la fecha de inicio: hoy cuenta de inmediato; si el inicio es una fecha futura, empieza a contar ese día. Déjalo vacío para contar por días de calendario (planes mensuales).
          </p>
          <div>
            <label className="label">Días de acceso</label>
            <div className="flex flex-wrap gap-2">
              {DIAS.map((d) => (
                <button type="button" key={d.v} onClick={() => toggleDia(d.v)}
                  className={`rounded-lg px-3 py-1.5 text-sm ${form.dias_acceso.includes(d.v) ? 'bg-aqua-500 text-white' : 'bg-slate-100 text-slate-500'}`}>
                  {d.l}
                </button>
              ))}
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input type="checkbox" checked={form.activa} onChange={(e) => setForm({ ...form, activa: e.target.checked })} /> Activa
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-outline" onClick={() => setModal(false)}>Cancelar</button>
            <button type="submit" className="btn-primary">{editId ? 'Guardar' : 'Crear'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog open={!!confirmar} onClose={() => setConfirmar(null)} onConfirm={() => confirmar?.accion()} mensaje={confirmar?.mensaje} textoConfirmar="Eliminar" peligro />
    </div>
  )
}
