import { Inbox } from 'lucide-react'
import { cn } from '../../lib/cn'

/** Estado vacío: ícono en círculo de agua, título, mensaje y acción. */
export default function EmptyState({ titulo = 'Sin datos', mensaje = '', icono: Icono = Inbox, accion, className }) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-border',
        'bg-card px-6 py-16 text-center',
        className,
      )}
    >
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-accent text-primary">
        <Icono size={28} />
      </div>
      <p className="font-semibold text-foreground">{titulo}</p>
      {mensaje && <p className="max-w-sm text-sm leading-relaxed text-muted-foreground">{mensaje}</p>}
      {accion && <div className="mt-2">{accion}</div>}
    </div>
  )
}
