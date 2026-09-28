import { useEffect, useRef, useState } from 'react'

/**
 * Envuelve contenido para que aparezca con animación al entrar en pantalla.
 * dir: 'up' | 'left' | 'right' | 'zoom'
 * delay: retraso en ms (para efecto escalonado)
 */
export default function Reveal({ children, dir = 'up', delay = 0, className = '', as: Tag = 'div' }) {
  const ref = useRef(null)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (typeof IntersectionObserver === 'undefined') { setVisible(true); return }

    // Alterna visible/oculto: el efecto se repite cada vez que el elemento
    // vuelve a entrar en pantalla (al subir y bajar de nuevo).
    const obs = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => setVisible(e.isIntersecting))
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [])

  const dirClass = dir === 'left' ? 'reveal-left' : dir === 'right' ? 'reveal-right' : dir === 'zoom' ? 'reveal-zoom' : ''

  return (
    <Tag
      ref={ref}
      className={`reveal ${dirClass} ${visible ? 'is-visible' : ''} ${className}`}
      style={{ transitionDelay: `${delay}ms` }}
    >
      {children}
    </Tag>
  )
}
