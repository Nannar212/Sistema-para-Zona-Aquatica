import { cn } from '../../lib/cn'
import { Card } from './Card'

/**
 * Métrica del panel: ícono en pastilla de color, rótulo y cifra grande.
 * Admite `delta` ('+12% vs. mes pasado') y `pie` para una nota al pie.
 */
const TONOS = {
  aqua: 'bg-accent text-primary',
  sol: 'bg-brillo/15 text-cian-700',
  cian: 'bg-brillo/15 text-cian-700',
  verde: 'bg-emerald-50 text-emerald-600',
  rojo: 'bg-red-50 text-red-600',
  marino: 'bg-aqua-700 text-white',
}

export default function StatsCard({ titulo, valor, icono: Icono, color = 'aqua', delta, pie, className }) {
  return (
    <Card flotante className={cn('flex items-start gap-4 p-5', className)}>
      {Icono && (
        <div
          className={cn(
            'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl',
            'transition-transform duration-300 ease-agua',
            TONOS[color] || TONOS.aqua,
          )}
        >
          <Icono size={22} />
        </div>
      )}
      <div className="min-w-0">
        <p className="text-sm font-medium text-muted-foreground">{titulo}</p>
        <p className="mt-0.5 text-2xl font-bold tracking-tight text-foreground">{valor}</p>
        {delta && <p className="mt-1 text-xs font-medium text-emerald-600">{delta}</p>}
        {pie && <p className="mt-1 text-xs text-muted-foreground">{pie}</p>}
      </div>
    </Card>
  )
}
