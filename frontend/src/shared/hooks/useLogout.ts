import { useNavigate } from 'react-router-dom'

export function useLogout() {
  const navigate = useNavigate()

  return () => {
    localStorage.removeItem('token')
    localStorage.removeItem('perfil')
    localStorage.removeItem('nome')
    localStorage.removeItem('empresaId')
    localStorage.removeItem('senhaTemporaria')
    navigate('/login', { replace: true })
  }
}
