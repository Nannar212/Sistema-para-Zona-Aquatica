import { useEffect, useState, useCallback } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell,
} from 'recharts'
import { TrendingUp, TrendingDown, Scale, Banknote, QrCode, Search } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../../services/api'
import { StatsCard, LoadingSpinner } from '../../components/ui'

const hoyISO = () => new Date().toISOString().slice(0, 10)
const primerDiaMes = () => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10) }
const COLORES = ['#ef9f27', '#185fa5', '#57a0e0', '#f4b53f', '#0c447c', '#85b7eb']

export default function FinanzasPage() {
  const [desde, setDesde] = useState(primerDiaMes())
  const [hasta, setHasta] = useState(hoyISO())
  const [data, setData] = useState(null)
  const [caja, setCaja] = useState(null)
  const [pagos, setPagos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [buscarCliente, setBuscarCliente] = useState('')
  const [filtroFecha, setFiltroFecha] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      const [r, c, p] = await Promise.all([
        api.get('/finanzas/resumen', { params: { desde, hasta } }),
        api.get('/pagos/resumen-hoy'),
        api.get('/pagos', { params: { desde, hasta } }),
      ])
      setData(r.data); setCaja(c.data); setPagos(p.data)
    } catch { toast.error('No se pudieron cargar las finanzas') } finally { setCargando(false) }
  }, [desde, hasta])
  useEffect(() => { cargar() }, [cargar])

  // Ingresos por cliente (con búsqueda por nombre y filtro por fecha de pago)
  const ingresosCliente = (() => {
    const agg = {}
    pagos.forEach((p) => {
      if (buscarCliente && !(p.cliente_nombre || '').toLowerCase().includes(buscarCliente.toLowerCase())) return
      if (filtroFecha && (p.fecha || '').slice(0, 10) !== filtroFecha) return
      const k = p.cliente_id
      if (!agg[k]) agg[k] = { cliente_id: k, cliente: p.cliente_nombre || '—', total: 0, efectivo: 0, qr: 0, cantidad: 0 }
      agg[k].total += p.monto
      agg[k].efectivo += p.monto_efectivo || 0
      agg[k].qr += p.monto_qr || 0
      agg[k].cantidad += 1
    })
    return Object.values(agg).sort((a, b) => b.total - a.total)
  })()
  const totalFiltrado = ingresosCliente.reduce((s, r) => s + r.total, 0)
  const totalEf = ingresosCliente.reduce((s, r) => s + r.efectivo, 0)
  const totalQr = ingresosCliente.reduce((s, r) => s + r.qr, 0)

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <TrendingUp className="text-aqua-500" />
        <h1 className="text-xl font-semibold text-slate-800">Ingresos y egresos</h1>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div><label className="label">Desde</label><input type="date" className="input" value={desde} onChange={(e) => setDesde(e.target.value)} /></div>
        <div><label className="label">Hasta</label><input type="date" className="input" value={hasta} onChange={(e) => setHasta(e.target.value)} /></div>
      </div>

      {cargando || !data ? <LoadingSpinner /> : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatsCard titulo="Ingresos" valor={`Bs ${data.ingresos_total}`} icono={TrendingUp} color="verde" />
            <StatsCard titulo="Egresos" valor={`Bs ${data.egresos_total}`} icono={TrendingDown} color="rojo" />
            <StatsCard titulo="Balance" valor={`Bs ${data.balance}`} icono={Scale} color={data.balance >= 0 ? 'aqua' : 'rojo'} />
            <StatsCard titulo="Ingresos del mes (efectivo/QR)" valor={`${data.ingresos_efectivo} / ${data.ingresos_qr}`} icono={Banknote} color="sol" />
          </div>

          {/* Cierre de caja de hoy */}
          {caja && (
            <div className="card">
              <h3 className="mb-3 font-medium text-slate-700">Cierre de caja de hoy ({caja.cantidad} pagos)</h3>
              <div className="grid grid-cols-3 gap-3 text-center">
                <div className="rounded-lg bg-slate-50 p-3"><p className="text-xs text-slate-500">Efectivo</p><p className="flex items-center justify-center gap-1 text-lg font-semibold text-slate-800"><Banknote size={16} /> Bs {caja.total_efectivo}</p></div>
                <div className="rounded-lg bg-slate-50 p-3"><p className="text-xs text-slate-500">QR</p><p className="flex items-center justify-center gap-1 text-lg font-semibold text-slate-800"><QrCode size={16} /> Bs {caja.total_qr}</p></div>
                <div className="rounded-lg bg-aqua-50 p-3"><p className="text-xs text-aqua-600">Total</p><p className="text-lg font-semibold text-aqua-700">Bs {caja.total}</p></div>
              </div>
            </div>
          )}

          <div className="grid gap-6 lg:grid-cols-2">
            <div className="card">
              <h3 className="mb-3 font-medium text-slate-700">Ingresos vs egresos por día</h3>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={data.serie}>
                  <XAxis dataKey="fecha" tick={{ fontSize: 10 }} tickFormatter={(f) => f.slice(5)} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="ingresos" name="Ingresos" fill="#185fa5" radius={[3, 3, 0, 0]} />
                  <Bar dataKey="egresos" name="Egresos" fill="#ef9f27" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="card">
              <h3 className="mb-3 font-medium text-slate-700">Egresos por categoría</h3>
              {data.egresos_categoria.length === 0 ? (
                <p className="py-8 text-center text-sm text-slate-400">Sin egresos en el período.</p>
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie data={data.egresos_categoria} dataKey="monto" nameKey="categoria" cx="50%" cy="50%" outerRadius={95} label>
                      {data.egresos_categoria.map((_, i) => <Cell key={i} fill={COLORES[i % COLORES.length]} />)}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          {/* Historial por día */}
          <div className="card">
            <h3 className="mb-3 font-medium text-slate-700">Historial por día</h3>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-2 text-left font-medium text-slate-500">Fecha</th>
                    <th className="px-4 py-2 text-right font-medium text-slate-500">Ingresos</th>
                    <th className="px-4 py-2 text-right font-medium text-slate-500">Egresos</th>
                    <th className="px-4 py-2 text-right font-medium text-slate-500">Balance</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {[...data.serie].reverse().filter((d) => d.ingresos > 0 || d.egresos > 0).map((d) => (
                    <tr key={d.fecha} className="hover:bg-aqua-50/40">
                      <td className="px-4 py-2 text-slate-600">{new Date(d.fecha + 'T00:00:00').toLocaleDateString()}</td>
                      <td className="px-4 py-2 text-right text-green-600">Bs {d.ingresos.toFixed(2)}</td>
                      <td className="px-4 py-2 text-right text-red-500">Bs {d.egresos.toFixed(2)}</td>
                      <td className={`px-4 py-2 text-right font-medium ${d.ingresos - d.egresos >= 0 ? 'text-slate-800' : 'text-red-600'}`}>Bs {(d.ingresos - d.egresos).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {data.serie.filter((d) => d.ingresos > 0 || d.egresos > 0).length === 0 && (
                <p className="py-6 text-center text-sm text-slate-400">Sin movimientos en el período.</p>
              )}
            </div>
          </div>

          {/* Ingresos por cliente */}
          <div className="card">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
              <h3 className="font-medium text-slate-700">Ingresos por cliente</h3>
              <div className="flex flex-wrap items-center gap-2">
                <div className="relative">
                  <Search size={16} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input className="input h-9 w-44 pl-8 text-sm" placeholder="Buscar cliente..." value={buscarCliente} onChange={(e) => setBuscarCliente(e.target.value)} />
                </div>
                <input type="date" className="input h-9 w-40 text-sm" value={filtroFecha} onChange={(e) => setFiltroFecha(e.target.value)} title="Fecha de pago" />
                {filtroFecha && <button className="text-xs text-aqua-600 hover:underline" onClick={() => setFiltroFecha('')}>Quitar fecha</button>}
              </div>
            </div>
            {ingresosCliente.length === 0 ? (
              <p className="py-6 text-center text-sm text-slate-400">Sin ingresos {buscarCliente || filtroFecha ? 'con esos filtros' : 'en el período'}.</p>
            ) : (
              <div className="max-h-96 overflow-y-auto overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="sticky top-0 bg-slate-50">
                    <tr>
                      <th className="px-4 py-2 text-left font-medium text-slate-500">Cliente</th>
                      <th className="px-4 py-2 text-right font-medium text-slate-500">Pagos</th>
                      <th className="px-4 py-2 text-right font-medium text-slate-500">Efectivo</th>
                      <th className="px-4 py-2 text-right font-medium text-slate-500">QR</th>
                      <th className="px-4 py-2 text-right font-medium text-slate-500">Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {ingresosCliente.map((r) => (
                      <tr key={r.cliente_id} className="hover:bg-aqua-50/40">
                        <td className="px-4 py-2 text-slate-700">{r.cliente}</td>
                        <td className="px-4 py-2 text-right text-slate-500">{r.cantidad}</td>
                        <td className="px-4 py-2 text-right text-slate-500">Bs {r.efectivo.toFixed(2)}</td>
                        <td className="px-4 py-2 text-right text-slate-500">Bs {r.qr.toFixed(2)}</td>
                        <td className="px-4 py-2 text-right font-medium text-green-600">Bs {r.total.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr className="border-t border-slate-200 bg-slate-50">
                      <td className="px-4 py-2 font-medium text-slate-600">Total</td>
                      <td></td>
                      <td className="px-4 py-2 text-right font-medium text-slate-600">Bs {totalEf.toFixed(2)}</td>
                      <td className="px-4 py-2 text-right font-medium text-slate-600">Bs {totalQr.toFixed(2)}</td>
                      <td className="px-4 py-2 text-right font-semibold text-green-700">Bs {totalFiltrado.toFixed(2)}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
