const ESTILOS = {
  azul: 'bg-aqua-50 text-aqua-700',
  amarillo: 'bg-sol-50 text-sol-800',
  verde: 'bg-green-100 text-green-700',
  rojo: 'bg-red-100 text-red-700',
  gris: 'bg-slate-100 text-slate-600',
}

export default function Badge({ children, color = 'gris' }) {
  return <span className={`badge ${ESTILOS[color] || ESTILOS.gris}`}>{children}</span>
}
