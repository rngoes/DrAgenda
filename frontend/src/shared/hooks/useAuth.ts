export type Perfil = 'ADMIN_SISTEMA' | 'ADMIN_EMPRESA' | 'STAFF' | 'PROFISSIONAL'

export interface AuthState {
  isAuthenticated: boolean
  token: string | null
  perfil: Perfil | null
  nome: string | null
  empresaId: number | null
}

export function useAuth(): AuthState {
  const token = localStorage.getItem('token')
  const perfil = localStorage.getItem('perfil') as Perfil | null
  const nome = localStorage.getItem('nome')
  const empresaIdRaw = localStorage.getItem('empresaId')
  return {
    isAuthenticated: !!token,
    token,
    perfil,
    nome,
    empresaId: empresaIdRaw ? Number(empresaIdRaw) : null,
  }
}
