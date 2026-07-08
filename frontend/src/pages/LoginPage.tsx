import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import api from '../shared/lib/axios'

const schema = z.object({
  email: z.string().email('Email inválido'),
  senha: z.string().min(1, 'Informe a senha'),
})
type LoginForm = z.infer<typeof schema>

export default function LoginPage() {
  const navigate = useNavigate()
  const [erro, setErro] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: LoginForm) => {
    setErro(null)
    try {
      const res = await api.post<{
        token: string; perfil: string; nome: string;
        empresaId: number | null; senhaTemporaria: boolean
      }>('/api/v1/auth/login', { email: data.email, senha: data.senha })

      const { token, perfil, nome, empresaId, senhaTemporaria } = res.data
      localStorage.setItem('token', token)
      localStorage.setItem('perfil', perfil)
      localStorage.setItem('nome', nome)
      localStorage.setItem('empresaId', String(empresaId ?? ''))
      localStorage.setItem('senhaTemporaria', String(senhaTemporaria))

      navigate(senhaTemporaria ? '/trocar-senha' : '/agenda', { replace: true })
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'response' in e) {
        const err = e as { response?: { status?: number } }
        if (err.response?.status === 401) {
          setErro('Email ou senha incorretos')
          return
        }
      }
      setErro('Erro ao conectar ao servidor. Tente novamente.')
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[var(--bg-base)] px-4">
      <div className="w-full max-w-sm">
        <h1 className="font-brand font-medium text-3xl text-center mb-8">
          <span className="font-light text-[var(--text-primary)]">Dr</span>
          <span className="text-brand-500">Agenda</span>
        </h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-[var(--text-primary)] mb-1">
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              className="w-full rounded-radius-md border border-[var(--border)] bg-[var(--bg-base)] px-3 py-2 text-base text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-brand-500"
              {...register('email')}
            />
            {errors.email && (
              <p className="mt-1 text-xs text-error">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="senha" className="block text-sm font-medium text-[var(--text-primary)] mb-1">
              Senha
            </label>
            <input
              id="senha"
              type="password"
              autoComplete="current-password"
              className="w-full rounded-radius-md border border-[var(--border)] bg-[var(--bg-base)] px-3 py-2 text-base text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-brand-500"
              {...register('senha')}
            />
            {errors.senha && (
              <p className="mt-1 text-xs text-error">{errors.senha.message}</p>
            )}
          </div>

          {erro && (
            <p className="text-sm text-error text-center" role="alert">{erro}</p>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-brand-500 hover:bg-brand-600 text-white font-medium py-2 rounded-radius-md transition-colors disabled:opacity-50"
          >
            {isSubmitting ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </div>

      <footer className="mt-8 text-center">
        <a href="/politica" className="text-sm text-[var(--text-secondary)] underline hover:text-[var(--text-primary)]">
          Política de Privacidade
        </a>
      </footer>
    </div>
  )
}
