import { Link } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'

export default function NoAutorizadoPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-100 p-4 text-center">
      <ShieldAlert size={48} className="text-sol-400" />
      <h1 className="text-2xl font-semibold text-slate-800">Acceso no autorizado</h1>
      <p className="text-slate-500">No tienes permisos para ver esta sección.</p>
      <Link to="/app" className="btn-primary mt-2">Volver al inicio</Link>
    </div>
  )
}
