import { useEffect, useRef, useState, useCallback } from 'react'
import { ChevronLeft, ChevronRight, Image as ImageIcon } from 'lucide-react'

/**
 * Carrusel de imágenes con auto-avance, flechas, puntos y swipe táctil.
 */
export default function Carrusel({ imagenes = [], intervalo = 4500 }) {
  const fotos = imagenes.filter(Boolean)
  const [i, setI] = useState(0)
  const [pausado, setPausado] = useState(false)
  const touchX = useRef(null)

  const total = fotos.length
  const ir = useCallback((n) => setI((prev) => (n + total) % total), [total])
  const siguiente = useCallback(() => ir(i + 1), [i, ir])
  const anterior = useCallback(() => ir(i - 1), [i, ir])

  useEffect(() => {
    if (total <= 1 || pausado) return
    const t = setInterval(() => setI((prev) => (prev + 1) % total), intervalo)
    return () => clearInterval(t)
  }, [total, pausado, intervalo])

  if (total === 0) return null

  const onTouchStart = (e) => { touchX.current = e.touches[0].clientX }
  const onTouchEnd = (e) => {
    if (touchX.current == null) return
    const dx = e.changedTouches[0].clientX - touchX.current
    if (Math.abs(dx) > 40) (dx < 0 ? siguiente() : anterior())
    touchX.current = null
  }

  return (
    <div
      className="relative mx-auto max-w-4xl overflow-hidden rounded-2xl border border-border bg-card shadow-agua"
      onMouseEnter={() => setPausado(true)}
      onMouseLeave={() => setPausado(false)}
      onTouchStart={onTouchStart}
      onTouchEnd={onTouchEnd}
    >
      <div
        className="flex transition-transform duration-700 ease-out"
        style={{ transform: `translateX(-${i * 100}%)` }}
      >
        {fotos.map((url, idx) => (
          <div key={idx} className="w-full flex-shrink-0">
            <div className="aspect-[16/10] w-full bg-accent sm:aspect-[16/9]">
              {url
                ? <img src={url} alt={`Foto ${idx + 1}`} className="h-full w-full object-cover" />
                : <div className="flex h-full items-center justify-center text-primary/40"><ImageIcon size={30} /></div>}
            </div>
          </div>
        ))}
      </div>

      {total > 1 && (
        <>
          <button onClick={anterior} aria-label="Anterior"
            className="absolute left-3 top-1/2 -translate-y-1/2 rounded-full bg-card/90 p-2.5 text-primary shadow-agua-md backdrop-blur-sm transition-all duration-300 ease-agua hover:scale-105 hover:bg-card">
            <ChevronLeft size={20} />
          </button>
          <button onClick={siguiente} aria-label="Siguiente"
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full bg-card/90 p-2.5 text-primary shadow-agua-md backdrop-blur-sm transition-all duration-300 ease-agua hover:scale-105 hover:bg-card">
            <ChevronRight size={20} />
          </button>
          <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-2">
            {fotos.map((_, idx) => (
              <button key={idx} onClick={() => setI(idx)} aria-label={`Ir a la foto ${idx + 1}`}
                className={`h-2.5 rounded-full transition-all duration-300 ease-agua ${idx === i ? 'w-7 bg-brillo' : 'w-2.5 bg-white/70 hover:bg-white'}`} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
