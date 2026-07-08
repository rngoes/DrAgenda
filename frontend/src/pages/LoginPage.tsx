export default function LoginPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[var(--bg-base)]">
      <div className="w-full max-w-sm px-4">
        {/* TODO Story 2.1: formulário de login completo */}
        <p className="text-center text-[var(--text-secondary)]">Login — em breve</p>
      </div>
      <footer className="mt-8 text-center">
        <a
          href="/politica"
          className="text-sm text-[var(--text-secondary)] underline hover:text-[var(--text-primary)]"
        >
          Política de Privacidade
        </a>
      </footer>
    </div>
  )
}
