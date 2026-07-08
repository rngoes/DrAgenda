import { useLogout } from '../shared/hooks/useLogout'
import { useAuth } from '../shared/hooks/useAuth'

export default function MenuPage() {
  const logout = useLogout()
  const { nome, perfil } = useAuth()

  return (
    <div className="min-h-screen bg-[var(--bg-base)] px-4 py-8">
      <div className="max-w-sm mx-auto">
        <h1 className="text-xl font-semibold text-[var(--text-primary)] mb-2">Menu</h1>
        {nome && (
          <p className="text-sm text-[var(--text-secondary)] mb-6">
            {nome} · {perfil}
          </p>
        )}
        <button
          onClick={logout}
          className="w-full border border-error text-error font-medium py-2 rounded-radius-md hover:bg-red-50 transition-colors"
        >
          Sair
        </button>
      </div>
    </div>
  )
}
