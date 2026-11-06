import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'
import { cn } from '../../lib/cn'

/**
 * Diálogo centrado con velo difuminado. Mantiene la API anterior
 * (open, onClose, title, children, maxWidth) y añade lo que faltaba:
 * cierre con Escape, bloqueo del scroll del fondo y foco inicial
 * dentro del diálogo.
 */
export default function Modal({ open, onClose, title, children, maxWidth = 'max-w-lg', pie }) {
  const panel = useRef(null)

  useEffect(() => {
    if (!open) return
    const alPulsar = (e) => { if (e.key === 'Escape') onClose?.() }
    document.addEventListener('keydown', alPulsar)

    // Bloquea el scroll del documento mientras el diálogo está abierto.
    const overflowPrevio = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    // Lleva el foco al diálogo para que Escape y el tabulador funcionen
    // sin obligar al usuario a hacer clic primero.
    panel.current?.focus()

    return () => {
      document.removeEventListener('keydown', alPulsar)
      document.body.style.overflow = overflowPrevio
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto p-4 sm:items-center"
      role="dialog"
      aria-modal="true"
      aria-label={typeof title === 'string' ? title : undefined}
    >
      <div
        className="fixed inset-0 bg-aqua-900/50 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        ref={panel}
        tabIndex={-1}
        className={cn(
          'relative w-full animate-aparecer-escala rounded-2xl border border-border bg-card shadow-agua-lg',
          'focus:outline-none',
          maxWidth,
        )}
      >
        <div className="flex items-start justify-between gap-4 border-b border-border px-6 py-4">
          <h3 className="text-base font-semibold text-foreground">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            className="-mr-1.5 -mt-0.5 rounded-lg p-1.5 text-muted-foreground transition-all duration-300 ease-agua hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-ring/30"
            aria-label="Cerrar"
          >
            <X size={18} />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
        {pie && <div className="flex justify-end gap-3 border-t border-border px-6 py-4">{pie}</div>}
      </div>
    </div>
  )
}
