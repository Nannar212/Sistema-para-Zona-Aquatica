export default function LoadingSpinner({ label = '' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-8">
      <div className="h-10 w-10 animate-spin rounded-full border-4 border-aqua-200 border-t-aqua-500" />
      {label && <p className="text-sm text-slate-500">{label}</p>}
    </div>
  )
}
