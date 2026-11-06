import { cn } from '../../lib/cn'

/**
 * Etiqueta de estado. Se conservan los nombres de color que ya usan las
 * pantallas del panel (azul, amarillo, verde, rojo, gris) y se añaden
 * los semánticos nuevos.
 */
const ESTILOS = {
  // Nombres heredados
  azul: 'bg-accent text-accent-foreground',
  amarillo: 'bg-cian-100 text-cian-700',
  verde: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-600/20',
  rojo: 'bg-red-50 text-red-700 ring-1 ring-red-600/20',
  gris: 'bg-muted text-muted-foreground ring-1 ring-border',
  // Nombres semánticos
  primario: 'bg-primary/10 text-primary ring-1 ring-primary/20',
  brillo: 'bg-brillo/15 text-cian-700 ring-1 ring-brillo/30',
  exito: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-600/20',
  alerta: 'bg-amber-50 text-amber-700 ring-1 ring-amber-600/20',
  peligro: 'bg-red-50 text-red-700 ring-1 ring-red-600/20',
  marino: 'bg-aqua-700 text-white',
}

export default function Badge({ children, color = 'gris', className, punto = false }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
        'transition-colors duration-300 ease-agua',
        ESTILOS[color] || ESTILOS.gris,
        className,
      )}
    >
      {punto && <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" aria-hidden="true" />}
      {children}
    </span>
  )
}
