import { useState, useEffect } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import api from '../services/api'

const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']
const DIAS_SEM = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const COLOR = {
  presente: 'bg-green-500 text-white',
  falta: 'bg-red-500 text-white',
  justificada: 'bg-yellow-500 text-yellow-950',
}

const dosDig = (n) => String(n).padStart(2, '0')

export default function CalendarioAsistencia({ clienteId }) {
  const hoy = new Date()
  const [ref, setRef] = useState({ y: hoy.getFullYear(), m: hoy.getMonth() })
  const [data, setData] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    const { y, m } = ref
    const ultimo = new Date(y, m + 1, 0).getDate()
    const desde = `${y}-${dosDig(m + 1)}-01`
    const hasta = `${y}-${dosDig(m + 1)}-${dosDig(ultimo)}`
    setCargando(true)
    api.get(`/asistencias/cliente/${clienteId}/rango`, { params: { desde, hasta } })
      .then(({ data }) => setData(data))
      .catch(() => setData(null))
      .finally(() => setCargando(false))
  }, [clienteId, ref])

  const cambiarMes = (delta) => setRef((r) => {
    const nueva = new Date(r.y, r.m + delta, 1)
    return { y: nueva.getFullYear(), m: nueva.getMonth() }
  })

  const { y, m } = ref
  const primerDia = (new Date(y, m, 1).getDay() + 6) % 7 // Lunes = 0
  const diasMes = new Date(y, m + 1, 0).getDate()
  const celdas = [...Array(primerDia).fill(null), ...Array.from({ length: diasMes }, (_, i) => i + 1)]
  const asis = data?.asistencias || {}
  const tot = data?.totales || { presente: 0, falta: 0, justificada: 0 }
  const esHoy = (d) => y === hoy.getFullYear() && m === hoy.getMonth() && d === hoy.getDate()

  return (
    <div>
      {/* Totales */}
      <div className="mb-4 grid grid-cols-3 gap-2 text-center">
        <div className="rounded-lg bg-green-50 py-2"><p className="text-2xl font-semibold text-green-600">{tot.presente}</p><p className="text-xs text-green-700">Asistencias</p></div>
        <div className="rounded-lg bg-red-50 py-2"><p className="text-2xl font-semibold text-red-500">{tot.falta}</p><p className="text-xs text-red-600">Faltas</p></div>
        <div className="rounded-lg bg-yellow-50 py-2"><p className="text-2xl font-semibold text-yellow-600">{tot.justificada}</p><p className="text-xs text-yellow-700">Justificadas</p></div>
      </div>

      {/* Navegación de mes */}
      <div className="mb-3 flex items-center justify-between">
        <button className="rounded-lg p-2 text-slate-500 hover:bg-slate-100" onClick={() => cambiarMes(-1)}><ChevronLeft size={18} /></button>
        <span className="font-medium text-slate-700">{MESES[m]} {y}</span>
        <button className="rounded-lg p-2 text-slate-500 hover:bg-slate-100" onClick={() => cambiarMes(1)}><ChevronRight size={18} /></button>
      </div>

      {/* Días de la semana */}
      <div className="grid grid-cols-7 gap-1 text-center text-xs font-medium text-slate-400">
        {DIAS_SEM.map((d) => <div key={d} className="py-1">{d}</div>)}
      </div>

      {/* Grilla */}
      <div className="grid grid-cols-7 gap-1">
        {celdas.map((d, i) => {
          if (d === null) return <div key={`b${i}`} />
          const iso = `${y}-${dosDig(m + 1)}-${dosDig(d)}`
          const estado = asis[iso]
          const clase = estado ? COLOR[estado] : 'text-slate-600'
          return (
            <div key={iso}
              className={`flex aspect-square items-center justify-center rounded-lg text-sm ${clase} ${esHoy(d) ? 'ring-2 ring-aqua-400' : ''}`}
              title={estado ? estado : ''}>
              {d}
            </div>
          )
        })}
      </div>

      {/* Leyenda */}
      <div className="mt-4 flex flex-wrap justify-center gap-4 text-xs text-slate-500">
        <span className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-green-500" /> Asistió</span>
        <span className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-red-500" /> Faltó</span>
        <span className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-yellow-500" /> Justificada</span>
      </div>

      {cargando && <p className="mt-2 text-center text-xs text-slate-400">Cargando…</p>}
    </div>
  )
}
