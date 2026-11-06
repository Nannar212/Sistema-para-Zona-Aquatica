import { Search, X } from 'lucide-react'
import { cn } from '../../lib/cn'
import { Input } from './Campos'

/** Buscador con lupa y botón de limpiar cuando hay texto. */
export default function SearchBar({ value, onChange, placeholder = 'Buscar...', className }) {
  return (
    <div className={cn('relative w-full max-w-sm', className)}>
      <Search
        size={18}
        aria-hidden="true"
        className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground"
      />
      <Input
        type="search"
        className="pl-11 pr-10"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label="Limpiar búsqueda"
          className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-muted-foreground transition-colors duration-300 hover:bg-muted hover:text-foreground"
        >
          <X size={15} />
        </button>
      )}
    </div>
  )
}
