export default function StatsCard({ titulo, valor, icono: Icono, color = 'aqua' }) {
  const fondo = {
    aqua: 'bg-aqua-50 text-aqua-600',
    sol: 'bg-sol-50 text-sol-600',
    verde: 'bg-green-50 text-green-600',
    rojo: 'bg-red-50 text-red-600',
  }[color] || 'bg-aqua-50 text-aqua-600'

  return (
    <div className="card flex items-center gap-4">
      {Icono && (
        <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${fondo}`}>
          <Icono size={24} />
        </div>
      )}
      <div>
        <p className="text-sm text-slate-500">{titulo}</p>
        <p className="text-2xl font-semibold text-slate-800">{valor}</p>
      </div>
    </div>
  )
}
