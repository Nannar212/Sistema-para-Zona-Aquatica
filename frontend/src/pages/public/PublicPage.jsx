import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Waves, Thermometer, Baby, LifeBuoy, Droplet, Clock, Users, Sun, Star, Shield,
  MapPin, Phone, MessageCircle, Check, Menu, X, ChevronDown, ArrowRight, Sparkles,
} from 'lucide-react'
import api from '../../services/api'
import Reveal from '../../components/Reveal'
import Carrusel from '../../components/Carrusel'
import { Button, Card, LoadingSpinner, Seccion, TituloSeccion, OlasAnimadas } from '../../components/ui'
import { cn } from '../../lib/cn'

const ICONOS = {
  temperature: Thermometer, kid: Baby, lifebuoy: LifeBuoy, droplet: Droplet,
  clock: Clock, users: Users, wave: Waves, sun: Sun, star: Star, shield: Shield,
}
const DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

// Íconos de redes (SVG en línea, sin dependencia extra)
const Red = ({ tipo }) => {
  const p = {
    facebook: <path d="M18 2h-3a5 5 0 00-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 011-1h3z" />,
    instagram: <><rect x="2" y="2" width="20" height="20" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.5" cy="6.5" r="1" /></>,
    youtube: <><path d="M22.5 6.5a2.8 2.8 0 00-2-2C18.8 4 12 4 12 4s-6.8 0-8.5.5a2.8 2.8 0 00-2 2A29 29 0 001 12a29 29 0 00.5 5.5 2.8 2.8 0 002 2C5.2 20 12 20 12 20s6.8 0 8.5-.5a2.8 2.8 0 002-2A29 29 0 0023 12a29 29 0 00-.5-5.5z" /><polygon points="10 15 15 12 10 9" /></>,
    tiktok: <path d="M15 3c.3 2 1.6 3.6 4 4v3c-1.5 0-2.9-.5-4-1.3V15a5 5 0 11-5-5c.3 0 .7 0 1 .1v3.1A2 2 0 1013 15V3z" />,
  }[tipo]
  const relleno = tipo === 'facebook' || tipo === 'tiktok'
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill={relleno ? 'currentColor' : 'none'}
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {p}
    </svg>
  )
}

export default function PublicPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(false)
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [scrolleado, setScrolleado] = useState(false)

  useEffect(() => {
    api.get('/publico/contenido').then(({ data }) => setData(data)).catch(() => setError(true))
  }, [])

  // El navbar arranca transparente sobre el hero y se vuelve sólido al bajar.
  useEffect(() => {
    const alScrollear = () => setScrolleado(window.scrollY > 24)
    alScrollear()
    window.addEventListener('scroll', alScrollear, { passive: true })
    return () => window.removeEventListener('scroll', alScrollear)
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

  // Con el menú móvil abierto, el fondo no debe poder scrollear.
  useEffect(() => {
    if (!menuAbierto) return
    const previo = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = previo }
  }, [menuAbierto])

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-aqua-700 px-6 text-center text-white">
        <Waves size={44} className="text-brillo" />
        <p className="text-lg font-medium">No se pudo cargar la información.</p>
        <p className="max-w-sm text-sm text-aqua-100">
          Revisa tu conexión e inténtalo de nuevo en un momento.
        </p>
        <Button variant="brillo" className="mt-2" onClick={() => window.location.reload()}>
          Reintentar
        </Button>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-aqua-700">
        <LoadingSpinner claro label="Cargando…" />
      </div>
    )
  }

  const c = data.contenido || {}
  const planes = data.planes || []
  const horarios = data.horarios || []

  const digitos = (c.whatsapp || '').replace(/\D/g, '')
  const waLink = c.whatsapp_link || (digitos ? `https://wa.me/${digitos}` : '')
  const consultaMapa = c.direccion || c.google_maps_link || ''
  const mapaEmbed = consultaMapa
    ? `https://www.google.com/maps?q=${encodeURIComponent(consultaMapa)}&z=15&output=embed`
    : ''
  const mapaClick = c.google_maps_link || (c.direccion ? `https://www.google.com/maps?q=${encodeURIComponent(c.direccion)}` : '')
  const redes = [['facebook', c.facebook], ['instagram', c.instagram], ['tiktok', c.tiktok], ['youtube', c.youtube]]
    .filter(([, u]) => u)

  const hayInstalaciones = Boolean(c.modelo_3d_url) || (c.galeria || []).length > 0
  const hayHorarios = c.mostrar_horarios && horarios.length > 0
  const hayPlanes = c.mostrar_planes && planes.length > 0
  const hayBeneficios = (c.estadisticas || []).length > 0 || (c.destacados || []).length > 0
  const nombre = c.nombre || 'Zona Aquática'

  // Los fondos alternan blanco / agua clara, pero el admin puede apagar
  // secciones enteras desde el panel. Si se asignara el color a mano, al
  // ocultar una quedarían dos secciones del mismo tono seguidas y la ola
  // divisoria se pintaría del color equivocado. Así que la alternancia se
  // calcula sobre las secciones que de verdad se renderizan.
  const secciones = [
    hayBeneficios && 'beneficios',
    hayHorarios && 'horarios',
    hayPlanes && 'planes',
    hayInstalaciones && 'instalaciones',
    mapaEmbed && 'ubicacion',
  ].filter(Boolean)

  const fondoDe = {}
  secciones.forEach((s, i) => { fondoDe[s] = i % 2 === 0 ? 'card' : 'background' })

  // La primera sección no lleva cresta: el hero ya cierra con olas animadas.
  const propsSeccion = (clave) => ({ fondo: fondoDe[clave], ola: secciones[0] !== clave })

  // Color de las olas del hero = fondo de la sección que viene justo debajo.
  const colorOlaHero = secciones.length === 0
    ? 'text-aqua-700'
    : fondoDe[secciones[0]] === 'card' ? 'text-card' : 'text-background'

  // Y la cresta de la banda de cierre debe montar sobre la última sección.
  const olaCierre = secciones.length > 0

  const enlaces = [
    { href: '#beneficios', label: 'Beneficios', mostrar: (c.destacados || []).length > 0 },
    { href: '#horarios', label: 'Horarios', mostrar: hayHorarios },
    { href: '#planes', label: 'Planes', mostrar: hayPlanes },
    { href: '#instalaciones', label: 'Instalaciones', mostrar: hayInstalaciones },
    { href: '#contacto', label: 'Contacto', mostrar: true },
  ].filter((e) => e.mostrar)

  const Logo = ({ size = 40, className }) => (
    c.logo_url
      ? <img src={c.logo_url} alt={nombre} className={cn('rounded-full object-cover', className)} style={{ height: size, width: size }} />
      : (
        <div
          className={cn('flex items-center justify-center rounded-full bg-gradient-to-br from-brillo to-primary text-white', className)}
          style={{ height: size, width: size }}
        >
          <Waves size={size * 0.5} />
        </div>
      )
  )

  // El CTA lleva a WhatsApp si está configurado; si no, baja a los planes.
  const Inscribete = ({ children, ...props }) => (
    waLink
      ? <Button as="a" href={waLink} target="_blank" rel="noreferrer" {...props}>{children}</Button>
      : <Button as="a" href={hayPlanes ? '#planes' : '#contacto'} {...props}>{children}</Button>
  )

  // Resalta la última palabra del titular con el degradado de agua.
  const titular = (c.hero_titulo || nombre).trim()
  const corte = titular.lastIndexOf(' ')
  const titularInicio = corte > 0 ? titular.slice(0, corte) : titular
  const titularFinal = corte > 0 ? titular.slice(corte + 1) : ''

  return (
    <div className="min-h-screen overflow-x-hidden bg-background font-sans text-foreground">
      {/* ─────────────── Navbar ─────────────── */}
      <header
        className={cn(
          'fixed inset-x-0 top-0 z-40 transition-all duration-300 ease-agua',
          scrolleado
            ? 'border-b border-border bg-card/90 py-2 shadow-agua backdrop-blur-md'
            : 'border-b border-transparent py-4',
        )}
      >
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <a href="#inicio" className="flex items-center gap-2.5">
            <Logo size={38} />
            <span className={cn('font-semibold transition-colors duration-300', scrolleado ? 'text-foreground' : 'text-white')}>
              {nombre}
            </span>
          </a>

          <nav className="hidden items-center gap-1 md:flex">
            {enlaces.map((e) => (
              <a
                key={e.href}
                href={e.href}
                className={cn(
                  'rounded-lg px-3.5 py-2 text-sm font-medium transition-all duration-300 ease-agua',
                  scrolleado
                    ? 'text-muted-foreground hover:bg-accent hover:text-primary'
                    : 'text-aqua-100 hover:bg-white/10 hover:text-white',
                )}
              >
                {e.label}
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <Inscribete variant={scrolleado ? 'default' : 'brillo'} size="sm" className="hidden sm:inline-flex">
              Inscríbete
            </Inscribete>
            <button
              type="button"
              onClick={() => setMenuAbierto(true)}
              aria-label="Abrir menú"
              className={cn(
                'rounded-lg p-2 transition-colors duration-300 md:hidden',
                scrolleado ? 'text-foreground hover:bg-muted' : 'text-white hover:bg-white/10',
              )}
            >
              <Menu size={22} />
            </button>
          </div>
        </div>
      </header>

      {/* ─────────────── Menú móvil ─────────────── */}
      {menuAbierto && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div className="absolute inset-0 bg-aqua-900/60 backdrop-blur-sm" onClick={() => setMenuAbierto(false)} />
          <div className="absolute inset-y-0 right-0 flex w-80 max-w-[85%] animate-aparecer-escala flex-col bg-card p-6 shadow-agua-lg">
            <div className="mb-8 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Logo size={36} />
                <span className="font-semibold">{nombre}</span>
              </div>
              <button
                type="button"
                onClick={() => setMenuAbierto(false)}
                aria-label="Cerrar menú"
                className="rounded-lg p-2 text-muted-foreground transition-colors duration-300 hover:bg-muted hover:text-foreground"
              >
                <X size={20} />
              </button>
            </div>
            <nav className="flex flex-col gap-1">
              {enlaces.map((e) => (
                <a
                  key={e.href}
                  href={e.href}
                  onClick={() => setMenuAbierto(false)}
                  className="flex items-center justify-between rounded-xl px-4 py-3.5 text-base font-medium text-foreground transition-all duration-300 ease-agua hover:bg-accent hover:text-primary"
                >
                  {e.label}
                  <ArrowRight size={16} className="text-muted-foreground" />
                </a>
              ))}
            </nav>
            <div className="mt-auto flex flex-col gap-3 pt-6">
              <Inscribete size="lg" className="w-full" onClick={() => setMenuAbierto(false)}>
                {waLink && <MessageCircle size={18} />}
                Inscríbete
              </Inscribete>
              <Link
                to="/login"
                className="text-center text-sm text-muted-foreground underline-offset-4 hover:underline"
              >
                Ingreso del personal
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* ─────────────── Hero ─────────────── */}
      <section id="inicio" className="relative isolate overflow-hidden px-4 pb-36 pt-36 text-center sm:px-6 sm:pb-44 sm:pt-44">
        {/* Fondo: foto con velo marino, o degradado de agua profunda */}
        {c.hero_imagen_url ? (
          <>
            <div
              className="absolute inset-0 -z-20 bg-cover bg-center"
              style={{ backgroundImage: `url(${c.hero_imagen_url})` }}
              aria-hidden="true"
            />
            <div className="absolute inset-0 -z-10 bg-gradient-to-b from-aqua-800/85 via-aqua-700/80 to-aqua-700/95" aria-hidden="true" />
          </>
        ) : (
          <>
            <div className="absolute inset-0 -z-20 bg-gradient-to-br from-aqua-800 via-aqua-700 to-primary" aria-hidden="true" />
            {/* Dos luces difusas: dan la sensación de sol entrando al agua */}
            <div className="absolute -left-24 top-0 -z-10 h-96 w-96 rounded-full bg-brillo/25 blur-3xl" aria-hidden="true" />
            <div className="absolute -right-20 bottom-10 -z-10 h-80 w-80 rounded-full bg-cian-300/20 blur-3xl" aria-hidden="true" />
          </>
        )}

        {/* Burbujas decorativas */}
        <div className="pointer-events-none absolute inset-0 -z-10 overflow-hidden" aria-hidden="true">
          <span className="flotar absolute left-[12%] top-1/3 h-3 w-3 rounded-full bg-white/25" />
          <span className="flotar absolute left-[22%] top-2/3 h-2 w-2 rounded-full bg-white/20" style={{ animationDelay: '0.8s' }} />
          <span className="flotar absolute right-[16%] top-1/4 h-4 w-4 rounded-full bg-white/20" style={{ animationDelay: '1.4s' }} />
          <span className="flotar absolute right-[26%] top-3/4 h-2.5 w-2.5 rounded-full bg-white/25" style={{ animationDelay: '2s' }} />
        </div>

        <Reveal className="relative mx-auto max-w-3xl" dir="up">
          {c.hero_badge && (
            <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-4 py-1.5 text-xs font-semibold uppercase tracking-wider text-white backdrop-blur-sm">
              <Sparkles size={14} className="text-brillo" />
              {c.hero_badge}
            </span>
          )}

          <h1 className="text-4xl font-bold leading-[1.1] text-white sm:text-6xl">
            {titularInicio}
            {titularFinal && <> <span className="texto-agua">{titularFinal}</span></>}
          </h1>

          {c.hero_subtitulo && (
            <p className="mx-auto mt-6 max-w-xl text-lg leading-relaxed text-aqua-100">{c.hero_subtitulo}</p>
          )}

          <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
            {hayPlanes && (
              <Button as="a" href="#planes" variant="brillo" size="lg">
                Ver planes
                <ArrowRight size={18} />
              </Button>
            )}
            {waLink && (
              <Button as="a" href={waLink} target="_blank" rel="noreferrer" variant="cristal" size="lg">
                <MessageCircle size={18} />
                Inscríbete por WhatsApp
              </Button>
            )}
          </div>
        </Reveal>

        {/* Pista de scroll */}
        {enlaces.length > 0 && (
          <a
            href={enlaces[0].href}
            aria-label={`Ir a ${enlaces[0].label}`}
            className="absolute bottom-28 left-1/2 hidden -translate-x-1/2 text-white/60 transition-colors duration-300 hover:text-white sm:block"
          >
            <ChevronDown size={26} className="flotar" />
          </a>
        )}

        {/* Olas animadas: toman el color de la sección siguiente */}
        <OlasAnimadas className={colorOlaHero} />
      </section>

      {/* ─────────────── Estadísticas + Beneficios ─────────────── */}
      {((c.estadisticas || []).length > 0 || (c.destacados || []).length > 0) && (
        <Seccion id="beneficios" {...propsSeccion('beneficios')} className="pt-12 sm:pt-16">
          {(c.estadisticas || []).length > 0 && (
            <div className="mb-20 grid grid-cols-2 gap-6 lg:grid-cols-4">
              {c.estadisticas.map((s, i) => (
                <Reveal key={i} dir="up" delay={i * 90}>
                  <div className="text-center">
                    <p className="bg-gradient-to-br from-primary to-brillo bg-clip-text text-4xl font-bold text-transparent sm:text-5xl">
                      {s.valor}
                    </p>
                    <p className="mt-2 text-sm font-medium text-muted-foreground">{s.etiqueta}</p>
                  </div>
                </Reveal>
              ))}
            </div>
          )}

          {(c.destacados || []).length > 0 && (
            <>
              <TituloSeccion
                antetitulo="Por qué elegirnos"
                titulo="Un lugar pensado para aprender a nadar"
                descripcion={c.sobre_texto}
              />
              <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                {c.destacados.map((d, i) => {
                  const Icono = ICONOS[d.icono] || Droplet
                  return (
                    <Reveal key={i} dir="up" delay={i * 90} className="h-full">
                      <Card flotante className="h-full p-7 text-center">
                        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-accent to-brillo/20 text-primary">
                          <Icono size={28} />
                        </div>
                        <h3 className="mt-5 text-base font-semibold text-foreground">{d.titulo}</h3>
                        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{d.texto}</p>
                      </Card>
                    </Reveal>
                  )
                })}
              </div>
            </>
          )}
        </Seccion>
      )}

      {/* ─────────────── Horarios ─────────────── */}
      {hayHorarios && (
        <Seccion id="horarios" {...propsSeccion('horarios')}>
          <TituloSeccion
            antetitulo="Horarios"
            titulo="Encuentra tu turno"
            descripcion="Elige la hora que mejor te acomode. Los cupos son limitados por turno."
          />
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {horarios.map((h, i) => (
              <Reveal key={h.id} dir="up" delay={i * 70} className="h-full">
                <Card flotante className="h-full p-6">
                  <div className="flex items-center gap-2 text-primary">
                    <Clock size={16} />
                    <p className="text-lg font-bold tracking-tight">{h.hora_inicio}–{h.hora_fin}</p>
                  </div>
                  <p className="mt-1.5 text-sm font-medium text-foreground">{h.nombre}</p>
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {(h.dias_semana || []).map((v) => (
                      <span key={v} className="rounded-md bg-accent px-2 py-0.5 text-xs font-medium text-accent-foreground">
                        {DIAS[v]}
                      </span>
                    ))}
                  </div>
                </Card>
              </Reveal>
            ))}
          </div>
        </Seccion>
      )}

      {/* ─────────────── Planes ─────────────── */}
      {hayPlanes && (
        <Seccion id="planes" {...propsSeccion('planes')}>
          <TituloSeccion
            antetitulo="Planes"
            titulo="Elige tu plan y empieza esta semana"
            descripcion="Sin inscripción ni costos escondidos. Pagas en efectivo o por QR."
          />
          <div className="grid items-stretch gap-7 sm:grid-cols-2 lg:grid-cols-3">
            {planes.map((p, i) => {
              const destacado = planes.length > 1 && i === 1
              const dias = (p.dias_acceso || []).map((v) => DIAS[v]).join(', ') || 'Lun–Vie'
              const caracteristicas = [
                `${p.duracion_dias} días de acceso`,
                `Días: ${dias}`,
                'Pago en efectivo o QR',
              ]
              if (p.descripcion) caracteristicas.push(p.descripcion)

              return (
                <Reveal key={p.id} dir="zoom" delay={i * 100} className="h-full">
                  <div
                    className={cn(
                      'destello relative flex h-full flex-col overflow-hidden rounded-2xl p-8 text-center',
                      'transition-all duration-300 ease-agua hover:-translate-y-1.5',
                      destacado
                        ? 'bg-gradient-to-br from-aqua-700 to-aqua-800 text-white shadow-agua-lg ring-2 ring-brillo lg:-mt-4 lg:scale-[1.04]'
                        : 'border border-border bg-card shadow-agua hover:border-brillo/30 hover:shadow-agua-lg',
                    )}
                  >
                    {destacado && (
                      <span className="absolute left-1/2 top-0 -translate-x-1/2 rounded-b-xl bg-brillo px-4 py-1.5 text-xs font-bold text-brillo-foreground shadow-brillo">
                        ★ MÁS ELEGIDO
                      </span>
                    )}

                    <div
                      className={cn(
                        'mx-auto flex h-16 w-16 items-center justify-center rounded-2xl',
                        destacado ? 'mt-4 bg-white/15 text-brillo' : 'bg-gradient-to-br from-accent to-brillo/20 text-primary',
                      )}
                    >
                      <Waves size={28} />
                    </div>

                    <h3 className={cn('mt-5 text-xl font-semibold', destacado ? 'text-white' : 'text-foreground')}>
                      {p.nombre}
                    </h3>

                    <div className="mt-5 flex items-baseline justify-center gap-1.5">
                      <span className={cn('text-sm font-medium', destacado ? 'text-aqua-100' : 'text-muted-foreground')}>Bs</span>
                      <span className={cn('text-5xl font-bold tracking-tight', destacado ? 'text-white' : 'text-primary')}>
                        {p.precio}
                      </span>
                    </div>
                    <p className={cn('mt-1 text-sm', destacado ? 'text-aqua-100' : 'text-muted-foreground')}>
                      por {p.duracion_dias} días
                    </p>

                    <div className={cn('my-7 border-t', destacado ? 'border-white/15' : 'border-border')} />

                    <ul className="mb-8 space-y-3 text-left text-sm">
                      {caracteristicas.map((f, k) => (
                        <li key={k} className="flex items-start gap-2.5">
                          <span
                            className={cn(
                              'mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full',
                              destacado ? 'bg-brillo/20 text-brillo' : 'bg-accent text-primary',
                            )}
                          >
                            <Check size={12} strokeWidth={3} />
                          </span>
                          <span className={destacado ? 'text-aqua-50' : 'text-muted-foreground'}>{f}</span>
                        </li>
                      ))}
                    </ul>

                    <Inscribete
                      variant={destacado ? 'brillo' : 'default'}
                      size="lg"
                      className="mt-auto w-full"
                    >
                      Inscribirme
                    </Inscribete>
                  </div>
                </Reveal>
              )
            })}
          </div>
        </Seccion>
      )}

      {/* ─────────────── Instalaciones ─────────────── */}
      {hayInstalaciones && (
        <Seccion id="instalaciones" {...propsSeccion('instalaciones')}>
          <TituloSeccion
            antetitulo="Instalaciones"
            titulo="Conoce la piscina por dentro"
            descripcion="Mira cómo es antes de venir: agua templada, carriles señalizados y zona para los más chicos."
          />

          {c.modelo_3d_url && (
            <Reveal dir="zoom">
              <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-agua">
                <model-viewer
                  src={c.modelo_3d_url}
                  alt="Modelo 3D de la piscina"
                  camera-controls=""
                  auto-rotate=""
                  ar=""
                  shadow-intensity="1"
                  style={{ width: '100%', height: '460px', backgroundColor: '#eff8fb' }}
                ></model-viewer>
              </div>
              <p className="mt-3 text-center text-xs text-muted-foreground">
                Arrastra para girar · usa la rueda del mouse o pellizca para acercar
              </p>
            </Reveal>
          )}

          {(c.galeria || []).length > 0 && (
            <Reveal dir="zoom" className={cn('block', c.modelo_3d_url && 'mt-12')}>
              <Carrusel imagenes={c.galeria} />
            </Reveal>
          )}
        </Seccion>
      )}

      {/* ─────────────── Ubicación ─────────────── */}
      {mapaEmbed && (
        <Seccion {...propsSeccion('ubicacion')}>
          <TituloSeccion antetitulo="Ubicación" titulo="Dónde estamos" descripcion={c.direccion} />
          <Reveal dir="up">
            <div className="relative overflow-hidden rounded-2xl border border-border shadow-agua">
              <iframe
                title="Ubicación"
                src={mapaEmbed}
                width="100%"
                height="380"
                loading="lazy"
                style={{ border: 0, display: 'block' }}
                referrerPolicy="no-referrer-when-downgrade"
              />
              {mapaClick && (
                <a href={mapaClick} target="_blank" rel="noreferrer" className="absolute inset-0" aria-label="Abrir en Google Maps" />
              )}
            </div>
            {mapaClick && (
              <div className="mt-6 text-center">
                <Button as="a" href={mapaClick} target="_blank" rel="noreferrer" size="lg">
                  <MapPin size={18} />
                  Cómo llegar
                </Button>
              </div>
            )}
          </Reveal>
        </Seccion>
      )}

      {/* ─────────────── Banda de cierre ─────────────── */}
      <Seccion fondo="marino" ola={olaCierre} className="overflow-hidden py-20 sm:py-24">
        <div className="pointer-events-none absolute -right-16 -top-16 h-72 w-72 rounded-full bg-brillo/15 blur-3xl" aria-hidden="true" />
        <Reveal dir="up" className="relative text-center">
          <h2 className="text-3xl font-bold text-white sm:text-4xl">¿Listo para meterte al agua?</h2>
          <p className="mx-auto mt-4 max-w-xl text-aqua-100">
            Escríbenos y reservamos tu cupo en el turno que elijas. Te respondemos el mismo día.
          </p>
          <div className="mt-9 flex flex-wrap justify-center gap-4">
            <Inscribete variant="brillo" size="lg">
              {waLink ? <><MessageCircle size={18} /> Escríbenos por WhatsApp</> : 'Quiero inscribirme'}
            </Inscribete>
            {c.telefono && (
              <Button as="a" href={`tel:${c.telefono.replace(/\s/g, '')}`} variant="cristal" size="lg">
                <Phone size={18} />
                {c.telefono}
              </Button>
            )}
          </div>
        </Reveal>
      </Seccion>

      {/* ─────────────── Footer ─────────────── */}
      <footer id="contacto" className="bg-aqua-800 px-4 py-16 text-white sm:px-6">
        <div className="mx-auto max-w-6xl">
          <div className="grid gap-12 sm:grid-cols-2 lg:grid-cols-3">
            <div>
              <div className="mb-5 flex items-center gap-2.5">
                <Logo size={42} />
                <span className="text-lg font-semibold">{nombre}</span>
              </div>
              <p className="text-sm leading-relaxed text-aqua-100">
                Escuela de natación en Quillacollo, Cochabamba. Clases para niños y adultos,
                todo el año.
              </p>
              {redes.length > 0 && (
                <div className="mt-6 flex gap-3">
                  {redes.map(([tipo, url]) => (
                    <a
                      key={tipo}
                      href={url}
                      target="_blank"
                      rel="noreferrer"
                      aria-label={tipo}
                      className="flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white transition-all duration-300 ease-agua hover:-translate-y-0.5 hover:bg-brillo hover:text-brillo-foreground"
                    >
                      <Red tipo={tipo} />
                    </a>
                  ))}
                </div>
              )}
            </div>

            <div>
              <h3 className="mb-5 text-sm font-semibold uppercase tracking-wider text-brillo">Contacto</h3>
              <ul className="space-y-3.5 text-sm text-aqua-100">
                {c.direccion && (
                  <li className="flex items-start gap-2.5">
                    <MapPin size={16} className="mt-0.5 shrink-0 text-brillo" />
                    {mapaClick
                      ? <a href={mapaClick} target="_blank" rel="noreferrer" className="transition-colors duration-300 hover:text-white">{c.direccion}</a>
                      : c.direccion}
                  </li>
                )}
                {c.horario_texto && (
                  <li className="flex items-start gap-2.5">
                    <Clock size={16} className="mt-0.5 shrink-0 text-brillo" />
                    {c.horario_texto}
                  </li>
                )}
                {c.telefono && (
                  <li className="flex items-start gap-2.5">
                    <Phone size={16} className="mt-0.5 shrink-0 text-brillo" />
                    <a href={`tel:${c.telefono.replace(/\s/g, '')}`} className="transition-colors duration-300 hover:text-white">
                      {c.telefono}
                    </a>
                  </li>
                )}
                {waLink && (
                  <li className="flex items-start gap-2.5">
                    <MessageCircle size={16} className="mt-0.5 shrink-0 text-brillo" />
                    <a href={waLink} target="_blank" rel="noreferrer" className="transition-colors duration-300 hover:text-white">
                      WhatsApp
                    </a>
                  </li>
                )}
              </ul>
            </div>

            {enlaces.length > 0 && (
              <div>
                <h3 className="mb-5 text-sm font-semibold uppercase tracking-wider text-brillo">Secciones</h3>
                <ul className="space-y-3.5 text-sm text-aqua-100">
                  {enlaces.map((e) => (
                    <li key={e.href}>
                      <a href={e.href} className="transition-colors duration-300 hover:text-white">{e.label}</a>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>

          <div className="mt-14 flex flex-col items-center gap-3 border-t border-white/10 pt-7 text-center text-xs text-aqua-200 sm:flex-row sm:justify-between sm:text-left">
            <p>© {new Date().getFullYear()} {nombre} · Quillacollo, Cochabamba</p>
            <Link to="/login" className="underline-offset-4 transition-colors duration-300 hover:text-white hover:underline">
              Ingreso del personal
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
