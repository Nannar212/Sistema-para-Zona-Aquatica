import { useEffect, useState, useCallback } from 'react'
import {
  Users, Plus, Pencil, Trash2, CreditCard, Phone, Clock, IdCard, FileText,
  AlertCircle, Ban, RotateCcw, RefreshCw, Waves, CalendarDays,
} from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { Modal, ConfirmDialog, Badge, SearchBar, EmptyState, LoadingSpinner } from '../components/ui'

const hoyISO = () => new Date().toISOString().slice(0, 10)

const fmtFecha = (iso) => {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

// Calcula la fecha de fin igual que el backend: por número de clases (n-ésimo día
// habilitado) o por duración en días. Solo para mostrar una vista previa.
function calcularFechaFinISO(m, inicioISO) {
  if (!m || !inicioISO) return null
  const toISO = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const inicio = new Date(inicioISO + 'T00:00:00')
  const dias = (m.dias_acceso && m.dias_acceso.length) ? m.dias_acceso : [0, 1, 2, 3, 4]
  const dset = new Set(dias)
  if (m.numero_clases && m.numero_clases > 0) {
    let d = new Date(inicio); let contados = 0; let guard = 0
    while (guard < 7 * m.numero_clases + 30) {
      const wdMon = (d.getDay() + 6) % 7
      if (dset.has(wdMon)) { contados++; if (contados >= m.numero_clases) return toISO(d) }
      d = new Date(d.getTime() + 86400000); guard++
    }
    return toISO(d)
  }
  const d = new Date(inicio.getTime() + (Number(m.duracion_dias) || 30) * 86400000)
  return toISO(d)
}
const VACIO = { nombres: '', apellidos: '', sexo: 'M', edad: '', horario_id: '', celular: '', membresia_id: '', fecha_inicio: hoyISO() }
const SEXO_LABEL = { M: 'Masculino', F: 'Femenino', Otro: 'Otro' }
const TABS = [
  { id: 'activo', label: 'Activos' },
  { id: 'inactivo', label: 'Inactivos' },
  { id: 'eliminado', label: 'Eliminados' },
]

function InfoMembresia({ m }) {
  if (!m) return <Badge color="gris">Sin membresía</Badge>
  if (m.vigente) {
    const color = m.clases_restantes <= 2 ? 'amarillo' : 'verde'
    return <Badge color={color}>{m.membresia_nombre} · {m.clases_restantes}/{m.clases_totales} clases</Badge>
  }
  return <Badge color="rojo">Vencida</Badge>
}

export default function ClientesPage() {
  const { isAdmin } = useAuth()
  const [tab, setTab] = useState('activo')
  const [clientes, setClientes] = useState([])
  const [membresias, setMembresias] = useState([])
  const [horarios, setHorarios] = useState([])
  const [conteo, setConteo] = useState({ activo: 0, inactivo: 0, eliminado: 0 })
  const [cargando, setCargando] = useState(true)
  const [busqueda, setBusqueda] = useState('')

  const [formModal, setFormModal] = useState(false)
  const [form, setForm] = useState(VACIO)
  const [editId, setEditId] = useState(null)

  const [detalle, setDetalle] = useState(null)
  const [renovar, setRenovar] = useState({ membresia_id: '', fecha_inicio: hoyISO() })
  const [pago, setPago] = useState(null)
  const [eliminar, setEliminar] = useState(null)   // { cliente, motivo, eliminar_pagos }
  const [inactivar, setInactivar] = useState(null) // { cliente, motivo }
  const [confirmar, setConfirmar] = useState(null)

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      const [c, m, h, ct] = await Promise.all([
        api.get('/clientes', { params: { q: busqueda, estado: tab } }),
        api.get('/membresias', { params: { activas: true } }),
        api.get('/horarios', { params: { activos: true } }),
        api.get('/clientes/contadores'),
      ])
      setClientes(c.data); setMembresias(m.data); setHorarios(h.data); setConteo(ct.data)
    } catch { toast.error('No se pudieron cargar los datos') } finally { setCargando(false) }
  }, [busqueda, tab])
  useEffect(() => { cargar() }, [cargar])

  const abrirNuevo = () => { setForm(VACIO); setEditId(null); setFormModal(true) }
  const abrirEditar = (c) => {
    setForm({ nombres: c.nombres, apellidos: c.apellidos, sexo: c.sexo, edad: c.edad ?? '', horario_id: c.horario_id ?? '', celular: c.celular ?? '', membresia_id: '', fecha_inicio: hoyISO() })
    setEditId(c.id); setFormModal(true)
  }

  const verDetalle = async (id) => {
    try { const { data } = await api.get(`/clientes/${id}`); setDetalle(data); setRenovar({ membresia_id: '', fecha_inicio: hoyISO() }) }
    catch { toast.error('No se pudo cargar el cliente') }
  }

  const guardar = async (e) => {
    e.preventDefault()
    const payload = { ...form, edad: form.edad === '' ? null : Number(form.edad), horario_id: form.horario_id || null, membresia_id: form.membresia_id || null }
    try {
      if (editId) {
        await api.put(`/clientes/${editId}`, payload); toast.success('Cliente actualizado')
        setFormModal(false); cargar()
      } else {
        const { data } = await api.post('/clientes', payload); toast.success('Cliente registrado')
        setFormModal(false); await cargar(); verDetalle(data.cliente.id)
      }
    } catch (error) { toast.error(error.response?.data?.error || 'Error al guardar') }
  }

  const doEliminar = async () => {
    try {
      await api.delete(`/clientes/${eliminar.cliente.id}`, { data: { motivo: eliminar.motivo, eliminar_pagos: eliminar.eliminar_pagos } })
      toast.success('Cliente eliminado')
      setEliminar(null); cargar()
    } catch (error) { toast.error(error.response?.data?.error || 'Error al eliminar') }
  }
  const doInactivar = async () => {
    try {
      await api.patch(`/clientes/${inactivar.cliente.id}/inactivar`, { motivo: inactivar.motivo })
      toast.success('Cliente marcado como inactivo')
      setInactivar(null); cargar()
    } catch (error) { toast.error(error.response?.data?.error || 'Error') }
  }
  const doRestaurar = async (c) => {
    try { await api.patch(`/clientes/${c.id}/restaurar`); toast.success('Cliente recuperado'); cargar() }
    catch (error) { toast.error(error.response?.data?.error || 'Error') }
  }

  // ─── Pagos (con saldo/abono y fecha) ───
  const abrirPago = (cliente) => {
    const m = cliente.membresia_activa
    const sugerido = m ? (m.saldo_pendiente > 0 ? m.saldo_pendiente : m.total) : 0
    setPago({ cliente, metodo: 'efectivo', monto: sugerido ? String(sugerido) : '', efectivo: '', qr: '', saldo: m ? m.saldo_pendiente : null, fecha: hoyISO() })
  }
  const registrarPago = async (e) => {
    e.preventDefault()
    const payload = { cliente_id: pago.cliente.id, metodo: pago.metodo, monto: Number(pago.monto || 0), monto_efectivo: Number(pago.efectivo || 0), monto_qr: Number(pago.qr || 0), fecha: pago.fecha }
    try {
      const { data } = await api.post('/pagos', payload)
      toast.success(data.saldo_pendiente > 0 ? `Pago registrado. Saldo: Bs ${data.saldo_pendiente}` : 'Pago registrado')
      setPago(null); cargar(); if (detalle) verDetalle(detalle.id)
    } catch (error) { toast.error(error.response?.data?.error || 'Error al registrar pago') }
  }

  const asignarMembresia = async (clienteId) => {
    if (!renovar.membresia_id) { toast.error('Selecciona una membresía'); return }
    try {
      await api.post(`/clientes/${clienteId}/membresia`, { membresia_id: Number(renovar.membresia_id), fecha_inicio: renovar.fecha_inicio })
      toast.success('Membresía asignada'); cargar(); verDetalle(clienteId)
    } catch (error) { toast.error(error.response?.data?.error || 'Error al asignar') }
  }

  const verRecibo = async (pagoId) => {
    try { const resp = await api.get(`/pagos/${pagoId}/recibo`, { responseType: 'blob' }); window.open(URL.createObjectURL(resp.data), '_blank') }
    catch { toast.error('No se pudo generar el recibo') }
  }

  const totalSel = membresias.find((m) => String(m.id) === String(form.membresia_id))

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Users className="text-aqua-500" />
          <h1 className="text-xl font-semibold text-slate-800">Clientes</h1>
        </div>
        <button className="btn-primary" onClick={abrirNuevo}><Plus size={18} /> Nuevo cliente</button>
      </div>

      {/* Pestañas */}
      <div className="flex flex-wrap gap-2 border-b border-slate-200">
        {TABS.map((t) => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className={`-mb-px flex items-center gap-2 border-b-2 px-4 py-2 text-sm font-medium transition ${tab === t.id ? 'border-aqua-500 text-aqua-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
            {t.label}
            <span className={`rounded-full px-2 py-0.5 text-xs ${tab === t.id ? 'bg-aqua-100 text-aqua-700' : 'bg-slate-100 text-slate-500'}`}>{conteo[t.id] ?? 0}</span>
          </button>
        ))}
      </div>

      <SearchBar value={busqueda} onChange={setBusqueda} placeholder="Buscar por nombre o celular" />

      {cargando ? <LoadingSpinner /> : clientes.length === 0 ? (
        <EmptyState titulo={`Sin clientes ${tab === 'activo' ? 'activos' : tab === 'inactivo' ? 'inactivos' : 'eliminados'}`} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {clientes.map((c) => {
            const manualInactivo = c.estado === 'inactivo'
            return (
              <div key={c.id} className="card space-y-3">
                <div className="flex items-start justify-between">
                  <button className="text-left" onClick={() => verDetalle(c.id)}>
                    <h3 className="font-semibold text-slate-800 hover:text-aqua-600">{c.nombre_completo}</h3>
                    <p className="text-xs text-slate-400">{SEXO_LABEL[c.sexo]} · {c.edad || '—'} años</p>
                  </button>
                  <InfoMembresia m={c.membresia_activa} />
                </div>
                <div className="space-y-1 text-sm text-slate-500">
                  <p className="flex items-center gap-2"><Clock size={14} /> {c.horario || 'Sin horario'}</p>
                  <p className="flex items-center gap-2"><Phone size={14} /> {c.celular || '—'}</p>
                  {c.membresia_activa && (
                    <p className="flex items-center gap-2"><CalendarDays size={14} /> {fmtFecha(c.membresia_activa.fecha_inicio)} → {fmtFecha(c.membresia_activa.fecha_fin_efectiva)}</p>
                  )}
                  {tab === 'activo' && c.membresia_activa?.vigente && (
                    <p className="flex items-center gap-2 text-aqua-600"><Waves size={14} /> {c.membresia_activa.clases_restantes} de {c.membresia_activa.clases_totales} clases restantes</p>
                  )}
                  {tab === 'activo' && c.membresia_activa?.saldo_pendiente > 0 && (
                    <p className="flex items-center gap-2 text-red-500"><AlertCircle size={14} /> Debe Bs {c.membresia_activa.saldo_pendiente}</p>
                  )}
                  {tab !== 'activo' && c.motivo && (
                    <p className="flex items-center gap-2 text-slate-500"><AlertCircle size={14} /> {c.motivo}</p>
                  )}
                </div>

                {/* Acciones según pestaña */}
                <div className="flex flex-wrap justify-end gap-1 border-t border-slate-100 pt-2">
                  {tab === 'activo' && (
                    <>
                      <button className="btn-sol px-2.5 py-1.5 text-xs" onClick={() => abrirPago(c)}><CreditCard size={14} /> Pago</button>
                      <button className="rounded p-2 text-slate-500 hover:bg-aqua-50 hover:text-aqua-600" title="Editar" onClick={() => abrirEditar(c)}><Pencil size={16} /></button>
                      {isAdmin && <button className="rounded p-2 text-slate-500 hover:bg-slate-100" title="Marcar inactivo" onClick={() => setInactivar({ cliente: c, motivo: '' })}><Ban size={16} /></button>}
                      {isAdmin && <button className="rounded p-2 text-slate-500 hover:bg-red-50 hover:text-red-600" title="Eliminar" onClick={() => setEliminar({ cliente: c, motivo: '', eliminar_pagos: false })}><Trash2 size={16} /></button>}
                    </>
                  )}
                  {tab === 'inactivo' && (
                    <>
                      <button className="btn-primary px-2.5 py-1.5 text-xs" onClick={() => verDetalle(c.id)}><RefreshCw size={14} /> Renovar</button>
                      {isAdmin && manualInactivo && <button className="rounded p-2 text-slate-500 hover:bg-green-50 hover:text-green-600" title="Recuperar" onClick={() => doRestaurar(c)}><RotateCcw size={16} /></button>}
                      <button className="rounded p-2 text-slate-500 hover:bg-aqua-50 hover:text-aqua-600" title="Editar" onClick={() => abrirEditar(c)}><Pencil size={16} /></button>
                      {isAdmin && <button className="rounded p-2 text-slate-500 hover:bg-red-50 hover:text-red-600" title="Eliminar" onClick={() => setEliminar({ cliente: c, motivo: '', eliminar_pagos: false })}><Trash2 size={16} /></button>}
                    </>
                  )}
                  {tab === 'eliminado' && isAdmin && (
                    <button className="btn-outline px-2.5 py-1.5 text-xs" onClick={() => doRestaurar(c)}><RotateCcw size={14} /> Restaurar</button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Modal crear/editar cliente */}
      <Modal open={formModal} onClose={() => setFormModal(false)} title={editId ? 'Editar cliente' : 'Nuevo cliente'}>
        <form onSubmit={guardar} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div><label className="label">Nombres</label><input className="input" value={form.nombres} onChange={(e) => setForm({ ...form, nombres: e.target.value })} required /></div>
            <div><label className="label">Apellidos</label><input className="input" value={form.apellidos} onChange={(e) => setForm({ ...form, apellidos: e.target.value })} required /></div>
            <div><label className="label">Sexo</label>
              <select className="input" value={form.sexo} onChange={(e) => setForm({ ...form, sexo: e.target.value })}>
                <option value="M">Masculino</option><option value="F">Femenino</option><option value="Otro">Otro</option>
              </select>
            </div>
            <div><label className="label">Edad</label><input type="number" min="0" className="input" value={form.edad} onChange={(e) => setForm({ ...form, edad: e.target.value })} /></div>
            <div><label className="label">Horario</label>
              <select className="input" value={form.horario_id} onChange={(e) => setForm({ ...form, horario_id: e.target.value })}>
                <option value="">Seleccionar…</option>
                {horarios.map((h) => <option key={h.id} value={h.id}>{h.nombre} ({h.hora_inicio}–{h.hora_fin})</option>)}
              </select>
            </div>
            <div><label className="label">Celular</label><input className="input" value={form.celular} onChange={(e) => setForm({ ...form, celular: e.target.value })} placeholder="Ej: 700 00000" /></div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className="label">Membresía {editId && <span className="text-xs text-slate-400">(opcional)</span>}</label>
              <select className="input" value={form.membresia_id} onChange={(e) => setForm({ ...form, membresia_id: e.target.value })}>
                <option value="">{editId ? 'No cambiar' : 'Sin membresía por ahora'}</option>
                {membresias.map((m) => <option key={m.id} value={m.id}>{m.nombre} · Bs {m.precio} · {m.duracion_dias}d</option>)}
              </select>
            </div>
            {form.membresia_id && (
              <div><label className="label">Inicio de membresía</label><input type="date" className="input" value={form.fecha_inicio} onChange={(e) => setForm({ ...form, fecha_inicio: e.target.value })} /></div>
            )}
          </div>
          {totalSel && <p className="rounded-lg bg-aqua-50 px-3 py-2 text-xs text-aqua-700">Podrás registrar el pago (total o parcial) después de guardar. Precio: Bs {totalSel.precio}.</p>}
          {totalSel && form.fecha_inicio && (
            <p className="rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
              <CalendarDays size={13} className="mr-1 inline align-[-2px]" />
              Fecha de fin según la membresía: <b>{fmtFecha(calcularFechaFinISO(totalSel, form.fecha_inicio))}</b>. No se edita aquí; solo cambia con las justificaciones por asistencia.
            </p>
          )}
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-outline" onClick={() => setFormModal(false)}>Cancelar</button>
            <button type="submit" className="btn-primary">{editId ? 'Guardar' : 'Registrar'}</button>
          </div>
        </form>
      </Modal>

      {/* Modal detalle */}
      <Modal open={!!detalle} onClose={() => setDetalle(null)} title="Ficha del cliente" maxWidth="max-w-xl">
        {detalle && (
          <div className="space-y-4">
            <div className="rounded-xl bg-aqua-50 p-4">
              <h3 className="text-lg font-semibold text-slate-800">{detalle.nombre_completo}</h3>
              <div className="mt-2 grid grid-cols-2 gap-2 text-sm text-slate-600">
                <p className="flex items-center gap-2"><IdCard size={15} /> {SEXO_LABEL[detalle.sexo]} · {detalle.edad || '—'} años</p>
                <p className="flex items-center gap-2"><Phone size={15} /> {detalle.celular || '—'}</p>
                <p className="flex items-center gap-2"><Clock size={15} /> {detalle.horario || 'Sin horario'}</p>
                <p><InfoMembresia m={detalle.membresia_activa} /></p>
              </div>
              {detalle.membresia_activa && (
                <div className="mt-2 flex flex-wrap gap-3 text-sm">
                  <span className="text-slate-500">Clases: <b className="text-aqua-600">{detalle.membresia_activa.clases_restantes}</b> de {detalle.membresia_activa.clases_totales} restantes</span>
                  <span className="text-slate-500">Vence: <b className="text-slate-700">{detalle.membresia_activa.fecha_fin_efectiva}</b></span>
                  <span className="text-slate-500">Saldo: <b className={detalle.membresia_activa.saldo_pendiente > 0 ? 'text-red-500' : 'text-slate-700'}>Bs {detalle.membresia_activa.saldo_pendiente}</b></span>
                </div>
              )}
            </div>

            <div className="flex flex-wrap items-end gap-2 rounded-lg border border-slate-100 p-3">
              <button className="btn-sol" onClick={() => abrirPago(detalle)}><CreditCard size={16} /> Registrar pago</button>
              <div className="flex-1" />
              <div>
                <label className="label">Renovar / cambiar plan</label>
                <select className="input" value={renovar.membresia_id} onChange={(e) => setRenovar({ ...renovar, membresia_id: e.target.value })}>
                  <option value="">Membresía…</option>
                  {membresias.map((m) => <option key={m.id} value={m.id}>{m.nombre} · Bs {m.precio}</option>)}
                </select>
              </div>
              <div><label className="label">Inicio</label><input type="date" className="input" value={renovar.fecha_inicio} onChange={(e) => setRenovar({ ...renovar, fecha_inicio: e.target.value })} /></div>
              <button className="btn-primary" onClick={() => asignarMembresia(detalle.id)}>Asignar</button>
            </div>

            <div>
              <h4 className="mb-2 text-sm font-medium text-slate-600">Últimos pagos</h4>
              {(!detalle.pagos || detalle.pagos.length === 0) ? <p className="text-sm text-slate-400">Sin pagos registrados.</p> : (
                <ul className="divide-y divide-slate-100 rounded-lg border border-slate-100">
                  {detalle.pagos.slice(0, 8).map((p) => (
                    <li key={p.id} className="flex items-center justify-between px-3 py-2 text-sm">
                      <span className="text-slate-500">{new Date(p.fecha).toLocaleDateString()}</span>
                      <span className="capitalize text-slate-500">{p.concepto === 'abono' ? 'Abono' : 'Pago'} · {p.metodo}</span>
                      <span className="font-medium text-slate-800">Bs {p.monto}</span>
                      <button className="text-aqua-600 hover:text-aqua-700" title="Recibo PDF" onClick={() => verRecibo(p.id)}><FileText size={16} /></button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Modal registrar pago */}
      <Modal open={!!pago} onClose={() => setPago(null)} title="Registrar pago" maxWidth="max-w-md">
        {pago && (
          <form onSubmit={registrarPago} className="space-y-3">
            <p className="text-sm text-slate-500">Cliente: <span className="font-medium text-slate-700">{pago.cliente.nombre_completo}</span></p>
            {pago.saldo != null && pago.saldo > 0 && <p className="rounded-lg bg-sol-50 px-3 py-2 text-sm text-sol-800">Saldo pendiente: <b>Bs {pago.saldo}</b>. Puedes cobrar el total o una parte (fianza).</p>}
            <div>
              <label className="label">Método de pago</label>
              <div className="grid grid-cols-3 gap-2">
                {['efectivo', 'qr', 'mixto'].map((m) => (
                  <button type="button" key={m} onClick={() => setPago({ ...pago, metodo: m })} className={`rounded-lg px-3 py-2 text-sm capitalize ${pago.metodo === m ? 'bg-aqua-500 text-white' : 'bg-slate-100 text-slate-500'}`}>{m}</button>
                ))}
              </div>
            </div>
            {pago.metodo === 'mixto' ? (
              <div className="grid grid-cols-2 gap-3">
                <div><label className="label">Efectivo (Bs)</label><input type="number" min="0" step="0.01" className="input" value={pago.efectivo} onChange={(e) => setPago({ ...pago, efectivo: e.target.value })} required /></div>
                <div><label className="label">QR (Bs)</label><input type="number" min="0" step="0.01" className="input" value={pago.qr} onChange={(e) => setPago({ ...pago, qr: e.target.value })} required /></div>
                <p className="col-span-2 text-sm text-slate-500">Total: <span className="font-semibold text-slate-800">Bs {(Number(pago.efectivo || 0) + Number(pago.qr || 0)).toFixed(2)}</span></p>
              </div>
            ) : (
              <div><label className="label">Monto (Bs)</label><input type="number" min="0" step="0.01" className="input" value={pago.monto} onChange={(e) => setPago({ ...pago, monto: e.target.value })} required /></div>
            )}
            <div><label className="label">Fecha del pago</label><input type="date" className="input" value={pago.fecha} onChange={(e) => setPago({ ...pago, fecha: e.target.value })} /><p className="mt-1 text-xs text-slate-400">Puedes registrar un pago de días anteriores.</p></div>
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" className="btn-outline" onClick={() => setPago(null)}>Cancelar</button>
              <button type="submit" className="btn-primary">Registrar</button>
            </div>
          </form>
        )}
      </Modal>

      {/* Modal eliminar (con motivo + devolución) */}
      <Modal open={!!eliminar} onClose={() => setEliminar(null)} title="Eliminar cliente" maxWidth="max-w-md">
        {eliminar && (
          <div className="space-y-3">
            <p className="text-sm text-slate-600">Vas a eliminar a <b>{eliminar.cliente.nombre_completo}</b>. Podrás recuperarlo desde la pestaña Eliminados.</p>
            <div><label className="label">Motivo</label><textarea className="input" rows={2} value={eliminar.motivo} onChange={(e) => setEliminar({ ...eliminar, motivo: e.target.value })} placeholder="Ej: canceló y se le devolvió el dinero" /></div>
            <label className="flex items-start gap-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              <input type="checkbox" className="mt-0.5" checked={eliminar.eliminar_pagos} onChange={(e) => setEliminar({ ...eliminar, eliminar_pagos: e.target.checked })} />
              <span>Eliminar también sus pagos (devolución). El dinero dejará de contar como ingreso.</span>
            </label>
            <div className="flex justify-end gap-2 pt-2">
              <button className="btn-outline" onClick={() => setEliminar(null)}>Cancelar</button>
              <button className="btn-danger" onClick={doEliminar}>Eliminar</button>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal inactivar */}
      <Modal open={!!inactivar} onClose={() => setInactivar(null)} title="Marcar como inactivo" maxWidth="max-w-md">
        {inactivar && (
          <div className="space-y-3">
            <p className="text-sm text-slate-600">Cliente: <b>{inactivar.cliente.nombre_completo}</b></p>
            <div><label className="label">Motivo</label><textarea className="input" rows={2} value={inactivar.motivo} onChange={(e) => setInactivar({ ...inactivar, motivo: e.target.value })} placeholder="Ej: dejó de asistir, viaje, etc." /></div>
            <div className="flex justify-end gap-2 pt-2">
              <button className="btn-outline" onClick={() => setInactivar(null)}>Cancelar</button>
              <button className="btn-primary" onClick={doInactivar}>Marcar inactivo</button>
            </div>
          </div>
        )}
      </Modal>

      <ConfirmDialog open={!!confirmar} onClose={() => setConfirmar(null)} onConfirm={() => confirmar?.accion()} mensaje={confirmar?.mensaje} textoConfirmar="Confirmar" peligro />
    </div>
  )
}
