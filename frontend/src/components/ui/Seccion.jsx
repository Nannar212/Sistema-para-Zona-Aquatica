import { cn } from '../../lib/cn'
import Reveal from '../Reveal'

/**
 * Piezas de maquetación de la página pública: secciones con espaciado
 * generoso, divisores de ola entre ellas y la banda de olas animadas
 * del hero. Todas pintan con tokens, así que la ola siempre toma
 * exactamente el color de la sección que encabeza.
 */

// Una cresta por sección: el borde de arriba se dibuja con el color de
// fondo de la propia sección, de modo que "monta" sobre la anterior.
const CRESTA = 'M0,90 L0,48 C240,12 480,84 720,48 C960,12 1200,84 1440,48 L1440,90 Z'

// Perfil largo (dos ciclos) para poder desplazarlo en bucle sin cortes.
const OLA_LARGA =
  'M0,60 C240,110 480,10 720,60 C960,110 1200,10 1440,60 ' +
  'C1680,110 1920,10 2160,60 C2400,110 2640,10 2880,60 L2880,120 L0,120 Z'

const FONDOS = {
  card: 'bg-card',
  background: 'bg-background',
  muted: 'bg-muted',
  marino: 'bg-aqua-700 text-white',
}

/** Divisor de ola pegado al borde superior de una sección. */
export function OlaDivisor({ className }) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'pointer-events-none absolute inset-x-0 top-0 -translate-y-[98%] overflow-hidden leading-none',
        className,
      )}
    >
      <svg viewBox="0 0 1440 90" preserveAspectRatio="none" className="block h-10 w-full sm:h-16">
        <path fill="currentColor" d={CRESTA} />
      </svg>
    </div>
  )
}

/**
 * Banda de dos olas animadas, para cerrar el hero. Se le pasa el color
 * de la sección siguiente con `text-*` y la ola de atrás se pinta con
 * la misma tinta a media opacidad, que da el efecto de profundidad.
 */
export function OlasAnimadas({ className }) {
  return (
    <div
      aria-hidden="true"
      className={cn('pointer-events-none absolute inset-x-0 bottom-0 overflow-hidden leading-none', className)}
    >
      <svg className="ola-2 block h-14 w-[200%] sm:h-20" viewBox="0 0 2880 120" preserveAspectRatio="none">
        <path fill="currentColor" fillOpacity="0.4" d={OLA_LARGA} />
      </svg>
      <svg className="ola-1 -mt-10 block h-14 w-[200%] sm:h-20" viewBox="0 0 2880 120" preserveAspectRatio="none">
        <path fill="currentColor" d={OLA_LARGA} />
      </svg>
    </div>
  )
}

/**
 * Sección de la landing. El espaciado vertical es amplio a propósito
 * (py-20 / py-28 en pantallas grandes) para dar la sensación de aire.
 *
 *   <Seccion id="planes" fondo="card" ola>…</Seccion>
 */
export function Seccion({ id, fondo = 'card', ola = false, className, contenedor = true, children }) {
  const colorOla = { card: 'text-card', background: 'text-background', muted: 'text-muted', marino: 'text-aqua-700' }[fondo]
  return (
    <section id={id} className={cn('relative px-4 py-20 sm:px-6 sm:py-28', FONDOS[fondo], className)}>
      {ola && <OlaDivisor className={colorOla} />}
      {contenedor ? <div className="mx-auto w-full max-w-6xl">{children}</div> : children}
    </section>
  )
}

/** Antetítulo + título + bajada, centrados, con aparición escalonada. */
export function TituloSeccion({ antetitulo, titulo, descripcion, className, claro = false }) {
  return (
    <div className={cn('mx-auto mb-14 max-w-2xl text-center', className)}>
      {antetitulo && (
        <Reveal dir="up">
          <span
            className={cn(
              'mb-4 inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-xs font-semibold uppercase tracking-wider',
              claro ? 'bg-white/15 text-white' : 'bg-accent text-accent-foreground',
            )}
          >
            {antetitulo}
          </span>
        </Reveal>
      )}
      <Reveal dir="up" delay={70}>
        <h2 className={cn('text-3xl font-bold sm:text-4xl', claro ? 'text-white' : 'text-foreground')}>
          {titulo}
        </h2>
      </Reveal>
      {descripcion && (
        <Reveal dir="up" delay={140}>
          <p className={cn('mt-4 text-base leading-relaxed', claro ? 'text-aqua-100' : 'text-muted-foreground')}>
            {descripcion}
          </p>
        </Reveal>
      )}
    </div>
  )
}
