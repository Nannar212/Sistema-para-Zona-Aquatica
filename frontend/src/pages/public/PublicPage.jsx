import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Waves, Thermometer, Baby, LifeBuoy, Droplet, Clock, Users, Sun, Star, Shield,
  MapPin, Phone, MessageCircle, Check,
} from 'lucide-react'
import api from '../../services/api'
import Reveal from '../../components/Reveal'
import Carrusel from '../../components/Carrusel'

const ICONOS = { temperature: Thermometer, kid: Baby, lifebuoy: LifeBuoy, droplet: Droplet, clock: Clock, users: Users, wave: Waves, sun: Sun, star: Star, shield: Shield }
const DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const OLA_PATH = 'M0,60 C240,110 480,10 720,60 C960,110 1200,10 1440,60 C1680,110 1920,10 2160,60 C2400,110 2640,10 2880,60 L2880,120 L0,120 Z'

// Íconos de redes (SVG en línea)
const Red = ({ tipo }) => {
  const p = {
    facebook: <path d="M18 2h-3a5 5 0 00-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 011-1h3z" />,
    instagram: <><rect x="2" y="2" width="20" height="20" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.5" cy="6.5" r="1" /></>,
    youtube: <><path d="M22.5 6.5a2.8 2.8 0 00-2-2C18.8 4 12 4 12 4s-6.8 0-8.5.5a2.8 2.8 0 00-2 2A29 29 0 001 12a29 29 0 00.5 5.5 2.8 2.8 0 002 2C5.2 20 12 20 12 20s6.8 0 8.5-.5a2.8 2.8 0 002-2A29 29 0 0023 12a29 29 0 00-.5-5.5z" /><polygon points="10 15 15 12 10 9" /></>,
    tiktok: <path d="M15 3c.3 2 1.6 3.6 4 4v3c-1.5 0-2.9-.5-4-1.3V15a5 5 0 11-5-5c.3 0 .7 0 1 .1v3.1A2 2 0 1013 15V3z" />,
  }[tipo]
  const fill = tipo === 'facebook' || tipo === 'tiktok'
  return <svg viewBox="0 0 24 24" width="18" height="18" fill={fill ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">{p}</svg>
}

// Divisor de ola en la parte superior de una sección (el relleno = color de la sección)
const OlaTop = ({ fill }) => (
  <div className="pointer-events-none absolute inset-x-0 top-0 -translate-y-[98%] overflow-hidden leading-none" aria-hidden="true">
    <svg viewBox="0 0 1440 90" preserveAspectRatio="none" className="block h-10 w-full sm:h-16">
      <path fill={fill} d="M0,90 L0,48 C240,12 480,84 720,48 C960,12 1200,84 1440,48 L1440,90 Z" />
    </svg>
  </div>
)

export default function PublicPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    api.get('/publico/contenido').then(({ data }) => setData(data)).catch(() => setError(true))
  }, [])

  // Carga el visor 3D (model-viewer) desde CDN solo si hay un modelo 3D
  useEffect(() => {
    if (data?.contenido?.modelo_3d_url && !document.getElementById('model-viewer-cdn')) {
      const s = document.createElement('script')
      s.type = 'module'
      s.id = 'model-viewer-cdn'
      s.src = 'https://cdn.jsdelivr.net/npm/@google/model-viewer/dist/model-viewer.min.js'
      document.head.appendChild(s)
    }
  }, [data])

  if (error) return <div className="flex min-h-screen items-center justify-center bg-aqua-600 text-white">No se pudo cargar la información.</div>
  if (!data) return <div className="flex min-h-screen items-center justify-center bg-aqua-600"><div className="h-10 w-10 animate-spin rounded-full border-4 border-white/30 border-t-white" /></div>

  const c = data.contenido || {}
  const planes = data.planes || []
  const horarios = data.horarios || []

  const digits = (c.whatsapp || '').replace(/\D/g, '')
  const waLink = c.whatsapp_link || (digits ? `https://wa.me/${digits}` : '')
  const mapsQuery = c.direccion || c.google_maps_link || ''
  const mapsEmbed = mapsQuery ? `https://www.google.com/maps?q=${encodeURIComponent(mapsQuery)}&z=15&output=embed` : ''
  const mapsClick = c.google_maps_link || (c.direccion ? `https://www.google.com/maps?q=${encodeURIComponent(c.direccion)}` : '')
  const redes = [['facebook', c.facebook], ['instagram', c.instagram], ['tiktok', c.tiktok], ['youtube', c.youtube]].filter(([, u]) => u)

  const Logo = ({ size = 36 }) => (
    c.logo_url
      ? <img src={c.logo_url} alt={c.nombre} className="rounded-full object-cover" style={{ height: size, width: size }} />
      : <div className="flex items-center justify-center rounded-full bg-aqua-500 text-white" style={{ height: size, width: size }}><Waves size={size * 0.5} /></div>
  )

  const Inscribete = ({ className, children }) => (
    waLink
      ? <a href={waLink} target="_blank" rel="noreferrer" className={className}>{children}</a>
      : <a href="#planes" className={className}>{children}</a>
  )

  const cardHover = 'transition duration-300 hover:-translate-y-1.5 hover:shadow-xl'

  return (
    <div className="min-h-screen overflow-x-hidden bg-white font-sans text-slate-800">
      {/* Navbar */}
      <header className="sticky top-0 z-30 border-b border-slate-100 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2">
            <Logo size={36} />
            <span className="font-semibold">{c.nombre || 'Zona Aquática'}</span>
          </div>
          <nav className="hidden items-center gap-6 text-sm text-slate-600 md:flex">
            <a href="#beneficios" className="hover:text-aqua-600">Beneficios</a>
            {c.mostrar_horarios && <a href="#horarios" className="hover:text-aqua-600">Horarios</a>}
            {c.mostrar_planes && <a href="#planes" className="hover:text-aqua-600">Planes</a>}
            {(c.modelo_3d_url || (c.galeria || []).length > 0) && <a href="#instalaciones" className="hover:text-aqua-600">Instalaciones</a>}
            <a href="#contacto" className="hover:text-aqua-600">Contacto</a>
          </nav>
          <Inscribete className="rounded-lg bg-sol-200 px-4 py-2 text-sm font-medium text-sol-800 transition hover:bg-sol-300">Inscríbete</Inscribete>
        </div>
      </header>

      {/* Hero con olas animadas */}
      <section
        className={`relative overflow-hidden px-4 pb-28 pt-16 text-center text-white ${c.hero_imagen_url ? '' : 'bg-aqua-500'}`}
        style={c.hero_imagen_url ? { backgroundImage: `url(${c.hero_imagen_url})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}}
      >
        {c.hero_imagen_url && <div className="absolute inset-0 bg-aqua-700/75" />}
        <Reveal className="relative mx-auto max-w-3xl" dir="up">
          {c.hero_badge && <span className="flotar mb-4 inline-block rounded-full bg-sol-200 px-4 py-1 text-xs font-medium text-sol-800">{c.hero_badge}</span>}
          <h1 className="mb-4 text-3xl font-bold leading-tight sm:text-5xl">{c.hero_titulo || 'Zona Aquática'}</h1>
          <p className="mx-auto mb-8 max-w-xl text-aqua-100">{c.hero_subtitulo}</p>
          <div className="flex flex-wrap justify-center gap-3">
            <a href="#planes" className="rounded-lg bg-sol-200 px-6 py-3 font-medium text-sol-800 transition hover:bg-sol-300 hover:shadow-lg">Ver planes</a>
            {waLink && <a href={waLink} target="_blank" rel="noreferrer" className="rounded-lg border border-aqua-200 px-6 py-3 font-medium text-white transition hover:bg-aqua-400/40"><span className="inline-flex items-center gap-2"><MessageCircle size={18} /> Inscríbete por WhatsApp</span></a>}
          </div>
        </Reveal>
        {/* Olas animadas */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 overflow-hidden leading-none">
          <svg className="ola-2 block h-14 w-[200%] sm:h-20" viewBox="0 0 2880 120" preserveAspectRatio="none"><path fill="#ffffff" fillOpacity="0.4" d={OLA_PATH} /></svg>
          <svg className="ola-1 -mt-10 block h-14 w-[200%] sm:h-20" viewBox="0 0 2880 120" preserveAspectRatio="none"><path fill="#ffffff" d={OLA_PATH} /></svg>
        </div>
      </section>

      {/* Estadísticas + Beneficios (blanco) */}
      <section id="beneficios" className="bg-white px-4 pb-16 pt-4">
        {(c.estadisticas || []).length > 0 && (
          <div className="mx-auto mb-14 flex max-w-5xl flex-wrap justify-around gap-6 text-center">
            {c.estadisticas.map((s, i) => (
              <Reveal key={i} dir="up" delay={i * 90}>
                <p className="text-3xl font-bold text-aqua-600">{s.valor}</p>
                <p className="text-sm text-aqua-500">{s.etiqueta}</p>
              </Reveal>
            ))}
          </div>
        )}
        {(c.destacados || []).length > 0 && (
          <div className="mx-auto max-w-6xl">
            <Reveal dir="up"><h2 className="mb-2 text-center text-2xl font-semibold sm:text-3xl">¿Por qué elegirnos?</h2></Reveal>
            <Reveal dir="up" delay={80}><p className="mb-8 text-center text-slate-500">{c.sobre_texto}</p></Reveal>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {c.destacados.map((d, i) => {
                const Icon = ICONOS[d.icono] || Droplet
                return (
                  <Reveal key={i} dir="up" delay={i * 90}>
                    <div className={`rounded-2xl border border-slate-100 p-6 text-center shadow-sm ${cardHover}`}>
                      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-aqua-50 text-aqua-500"><Icon size={26} /></div>
                      <h3 className="mt-3 font-medium">{d.titulo}</h3>
                      <p className="mt-1 text-sm text-slate-500">{d.texto}</p>
                    </div>
                  </Reveal>
                )
              })}
            </div>
          </div>
        )}
      </section>

      {/* Horarios (aqua-50) */}
      {c.mostrar_horarios && horarios.length > 0 && (
        <section id="horarios" className="relative bg-aqua-50 px-4 pb-16 pt-10">
          <OlaTop fill="#e6f1fb" />
          <div className="mx-auto max-w-5xl">
            <Reveal dir="up"><h2 className="mb-8 text-center text-2xl font-semibold sm:text-3xl">Horarios</h2></Reveal>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              {horarios.map((h, i) => (
                <Reveal key={h.id} dir="up" delay={i * 70}>
                  <div className={`rounded-2xl border border-slate-100 bg-white p-4 text-center shadow-sm ${cardHover}`}>
                    <p className="font-medium text-aqua-600">{h.hora_inicio}–{h.hora_fin}</p>
                    <p className="text-sm text-slate-700">{h.nombre}</p>
                    <div className="mt-2 flex flex-wrap justify-center gap-1">
                      {(h.dias_semana || []).map((v) => <span key={v} className="rounded bg-aqua-50 px-1.5 py-0.5 text-xs text-aqua-700">{DIAS[v]}</span>)}
                    </div>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Planes (blanco) */}
      {c.mostrar_planes && planes.length > 0 && (
        <section id="planes" className="relative bg-white px-4 pb-16 pt-10">
          <OlaTop fill="#ffffff" />
          <div className="mx-auto max-w-6xl">
            <Reveal dir="up"><h2 className="mb-2 text-center text-2xl font-semibold sm:text-3xl">Planes</h2></Reveal>
            <Reveal dir="up" delay={80}><p className="mb-8 text-center text-slate-500">Elige el plan que mejor se adapte a ti.</p></Reveal>
            <div className="grid items-stretch gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {planes.map((p, i) => {
                const destacado = i === 1
                const dias = (p.dias_acceso || []).map((v) => DIAS[v]).join(', ') || 'Lun–Vie'
                const feats = [
                  `${p.duracion_dias} días de acceso`,
                  `Días: ${dias}`,
                  'Pago en efectivo o QR',
                ]
                if (p.descripcion) feats.push(p.descripcion)
                return (
                  <Reveal key={p.id} dir="zoom" delay={i * 100} className="h-full">
                    <div className={`relative flex h-full flex-col rounded-2xl p-6 text-center ${cardHover} ${destacado
                      ? 'bg-aqua-600 text-white shadow-xl ring-2 ring-aqua-400 lg:-mt-2 lg:scale-[1.04]'
                      : 'border border-slate-100 bg-white shadow-sm'}`}>
                      {destacado && (
                        <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-sol-300 px-4 py-1 text-xs font-semibold text-sol-900 shadow">★ Más elegido</span>
                      )}
                      <div className={`mx-auto flex h-14 w-14 items-center justify-center rounded-2xl ${destacado ? 'bg-white/15 text-white' : 'bg-aqua-50 text-aqua-500'}`}>
                        <Waves size={26} />
                      </div>
                      <h3 className={`mt-3 text-lg font-semibold ${destacado ? 'text-white' : 'text-slate-800'}`}>{p.nombre}</h3>
                      <div className="mt-3 flex items-baseline justify-center gap-1">
                        <span className={`text-sm ${destacado ? 'text-aqua-100' : 'text-slate-400'}`}>Bs</span>
                        <span className={`text-4xl font-bold ${destacado ? 'text-white' : 'text-aqua-600'}`}>{p.precio}</span>
                      </div>
                      <p className={`text-sm ${destacado ? 'text-aqua-100' : 'text-slate-500'}`}>por {p.duracion_dias} días</p>
                      <div className={`my-4 border-t ${destacado ? 'border-white/20' : 'border-slate-100'}`} />
                      <ul className="mb-5 space-y-2 text-left text-sm">
                        {feats.map((f, k) => (
                          <li key={k} className="flex items-start gap-2">
                            <Check size={16} className={destacado ? 'mt-0.5 shrink-0 text-sol-200' : 'mt-0.5 shrink-0 text-aqua-500'} />
                            <span className={destacado ? 'text-aqua-50' : 'text-slate-600'}>{f}</span>
                          </li>
                        ))}
                      </ul>
                      <Inscribete className={`mt-auto rounded-lg px-4 py-2.5 text-sm font-semibold transition ${destacado
                        ? 'bg-sol-300 text-sol-900 hover:bg-sol-200'
                        : 'bg-aqua-500 text-white hover:bg-aqua-600'}`}>Inscribirme</Inscribete>
                    </div>
                  </Reveal>
                )
              })}
            </div>
          </div>
        </section>
      )}

      {/* Ubicación + Instalaciones (aqua-50) */}
      {(mapsEmbed || (c.galeria || []).length > 0 || c.modelo_3d_url) && (
        <section className="relative bg-aqua-50 px-4 pb-16 pt-10">
          <OlaTop fill="#e6f1fb" />
          <div className="mx-auto max-w-5xl space-y-14">
            {mapsEmbed && (
              <div>
                <Reveal dir="up"><h2 className="mb-6 text-center text-2xl font-semibold sm:text-3xl">Dónde estamos</h2></Reveal>
                <Reveal dir="up" delay={80}>
                  <div className="relative overflow-hidden rounded-2xl border border-slate-200 shadow-sm">
                    <iframe title="Ubicación" src={mapsEmbed} width="100%" height="320" loading="lazy" style={{ border: 0 }} referrerPolicy="no-referrer-when-downgrade" />
                    {mapsClick && <a href={mapsClick} target="_blank" rel="noreferrer" className="absolute inset-0" aria-label="Abrir en Google Maps" />}
                  </div>
                  {mapsClick && <div className="mt-3 text-center"><a href={mapsClick} target="_blank" rel="noreferrer" className="btn-primary inline-flex"><MapPin size={16} /> Cómo llegar</a></div>}
                </Reveal>
              </div>
            )}

            {(c.modelo_3d_url || (c.galeria || []).length > 0) && (
              <div id="instalaciones">
                <Reveal dir="up"><h2 className="mb-2 text-center text-2xl font-semibold sm:text-3xl">Nuestras instalaciones</h2></Reveal>
                <Reveal dir="up" delay={60}><p className="mb-8 text-center text-slate-500">Conoce la piscina por dentro antes de venir.</p></Reveal>
                {c.modelo_3d_url && (
                  <Reveal dir="zoom">
                    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                      <model-viewer
                        src={c.modelo_3d_url}
                        alt="Modelo 3D de la piscina"
                        camera-controls=""
                        auto-rotate=""
                        ar=""
                        shadow-intensity="1"
                        style={{ width: '100%', height: '440px', backgroundColor: '#eaf2fb' }}
                      ></model-viewer>
                    </div>
                    <p className="mt-2 text-center text-xs text-slate-500">Arrastra para girar · usa la rueda del mouse o pellizca para acercar</p>
                  </Reveal>
                )}
                {(c.galeria || []).length > 0 && (
                  <Reveal dir="zoom" className="mt-10 block"><Carrusel imagenes={c.galeria} /></Reveal>
                )}
              </div>
            )}
          </div>
        </section>
      )}

      {/* Footer (aqua-600) */}
      <footer id="contacto" className="relative bg-aqua-600 px-4 py-12 text-white">
        <OlaTop fill="#0c447c" />
        <Reveal className="mx-auto grid max-w-5xl gap-8 sm:grid-cols-2" dir="up">
          <div>
            <div className="mb-3 flex items-center gap-2">
              <Logo size={36} />
              <span className="text-lg font-semibold">{c.nombre || 'Zona Aquática'}</span>
            </div>
            <ul className="space-y-2 text-sm text-aqua-100">
              {c.direccion && <li className="flex items-center gap-2"><MapPin size={16} /> {c.direccion}</li>}
              {c.horario_texto && <li className="flex items-center gap-2"><Clock size={16} /> {c.horario_texto}</li>}
              {c.telefono && <li className="flex items-center gap-2"><Phone size={16} /> {c.telefono}</li>}
              {waLink && <li className="flex items-center gap-2"><MessageCircle size={16} /> <a href={waLink} target="_blank" rel="noreferrer" className="underline">WhatsApp</a></li>}
            </ul>
            {redes.length > 0 && (
              <div className="mt-4 flex gap-3">
                {redes.map(([tipo, url]) => (
                  <a key={tipo} href={url} target="_blank" rel="noreferrer" aria-label={tipo}
                    className="flex h-9 w-9 items-center justify-center rounded-full bg-white/15 transition hover:bg-white/25"><Red tipo={tipo} /></a>
                ))}
              </div>
            )}
          </div>
          <div className="flex flex-col items-start justify-center gap-3 sm:items-end">
            <p className="text-aqua-100">¿Listo para nadar?</p>
            <Inscribete className="rounded-lg bg-sol-200 px-6 py-3 font-medium text-sol-800 transition hover:bg-sol-300 hover:shadow-lg">Inscríbete hoy</Inscribete>
          </div>
        </Reveal>
        <div className="mx-auto mt-8 max-w-5xl border-t border-aqua-500 pt-4 text-center text-xs text-aqua-200">
          © {new Date().getFullYear()} {c.nombre || 'Zona Aquática'} · Quillacollo, Cochabamba ·{' '}
          <Link to="/login" className="underline hover:text-white">Ingreso del personal</Link>
        </div>
      </footer>
    </div>
  )
}
