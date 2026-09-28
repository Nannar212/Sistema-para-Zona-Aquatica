import { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import api from '../../services/api'

export default function MainLayout() {
  const [desktopOpen, setDesktopOpen] = useState(true)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [logo, setLogo] = useState('')

  useEffect(() => {
    api.get('/publico/contenido')
      .then(({ data }) => setLogo(data.contenido?.logo_url || ''))
      .catch(() => {})
  }, [])

  const toggle = () => {
    if (window.matchMedia('(min-width: 1024px)').matches) setDesktopOpen((v) => !v)
    else setMobileOpen((v) => !v)
  }

  return (
    <div className="flex h-screen bg-slate-100">
      {/* Sidebar escritorio (colapsable) */}
      {desktopOpen && (
        <div className="hidden lg:block">
          <Sidebar logo={logo} />
        </div>
      )}

      {/* Sidebar móvil (cajón) */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 flex lg:hidden">
          <div className="absolute inset-0 bg-slate-900/50" onClick={() => setMobileOpen(false)} />
          <div className="relative z-50">
            <Sidebar logo={logo} onNavigate={() => setMobileOpen(false)} />
          </div>
        </div>
      )}

      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar onMenu={toggle} />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
