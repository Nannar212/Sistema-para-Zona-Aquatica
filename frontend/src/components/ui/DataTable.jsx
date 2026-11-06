import EmptyState from './EmptyState'
import { cn } from '../../lib/cn'

/**
 * Tabla de datos.
 *   columnas: [{ key, titulo, render?(fila), alinear?: 'izq'|'centro'|'der', className? }]
 *   datos: array de objetos
 *   onFilaClick?: (fila) => void
 */
const ALINEAR = { izq: 'text-left', centro: 'text-center', der: 'text-right' }

export default function DataTable({ columnas, datos, vacio, onFilaClick, className }) {
  if (!datos || datos.length === 0) {
    return vacio || <EmptyState titulo="Sin registros" />
  }
  return (
    <div className={cn('overflow-hidden rounded-2xl border border-border bg-card shadow-agua', className)}>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-muted/60">
              {columnas.map((c) => (
                <th
                  key={c.key}
                  scope="col"
                  className={cn(
                    'whitespace-nowrap px-5 py-3.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground',
                    ALINEAR[c.alinear] || ALINEAR.izq,
                  )}
                >
                  {c.titulo}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {datos.map((fila, i) => (
              <tr
                key={fila.id ?? i}
                onClick={onFilaClick ? () => onFilaClick(fila) : undefined}
                className={cn(
                  'transition-colors duration-300 ease-agua hover:bg-accent/50',
                  onFilaClick && 'cursor-pointer',
                )}
              >
                {columnas.map((c) => (
                  <td
                    key={c.key}
                    className={cn('px-5 py-3.5 text-foreground', ALINEAR[c.alinear] || ALINEAR.izq, c.className)}
                  >
                    {c.render ? c.render(fila) : fila[c.key]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
