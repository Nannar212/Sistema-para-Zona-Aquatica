import { cn } from '../../lib/cn'

/** Rueda de carga. `claro` la invierte para fondos marinos. */
export default function LoadingSpinner({ label = '', claro = false, className }) {
  return (
    <div className={cn('flex flex-col items-center justify-center gap-3 py-10', className)} role="status">
      <div
        className={cn(
          'h-10 w-10 animate-spin rounded-full border-4',
          claro ? 'border-white/25 border-t-white' : 'border-accent border-t-brillo',
        )}
      />
      {label && (
        <p className={cn('text-sm', claro ? 'text-white/80' : 'text-muted-foreground')}>{label}</p>
      )}
      <span className="sr-only">Cargando</span>
    </div>
  )
}
