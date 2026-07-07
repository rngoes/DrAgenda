# Story 1.3: Fundação do Frontend — Design Tokens, Roteamento e Layout Base

Status: ready-for-dev

## Story

Como usuário do DrAgenda,
Quero uma interface com identidade visual consistente, navegação estruturada por perfil e um QueryClient global configurado,
Para que o produto seja reconhecível, navegável e pronto para receber as telas de funcionalidade nos Epics seguintes.

## Acceptance Criteria

**AC-1 — Design tokens no Tailwind**
- Given o frontend é carregado em qualquer dispositivo
- When os estilos são aplicados
- Then o Tailwind CSS usa os tokens customizados definidos em `tailwind.config.ts`:
  - Cor de marca: `brand-500: #0D9488`, `brand-400: #14B8A6`, `brand-600: #0F766E`, `brand-50: #F0FDFA`
  - Escala neutra completa: `bg-base`, `bg-subtle`, `bg-muted`, `border`, `text-primary`, `text-secondary`, `text-disabled`, `overlay`
  - 6 cores de status: `status-pending: #D97706`, `status-confirmed: #2563EB`, `status-present: #7C3AED`, `status-done: #16A34A`, `status-cancelled: #4B5563`, `status-noshow: #DC2626`
  - Cores semânticas: `success`, `error`, `warning`, `info`
  - Tokens de sombra: `shadow-sm`, `shadow-md`, `shadow-lg`, `shadow-xl`
  - Tokens de border-radius: `radius-sm` (4px) a `radius-full` (9999px)
- And suporte a modo claro e escuro via `prefers-color-scheme` (class `dark` no `<html>`)

**AC-2 — Fontes Inter e DM Sans**
- Given o frontend carrega
- When os estilos de tipografia são aplicados
- Then a fonte Inter é carregada via Google Fonts (ou local) como font-family padrão para toda interface
- And DM Sans é carregada para uso no wordmark "DrAgenda"
- And campos de formulário e labels usam mínimo 16px (evita zoom automático iOS/Android)

**AC-3 — Roteamento protegido por perfil**
- Given o usuário acessa qualquer rota autenticada sem estar logado
- When o React Router avalia a rota
- Then é redirecionado para `/login`

- Given o usuário está autenticado
- When navega entre `/agenda`, `/buscar`, `/clientes`, `/configuracoes`, `/menu`
- Then o React Router renderiza o componente correto sem recarregamento de página

**AC-4 — TanStack Query configurado globalmente**
- Given o frontend inicializa
- When o `QueryClientProvider` é montado no `main.tsx`
- Then o `QueryClient` está configurado com `defaultOptions: { queries: { staleTime: 30000, retry: 2 } }`

**AC-5 — Axios com interceptor JWT**
- Given o usuário está autenticado e faz qualquer chamada à API
- When a requisição é enviada via `shared/lib/axios.ts`
- Then o header `Authorization: Bearer {token}` é injetado automaticamente a partir do `localStorage`
- And se a resposta for 401, o token é removido do localStorage e o usuário é redirecionado para `/login`

**AC-6 — Build de produção limpo**
- Given o frontend está pronto para deploy
- When `npm run build` é executado
- Then o build completa sem erros de TypeScript (`tsc --noEmit`)
- And sem warnings de ESLint
- And o `manifest.json` PWA está presente (configurado na Story 1.1)

## Tasks / Subtasks

- [ ] **Task 1 — Configurar Tailwind com todos os tokens** (AC-1)
  - [ ] Editar `frontend/tailwind.config.ts` adicionando `theme.extend.colors` com todas as cores de marca, status, neutros e semânticas
  - [ ] Adicionar `theme.extend.boxShadow` com `shadow-sm/md/lg/xl`
  - [ ] Adicionar `theme.extend.borderRadius` com `radius-sm/md/lg/xl/full`
  - [ ] Configurar `darkMode: 'class'` no Tailwind para ativar via classe `dark` no `<html>`
  - [ ] Criar `src/styles/globals.css` com CSS variables para tokens de neutros (light/dark) e lógica `@media (prefers-color-scheme: dark)` que adiciona classe `dark` ao `<html>`

- [ ] **Task 2 — Carregar fontes Inter e DM Sans** (AC-2)
  - [ ] Adicionar Google Fonts import em `index.html`: `Inter` (400, 500, 600, 700) e `DM Sans` (300, 500)
  - [ ] Definir `fontFamily: { sans: ['Inter', 'sans-serif'], brand: ['DM Sans', 'sans-serif'] }` no `tailwind.config.ts`

- [ ] **Task 3 — Configurar TanStack Query** (AC-4)
  - [ ] Instalar: `npm install @tanstack/react-query`
  - [ ] Criar `src/shared/lib/queryClient.ts` com `QueryClient` configurado: `staleTime: 30000`, `retry: 2`, `refetchOnWindowFocus: false`
  - [ ] Envolver `<App />` com `<QueryClientProvider client={queryClient}>` em `main.tsx`

- [ ] **Task 4 — Configurar Axios com interceptor JWT** (AC-5)
  - [ ] Instalar: `npm install axios`
  - [ ] Criar `src/shared/lib/axios.ts` com `baseURL: import.meta.env.VITE_API_URL`
  - [ ] Adicionar interceptor de request: injeta `Authorization: Bearer {token}` do `localStorage.getItem('token')`
  - [ ] Adicionar interceptor de response: em 401, chama `localStorage.removeItem('token')` + `window.location.href = '/login'`

- [ ] **Task 5 — Configurar React Router e rotas protegidas** (AC-3)
  - [ ] Instalar: `npm install react-router-dom`
  - [ ] Criar `src/shared/hooks/useAuth.ts` que lê token/perfil do `localStorage` e expõe `{ isAuthenticated, perfil, nome }`
  - [ ] Criar `src/shared/components/ProtectedRoute.tsx` que usa `useAuth` e redireciona para `/login` se não autenticado
  - [ ] Criar `src/App.tsx` com `<BrowserRouter>` + `<Routes>` cobrindo:
    - `/login` → `<LoginPage />` (stub)
    - `/politica` → `<PoliticaPage />` (stub, sem ProtectedRoute)
    - `/cancelar/:token` → `<AutoatendimentoPage />` (stub, sem ProtectedRoute)
    - `/agenda` → `<ProtectedRoute> <AgendaPage />` (stub)
    - `/buscar` → `<ProtectedRoute> <BuscarPage />` (stub, somente Staff)
    - `/clientes/*` → `<ProtectedRoute> <ClientesPage />` (stub, somente Staff)
    - `/configuracoes/*` → `<ProtectedRoute> <ConfiguracoesPage />` (stub, somente Admin Empresa)
    - `/menu` → `<ProtectedRoute> <MenuPage />` (stub)
    - `/` → redirect para `/agenda`

- [ ] **Task 6 — Criar stubs de todas as páginas** (AC-3, AC-6)
  - [ ] Criar `src/pages/LoginPage.tsx` — stub: `<div>Login</div>`
  - [ ] Criar `src/pages/AgendaPage.tsx` — stub
  - [ ] Criar `src/pages/BuscarPage.tsx` — stub
  - [ ] Criar `src/pages/ClientesPage.tsx` — stub
  - [ ] Criar `src/pages/ConfiguracoesPage.tsx` — stub
  - [ ] Criar `src/pages/MenuPage.tsx` — stub
  - [ ] Criar `src/pages/AutoatendimentoPage.tsx` — stub
  - [ ] Criar `src/pages/PoliticaPage.tsx` — stub com texto placeholder de LGPD

- [ ] **Task 7 — Configurar ESLint + TypeScript strict** (AC-6)
  - [ ] Verificar que `tsconfig.json` tem `"strict": true` e `"noImplicitAny": true`
  - [ ] Configurar `eslint.config.js` (ou `.eslintrc`) com regras React + TypeScript
  - [ ] Adicionar script `"lint": "eslint src --max-warnings 0"` ao `package.json`
  - [ ] Adicionar script `"type-check": "tsc --noEmit"` ao `package.json`
  - [ ] Garantir que `ci-frontend.yml` executa `npm run lint` e `npm run type-check`

- [ ] **Task 8 — Instalar Shadcn/ui base** (dependência para Epics futuros)
  - [ ] Executar `npx shadcn-ui@latest init` e aceitar defaults (style: default, baseColor: slate, CSS variables: yes)
  - [ ] Instalar componentes base necessários nas próximas stories: `npx shadcn-ui@latest add button dialog sheet toast badge`
  - [ ] Verificar que os componentes são copiados para `src/shared/components/ui/` (não são dependência npm — são copiados)

## Dev Notes

### Pacotes a Instalar

```bash
# Na pasta frontend/
npm install @tanstack/react-query axios react-router-dom lucide-react
npm install -D @tanstack/eslint-plugin-query @typescript-eslint/eslint-plugin
npx shadcn-ui@latest init
npx shadcn-ui@latest add button dialog sheet toast badge
```

> **Lucide React** é instalado aqui porque será usado extensivamente nos Epics seguintes (StatusBadge, cards, etc.). Melhor instalar agora do que gerenciar como dependência futura.

### `tailwind.config.ts` — Tokens Completos

```typescript
import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Marca
        brand: {
          50:  '#F0FDFA',
          400: '#14B8A6',
          500: '#0D9488',
          600: '#0F766E',
        },
        // Status dos agendamentos
        status: {
          pending:   '#D97706',  // amber-600
          confirmed: '#2563EB',  // blue-600
          present:   '#7C3AED',  // violet-600
          done:      '#16A34A',  // green-600
          cancelled: '#4B5563',  // gray-600
          noshow:    '#DC2626',  // red-600
        },
        // Backgrounds de status (fundo dos cards)
        'status-bg': {
          pending:   '#FFFBEB',
          confirmed: '#EFF6FF',
          present:   '#F5F3FF',
          done:      '#F0FDF4',
          cancelled: '#F9FAFB',
          noshow:    '#FEF2F2',
        },
        // Semânticas de sistema
        success: '#16A34A',
        error:   '#DC2626',
        warning: '#D97706',
        info:    '#2563EB',
      },
      boxShadow: {
        sm: '0 1px 2px rgba(0,0,0,0.05)',
        md: '0 4px 6px rgba(0,0,0,0.07)',
        lg: '0 10px 15px rgba(0,0,0,0.10)',
        xl: '0 20px 25px rgba(0,0,0,0.12)',
      },
      borderRadius: {
        'radius-sm':   '4px',
        'radius-md':   '8px',
        'radius-lg':   '12px',
        'radius-xl':   '16px',
        'radius-full': '9999px',
      },
      fontFamily: {
        sans:  ['Inter', 'sans-serif'],
        brand: ['DM Sans', 'sans-serif'],
      },
    },
  },
  plugins: [],
}

export default config
```

### `src/styles/globals.css` — CSS Variables e Dark Mode

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

:root {
  --bg-base:        #FFFFFF;
  --bg-subtle:      #F8FAFC;
  --bg-muted:       #F1F5F9;
  --border:         #E2E8F0;
  --text-primary:   #0F172A;
  --text-secondary: #64748B;
  --text-disabled:  #CBD5E1;
  --overlay:        rgba(15,23,42,0.5);
}

.dark {
  --bg-base:        #0F172A;
  --bg-subtle:      #1E293B;
  --bg-muted:       #334155;
  --border:         #334155;
  --text-primary:   #F8FAFC;
  --text-secondary: #94A3B8;
  --text-disabled:  #475569;
  --overlay:        rgba(0,0,0,0.7);
}

/* Ativa dark mode automaticamente via preferência do sistema */
@media (prefers-color-scheme: dark) {
  html { @apply dark; }
}

/* Mínimo absoluto de fonte — previne zoom automático iOS/Android */
input, textarea, select, label {
  font-size: 16px;
}
```

### `src/shared/lib/queryClient.ts`

```typescript
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,        // 30s — dados frescos sem refetch
      retry: 2,                 // 2 tentativas em erro de rede
      refetchOnWindowFocus: false, // sem refetch ao trocar aba
    },
  },
})
```

### `src/shared/lib/axios.ts`

```typescript
import axiosLib from 'axios'

const api = axiosLib.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Injetar JWT em toda requisição
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Redirecionar para login em 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
```

### `src/shared/hooks/useAuth.ts`

```typescript
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
```

> **Nota:** `useAuth` lê do `localStorage` diretamente nesta story. A Story 2.1 irá popular o `localStorage` após o login bem-sucedido. Os campos `perfil`, `nome` e `empresaId` são gravados pela Story 2.1; esta story apenas define a interface de leitura.

### `src/shared/components/ProtectedRoute.tsx`

```typescript
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Outlet />
}
```

### `src/main.tsx` — Estrutura Final

```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './shared/lib/queryClient'
import App from './App'
import './styles/globals.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>
)
```

### Estrutura de Rotas em `App.tsx`

```typescript
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
        {/* Rotas públicas */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/politica" element={<PoliticaPage />} />
        <Route path="/cancelar/:token" element={<AutoatendimentoPage />} />

        {/* Rotas protegidas */}
        <Route element={<ProtectedRoute />}>
          <Route path="/agenda" element={<AgendaPage />} />
          <Route path="/agenda/dia" element={<AgendaPage />} />
          <Route path="/agenda/semana" element={<AgendaPage />} />
          <Route path="/buscar" element={<BuscarPage />} />
          <Route path="/clientes/*" element={<ClientesPage />} />
          <Route path="/configuracoes/*" element={<ConfiguracoesPage />} />
          <Route path="/menu" element={<MenuPage />} />
        </Route>

        {/* Redirect default */}
        <Route path="/" element={<Navigate to="/agenda" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
```

### Shadcn/ui — Notas de Configuração

- `npx shadcn-ui@latest init`: quando perguntado sobre `baseColor`, escolher **slate** (neutro consistente com o design system)
- Os componentes são **copiados** para `src/shared/components/ui/` — não são uma dependência npm gerenciada
- Após init, o `globals.css` do Shadcn será gerado com CSS variables — **mesclar** com o `globals.css` da Task 1 (não sobrescrever os tokens de status e marca)
- Shadcn adiciona automaticamente `tailwind-merge` e `clsx` como dependências — isso é esperado

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `frontend/tailwind.config.ts` | **UPDATE** | Adicionar `theme.extend` completo |
| `frontend/src/styles/globals.css` | NEW | CSS variables light/dark + dark mode media query |
| `frontend/index.html` | **UPDATE** | Google Fonts link (Inter + DM Sans) |
| `frontend/src/main.tsx` | **UPDATE** | Envolver com `QueryClientProvider` |
| `frontend/src/App.tsx` | **UPDATE** | Rotas completas com `ProtectedRoute` |
| `frontend/src/shared/lib/queryClient.ts` | NEW | `QueryClient` com defaults |
| `frontend/src/shared/lib/axios.ts` | NEW | Axios com interceptors JWT |
| `frontend/src/shared/hooks/useAuth.ts` | NEW | Leitura de auth do localStorage |
| `frontend/src/shared/components/ProtectedRoute.tsx` | NEW | Guard de autenticação |
| `frontend/src/pages/LoginPage.tsx` | NEW | Stub |
| `frontend/src/pages/AgendaPage.tsx` | NEW | Stub |
| `frontend/src/pages/BuscarPage.tsx` | NEW | Stub |
| `frontend/src/pages/ClientesPage.tsx` | NEW | Stub |
| `frontend/src/pages/ConfiguracoesPage.tsx` | NEW | Stub |
| `frontend/src/pages/MenuPage.tsx` | NEW | Stub |
| `frontend/src/pages/AutoatendimentoPage.tsx` | NEW | Stub |
| `frontend/src/pages/PoliticaPage.tsx` | NEW | Stub com texto LGPD placeholder |
| `frontend/src/shared/components/ui/` | NEW | Shadcn/ui copiados (button, dialog, sheet, toast, badge) |
| `frontend/package.json` | **UPDATE** | Adicionar scripts `lint` e `type-check` |

### Aviso — Conflito Shadcn + globals.css

O `npx shadcn-ui@latest init` gera seu próprio `globals.css` com CSS variables da paleta Shadcn. **Não substituir** o `globals.css` já criado na Task 1. Processo correto:
1. Rodar `shadcn init`
2. O comando cria/sobrescreve `globals.css` — **após o init**, reintegrar manualmente os tokens de `--bg-base`, `--status-*`, etc. que foram definidos na Task 1
3. As variáveis do Shadcn (`--background`, `--foreground`, `--primary`, etc.) coexistem com os tokens personalizados

### Referências

- [Source: DESIGN.md#1.2 Paleta de Cores] — todos os valores hex dos tokens
- [Source: DESIGN.md#1.3 Tipografia] — Inter + DM Sans, escala, mínimo 16px
- [Source: DESIGN.md#1.4 Espaçamento e Grid] — base 4px, grid mobile/desktop
- [Source: DESIGN.md#1.5 Bordas e Elevação] — sombras e border-radius
- [Source: DESIGN.md#1.6 Iconografia] — Lucide Icons, tamanhos
- [Source: EXPERIENCE.md#2.1 Mapa de Telas] — rotas canônicas
- [Source: EXPERIENCE.md#2.2 Navegação por Perfil] — BottomTabBar por perfil (implementada em Story 2.2)
- [Source: architecture.md#Arquitetura Frontend] — versões: TanStack Query 5.x, React Router v6, Shadcn/ui + Tailwind 3.x
- [Source: architecture.md#Fronteiras de Integração] — axios.ts com interceptor JWT
- [Source: epics.md#Story 1.3] — Acceptance Criteria

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
