import EmptyState from './EmptyState'

/**
 * columnas: [{ key, titulo, render?(fila) }]
 * datos: array de objetos
 */
export default function DataTable({ columnas, datos, vacio }) {
  if (!datos || datos.length === 0) {
    return vacio || <EmptyState titulo="Sin registros" />
  }
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50">
          <tr>
            {columnas.map((c) => (
              <th key={c.key} className="px-4 py-3 text-left font-medium text-slate-500">
                {c.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {datos.map((fila, i) => (
            <tr key={fila.id ?? i} className="hover:bg-aqua-50/40">
              {columnas.map((c) => (
                <td key={c.key} className="px-4 py-3 text-slate-700">
                  {c.render ? c.render(fila) : fila[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
