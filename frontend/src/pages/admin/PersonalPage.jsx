import { useEffect, useState } from 'react'
import { UserCog, Plus, Pencil, KeyRound, Power, Trash2, Copy, Check } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../../services/api'
import { DataTable, Modal, ConfirmDialog, Badge, SearchBar, LoadingSpinner } from '../../components/ui'

const VACIO = { nombres: '', apellidos: '', ci: '', username: '', edad: '', rol: 'recepcionista' }

export default function PersonalPage() {
  const [usuarios, setUsuarios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [busqueda, setBusqueda] = useState('')

  const [modal, setModal] = useState(false)
  const [form, setForm] = useState(VACIO)
  const [editId, setEditId] = useState(null)

  const [passModal, setPassModal] = useState(null) // { titulo, password }
  const [copiado, setCopiado] = useState(false)
  const [confirmar, setConfirmar] = useState(null) // { mensaje, accion, peligro, texto }

  const cargar = async () => {
    setCargando(true)
    try {
      const { data } = await api.get('/personal', { params: { inactivos: true } })
      setUsuarios(data)
    } catch {
      toast.error('No se pudo cargar el personal')
    } finally {
      setCargando(false)
    }
  }
  useEffect(() => { cargar() }, [])

  const abrirNuevo = () => { setForm(VACIO); setEditId(null); setModal(true) }
  const abrirEditar = (u) => {
    setForm({ nombres: u.nombres, apellidos: u.apellidos, ci: u.ci, username: u.username, edad: u.edad ?? '', rol: u.rol })
    setEditId(u.id)
    setModal(true)
  }

  const guardar = async (e) => {
    e.preventDefault()
    const payload = { ...form, edad: form.edad === '' ? null : Number(form.edad) }
    try {
      if (editId) {
        await api.put(`/personal/${editId}`, payload)
        toast.success('Usuario actualizado')
      } else {
        const { data } = await api.post('/personal', payload)
        setPassModal({ titulo: 'Usuario creado', password: data.password_temporal })
        toast.success('Usuario creado')
      }
      setModal(false)
      cargar()
    } catch (error) {
      toast.error(error.response?.data?.error || 'Error al guardar')
    }
  }

  const resetear = async (u) => {
    try {
      const { data } = await api.post(`/personal/${u.id}/reset-password`)
      setPassModal({ titulo: `Nueva contraseña de ${u.username}`, password: data.password_temporal })
    } catch (error) {
      toast.error(error.response?.data?.error || 'Error al restablecer')
    }
  }

  const cambiarEstado = async (u) => {
    try {
      await api.patch(`/personal/${u.id}/estado`)
      cargar()
    } catch (error) {
      toast.error(error.response?.data?.error || 'Error al cambiar estado')
    }
  }

  const eliminar = async (u) => {
    try {
      await api.delete(`/personal/${u.id}`)
      toast.success('Usuario eliminado')
      cargar()
    } catch (error) {
      toast.error(error.response?.data?.error || 'Error al eliminar')
    }
  }

  const copiar = (texto) => {
    navigator.clipboard?.writeText(texto)
    setCopiado(true)
    setTimeout(() => setCopiado(false), 1500)
  }

  const filtrados = usuarios.filter((u) => {
    const t = busqueda.toLowerCase()
    return u.nombre_completo.toLowerCase().includes(t) || u.username.toLowerCase().includes(t) || (u.ci || '').includes(t)
  })

  const columnas = [
    { key: 'nombre', titulo: 'Nombre', render: (u) => (
      <div>
        <p className="font-medium text-slate-800">{u.nombre_completo}</p>
        <p className="text-xs text-slate-400">CI: {u.ci}</p>
      </div>
    ) },
    { key: 'username', titulo: 'Usuario', render: (u) => <span className="font-mono text-slate-600">{u.username}</span> },
    { key: 'rol', titulo: 'Rol', render: (u) => (
      <Badge color={u.rol === 'admin' ? 'azul' : 'amarillo'}>
        {u.rol === 'admin' ? 'Administrador' : 'Recepcionista'}
      </Badge>
    ) },
    { key: 'activo', titulo: 'Estado', render: (u) => (
      <Badge color={u.activo ? 'verde' : 'rojo'}>{u.activo ? 'Activo' : 'Inactivo'}</Badge>
    ) },
    { key: 'acciones', titulo: '', render: (u) => (
      <div className="flex justify-end gap-1">
        <button className="rounded p-2 text-slate-500 hover:bg-aqua-50 hover:text-aqua-600" title="Editar" onClick={() => abrirEditar(u)}><Pencil size={16} /></button>
        <button className="rounded p-2 text-slate-500 hover:bg-sol-50 hover:text-sol-600" title="Restablecer contraseña"
          onClick={() => setConfirmar({ mensaje: `¿Generar una nueva contraseña temporal para ${u.username}?`, texto: 'Restablecer', accion: () => resetear(u) })}><KeyRound size={16} /></button>
        <button className="rounded p-2 text-slate-500 hover:bg-slate-100" title={u.activo ? 'Desactivar' : 'Activar'} onClick={() => cambiarEstado(u)}><Power size={16} /></button>
        <button className="rounded p-2 text-slate-500 hover:bg-red-50 hover:text-red-600" title="Eliminar"
          onClick={() => setConfirmar({ mensaje: `¿Eliminar al usuario ${u.username}? Esta acción no se puede deshacer.`, texto: 'Eliminar', peligro: true, accion: () => eliminar(u) })}><Trash2 size={16} /></button>
      </div>
    ) },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <UserCog className="text-aqua-500" />
          <h1 className="text-xl font-semibold text-slate-800">Personal</h1>
        </div>
        <button className="btn-primary" onClick={abrirNuevo}><Plus size={18} /> Nuevo usuario</button>
      </div>

      <SearchBar value={busqueda} onChange={setBusqueda} placeholder="Buscar por nombre, usuario o CI" />

      {cargando ? <LoadingSpinner label="Cargando..." /> : <DataTable columnas={columnas} datos={filtrados} />}

      {/* Modal crear/editar */}
      <Modal open={modal} onClose={() => setModal(false)} title={editId ? 'Editar usuario' : 'Nuevo usuario'}>
        <form onSubmit={guardar} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div><label className="label">Nombres</label><input className="input" value={form.nombres} onChange={(e) => setForm({ ...form, nombres: e.target.value })} required /></div>
            <div><label className="label">Apellidos</label><input className="input" value={form.apellidos} onChange={(e) => setForm({ ...form, apellidos: e.target.value })} required /></div>
            <div><label className="label">Carnet de identidad</label><input className="input" value={form.ci} onChange={(e) => setForm({ ...form, ci: e.target.value })} required /></div>
            <div><label className="label">Edad</label><input type="number" className="input" value={form.edad} onChange={(e) => setForm({ ...form, edad: e.target.value })} /></div>
            <div><label className="label">Usuario</label><input className="input" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required /></div>
            <div><label className="label">Rol</label>
              <select className="input" value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value })}>
                <option value="recepcionista">Recepcionista</option>
                <option value="admin">Administrador</option>
              </select>
            </div>
          </div>
          {!editId && (
            <p className="rounded-lg bg-aqua-50 px-3 py-2 text-xs text-aqua-700">
              Se generará una contraseña temporal automáticamente. Se mostrará una sola vez para entregarla al usuario.
            </p>
          )}
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-outline" onClick={() => setModal(false)}>Cancelar</button>
            <button type="submit" className="btn-primary">{editId ? 'Guardar' : 'Crear usuario'}</button>
          </div>
        </form>
      </Modal>

      {/* Modal contraseña temporal */}
      <Modal open={!!passModal} onClose={() => setPassModal(null)} title={passModal?.titulo || ''} maxWidth="max-w-md">
        <p className="text-sm text-slate-600">Copia esta contraseña temporal y entrégasela al usuario. No podrás volver a verla.</p>
        <div className="mt-3 flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
          <span className="font-mono text-lg text-slate-800">{passModal?.password}</span>
          <button className="btn-ghost" onClick={() => copiar(passModal?.password)}>
            {copiado ? <Check size={16} /> : <Copy size={16} />} {copiado ? 'Copiado' : 'Copiar'}
          </button>
        </div>
        <div className="mt-4 flex justify-end">
          <button className="btn-primary" onClick={() => setPassModal(null)}>Entendido</button>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!confirmar}
        onClose={() => setConfirmar(null)}
        onConfirm={() => confirmar?.accion()}
        mensaje={confirmar?.mensaje}
        textoConfirmar={confirmar?.texto}
        peligro={confirmar?.peligro}
      />
    </div>
  )
}
