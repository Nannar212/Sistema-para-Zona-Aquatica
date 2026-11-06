import { forwardRef } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '../../lib/cn'

/**
 * Campos de formulario: Input, Textarea, Select, Label, Checkbox y
 * Field (etiqueta + control + error, el patrón que más se repite en el
 * panel). Misma estética de agua: esquina suave, borde que se ilumina
 * en cian y anillo de foco de 4px.
 */

const BASE_CAMPO =
  'w-full rounded-lg border border-input bg-card text-sm text-foreground ' +
  'placeholder:text-muted-foreground/70 ' +
  'transition-all duration-300 ease-agua ' +
  'focus:border-brillo focus:outline-none focus:ring-4 focus:ring-brillo/20 ' +
  'disabled:cursor-not-allowed disabled:bg-muted disabled:opacity-60'

export const Input = forwardRef(function Input({ className, error, ...props }, ref) {
  return (
    <input
      ref={ref}
      aria-invalid={error ? true : undefined}
      className={cn(
        BASE_CAMPO,
        'h-11 px-3.5',
        error && 'border-destructive focus:border-destructive focus:ring-destructive/20',
        className,
      )}
      {...props}
    />
  )
})

export const Textarea = forwardRef(function Textarea({ className, error, rows = 4, ...props }, ref) {
  return (
    <textarea
      ref={ref}
      rows={rows}
      aria-invalid={error ? true : undefined}
      className={cn(
        BASE_CAMPO,
        'resize-y px-3.5 py-2.5 leading-relaxed',
        error && 'border-destructive focus:border-destructive focus:ring-destructive/20',
        className,
      )}
      {...props}
    />
  )
})

/** Select nativo, con la flecha repintada para que no se vea del sistema. */
export const Select = forwardRef(function Select({ className, children, error, ...props }, ref) {
  return (
    <div className="relative">
      <select
        ref={ref}
        aria-invalid={error ? true : undefined}
        className={cn(
          BASE_CAMPO,
          'h-11 cursor-pointer appearance-none pl-3.5 pr-10',
          error && 'border-destructive focus:border-destructive focus:ring-destructive/20',
          className,
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown
        size={16}
        aria-hidden="true"
        className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-muted-foreground"
      />
    </div>
  )
})

export const Label = forwardRef(function Label({ className, obligatorio, children, ...props }, ref) {
  return (
    <label ref={ref} className={cn('block text-sm font-medium text-foreground', className)} {...props}>
      {children}
      {obligatorio && <span className="ml-0.5 text-destructive" aria-hidden="true">*</span>}
    </label>
  )
})

export const Checkbox = forwardRef(function Checkbox({ className, label, id, ...props }, ref) {
  const control = (
    <input
      ref={ref}
      id={id}
      type="checkbox"
      className={cn(
        'h-5 w-5 shrink-0 cursor-pointer rounded border-input text-primary',
        'transition-all duration-300 ease-agua',
        'focus:ring-4 focus:ring-brillo/20 focus:ring-offset-0',
        className,
      )}
      {...props}
    />
  )
  if (!label) return control
  return (
    <label htmlFor={id} className="flex cursor-pointer items-center gap-2.5 text-sm text-foreground">
      {control}
      {label}
    </label>
  )
})

/**
 * Etiqueta + control + mensaje de error, con el espaciado ya resuelto.
 *
 *   <Field label="Nombres" obligatorio error={errores.nombres}>
 *     <Input value={...} onChange={...} />
 *   </Field>
 */
export function Field({ label, obligatorio, error, ayuda, htmlFor, className, children }) {
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      {label && <Label htmlFor={htmlFor} obligatorio={obligatorio}>{label}</Label>}
      {children}
      {error
        ? <p className="text-xs font-medium text-destructive">{error}</p>
        : ayuda && <p className="text-xs text-muted-foreground">{ayuda}</p>}
    </div>
  )
}
