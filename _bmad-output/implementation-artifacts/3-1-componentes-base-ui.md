# Story 3.1: Componentes Base de UI e Design System

Status: ready-for-dev

## Story

Como usuário do DrAgenda,
Quero que toda a interface use cores, ícones e rótulos consistentes para indicar o estado dos agendamentos,
Para que eu reconheça o status de qualquer agendamento em qualquer tela sem depender apenas de cor.

## Acceptance Criteria

**AC-1 — `StatusBadge` com ícone + rótulo + borda lateral (UX-DR2)**
- Given o componente `StatusBadge` é renderizado com qualquer um dos 6 status
- When exibido na interface
- Then exibe obrigatoriamente: cor de fundo do token + borda lateral colorida 4px + ícone Lucide + rótulo de texto
- And nenhum status é diferenciado apenas por cor (WCAG 2.1 AA — regra inviolável)
- And os 6 status mapeiam exatamente conforme DESIGN.md:

| Status | Cor Principal | Fundo | Ícone Lucide | Rótulo |
|---|---|---|---|---|
| `PENDENTE` | `#D97706` | `#FFFBEB` | `Clock` | "Pendente" |
| `CONFIRMADO` | `#2563EB` | `#EFF6FF` | `CheckCircle` | "Confirmado" |
| `PRESENTE` | `#7C3AED` | `#F5F3FF` | `UserCheck` | "Presente" |
| `CONCLUIDO` | `#16A34A` | `#F0FDF4` | `CircleCheckBig` | "Concluído" |
| `CANCELADO` | `#4B5563` | `#F9FAFB` | `XCircle` | "Cancelado" |
| `NOSHOW` | `#DC2626` | `#FEF2F2` | `UserX` | "NoShow" |

**AC-2 — Skeleton screens durante carregamento**
- Given qualquer tela carrega dados da API (`isLoading = true`)
- When a requisição está em andamento
- Then skeleton screens com animação shimmer substituem os elementos de conteúdo
- And na agenda: 3 cards placeholder com shimmer
- And em listas: linhas placeholder com shimmer

**AC-3 — Componente `Toast` global**
- Given uma ação ocorre no sistema (sucesso, erro, aviso, info)
- When o toast é disparado
- Then aparece no topo central com: ícone + texto descritivo + cor correspondente ao tipo
- And tipos: sucesso `#16A34A` · erro `#DC2626` · aviso `#D97706` · info `#2563EB`
- And durações: sucesso 3s · info 3s · aviso 4s · erro 5s
- And acessível via `aria-live="polite"` para leitores de tela

## Tasks / Subtasks

- [ ] **Task 1 — `StatusBadge.tsx`** (AC-1)
  - [ ] Criar `src/shared/components/StatusBadge.tsx`
  - [ ] Definir tipo `AgendamentoStatus = 'PENDENTE' | 'CONFIRMADO' | 'PRESENTE' | 'CONCLUIDO' | 'CANCELADO' | 'NOSHOW'`
  - [ ] Exportar `STATUS_CONFIG` como `Record<AgendamentoStatus, StatusConfig>` — mapa único de verdade para toda a app
  - [ ] Props: `status: AgendamentoStatus`, `size?: 'sm' | 'md'` (padrão `'md'`)
  - [ ] Estrutura: `<span>` com `bg`, `border-l-4`, ícone Lucide 16px, rótulo de texto
  - [ ] Escrever testes unitários com React Testing Library: todos os 6 status renderizam ícone + rótulo corretos

- [ ] **Task 2 — `Skeleton.tsx` e `SkeletonCard.tsx`** (AC-2)
  - [ ] Criar `src/shared/components/Skeleton.tsx`: componente genérico com animação shimmer
  - [ ] Criar `src/shared/components/SkeletonCard.tsx`: placeholder de card de agendamento (3 linhas de texto shimmer)
  - [ ] Criar `src/shared/components/SkeletonList.tsx`: placeholder de listagem (5 linhas shimmer de largura variada)
  - [ ] Animação shimmer via Tailwind: `animate-pulse bg-[var(--bg-muted)] rounded`

- [ ] **Task 3 — `Toast` global (provider + hook)** (AC-3)
  - [ ] Criar `src/shared/components/ToastProvider.tsx`: provider que gerencia fila de toasts via `useState`
  - [ ] Criar `src/shared/hooks/useToast.ts`: hook que expõe `toast.success(msg)`, `toast.error(msg)`, `toast.warning(msg)`, `toast.info(msg)`
  - [ ] Criar `src/shared/components/Toast.tsx`: componente visual individual (ícone + mensagem + barra de progresso de duração)
  - [ ] Posicionamento: `fixed top-4 left-1/2 -translate-x-1/2 z-[100]`
  - [ ] Adicionar `<ToastProvider>` ao `main.tsx` envolvendo toda a app
  - [ ] `aria-live="polite"` no container de toasts para acessibilidade

- [ ] **Task 4 — `useToast` integrado nas chamadas de API existentes** (AC-3)
  - [ ] Atualizar `LoginPage.tsx` (Story 2.1): erros de login exibidos via `toast.error()` em vez de estado local
  - [ ] Atualizar `TrocarSenhaPage.tsx` (Story 2.6): sucesso via `toast.success('Senha alterada com sucesso')`
  - [ ] Este padrão se torna obrigatório para todas as stories futuras

- [ ] **Task 5 — Exportar tipos e constantes compartilhados** (AC-1)
  - [ ] Criar `src/shared/types/agendamento.ts` com `AgendamentoStatus` e `STATUS_CONFIG`
  - [ ] Criar (ou atualizar) `src/shared/index.ts` re-exportando: `StatusBadge`, `Skeleton`, `SkeletonCard`, `SkeletonList`, `useToast`, tipos

- [ ] **Task 6 — Testes** (AC-1, AC-2, AC-3)
  - [ ] `StatusBadge.test.tsx`: todos os 6 status renderizam cor + ícone + rótulo corretos
  - [ ] `StatusBadge.test.tsx`: snapshot test para garantir que mudanças no design system são detectadas
  - [ ] `useToast.test.ts`: chamar `toast.success()` adiciona item na fila; após duração, é removido

## Dev Notes

### `STATUS_CONFIG` — Fonte Única de Verdade

```ts
// src/shared/types/agendamento.ts

import {
  Clock, CheckCircle, UserCheck, CircleCheckBig, XCircle, UserX,
  type LucideIcon
} from 'lucide-react'

export type AgendamentoStatus =
  | 'PENDENTE' | 'CONFIRMADO' | 'PRESENTE' | 'CONCLUIDO' | 'CANCELADO' | 'NOSHOW'

export type StatusConfig = {
  cor: string        // cor principal (texto, ícone, borda)
  fundo: string      // cor de fundo do badge
  icone: LucideIcon
  rotulo: string     // rótulo de texto obrigatório
}

export const STATUS_CONFIG: Record<AgendamentoStatus, StatusConfig> = {
  PENDENTE:   { cor: '#D97706', fundo: '#FFFBEB', icone: Clock,          rotulo: 'Pendente'   },
  CONFIRMADO: { cor: '#2563EB', fundo: '#EFF6FF', icone: CheckCircle,    rotulo: 'Confirmado' },
  PRESENTE:   { cor: '#7C3AED', fundo: '#F5F3FF', icone: UserCheck,      rotulo: 'Presente'   },
  CONCLUIDO:  { cor: '#16A34A', fundo: '#F0FDF4', icone: CircleCheckBig, rotulo: 'Concluído'  },
  CANCELADO:  { cor: '#4B5563', fundo: '#F9FAFB', icone: XCircle,        rotulo: 'Cancelado'  },
  NOSHOW:     { cor: '#DC2626', fundo: '#FEF2F2', icone: UserX,          rotulo: 'NoShow'     },
}
```

> **Importante:** `STATUS_CONFIG` é a única fonte de verdade para cores e ícones de status em toda a aplicação. Todas as telas do Epic 4 em diante importam este objeto — nunca hardcoding de cores inline.

### `StatusBadge.tsx` — Implementação

```tsx
// src/shared/components/StatusBadge.tsx
import { STATUS_CONFIG, type AgendamentoStatus } from '../types/agendamento'

type Props = {
  status: AgendamentoStatus
  size?: 'sm' | 'md'
}

export function StatusBadge({ status, size = 'md' }: Props) {
  const config = STATUS_CONFIG[status]
  const Icon = config.icone
  const iconSize = size === 'sm' ? 12 : 16
  const textClass = size === 'sm' ? 'text-xs' : 'text-sm'

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-radius-sm font-medium ${textClass}`}
      style={{
        backgroundColor: config.fundo,
        borderLeft: `4px solid ${config.cor}`,
        color: config.cor,
      }}
    >
      <Icon size={iconSize} aria-hidden="true" />
      <span>{config.rotulo}</span>
    </span>
  )
}
```

> **Regra WCAG:** A borda lateral 4px + ícone + rótulo garantem que daltônicos reconheçam qualquer status sem depender de cor. Não remover nenhum desses 3 elementos.

### `Skeleton.tsx` — Componente Base

```tsx
// src/shared/components/Skeleton.tsx
import { cn } from '@/lib/utils'   // cn = clsx utility do Shadcn

type Props = {
  className?: string
}

export function Skeleton({ className }: Props) {
  return (
    <div
      className={cn('animate-pulse rounded-radius-sm bg-[var(--bg-muted)]', className)}
      aria-hidden="true"
    />
  )
}

// src/shared/components/SkeletonCard.tsx
export function SkeletonCard() {
  return (
    <div className="p-4 rounded-radius-md border border-[var(--border)] space-y-2">
      <Skeleton className="h-4 w-1/3" />
      <Skeleton className="h-3 w-2/3" />
      <Skeleton className="h-3 w-1/2" />
    </div>
  )
}

// src/shared/components/SkeletonList.tsx
export function SkeletonList({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex items-center gap-3">
          <Skeleton className="h-8 w-8 rounded-full flex-shrink-0" />
          <div className="flex-1 space-y-1.5">
            <Skeleton className="h-3" style={{ width: `${60 + (i % 3) * 15}%` }} />
            <Skeleton className="h-2.5 w-2/5" />
          </div>
        </div>
      ))}
    </div>
  )
}
```

### `ToastProvider` + `useToast`

```tsx
// src/shared/hooks/useToast.ts
import { useContext } from 'react'
import { ToastContext } from '../components/ToastProvider'

export function useToast() {
  return useContext(ToastContext)
}

// src/shared/components/ToastProvider.tsx
import { createContext, useState, useCallback, useRef } from 'react'
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-react'

type ToastType = 'success' | 'error' | 'warning' | 'info'
type ToastItem = { id: number; type: ToastType; message: string }

const DURATIONS: Record<ToastType, number> = {
  success: 3000,
  info:    3000,
  warning: 4000,
  error:   5000,
}

const STYLES: Record<ToastType, { bg: string; icon: typeof CheckCircle }> = {
  success: { bg: '#16A34A', icon: CheckCircle    },
  error:   { bg: '#DC2626', icon: XCircle        },
  warning: { bg: '#D97706', icon: AlertTriangle  },
  info:    { bg: '#2563EB', icon: Info           },
}

type ToastContextValue = {
  success: (msg: string) => void
  error:   (msg: string) => void
  warning: (msg: string) => void
  info:    (msg: string) => void
}

export const ToastContext = createContext<ToastContextValue>({
  success: () => {},
  error:   () => {},
  warning: () => {},
  info:    () => {},
})

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const nextId = useRef(0)

  const addToast = useCallback((type: ToastType, message: string) => {
    const id = ++nextId.current
    setToasts(prev => [...prev, { id, type, message }])
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id))
    }, DURATIONS[type])
  }, [])

  const value: ToastContextValue = {
    success: (msg) => addToast('success', msg),
    error:   (msg) => addToast('error',   msg),
    warning: (msg) => addToast('warning', msg),
    info:    (msg) => addToast('info',    msg),
  }

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        className="fixed top-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 min-w-[280px] max-w-sm"
        aria-live="polite"
        aria-atomic="false"
      >
        {toasts.map(toast => {
          const { bg, icon: Icon } = STYLES[toast.type]
          return (
            <div
              key={toast.id}
              className="flex items-center gap-3 px-4 py-3 rounded-radius-md text-white shadow-elevation-2 text-sm"
              style={{ backgroundColor: bg }}
              role="status"
            >
              <Icon size={18} aria-hidden="true" />
              <span className="flex-1">{toast.message}</span>
              <button
                onClick={() => setToasts(prev => prev.filter(t => t.id !== toast.id))}
                className="opacity-70 hover:opacity-100 transition-opacity"
                aria-label="Fechar notificação"
              >
                <X size={16} />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}
```

### Integração no `main.tsx`

```tsx
// main.tsx — envolver a app com ToastProvider:
import { ToastProvider } from './shared/components/ToastProvider'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastProvider>
          <App />
        </ToastProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
)
```

### Padrão de Uso do `useToast` nas Stories Futuras

```tsx
// Exemplo — criar agendamento (Story 4.2):
const toast = useToast()

const { mutate } = useMutation({
  mutationFn: criarAgendamento,
  onSuccess: () => {
    toast.success('Agendamento criado com sucesso')
    queryClient.invalidateQueries({ queryKey: ['agendamentos'] })
  },
  onError: (err) => {
    toast.error(err.message || 'Erro ao criar agendamento')
  },
})
```

> Este padrão é obrigatório a partir desta story. Nenhuma tela das stories seguintes deve usar `alert()`, `console.error()` ou estado local para feedback de ações.

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `src/shared/types/agendamento.ts` | NEW | `AgendamentoStatus` + `STATUS_CONFIG` — fonte única |
| `src/shared/components/StatusBadge.tsx` | NEW | Borda lateral 4px + ícone + rótulo |
| `src/shared/components/Skeleton.tsx` | NEW | Componente base shimmer |
| `src/shared/components/SkeletonCard.tsx` | NEW | Placeholder de card de agendamento |
| `src/shared/components/SkeletonList.tsx` | NEW | Placeholder de listagem |
| `src/shared/components/ToastProvider.tsx` | NEW | Provider com queue de toasts |
| `src/shared/hooks/useToast.ts` | NEW | Hook para disparar toasts |
| `src/main.tsx` | **UPDATE** | Envolver com `<ToastProvider>` |
| `src/pages/LoginPage.tsx` | **UPDATE** | Erros via `toast.error()` |
| `src/pages/TrocarSenhaPage.tsx` | **UPDATE** | Sucesso via `toast.success()` |
| `src/shared/index.ts` | NEW (ou UPDATE) | Re-export de componentes compartilhados |
| `src/shared/components/StatusBadge.test.tsx` | NEW | Testes dos 6 status |
| `src/shared/hooks/useToast.test.ts` | NEW | Fila de toasts |

### Referências

- [Source: epics.md#Story 3.1] — Acceptance Criteria completos e UX-DR2
- [Source: DESIGN.md#Cores de Status dos Agendamentos] — mapeamento exato cor/ícone/rótulo por status
- [Source: DESIGN.md#Paleta Neutra] — tokens `bg-muted`, `border`, `elevation`
- [Source: architecture.md#Frontend Stack] — TanStack Query, React 18, Lucide Icons

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
