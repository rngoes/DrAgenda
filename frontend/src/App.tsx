import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './shared/components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import AgendaPage from './pages/AgendaPage'
import BuscarPage from './pages/BuscarPage'
import ClientesPage from './pages/ClientesPage'
import ConfiguracoesPage from './pages/ConfiguracoesPage'
import MenuPage from './pages/MenuPage'
import AutoatendimentoPage from './pages/AutoatendimentoPage'
import PoliticaPage from './pages/PoliticaPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/politica" element={<PoliticaPage />} />
        <Route path="/cancelar/:token" element={<AutoatendimentoPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/agenda" element={<AgendaPage />} />
          <Route path="/agenda/dia" element={<AgendaPage />} />
          <Route path="/agenda/semana" element={<AgendaPage />} />
          <Route path="/buscar" element={<BuscarPage />} />
          <Route path="/clientes/*" element={<ClientesPage />} />
          <Route path="/configuracoes/*" element={<ConfiguracoesPage />} />
          <Route path="/menu" element={<MenuPage />} />
        </Route>
        <Route path="/" element={<Navigate to="/agenda" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
