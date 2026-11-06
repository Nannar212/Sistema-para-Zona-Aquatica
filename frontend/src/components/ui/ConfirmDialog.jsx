import { AlertTriangle, HelpCircle } from 'lucide-react'
import Modal from './Modal'
import Button from './Button'

/** Confirmación de una acción. `peligro` la pinta en rojo. */
export default function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title = 'Confirmar',
  mensaje = '¿Estás seguro?',
  textoConfirmar = 'Confirmar',
  peligro = false,
}) {
  const Icono = peligro ? AlertTriangle : HelpCircle
  return (
    <Modal open={open} onClose={onClose} title={title} maxWidth="max-w-md">
      <div className="flex gap-4">
        <div
          className={
            peligro
              ? 'flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-red-50 text-destructive'
              : 'flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-accent text-primary'
          }
        >
          <Icono size={20} />
        </div>
        <p className="pt-2 text-sm leading-relaxed text-muted-foreground">{mensaje}</p>
      </div>
      <div className="mt-6 flex justify-end gap-3">
        <Button variant="outline" onClick={onClose}>Cancelar</Button>
        <Button
          variant={peligro ? 'destructive' : 'default'}
          onClick={() => { onConfirm(); onClose() }}
        >
          {textoConfirmar}
        </Button>
      </div>
    </Modal>
  )
}
