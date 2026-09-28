import { Link } from 'react-router-dom'
import { Waves } from 'lucide-react'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-100 p-4 text-center">
      <Waves size={48} className="text-aqua-400" />
      <h1 className="text-3xl font-semibold text-slate-800">404</h1>
      <p className="text-slate-500">La página que buscas no existe.</p>
      <Link to="/app" className="btn-primary mt-2">Ir al inicio</Link>
    </div>
  )
}
