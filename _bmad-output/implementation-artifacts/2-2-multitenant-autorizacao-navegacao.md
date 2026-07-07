# Story 2.2: Middleware Multi-Tenant, Autorização por Perfil e Navegação

Status: ready-for-dev

## Story

Como sistema,
Quero garantir que cada usuário acesse apenas os dados da sua empresa e apenas as funcionalidades do seu perfil,
Para que não haja vazamento de dados entre clínicas nem acesso não autorizado a funcionalidades.

## Acceptance Criteria

**AC-1 — empresaId sempre do JWT, nunca de parâmetros**
- Given qualquer endpoint autenticado recebe uma requisição
- When o `JwtAuthenticationFilter` (Story 2.1) processa o token
- Then o `empresaId` está disponível via `SecurityUtils.getEmpresaId()` no contexto de segurança
- And nenhum endpoint autenticado aceita `empresaId` como query param ou path param (NFR-004)

**AC-2 — Autorização por perfil (`@PreAuthorize`)**
- Given um usuário STAFF tenta acessar `GET /api/v1/configuracoes`
- When o `@PreAuthorize` avalia
- Then retorna HTTP 403

- Given um usuário PROFISSIONAL tenta acessar `GET /api/v1/clientes`
- When o `@PreAuthorize` avalia
- Then retorna HTTP 403

- Given um usuário não-ADMIN_SISTEMA tenta acessar `/api/v1/admin/**`
- When o `@PreAuthorize` avalia
- Then retorna HTTP 403

**AC-3 — Isolamento multi-tenant testado end-to-end (R1)**
- Given registros em duas empresas distintas (empresa A e empresa B) e usuário autenticado como empresa A
- When `GET /api/v1/profissionais` e `GET /api/v1/staff` são chamados
- Then a resposta contém apenas registros da empresa A
- And nenhum ID pertencente à empresa B aparece em nenhum item do response
- And este teste de integração explícito é critério de aceite obrigatório desta story

**AC-4 — BottomTabBar por perfil (UX-DR4, mobile ≤767px)**
- Given o usuário faz login com perfil STAFF
- When o frontend renderiza o layout principal
- Then a `BottomTabBar` exibe: `📅 Agenda` · `🔍 Buscar` · `👥 Clientes` · `··· Menu`

- Given o usuário faz login com perfil PROFISSIONAL
- When o frontend renderiza o layout principal
- Then a `BottomTabBar` exibe: `📅 Agenda` · `··· Menu`

- Given o usuário faz login com perfil ADMIN_EMPRESA
- When o frontend renderiza o layout principal
- Then a `BottomTabBar` exibe: `📅 Agenda` · `⚙️ Configurações` · `··· Menu`

**AC-5 — Sidebar desktop por perfil (≥768px)**
- Given o viewport é ≥ 768px
- When o layout é renderizado
- Then exibe sidebar fixa 240px à esquerda com wordmark "DrAgenda" no topo
- And itens condicionais: `🔍 Buscar` e `👥 Clientes` somente para Staff; `⚙️ Configurações` somente para Admin Empresa
- And FAB `[ + ]` visível no canto inferior direito da área de conteúdo (Staff e Profissional)

**AC-6 — Layout principal envolve páginas protegidas**
- Given o usuário está autenticado e acessa `/agenda`
- When o layout principal renderiza
- Then o `AppLayout` (com BottomTabBar ou Sidebar conforme viewport) envolve o conteúdo da página
- And a aba/item ativo é destacado visualmente com cor `brand-500`

## Tasks / Subtasks

- [ ] **Task 1 — Habilitar `@PreAuthorize` no Spring Security** (AC-2)
  - [ ] Adicionar `@EnableMethodSecurity` ao `SecurityConfig.java`
  - [ ] Verificar que as `ROLE_` authorities são inseridas corretamente no `JwtAuthenticationFilter` (Story 2.1): `new SimpleGrantedAuthority("ROLE_" + perfil)`

- [ ] **Task 2 — Stubs de controllers com autorização** (AC-2)
  - [ ] Criar `api/controllers/ConfiguracoesController.java` stub com `GET /api/v1/configuracoes` anotado com `@PreAuthorize("hasRole('ADMIN_EMPRESA')")`
  - [ ] Criar `api/controllers/ClienteController.java` stub com `GET /api/v1/clientes` anotado com `@PreAuthorize("hasRole('STAFF')")`
  - [ ] Criar `api/controllers/AdminEmpresaController.java` stub com `@PreAuthorize("hasRole('ADMIN_SISTEMA')")` no nível da classe
  - [ ] Criar `api/controllers/ProfissionalController.java` stub com `GET /api/v1/profissionais` anotado com `@PreAuthorize("hasAnyRole('ADMIN_EMPRESA')")`
  - [ ] Criar `api/controllers/StaffController.java` stub com `GET /api/v1/staff` anotado com `@PreAuthorize("hasRole('ADMIN_EMPRESA')")`
  - [ ] Os stubs retornam `ResponseEntity.ok(Collections.emptyList())` — implementação real ocorre nas stories 2.3–2.5

- [ ] **Task 3 — Filtro de `empresaId` em repositories** (AC-1, AC-3)
  - [ ] Criar `domain/repositories/ProfissionalRepository.java` com método:
    `Page<Profissional> findAllByEmpresaId(Long empresaId, Pageable pageable)`
  - [ ] Criar `domain/repositories/UsuarioRepository.java` (complementar ao da Story 1.2) adicionar:
    `Page<Usuario> findAllByEmpresaIdAndPerfil(Long empresaId, PerfilUsuario perfil, Pageable pageable)`
  - [ ] **Regra:** todo Repository multi-tenant usa `SecurityUtils.getEmpresaId()` no service — nunca recebe `empresaId` como parâmetro de controller

- [ ] **Task 4 — Teste de isolamento multi-tenant (R1 — obrigatório)** (AC-3)
  - [ ] Criar `test/.../MultiTenantIsolationIT.java` com `@SpringBootTest` + `@AutoConfigureMockMvc`
  - [ ] Setup: criar empresa A + empresa B + usuário ADMIN_EMPRESA em cada + profissional em cada
  - [ ] Autenticar como empresa A (obter JWT via `POST /api/v1/auth/login`)
  - [ ] Chamar `GET /api/v1/profissionais` e verificar: apenas profissional da empresa A retornado
  - [ ] Afirmar: nenhum `id` do profissional da empresa B aparece no response body
  - [ ] Repetir para `GET /api/v1/staff`
  - [ ] Este teste DEVE passar antes de marcar a story como concluída

- [ ] **Task 5 — Componente `AppLayout.tsx`** (AC-4, AC-5, AC-6)
  - [ ] Criar `src/shared/components/AppLayout.tsx` que usa `useAuth()` para ler `perfil`
  - [ ] Renderizar `BottomTabBar` quando `window.innerWidth < 768` (ou media query via `useMediaQuery` hook)
  - [ ] Renderizar `Sidebar` quando `window.innerWidth >= 768`
  - [ ] Envolver `<Outlet />` com padding adequado para não sobrepor a BottomTabBar (padding-bottom: 48px mobile)

- [ ] **Task 6 — Componente `BottomTabBar.tsx`** (AC-4)
  - [ ] Criar `src/shared/components/BottomTabBar.tsx`
  - [ ] Lógica de abas por perfil:
    - `STAFF`: Agenda · Buscar · Clientes · Menu
    - `PROFISSIONAL`: Agenda · Menu
    - `ADMIN_EMPRESA`: Agenda · Configurações · Menu
    - `ADMIN_SISTEMA`: Agenda · Menu (acesso de emergência — sem itens de clínica)
  - [ ] Usar `NavLink` do React Router para destacar aba ativa com `brand-500`
  - [ ] Posição `fixed bottom-0`, altura `h-12` (48px), `z-50`, fundo `bg-[var(--bg-base)]`, borda superior `border-t border-[var(--border)]`
  - [ ] Área tocável mínima 44×44px por item (WCAG)
  - [ ] `aria-label` descritivo em cada item: ex. `aria-label="Agenda"`, `aria-current="page"` na aba ativa

- [ ] **Task 7 — Componente `Sidebar.tsx`** (AC-5)
  - [ ] Criar `src/shared/components/Sidebar.tsx`
  - [ ] Wordmark "DrAgenda" no topo com `font-brand`
  - [ ] Itens de navegação condicionais por `perfil` (mesma lógica da BottomTabBar)
  - [ ] Largura fixa `w-60` (240px), `min-h-screen`, `border-r border-[var(--border)]`
  - [ ] Item ativo com fundo `brand-50` e texto `brand-600`
  - [ ] Separador entre navegação e rodapé (nome do usuário + "Sair")

- [ ] **Task 8 — Atualizar `App.tsx` para usar `AppLayout`** (AC-6)
  - [ ] Envolver todas as rotas protegidas com `<AppLayout />` como layout pai via `<Route element={<AppLayout />}>...`
  - [ ] `AppLayout` usa `<Outlet />` do React Router para renderizar o conteúdo da rota filha

- [ ] **Task 9 — Hook `useMediaQuery`** (AC-4, AC-5)
  - [ ] Criar `src/shared/hooks/useMediaQuery.ts`:
    ```typescript
    export function useMediaQuery(query: string): boolean {
      const [matches, setMatches] = useState(() => window.matchMedia(query).matches)
      useEffect(() => {
        const mq = window.matchMedia(query)
        const handler = (e: MediaQueryListEvent) => setMatches(e.matches)
        mq.addEventListener('change', handler)
        return () => mq.removeEventListener('change', handler)
      }, [query])
      return matches
    }
    ```
  - [ ] Usar em `AppLayout.tsx`: `const isDesktop = useMediaQuery('(min-width: 768px)')`

## Dev Notes

### `AppLayout.tsx` — Estrutura Completa

```tsx
// src/shared/components/AppLayout.tsx
import { Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useMediaQuery } from '../hooks/useMediaQuery'
import { BottomTabBar } from './BottomTabBar'
import { Sidebar } from './Sidebar'

export function AppLayout() {
  const { isAuthenticated } = useAuth()
  const isDesktop = useMediaQuery('(min-width: 768px)')

  if (!isAuthenticated) return <Navigate to="/login" replace />

  return (
    <div className="flex min-h-screen bg-[var(--bg-base)]">
      {isDesktop && <Sidebar />}
      <main className={`flex-1 ${!isDesktop ? 'pb-12' : ''}`}>
        <Outlet />
      </main>
      {!isDesktop && <BottomTabBar />}
    </div>
  )
}
```

> **Nota:** `AppLayout` duplica parcialmente a lógica do `ProtectedRoute` (Story 1.3). A partir desta story, use `AppLayout` como layout pai das rotas protegidas em vez de `ProtectedRoute` separado — `AppLayout` já faz o redirect se não autenticado. O `ProtectedRoute` criado na Story 1.3 pode ser removido ou mantido como alias.

### `BottomTabBar.tsx` — Estrutura

```tsx
// src/shared/components/BottomTabBar.tsx
import { NavLink } from 'react-router-dom'
import { Calendar, Search, Users, Settings, MoreHorizontal } from 'lucide-react'
import { useAuth, type Perfil } from '../hooks/useAuth'

type TabItem = { to: string; icon: React.ReactNode; label: string; ariaLabel: string }

function getTabItems(perfil: Perfil | null): TabItem[] {
  const agenda = { to: '/agenda', icon: <Calendar size={24} />, label: 'Agenda', ariaLabel: 'Agenda' }
  const buscar = { to: '/buscar', icon: <Search size={24} />, label: 'Buscar', ariaLabel: 'Buscar horário' }
  const clientes = { to: '/clientes', icon: <Users size={24} />, label: 'Clientes', ariaLabel: 'Clientes' }
  const config = { to: '/configuracoes', icon: <Settings size={24} />, label: 'Config', ariaLabel: 'Configurações' }
  const menu = { to: '/menu', icon: <MoreHorizontal size={24} />, label: 'Menu', ariaLabel: 'Menu' }

  switch (perfil) {
    case 'STAFF':          return [agenda, buscar, clientes, menu]
    case 'PROFISSIONAL':   return [agenda, menu]
    case 'ADMIN_EMPRESA':  return [agenda, config, menu]
    default:               return [agenda, menu]
  }
}

export function BottomTabBar() {
  const { perfil } = useAuth()
  const tabs = getTabItems(perfil)

  return (
    <nav
      aria-label="Navegação principal"
      className="fixed bottom-0 left-0 right-0 h-12 z-50 bg-[var(--bg-base)] border-t border-[var(--border)] flex items-stretch"
    >
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          aria-label={tab.ariaLabel}
          className={({ isActive }) =>
            `flex flex-1 flex-col items-center justify-center gap-0.5 min-h-[44px]
             text-xs transition-colors
             ${isActive
               ? 'text-brand-500'
               : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
             }`
          }
        >
          {({ isActive }) => (
            <>
              {tab.icon}
              <span>{tab.label}</span>
              {isActive && <span className="sr-only">(página atual)</span>}
            </>
          )}
        </NavLink>
      ))}
    </nav>
  )
}
```

### `Sidebar.tsx` — Estrutura

```tsx
// src/shared/components/Sidebar.tsx
import { NavLink } from 'react-router-dom'
import { Calendar, Search, Users, Settings, MoreHorizontal, LogOut } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { useLogout } from '../hooks/useLogout'

export function Sidebar() {
  const { perfil, nome } = useAuth()
  const logout = useLogout()

  return (
    <aside className="w-60 min-h-screen border-r border-[var(--border)] bg-[var(--bg-base)] flex flex-col">
      {/* Wordmark */}
      <div className="px-6 py-5 border-b border-[var(--border)]">
        <span className="font-brand text-xl font-medium">
          <span className="font-light text-[var(--text-primary)]">Dr</span>
          <span className="text-brand-500">Agenda</span>
        </span>
      </div>

      {/* Navegação */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        <SidebarLink to="/agenda" icon={<Calendar size={20} />} label="Agenda" />
        {perfil === 'STAFF' && (
          <>
            <SidebarLink to="/buscar" icon={<Search size={20} />} label="Buscar" />
            <SidebarLink to="/clientes" icon={<Users size={20} />} label="Clientes" />
          </>
        )}
        {perfil === 'ADMIN_EMPRESA' && (
          <SidebarLink to="/configuracoes" icon={<Settings size={20} />} label="Configurações" />
        )}
        <SidebarLink to="/menu" icon={<MoreHorizontal size={20} />} label="Menu" />
      </nav>

      {/* Rodapé: usuário + sair */}
      <div className="px-4 py-4 border-t border-[var(--border)]">
        <p className="text-sm text-[var(--text-secondary)] mb-2 truncate">{nome}</p>
        <button
          onClick={logout}
          className="flex items-center gap-2 text-sm text-[var(--text-secondary)] hover:text-error transition-colors"
        >
          <LogOut size={16} />
          Sair
        </button>
      </div>
    </aside>
  )
}

function SidebarLink({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 px-3 py-2 rounded-radius-md text-sm transition-colors
         ${isActive
           ? 'bg-brand-50 text-brand-600 font-medium'
           : 'text-[var(--text-secondary)] hover:bg-[var(--bg-subtle)] hover:text-[var(--text-primary)]'
         }`
      }
    >
      {icon}
      {label}
    </NavLink>
  )
}
```

### `App.tsx` — Atualização de Rotas com `AppLayout`

```tsx
// Substituir o bloco de rotas protegidas no App.tsx:
import { AppLayout } from './shared/components/AppLayout'

// No JSX de <Routes>:
<Route element={<AppLayout />}>
  <Route path="/agenda" element={<AgendaPage />} />
  <Route path="/agenda/dia" element={<AgendaPage />} />
  <Route path="/agenda/semana" element={<AgendaPage />} />
  <Route path="/buscar" element={<BuscarPage />} />
  <Route path="/clientes/*" element={<ClientesPage />} />
  <Route path="/configuracoes/*" element={<ConfiguracoesPage />} />
  <Route path="/menu" element={<MenuPage />} />
</Route>
// Remover o <Route element={<ProtectedRoute />}> anterior — AppLayout assume essa responsabilidade
```

### Autorização backend — Tabela de referência

| Endpoint | Perfis permitidos | Anotação |
|---|---|---|
| `GET /api/v1/profissionais` | ADMIN_EMPRESA | `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` |
| `GET /api/v1/staff` | ADMIN_EMPRESA | `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` |
| `GET /api/v1/clientes` | STAFF | `@PreAuthorize("hasRole('STAFF')")` |
| `GET /api/v1/configuracoes` | ADMIN_EMPRESA | `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` |
| `GET /api/v1/agendamentos` | STAFF, PROFISSIONAL, ADMIN_EMPRESA | `@PreAuthorize("hasAnyRole('STAFF','PROFISSIONAL','ADMIN_EMPRESA')")` |
| `POST /api/v1/agendamentos` | STAFF, PROFISSIONAL | `@PreAuthorize("hasAnyRole('STAFF','PROFISSIONAL')")` |
| `/api/v1/admin/**` | ADMIN_SISTEMA | `@PreAuthorize("hasRole('ADMIN_SISTEMA')")` |
| `DELETE /api/v1/clientes/{id}/anonimizar` | ADMIN_EMPRESA | `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` |

> As anotações acima cobrem os casos testados nesta story. Os controllers completos são implementados nas stories seguintes.

### Teste de Isolamento Multi-Tenant (R1)

```java
@SpringBootTest
@AutoConfigureMockMvc
class MultiTenantIsolationIT {

    @Autowired MockMvc mockMvc;
    @Autowired EmpresaRepository empresaRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired ProfissionalRepository profissionalRepo;
    @Autowired BCryptPasswordEncoder encoder;

    private String tokenEmpresaA;

    @BeforeEach
    void setup() throws Exception {
        // Criar empresa A
        Empresa empresaA = empresaRepo.save(new Empresa("Clínica A"));
        Empresa empresaB = empresaRepo.save(new Empresa("Clínica B"));

        // Admin Empresa A
        Usuario adminA = criarUsuario("admin-a@test.com", empresaA, PerfilUsuario.ADMIN_EMPRESA);
        // Profissional em cada empresa
        criarProfissional(empresaA);
        criarProfissional(empresaB);

        // Fazer login como empresa A
        String body = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"admin-a@test.com","senha":"Test123!"}"""))
            .andReturn().getResponse().getContentAsString();
        tokenEmpresaA = JsonPath.read(body, "$.token");
    }

    @Test
    void profissionaisRetornaApenasEmpresaA() throws Exception {
        mockMvc.perform(get("/api/v1/profissionais")
            .header("Authorization", "Bearer " + tokenEmpresaA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].empresaId").value(empresaA.getId()));
    }

    @Test
    void staffRetornaApenasEmpresaA() throws Exception {
        mockMvc.perform(get("/api/v1/staff")
            .header("Authorization", "Bearer " + tokenEmpresaA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].empresaId", everyItem(is(empresaA.getId().intValue()))));
    }
}
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `infrastructure/config/SecurityConfig.java` | **UPDATE** | Adicionar `@EnableMethodSecurity` |
| `api/controllers/ConfiguracoesController.java` | NEW | Stub com `@PreAuthorize` |
| `api/controllers/ClienteController.java` | NEW | Stub com `@PreAuthorize` |
| `api/controllers/AdminEmpresaController.java` | NEW | Stub com `@PreAuthorize` |
| `api/controllers/ProfissionalController.java` | NEW | Stub com `@PreAuthorize` |
| `api/controllers/StaffController.java` | NEW | Stub com `@PreAuthorize` |
| `domain/repositories/ProfissionalRepository.java` | NEW | `findAllByEmpresaId(...)` |
| `test/.../MultiTenantIsolationIT.java` | NEW | Teste R1 obrigatório |
| `src/shared/components/AppLayout.tsx` | NEW | BottomTabBar ou Sidebar por viewport |
| `src/shared/components/BottomTabBar.tsx` | NEW | UX-DR4, condicional por perfil |
| `src/shared/components/Sidebar.tsx` | NEW | Desktop 240px, condicional por perfil |
| `src/shared/hooks/useMediaQuery.ts` | NEW | Reactive breakpoint hook |
| `src/App.tsx` | **UPDATE** | Rotas protegidas usando `<AppLayout>` como layout pai |

### Referências

- [Source: epics.md#Story 2.2] — Acceptance Criteria completos e R1
- [Source: EXPERIENCE.md#2.2 Navegação por Perfil] — abas por perfil, sidebar desktop
- [Source: DESIGN.md#1.4 Espaçamento] — h-12 (48px) BottomTabBar, sidebar 240px
- [Source: DESIGN.md#1.5 Bordas e Elevação] — radius-md, border tokens
- [Source: architecture.md#Regras de Processo] — empresaId nunca de parâmetro de request
- [Source: architecture.md#Estratégia de Testes] — @SpringBootTest + MockMvc para integração

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
