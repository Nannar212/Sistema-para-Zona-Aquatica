import { useEffect, useState, useCallback } from 'react'
import { Wallet, Plus, Pencil, Trash2 } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../../services/api'
import { DataTable, Modal, ConfirmDialog, Badge, LoadingSpinner } from '../../components/ui'

const hoyISO = () => new Date().toISOString().slice(0, 10)
const primerDiaMes = () => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10) }
const CATEGORIAS = ['Cloro', 'Cisterna', 'Mantenimiento', 'Servicios', 'Insumos', 'Salarios', 'Otros']
const VACIO = { categoria: 'Cloro', descripcion: '', monto: '', metodo: 'efectivo', fecha: hoyISO() }

export default function GastosPage() {
  const [gastos, setGastos] = useState([])
  const [total, setTotal] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [desde, setDesde] = useState(primerDiaMes())
  const [hasta, setHasta] = useState(hoyISO())

  const [modal, setModal] = useState(false)
  const [form, setForm] = useState(VACIO)
  const [editId, setEditId] = useState(null)
  const [confirmar, setConfirmar] = useState(null)

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      const { data } = await api.get('/gastos', { params: { desde, hasta } })
      setGastos(data.gastos); setTotal(data.total)
    } catch { toast.error('No se pudieron cargar los gastos') } finally { setCargando(false) }
  }, [desde, hasta])
  useEffect(() => { cargar() }, [cargar])

  const abrirNuevo = () => { setForm(VACIO); setEditId(null); setModal(true) }
  const abrirEditar = (g) => {
    setForm({ categoria: g.categoria, descripcion: g.descripcion, monto: g.monto, metodo: g.metodo, fecha: g.fecha })
    setEditId(g.id); setModal(true)
  }

  const guardar = async (e) => {
    e.preventDefault()
    const payload = { ...form, monto: Number(form.monto || 0) }
    try {
      if (editId) { await api.put(`/gastos/${editId}`, payload); toast.success('Gasto actualizado') }
      else { await api.post('/gastos', payload); toast.success('Gasto registrado') }
      setModal(false); cargar()
    } catch (error) { toast.error(error.response?.data?.error || 'Error al guardar') }
  }

  const eliminar = async (g) => {
    try { await api.delete(`/gastos/${g.id}`); toast.success('Gasto eliminado'); cargar() }
    catch (error) { toast.error(error.response?.data?.error || 'Error al eliminar') }
  }

  const columnas = [
    { key: 'fecha', titulo: 'Fecha', render: (g) => new Date(g.fecha).toLocaleDateString() },
    { key: 'categoria', titulo: 'Categoría', render: (g) => <Badge color="azul">{g.categoria}</Badge> },
    { key: 'descripcion', titulo: 'Descripción', render: (g) => <span className="text-slate-600">{g.descripcion || '—'}</span> },
    { key: 'metodo', titulo: 'Método', render: (g) => <span className="capitalize text-slate-500">{g.metodo}</span> },
    { key: 'monto', titulo: 'Monto', render: (g) => <span className="font-medium text-slate-800">Bs {g.monto}</span> },
    { key: 'acciones', titulo: '', render: (g) => (
      <div className="flex justify-end gap-1">
        <button className="rounded p-2 text-slate-500 hover:bg-aqua-50 hover:text-aqua-600" onClick={() => abrirEditar(g)}><Pencil size={16} /></button>
        <button className="rounded p-2 text-slate-500 hover:bg-red-50 hover:text-red-600" onClick={() => setConfirmar({ mensaje: `¿Eliminar este gasto de Bs ${g.monto}?`, accion: () => eliminar(g) })}><Trash2 size={16} /></button>
      </div>
    ) },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Wallet className="text-aqua-500" />
          <h1 className="text-xl font-semibold text-slate-800">Gastos</h1>
        </div>
        <button className="btn-primary" onClick={abrirNuevo}><Plus size={18} /> Nuevo gasto</button>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div><label className="label">Desde</label><input type="date" className="input" value={desde} onChange={(e) => setDesde(e.target.value)} /></div>
        <div><label className="label">Hasta</label><input type="date" className="input" value={hasta} onChange={(e) => setHasta(e.target.value)} /></div>
        <div className="rounded-lg bg-red-50 px-4 py-2 text-sm text-red-700">Total del período: <b>Bs {total}</b></div>
      </div>

      {cargando ? <LoadingSpinner /> : <DataTable columnas={columnas} datos={gastos} />}

      <Modal open={modal} onClose={() => setModal(false)} title={editId ? 'Editar gasto' : 'Nuevo gasto'}>
        <form onSubmit={guardar} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div><label className="label">Categoría</label>
              <select className="input" value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })}>
                {CATEGORIAS.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div><label className="label">Fecha</label><input type="date" className="input" value={form.fecha} onChange={(e) => setForm({ ...form, fecha: e.target.value })} /></div>
            <div><label className="label">Monto (Bs)</label><input type="number" min="0" step="0.01" className="input" value={form.monto} onChange={(e) => setForm({ ...form, monto: e.target.value })} required /></div>
            <div><label className="label">Método</label>
              <select className="input" value={form.metodo} onChange={(e) => setForm({ ...form, metodo: e.target.value })}>
                <option value="efectivo">Efectivo</option><option value="qr">QR</option><option value="transferencia">Transferencia</option>
              </select>
            </div>
          </div>
          <div><label className="label">Descripción</label><textarea className="input" rows={2} value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} placeholder="Ej: 2 bidones de cloro" /></div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-outline" onClick={() => setModal(false)}>Cancelar</button>
            <button type="submit" className="btn-primary">{editId ? 'Guardar' : 'Registrar'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog open={!!confirmar} onClose={() => setConfirmar(null)} onConfirm={() => confirmar?.accion()} mensaje={confirmar?.mensaje} textoConfirmar="Eliminar" peligro />
    </div>
  )
}
