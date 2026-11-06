import { forwardRef } from 'react'
import { cn } from '../../lib/cn'

/**
 * Tarjeta blanca sobre el fondo agua, al estilo shadcn/ui.
 *
 *   <Card>
 *     <CardHeader>
 *       <CardTitle>Planes</CardTitle>
 *       <CardDescription>Elige el que mejor te quede</CardDescription>
 *     </CardHeader>
 *     <CardContent>…</CardContent>
 *     <CardFooter>…</CardFooter>
 *   </Card>
 *
 * `flotante` añade la elevación al pasar el cursor; se usa en rejillas
 * de tarjetas (planes, beneficios), no en paneles de datos.
 */
export const Card = forwardRef(function Card({ className, flotante = false, ...props }, ref) {
  return (
    <div
      ref={ref}
      className={cn(
        'rounded-2xl border border-border bg-card text-card-foreground shadow-agua',
        'transition-all duration-300 ease-agua',
        flotante && 'hover:-translate-y-1.5 hover:border-brillo/30 hover:shadow-agua-lg',
        className,
      )}
      {...props}
    />
  )
})

export const CardHeader = forwardRef(function CardHeader({ className, ...props }, ref) {
  return <div ref={ref} className={cn('flex flex-col gap-1.5 p-6 pb-0', className)} {...props} />
})

export const CardTitle = forwardRef(function CardTitle({ className, as: Tag = 'h3', ...props }, ref) {
  return <Tag ref={ref} className={cn('text-lg font-semibold leading-tight', className)} {...props} />
})

export const CardDescription = forwardRef(function CardDescription({ className, ...props }, ref) {
  return <p ref={ref} className={cn('text-sm text-muted-foreground', className)} {...props} />
})

export const CardContent = forwardRef(function CardContent({ className, ...props }, ref) {
  return <div ref={ref} className={cn('p-6', className)} {...props} />
})

export const CardFooter = forwardRef(function CardFooter({ className, ...props }, ref) {
  return <div ref={ref} className={cn('flex items-center gap-3 p-6 pt-0', className)} {...props} />
})

export default Card
