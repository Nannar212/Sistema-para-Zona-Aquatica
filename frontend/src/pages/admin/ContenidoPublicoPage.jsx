import { useEffect, useRef, useState } from 'react'
import {
  Globe, Plus, Trash2, ExternalLink, Save, Upload, Image as ImageIcon, X,
} from 'lucide-react'
import toast from 'react-hot-toast'
import axios from 'axios'
import api from '../../services/api'
import { LoadingSpinner } from '../../components/ui'

const ICONOS = ['temperature', 'kid', 'lifebuoy', 'droplet', 'clock', 'users', 'wave', 'sun', 'star', 'shield']
const API_URL = import.meta.env.VITE_API_URL || '/api'

async function subirImagen(file, tipo) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('tipo', tipo)
  // axios global: deja que el navegador ponga el boundary de multipart automáticamente
  const token = localStorage.getItem('access_token')
  const { data } = await axios.post(`${API_URL}/contenido/upload`, fd, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  return data.url
}

export default function ContenidoPublicoPage() {
  const [c, setC] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const logoRef = useRef(null)
  const heroRef = useRef(null)
  const galRef = useRef(null)
  const modeloRef = useRef(null)

  useEffect(() => {
    api.get('/contenido').then(({ data }) => setC(data))
      .catch(() => toast.error('No se pudo cargar el contenido'))
      .finally(() => setCargando(false))
  }, [])

  const set = (campo, valor) => setC((prev) => ({ ...prev, [campo]: valor }))
  const setLista = (campo, i, key, valor) => setC((prev) => {
    const arr = [...prev[campo]]; arr[i] = { ...arr[i], [key]: valor }; return { ...prev, [campo]: arr }
  })
  const addItem = (campo, plantilla) => setC((prev) => ({ ...prev, [campo]: [...(prev[campo] || []), plantilla] }))
  const delItem = (campo, i) => setC((prev) => ({ ...prev, [campo]: prev[campo].filter((_, j) => j !== i) }))

  const onUpload = async (e, tipo) => {
    const file = e.target.files?.[0]
    if (!file) return
    const t = toast.loading('Subiendo imagen...')
    try {
      const url = await subirImagen(file, tipo)
      if (tipo === 'logo') set('logo_url', url)
      else if (tipo === 'hero') set('hero_imagen_url', url)
      else addItem('galeria', url)
      toast.success('Imagen subida. No olvides Guardar.', { id: t })
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al subir', { id: t })
    } finally {
      e.target.value = ''
    }
  }

  const onSubirModelo = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const t = toast.loading('Subiendo modelo 3D (puede tardar)...')
    try {
      const fd = new FormData()
      fd.append('file', file)
      const token = localStorage.getItem('access_token')
      const { data } = await axios.post(`${API_URL}/contenido/upload-modelo`, fd, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      set('modelo_3d_url', data.url)
      toast.success('Modelo subido. No olvides Guardar.', { id: t })
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al subir el modelo', { id: t })
    } finally { e.target.value = '' }
  }

  const guardar = async () => {
    setGuardando(true)
    try { await api.put('/contenido', c); toast.success('Contenido actualizado') }
    catch (e) { toast.error(e.response?.data?.error || 'Error al guardar') }
    finally { setGuardando(false) }
  }

  if (cargando || !c) return <LoadingSpinner label="Cargando contenido..." />

  const field = (label, campo, textarea = false, placeholder = '') => (
    <div>
      <label className="label">{label}</label>
      {textarea
        ? <textarea className="input" rows={2} value={c[campo] || ''} onChange={(e) => set(campo, e.target.value)} placeholder={placeholder} />
        : <input className="input" value={c[campo] || ''} onChange={(e) => set(campo, e.target.value)} placeholder={placeholder} />}
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Globe className="text-aqua-500" />
          <h1 className="text-xl font-semibold text-slate-800">Página pública</h1>
        </div>
        <div className="flex gap-2">
          <a href="/" target="_blank" rel="noreferrer" className="btn-outline"><ExternalLink size={16} /> Ver página</a>
          <button className="btn-primary" onClick={guardar} disabled={guardando}><Save size={16} /> {guardando ? 'Guardando...' : 'Guardar'}</button>
        </div>
      </div>

      {/* Imágenes */}
      <div className="card space-y-4">
        <h3 className="font-medium text-slate-700">Imágenes</h3>
        <div className="grid gap-6 sm:grid-cols-2">
          {/* Logo */}
          <div>
            <label className="label">Logo (se ajusta a cuadrado)</label>
            <div className="flex items-center gap-3">
              <div className="flex h-20 w-20 items-center justify-center overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
                {c.logo_url ? <img src={c.logo_url} alt="logo" className="h-full w-full object-cover" /> : <ImageIcon className="text-slate-300" />}
              </div>
              <div className="flex flex-col gap-2">
                <button type="button" className="btn-outline" onClick={() => logoRef.current?.click()}><Upload size={16} /> Subir logo</button>
                {c.logo_url && <button type="button" className="text-sm text-red-500 hover:underline" onClick={() => set('logo_url', '')}>Quitar</button>}
              </div>
              <input ref={logoRef} type="file" accept="image/*" className="hidden" onChange={(e) => onUpload(e, 'logo')} />
            </div>
          </div>
          {/* Hero */}
          <div>
            <label className="label">Imagen del título (hero, rectangular)</label>
            <div className="flex items-center gap-3">
              <div className="flex h-20 w-32 items-center justify-center overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
                {c.hero_imagen_url ? <img src={c.hero_imagen_url} alt="hero" className="h-full w-full object-cover" /> : <ImageIcon className="text-slate-300" />}
              </div>
              <div className="flex flex-col gap-2">
                <button type="button" className="btn-outline" onClick={() => heroRef.current?.click()}><Upload size={16} /> Subir imagen</button>
                {c.hero_imagen_url && <button type="button" className="text-sm text-red-500 hover:underline" onClick={() => set('hero_imagen_url', '')}>Quitar</button>}
              </div>
              <input ref={heroRef} type="file" accept="image/*" className="hidden" onChange={(e) => onUpload(e, 'hero')} />
            </div>
          </div>
        </div>
      </div>

      {/* Portada */}
      <div className="card space-y-3">
        <h3 className="font-medium text-slate-700">Portada (hero)</h3>
        <div className="grid gap-3 sm:grid-cols-2">
          {field('Nombre de la piscina', 'nombre')}
          {field('Etiqueta (ubicación)', 'hero_badge')}
        </div>
        {field('Título principal', 'hero_titulo')}
        {field('Subtítulo', 'hero_subtitulo', true)}
        {field("Texto 'sobre la piscina'", 'sobre_texto', true)}
      </div>

      {/* Beneficios */}
      <div className="card space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-medium text-slate-700">Beneficios destacados</h3>
          <button className="btn-ghost text-sm" onClick={() => addItem('destacados', { icono: 'droplet', titulo: '', texto: '' })}><Plus size={16} /> Agregar</button>
        </div>
        {(c.destacados || []).map((d, i) => (
          <div key={i} className="grid grid-cols-12 items-end gap-2">
            <div className="col-span-3"><label className="label">Ícono</label>
              <select className="input" value={d.icono} onChange={(e) => setLista('destacados', i, 'icono', e.target.value)}>
                {ICONOS.map((ic) => <option key={ic} value={ic}>{ic}</option>)}
              </select>
            </div>
            <div className="col-span-4"><label className="label">Título</label><input className="input" value={d.titulo} onChange={(e) => setLista('destacados', i, 'titulo', e.target.value)} /></div>
            <div className="col-span-4"><label className="label">Texto</label><input className="input" value={d.texto} onChange={(e) => setLista('destacados', i, 'texto', e.target.value)} /></div>
            <button className="col-span-1 rounded p-2 text-red-500 hover:bg-red-50" onClick={() => delItem('destacados', i)}><Trash2 size={16} /></button>
          </div>
        ))}
      </div>

      {/* Estadísticas */}
      <div className="card space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-medium text-slate-700">Estadísticas</h3>
          <button className="btn-ghost text-sm" onClick={() => addItem('estadisticas', { valor: '', etiqueta: '' })}><Plus size={16} /> Agregar</button>
        </div>
        {(c.estadisticas || []).map((s, i) => (
          <div key={i} className="grid grid-cols-12 items-end gap-2">
            <div className="col-span-4"><label className="label">Valor</label><input className="input" value={s.valor} onChange={(e) => setLista('estadisticas', i, 'valor', e.target.value)} /></div>
            <div className="col-span-7"><label className="label">Etiqueta</label><input className="input" value={s.etiqueta} onChange={(e) => setLista('estadisticas', i, 'etiqueta', e.target.value)} /></div>
            <button className="col-span-1 rounded p-2 text-red-500 hover:bg-red-50" onClick={() => delItem('estadisticas', i)}><Trash2 size={16} /></button>
          </div>
        ))}
      </div>

      {/* Galería */}
      <div className="card space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-medium text-slate-700">Galería</h3>
          <div className="flex gap-2">
            <button className="btn-outline text-sm" onClick={() => galRef.current?.click()}><Upload size={16} /> Subir imagen</button>
            <button className="btn-ghost text-sm" onClick={() => addItem('galeria', '')}><Plus size={16} /> Agregar URL</button>
            <input ref={galRef} type="file" accept="image/*" className="hidden" onChange={(e) => onUpload(e, 'galeria')} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {(c.galeria || []).map((url, i) => (
            <div key={i} className="group relative">
              <div className="aspect-square overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
                {url ? <img src={url} alt={`Foto ${i + 1}`} className="h-full w-full object-cover" /> : <div className="flex h-full items-center justify-center text-slate-300"><ImageIcon /></div>}
              </div>
              <button className="absolute right-1 top-1 rounded-full bg-white/90 p-1 text-red-500 shadow hover:bg-white" onClick={() => delItem('galeria', i)}><X size={14} /></button>
              <input className="input mt-1 text-xs" placeholder="https://..." value={url}
                onChange={(e) => setC((prev) => { const g = [...prev.galeria]; g[i] = e.target.value; return { ...prev, galeria: g } })} />
            </div>
          ))}
        </div>
        <p className="text-xs text-slate-400">Puedes subir fotos desde tu dispositivo o pegar el enlace de una imagen.</p>
      </div>

      {/* Modelo 3D */}
      <div className="card space-y-3">
        <h3 className="font-medium text-slate-700">Recorrido 3D de las instalaciones</h3>
        <div className="flex flex-wrap items-center gap-3">
          <button type="button" className="btn-outline" onClick={() => modeloRef.current?.click()}><Upload size={16} /> Subir modelo (.glb)</button>
          {c.modelo_3d_url && <a href={c.modelo_3d_url} target="_blank" rel="noreferrer" className="text-sm text-aqua-600 hover:underline">Ver archivo actual</a>}
          {c.modelo_3d_url && <button type="button" className="text-sm text-red-500 hover:underline" onClick={() => set('modelo_3d_url', '')}>Quitar</button>}
          <input ref={modeloRef} type="file" accept=".glb,.gltf,model/gltf-binary" className="hidden" onChange={onSubirModelo} />
        </div>
        <p className="text-xs text-slate-400">
          Sube un archivo <b>.glb</b> (recomendado). En la web la gente podrá girarlo y hacer zoom. ¿No tienes uno? En el archivo <b>MODELO_3D.md</b> del proyecto está el paso a paso para crearlo con tu celular.
        </p>
      </div>

      {/* Contacto */}
      <div className="card space-y-3">
        <h3 className="font-medium text-slate-700">Contacto</h3>
        <div className="grid gap-3 sm:grid-cols-2">
          {field('Dirección', 'direccion')}
          {field('Horario (texto)', 'horario_texto')}
          {field('Teléfono', 'telefono')}
          {field('WhatsApp (número)', 'whatsapp', false, 'Ej: 78820899')}
          {field('WhatsApp (enlace directo)', 'whatsapp_link', false, 'https://wa.me/59178820899')}
          {field('Enlace de Google Maps', 'google_maps_link', false, 'https://maps.app.goo.gl/...')}
        </div>
        <p className="text-xs text-slate-400">Si pegas el enlace de WhatsApp, los botones "Inscríbete" abrirán ese chat. El enlace de Google Maps se usa para el minimapa y el botón "Cómo llegar".</p>
      </div>

      {/* Redes sociales */}
      <div className="card space-y-3">
        <h3 className="font-medium text-slate-700">Redes sociales</h3>
        <div className="grid gap-3 sm:grid-cols-2">
          {field('Facebook', 'facebook', false, 'https://facebook.com/...')}
          {field('Instagram', 'instagram', false, 'https://instagram.com/...')}
          {field('TikTok', 'tiktok', false, 'https://tiktok.com/@...')}
          {field('YouTube', 'youtube', false, 'https://youtube.com/@...')}
        </div>
      </div>

      {/* Visibilidad */}
      <div className="card">
        <div className="flex flex-wrap gap-6">
          <label className="flex items-center gap-2 text-sm text-slate-600"><input type="checkbox" checked={c.mostrar_planes} onChange={(e) => set('mostrar_planes', e.target.checked)} /> Mostrar sección de planes</label>
          <label className="flex items-center gap-2 text-sm text-slate-600"><input type="checkbox" checked={c.mostrar_horarios} onChange={(e) => set('mostrar_horarios', e.target.checked)} /> Mostrar horarios</label>
        </div>
      </div>

      <div className="flex justify-end">
        <button className="btn-primary" onClick={guardar} disabled={guardando}><Save size={16} /> {guardando ? 'Guardando...' : 'Guardar cambios'}</button>
      </div>
    </div>
  )
}
