/**
 * cn — une clases de Tailwind resolviendo conflictos, de modo que la
 * última gana. Hace el papel de clsx + tailwind-merge sin dependencias:
 *
 *   cn('p-6 rounded-xl', condicion && 'bg-primary', className)
 *   cn('p-6', 'p-8')                  → 'p-8'
 *   cn('text-sm text-muted-foreground', 'text-foreground')
 *                                     → 'text-sm text-foreground'
 *
 * Esto importa porque en Tailwind el orden de las clases en el atributo
 * no decide nada: manda el orden en la hoja de estilos. Sin resolver el
 * conflicto, un <Card className="p-8"> se quedaría con el p-6 interno.
 */

// Tamaños de `text-*` que son tipografía y no color.
const TAMANOS_TEXTO = new Set([
  'xs', 'sm', 'base', 'lg', 'xl', '2xl', '3xl', '4xl', '5xl', '6xl', '7xl', '8xl', '9xl',
])

// Valores de `border-*` que son grosor y no color.
const GROSORES_BORDE = new Set(['0', '2', '4', '8'])

// Prefijos cuyo conjunto de utilidades es mutuamente excluyente.
// Se prueban de más largo a más corto, así `min-w-` gana sobre `w-`.
const PREFIJOS = [
  'min-w-', 'max-w-', 'min-h-', 'max-h-', 'w-', 'h-', 'size-',
  'px-', 'py-', 'pt-', 'pr-', 'pb-', 'pl-', 'ps-', 'pe-', 'p-',
  'mx-', 'my-', 'mt-', 'mr-', 'mb-', 'ml-', 'ms-', 'me-', 'm-',
  'space-x-', 'space-y-', 'gap-x-', 'gap-y-', 'gap-',
  'rounded-', 'border-x-', 'border-y-', 'border-t-', 'border-r-',
  'border-b-', 'border-l-', 'border-',
  'bg-', 'from-', 'via-', 'to-', 'fill-', 'stroke-',
  'font-', 'leading-', 'tracking-', 'align-', 'whitespace-',
  'shadow-', 'ring-offset-', 'ring-', 'opacity-', 'blur-',
  'grid-cols-', 'grid-rows-', 'col-span-', 'row-span-', 'order-',
  'items-', 'justify-', 'content-', 'self-', 'place-',
  'inset-x-', 'inset-y-', 'inset-', 'top-', 'right-', 'bottom-', 'left-', 'z-',
  'duration-', 'delay-', 'ease-',
  'flex-shrink-', 'flex-grow-', 'flex-basis-', 'shrink-', 'grow-', 'basis-', 'flex-',
  'overflow-x-', 'overflow-y-', 'overflow-', 'object-', 'aspect-',
  'cursor-', 'pointer-events-', 'select-',
  'divide-x-', 'divide-y-', 'divide-',
  'translate-x-', 'translate-y-', 'scale-', 'rotate-',
  'col-start-', 'col-end-', 'row-start-', 'row-end-',
]

// Utilidades sueltas que compiten entre sí dentro de un mismo grupo.
const GRUPOS_SUELTOS = {
  display: ['block', 'inline-block', 'inline', 'flex', 'inline-flex', 'grid', 'inline-grid', 'hidden', 'contents', 'table'],
  posicion: ['static', 'fixed', 'absolute', 'relative', 'sticky'],
  'flex-dir': ['flex-row', 'flex-row-reverse', 'flex-col', 'flex-col-reverse'],
  'flex-wrap': ['flex-wrap', 'flex-wrap-reverse', 'flex-nowrap'],
  'text-align': ['text-left', 'text-center', 'text-right', 'text-justify'],
  'font-style': ['italic', 'not-italic'],
  decoracion: ['underline', 'overline', 'line-through', 'no-underline'],
  'texto-overflow': ['truncate', 'text-ellipsis', 'text-clip'],
}

const SUELTOS = new Map()
for (const [grupo, clases] of Object.entries(GRUPOS_SUELTOS)) {
  for (const c of clases) SUELTOS.set(c, grupo)
}

// Utilidades sin valor que deben competir con su familia con valor:
// `rounded` choca con `rounded-2xl`, `border` con `border-2`, etc.
const SIN_VALOR = {
  rounded: 'rounded-',
  shadow: 'shadow-',
  ring: 'ring-grosor',
  border: 'border-grosor',
  blur: 'blur-',
}
for (const [c, k] of Object.entries(SIN_VALOR)) SUELTOS.set(c, k)

/** Clave de conflicto de una clase, o null si no compite con nada. */
function clave(clase) {
  // Separa variantes (hover:, sm:, focus-visible:…) del cuerpo: dos
  // utilidades solo chocan si comparten exactamente las variantes.
  const corte = clase.lastIndexOf(':')
  const variantes = corte === -1 ? '' : clase.slice(0, corte + 1)
  let cuerpo = corte === -1 ? clase : clase.slice(corte + 1)

  // El `!` de importante y el `-` de valores negativos no cambian el grupo.
  if (cuerpo.startsWith('!')) cuerpo = cuerpo.slice(1)
  const negativo = cuerpo.startsWith('-')
  if (negativo) cuerpo = cuerpo.slice(1)

  const suelto = SUELTOS.get(cuerpo)
  if (suelto) return `${variantes}${suelto}`

  if (cuerpo.startsWith('text-')) {
    const v = cuerpo.slice(5)
    return `${variantes}text-${TAMANOS_TEXTO.has(v) ? 'tam' : 'color'}`
  }
  if (cuerpo.startsWith('border-') && !cuerpo.startsWith('border-x-') && !cuerpo.startsWith('border-y-')) {
    const v = cuerpo.slice(7)
    if (GROSORES_BORDE.has(v)) return `${variantes}border-grosor`
  }
  // `ring-2` es grosor; `ring-ring` o `ring-brillo/20` es color.
  if (cuerpo.startsWith('ring-') && !cuerpo.startsWith('ring-offset-')) {
    const v = cuerpo.slice(5)
    if (v === 'inset' || /^\d+$/.test(v)) return `${variantes}ring-grosor`
    return `${variantes}ring-color`
  }

  for (const p of PREFIJOS) {
    if (cuerpo.startsWith(p)) return `${variantes}${p}`
  }
  return null
}

export function cn(...entradas) {
  const clases = entradas
    .flat(Infinity)
    .filter((e) => typeof e === 'string' && e.trim() !== '')
    .join(' ')
    .split(/\s+/)

  const resultado = []
  const posicion = new Map()   // clave de conflicto → índice en `resultado`
  const vistas = new Set()     // clases idénticas, para no repetirlas

  for (const c of clases) {
    const k = clave(c)
    if (k === null) {
      if (!vistas.has(c)) { vistas.add(c); resultado.push(c) }
      continue
    }
    if (posicion.has(k)) {
      resultado[posicion.get(k)] = c   // la última gana, en el mismo sitio
    } else {
      posicion.set(k, resultado.length)
      resultado.push(c)
    }
  }
  return resultado.join(' ')
}

export default cn
