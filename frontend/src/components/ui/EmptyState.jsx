import { Inbox } from 'lucide-react'

export default function EmptyState({ titulo = 'Sin datos', mensaje = '', icono: Icono = Inbox, accion }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-slate-300 bg-white py-12 text-center">
      <Icono size={36} className="text-slate-300" />
      <p className="font-medium text-slate-600">{titulo}</p>
      {mensaje && <p className="max-w-xs text-sm text-slate-400">{mensaje}</p>}
      {accion && <div className="mt-2">{accion}</div>}
    </div>
  )
}
