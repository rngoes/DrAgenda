import { Link } from 'react-router-dom'

export default function AutoatendimentoPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[var(--bg-base)] px-4">
      <div className="w-full max-w-sm">
        {/* TODO Story 4.5: autoatendimento completo */}
        <p className="text-center text-[var(--text-secondary)]">Autoatendimento — em breve</p>
      </div>
      <footer className="mt-8 text-center">
        <Link
          to="/politica"
          className="text-sm text-[var(--text-secondary)] underline hover:text-[var(--text-primary)]"
        >
          Política de Privacidade
        </Link>
      </footer>
    </div>
  )
}
