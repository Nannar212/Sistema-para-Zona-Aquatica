import { forwardRef } from 'react'
import { cn } from '../../lib/cn'

/**
 * Botón con variantes, al estilo shadcn/ui.
 *
 *   <Button>Registrar pago</Button>
 *   <Button variant="brillo" size="lg">Inscríbete</Button>
 *   <Button variant="outline" size="icon"><X size={18} /></Button>
 *   <Button as="a" href={waLink} target="_blank">WhatsApp</Button>
 *   <Button as={Link} to="/login">Ingresar</Button>
 *
 * `as` sustituye al patrón asChild de shadcn: evita traer Radix Slot y
 * basta para enlaces y <Link> de react-router.
 */

export const VARIANTES_BOTON = {
  // Turquesa profundo, texto blanco (4.70:1). La acción principal.
  default: 'bg-primary text-primary-foreground shadow-agua hover:bg-primary-hover hover:shadow-agua-md',
  // Cian brillante con texto marino (6.27:1). Para el CTA del hero.
  brillo: 'bg-brillo text-brillo-foreground shadow-agua hover:bg-brillo-hover hover:shadow-brillo',
  secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/70',
  outline: 'border border-border bg-card text-foreground hover:border-primary/40 hover:bg-accent hover:text-accent-foreground',
  ghost: 'text-primary hover:bg-accent hover:text-accent-foreground',
  destructive: 'bg-destructive text-destructive-foreground shadow-agua hover:bg-destructive/90 hover:shadow-agua-md',
  link: 'text-primary underline-offset-4 hover:underline',
  // Sobre fondos oscuros (hero con foto, footer marino).
  cristal: 'border border-white/30 bg-white/10 text-white backdrop-blur-sm hover:border-white/50 hover:bg-white/20',
}

export const TAMANOS_BOTON = {
  sm: 'h-9 gap-1.5 rounded-lg px-3 text-sm',
  default: 'h-11 gap-2 rounded-lg px-5 text-sm',
  lg: 'h-14 gap-2.5 rounded-xl px-7 text-base',
  icon: 'h-10 w-10 rounded-lg',
  'icon-sm': 'h-8 w-8 rounded-lg',
}

const BASE =
  'inline-flex shrink-0 items-center justify-center whitespace-nowrap font-medium ' +
  'transition-all duration-300 ease-agua ' +
  'focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-ring/30 ' +
  'active:scale-[0.98] ' +
  'disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none ' +
  '[&_svg]:shrink-0'

const Button = forwardRef(function Button(
  { className, variant = 'default', size = 'default', as: Tag = 'button', type, ...props },
  ref,
) {
  return (
    <Tag
      ref={ref}
      // Un <a> no acepta type, y un <button> sin type envía el formulario
      // que lo contenga sin querer.
      type={Tag === 'button' ? (type || 'button') : type}
      className={cn(
        BASE,
        VARIANTES_BOTON[variant] || VARIANTES_BOTON.default,
        TAMANOS_BOTON[size] || TAMANOS_BOTON.default,
        className,
      )}
      {...props}
    />
  )
})

export default Button
