# Story 6.3: Estados de Interface, Acessibilidade e PWA Offline

Status: ready-for-dev

## Story

Como usuário do DrAgenda,
Quero que a interface informe claramente o estado de cada tela, seja acessível independente de limitações visuais e funcione com conexão instável,
Para que o sistema seja utilizável por todos os usuários em qualquer condição de conectividade.

## Acceptance Criteria

**AC-1 — Skeleton screens em todas as telas com carregamento (UX-DR11)**
- Given qualquer tela com lista de dados está carregando
- When a requisição está em andamento (`isLoading = true`)
- Then skeleton screens substituem os elementos visuais:
  - `AgendaPage`: 3 cards placeholder com shimmer
  - `ClientesPage`, `ProfissionaisPage`, `StaffPage`: linhas placeholder na lista
  - `RelatoriosPage`: placeholders nos cards de contador
  - `AgendamentoDetalhePage`: placeholder do cabeçalho + linhas de histórico
- And os skeletons têm dimensões equivalentes ao conteúdo real (sem layout shift — CLS ≈ 0)
- And o componente `Skeleton` da Story 3.1 é reutilizado — sem criar novos skeletons

**AC-2 — Estados vazios com mensagens contextuais**
- Given cada tela não possui dados para exibir
- When a lista está vazia (requisição concluída com `content = []`)
- Then a mensagem é contextual à tela:
  - `AgendaPage` sem agendamentos: `"Nenhum agendamento para hoje."` + FAB visível para Staff/Admin
  - `ClientesPage` sem clientes: `"Nenhum cliente cadastrado ainda."` + botão `"Cadastrar primeiro cliente"` → navega para `/clientes/novo`
  - `BuscarPage` sem vagas: `"Nenhum horário disponível nos próximos 30 dias para os filtros selecionados. Verifique as configurações de disponibilidade."` + link para `/configuracoes/disponibilidade`
  - `RelatoriosPage` sem dados no período: `"Nenhum agendamento encontrado no período selecionado."`
  - `AgendamentoDetalhePage` sem histórico: `"Nenhuma transição de status registrada."` (não deve ocorrer em produção — apenas em dados antigos)
- And ícone `<InboxIcon>` (Lucide) acompanha cada mensagem de estado vazio

**AC-3 — Banner offline (UX-DR11)**
- Given o dispositivo perde conectividade
- When o frontend detecta `navigator.onLine === false` ou o evento `offline` é disparado
- Then um banner amarelo fixo no topo é exibido: `"Sem conexão — algumas ações podem não funcionar"` com ícone `<WifiOff size={16}>`
- And botões de ação nos `AgendamentoCard` são desabilitados com `title="Sem conexão"` (tooltip nativo)
- And a agenda exibe dados do cache do TanStack Query (última sincronização) com timestamp: `"Atualizado às HH:mm"`

**AC-4 — Reconexão automática**
- Given o dispositivo estava offline e reconecta
- When o evento `online` é detectado
- Then o banner desaparece automaticamente
- And `queryClient.invalidateQueries()` é chamado para recarregar todos os dados imediatamente

**AC-5 — PWA: manifest e ícones**
- Given o usuário acessa o DrAgenda via browser mobile pela primeira vez
- When o Service Worker é registrado pelo `vite-plugin-pwa` (Story 1.1)
- Then o PWA é instalável com:
  - `manifest.json`: `display: "standalone"`, `theme_color: "#2563EB"`, `background_color: "#FFFFFF"`
  - Ícone 192×192px e 512×512px (formato PNG) em `/public/icons/`
  - `name: "DrAgenda"`, `short_name: "DrAgenda"`
- And recursos estáticos (JS, CSS, fontes) são cacheados para funcionamento offline básico

**AC-6 — `aria-label` em todos os elementos interativos sem texto visível**
- Given qualquer botão de ícone é renderizado
- When acessado via teclado ou leitor de tela
- Then tem `aria-label` descritivo:
  - FAB → `aria-label="Novo agendamento"`
  - Menu `···` → `aria-label="Mais ações para [nome do paciente] às [horário]"`
  - Botão de fechar Bottom Sheet → `aria-label="Fechar"`
  - Botão de voltar → `aria-label="Voltar"`
  - `<StatusBadge>` leitura → role `"status"` com `aria-label="Status: [nome do status]"`

**AC-7 — Focus trap em Bottom Sheets e Dialogs**
- Given um Bottom Sheet ou Dialog é aberto
- When o usuário navega com Tab
- Then o foco não escapa do componente enquanto aberto (focus trap)
- And ao fechar (Escape ou botão), o foco retorna ao elemento que o abriu (`previousActiveElement.focus()`)
- And implementar usando a API nativa do Shadcn/ui Dialog (já tem focus trap built-in) — verificar Bottom Sheet

**AC-8 — `aria-live` para mudanças de status**
- Given uma transição de status é executada com sucesso
- When o toast aparece
- Then a região de toast tem `aria-live="polite"` e `aria-atomic="true"` para que leitores de tela anunciem a mensagem
- And o `ToastProvider` da Story 3.1 deve verificar se já tem `aria-live` — adicionar se ausente

**AC-9 — Labels e `aria-required` em formulários**
- Given qualquer campo de formulário é renderizado
- When acessado via teclado ou leitor de tela
- Then todos os campos têm `<label>` associado via `htmlFor` (ou `aria-label` se label visual não é viável)
- And campos obrigatórios têm `aria-required="true"`
- And `aria-describedby` aponta para a mensagem de erro quando o campo tem erro de validação
- And auditoria cobre os formulários: `LoginPage`, `FormularioAgendamento`, `ClienteFormPage`, `TrocaSenhaPage`, `ConfiguracaoClinicaPage`

**AC-10 — Contraste WCAG 2.1 AA e áreas de toque**
- Given qualquer texto ou elemento interativo é renderizado
- When verificado por ferramenta de acessibilidade
- Then contraste mínimo WCAG 2.1 AA: texto normal ≥ 4.5:1, texto grande ≥ 3:1
- And área tocável mínima de 44×44px em todos os elementos interativos (botões, links, chips)
- And `StatusBadge` usa obrigatoriamente cor + ícone + rótulo de texto — nunca apenas cor (UX-DR2 / daltonismo)

## Tasks / Subtasks

- [ ] **Task 1 — Auditoria de skeletons existentes** (AC-1)
  - [ ] Verificar quais telas já têm skeleton (Story 3.1 criou `Skeleton`, `SkeletonCard`, `SkeletonList`)
  - [ ] Adicionar skeletons onde faltam: `AgendaPage` (3 cards), `RelatoriosPage` (contador cards), `AgendamentoDetalhePage` (header + histórico)
  - [ ] Garantir dimensões equivalentes ao conteúdo real em cada caso

- [ ] **Task 2 — Componente `EstadoVazio.tsx`** (AC-2)
  - [ ] Criar `src/shared/components/EstadoVazio.tsx`:
    ```tsx
    interface EstadoVazioProps {
      mensagem: string
      acao?: { label: string; onClick: () => void }
    }

    export function EstadoVazio({ mensagem, acao }: EstadoVazioProps) {
      return (
        <div className="flex flex-col items-center justify-center py-16 gap-3 text-muted-foreground">
          <Inbox size={40} strokeWidth={1.5} />
          <p className="text-sm text-center max-w-xs">{mensagem}</p>
          {acao && (
            <Button variant="outline" size="sm" onClick={acao.onClick}>
              {acao.label}
            </Button>
          )}
        </div>
      )
    }
    ```
  - [ ] Integrar `<EstadoVazio>` nas telas: `AgendaPage`, `ClientesPage`, `BuscarPage`, `RelatoriosPage`, `AgendamentoDetalhePage`

- [ ] **Task 3 — Hook `useOnlineStatus`** (AC-3, AC-4)
  - [ ] Criar `src/shared/hooks/useOnlineStatus.ts`:
    ```ts
    export function useOnlineStatus() {
      const [isOnline, setIsOnline] = useState(navigator.onLine)
      const queryClient = useQueryClient()

      useEffect(() => {
        const handleOnline = () => {
          setIsOnline(true)
          queryClient.invalidateQueries()  // recarregar ao reconectar
        }
        const handleOffline = () => setIsOnline(false)

        window.addEventListener('online', handleOnline)
        window.addEventListener('offline', handleOffline)
        return () => {
          window.removeEventListener('online', handleOnline)
          window.removeEventListener('offline', handleOffline)
        }
      }, [queryClient])

      return isOnline
    }
    ```

- [ ] **Task 4 — `BannerOffline.tsx`** (AC-3, AC-4)
  - [ ] Criar `src/shared/components/BannerOffline.tsx`:
    ```tsx
    export function BannerOffline() {
      const isOnline = useOnlineStatus()

      if (isOnline) return null

      return (
        <div
          role="alert"
          aria-live="assertive"
          className="fixed top-0 left-0 right-0 z-50
                     bg-amber-400 text-amber-900 text-sm
                     flex items-center justify-center gap-2 py-2 px-4">
          <WifiOff size={16} />
          Sem conexão — algumas ações podem não funcionar
        </div>
      )
    }
    ```
  - [ ] Adicionar `<BannerOffline>` no `AppLayout.tsx` acima do conteúdo principal
  - [ ] Timestamp de última sincronização: armazenar em `localStorage` quando `queryClient` atualizar dados

- [ ] **Task 5 — Desabilitar ações do `AgendamentoCard` offline** (AC-3)
  - [ ] Passar `isOnline` como prop para `AgendamentoCard.tsx`
  - [ ] Botões de ação: `disabled={!isOnline}` + `title={!isOnline ? 'Sem conexão' : undefined}`

- [ ] **Task 6 — PWA manifest e ícones** (AC-5)
  - [ ] Verificar configuração `vite-plugin-pwa` no `vite.config.ts` (Story 1.1 — deve ter configuração básica)
  - [ ] Criar/atualizar `manifest.json` em `public/`:
    ```json
    {
      "name": "DrAgenda",
      "short_name": "DrAgenda",
      "display": "standalone",
      "start_url": "/",
      "theme_color": "#2563EB",
      "background_color": "#FFFFFF",
      "icons": [
        { "src": "/icons/icon-192.png", "sizes": "192x192", "type": "image/png" },
        { "src": "/icons/icon-512.png", "sizes": "512x512", "type": "image/png" }
      ]
    }
    ```
  - [ ] Criar ícones placeholder em `public/icons/`: `icon-192.png` e `icon-512.png` (usar logo DrAgenda ou placeholder colorido)
  - [ ] Verificar que `vite-plugin-pwa` está configurado com `registerType: 'autoUpdate'` e `workbox.globPatterns` cobrindo JS/CSS/fontes

- [ ] **Task 7 — `aria-label` em elementos interativos** (AC-6)
  - [ ] Auditar e adicionar `aria-label` onde faltam:
    - `FAB` em `AgendaPage`: `aria-label="Novo agendamento"`
    - `DropdownMenuTrigger` do menu `···` em `AgendamentoCard`: `aria-label={\`Mais ações para ${nomeCliente} às ${horario}\`}`
    - Botões de fechar Bottom Sheets: `aria-label="Fechar"`
    - `<StatusBadge>`: adicionar `role="status"` e `aria-label={\`Status: ${label}\`}`

- [ ] **Task 8 — Focus trap e restauração de foco** (AC-7)
  - [ ] Verificar se `Dialog` do Shadcn/ui já implementa focus trap (sim — Radix UI tem built-in)
  - [ ] Para Bottom Sheets customizados (se não baseados em Radix `Dialog`): implementar focus trap com `useRef` + listener de Tab:
    ```ts
    // Capturar elemento que disparou a abertura:
    const triggerRef = useRef<HTMLElement | null>(null)
    const onOpen = () => {
      triggerRef.current = document.activeElement as HTMLElement
    }
    const onClose = () => {
      triggerRef.current?.focus()
    }
    ```

- [ ] **Task 9 — `aria-live` no `ToastProvider`** (AC-8)
  - [ ] Verificar `ToastProvider` da Story 3.1 — se usa Shadcn/ui `Toaster`, já tem `aria-live="polite"` built-in
  - [ ] Se implementação customizada: adicionar `aria-live="polite"` e `aria-atomic="true"` na região de toasts

- [ ] **Task 10 — Auditoria de `aria-required` e `aria-describedby` nos formulários** (AC-9)
  - [ ] Para cada campo obrigatório nos formulários listados:
    ```tsx
    <Input
      id="nome"
      aria-required="true"
      aria-describedby={errors.nome ? "nome-erro" : undefined}
    />
    {errors.nome && (
      <p id="nome-erro" className="text-xs text-red-600" role="alert">
        {errors.nome.message}
      </p>
    )}
    ```
  - [ ] Todos os campos de formulário devem ter `<label htmlFor="...">` associado

- [ ] **Task 11 — Auditoria de contraste e áreas de toque** (AC-10)
  - [ ] Verificar tokens de cor em `globals.css` (Story 1.3) — confirmar que `--foreground` / `--background` atendem 4.5:1
  - [ ] Verificar todos os botões: mínimo `min-h-[44px] min-w-[44px]` — corrigir os que forem menores
  - [ ] `StatusBadge` — confirmar que todos os 6 status têm: cor de borda + ícone Lucide + rótulo de texto (já definido na Story 3.1)
  - [ ] Chips de filtro na `AgendaPage`: garantir altura ≥ 44px

- [ ] **Task 12 — Testes de acessibilidade** (AC-6, AC-7, AC-8)
  - [ ] Instalar `@testing-library/jest-dom` (se ausente) e adicionar matchers de acessibilidade
  - [ ] Teste unitário `AgendamentoCardA11yTest`:
    - FAB tem `aria-label="Novo agendamento"`
    - Menu `···` tem `aria-label` com nome do paciente
    - Botões de ação desabilitados quando `isOnline=false`
  - [ ] Teste unitário `EstadoVazioTest`:
    - Renderiza ícone `<Inbox>`
    - Renderiza mensagem passada como prop
    - Botão de ação presente quando `acao` fornecida

## Dev Notes

### Ordem de Execução Recomendada

Esta story é uma story de polimento transversal — altera muitos componentes existentes. Executar em sequência:
1. Tasks 2 e 3 (componentes novos independentes)
2. Task 4 (banner offline + hook)
3. Tasks 1 e 5 (skeletons e botões offline)
4. Tasks 6 (PWA — sem dependências de outros componentes)
5. Tasks 7, 8, 9, 10, 11 (auditoria de acessibilidade)

### Verificação de Conformidade Shadcn/ui + Radix UI

Os componentes Shadcn/ui baseados em Radix UI (`Dialog`, `DropdownMenu`, `Popover`) já implementam:
- Focus trap (Radix `FocusScope`)
- Fechar com Escape
- Retorno de foco ao elemento trigger

Verificar se o Bottom Sheet customizado (Story 4.2) usa `Dialog` do Radix ou é um componente CSS puro. Se CSS puro, implementar focus trap manualmente.

### `vite-plugin-pwa` — Configuração Mínima

```ts
// vite.config.ts
import { VitePWA } from 'vite-plugin-pwa'

VitePWA({
  registerType: 'autoUpdate',
  manifest: {
    name: 'DrAgenda',
    short_name: 'DrAgenda',
    display: 'standalone',
    theme_color: '#2563EB',
    background_color: '#FFFFFF',
    icons: [
      { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
      { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
    ],
  },
  workbox: {
    globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
  },
})
```

### Timestamp de Última Sincronização

```tsx
// AgendaPage.tsx — salvar timestamp ao receber dados:
const { data, dataUpdatedAt } = useQuery({ ... })

const ultimaSincronizacao = dataUpdatedAt
  ? new Date(dataUpdatedAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
  : null

// Exibir quando offline:
{!isOnline && ultimaSincronizacao && (
  <span className="text-xs text-muted-foreground">
    Atualizado às {ultimaSincronizacao}
  </span>
)}
```

### Ordem de Prioridade de Fixes de Acessibilidade

Se o tempo for limitado, priorizar na seguinte ordem:
1. `aria-label` em FAB e menu `···` (impacto alto — elementos sem texto)
2. `aria-required` + `aria-describedby` nos formulários (impacto alto — formulários críticos)
3. Focus trap no Bottom Sheet (se não usar Radix)
4. `aria-live` no ToastProvider (se não usar Shadcn/ui padrão)
5. Auditoria de área de toque 44×44px

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `src/shared/components/EstadoVazio.tsx` | NEW | Com `<Inbox>` Lucide |
| `src/shared/hooks/useOnlineStatus.ts` | NEW | Eventos `online`/`offline` |
| `src/shared/components/BannerOffline.tsx` | NEW | `role="alert"` + `aria-live="assertive"` |
| `src/shared/components/AgendamentoCard.tsx` | **UPDATE** | `disabled` offline + `aria-label` menu `···` |
| `src/pages/AgendaPage.tsx` | **UPDATE** | 3 skeletons + estado vazio + timestamp offline |
| `src/pages/ClientesPage.tsx` | **UPDATE** | Estado vazio + `<Paginacao>` |
| `src/pages/BuscarPage.tsx` | **UPDATE** | Estado vazio + link config |
| `src/pages/RelatoriosPage.tsx` | **UPDATE** | Skeletons nos cards de contador |
| `src/pages/AgendamentoDetalhePage.tsx` | **UPDATE** | Skeleton header + histórico |
| `src/AppLayout.tsx` | **UPDATE** | `<BannerOffline>` |
| `public/manifest.json` | NEW/UPDATE | `display:standalone` |
| `public/icons/icon-192.png` | NEW | Ícone PWA |
| `public/icons/icon-512.png` | NEW | Ícone PWA |
| `vite.config.ts` | **UPDATE** (verificar) | `VitePWA` com manifest completo |
| `src/shared/components/StatusBadge.tsx` | **UPDATE** | `role="status"` + `aria-label` |
| `src/pages/agenda/FormularioAgendamento.tsx` | **UPDATE** | `aria-required` + `aria-describedby` |
| `src/pages/clientes/ClienteFormPage.tsx` | **UPDATE** | `aria-required` + `aria-describedby` |
| `src/pages/auth/LoginPage.tsx` | **UPDATE** | `aria-required` |
| `src/pages/auth/TrocaSenhaPage.tsx` | **UPDATE** | `aria-required` |
| `test/.../AgendamentoCardA11yTest.tsx` | NEW | |
| `test/.../EstadoVazioTest.tsx` | NEW | |

### Referências

- [Source: epics.md#Story 6.3] — Acceptance Criteria completos e UX-DR2, UX-DR11
- [Source: 3-1-componentes-base-ui.md] — `Skeleton`, `SkeletonCard`, `SkeletonList`, `ToastProvider`
- [Source: 1-1-monorepo-cicd-deploy.md] — `vite-plugin-pwa` configuração inicial
- [Source: 1-3-frontend-design-tokens-routing.md] — `globals.css` tokens de cor, `AppLayout`
- [Source: 4-1-agenda-dashboard.md] — `AgendamentoCard`, FAB, polling

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
