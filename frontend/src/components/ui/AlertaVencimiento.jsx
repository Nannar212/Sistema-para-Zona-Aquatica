import { AlertTriangle } from 'lucide-react'
import Badge from './Badge'

export function textoVencimiento(dias) {
  if (dias <= 0) return 'Vence hoy'
  if (dias === 1) return 'Vence mañana'
  return `Vence en ${dias} días`
}

/** Alerta de membresía por vencer. `compacta` deja solo el ícono, para las tablas. */
export default function AlertaVencimiento({ dias, compacta = false }) {
  const texto = textoVencimiento(dias)
  if (compacta) {
    return (
      <span role="img" aria-label={texto} title={texto} className="shrink-0 text-amber-600">
        <AlertTriangle size={16} aria-hidden="true" />
      </span>
    )
  }
  return <Badge color="alerta"><AlertTriangle size={12} aria-hidden="true" /> {texto}</Badge>
}
