# Story 4.1: Dashboard de Agenda — Estrutura de Dados, Cards e Navegação

Status: ready-for-dev

## Story

Como usuário do DrAgenda,
Quero visualizar a agenda do dia e da semana com cards coloridos por status e navegar entre datas,
Para que eu saiba imediatamente o estado de cada agendamento sem ambiguidade.

## Acceptance Criteria

**AC-1 — Migration V8: tabela `agendamentos`**
- Given a migration `V{yyyyMMddHHmm}__create_agendamentos.sql` é aplicada
- When o sistema inicializa
- Then a tabela `agendamentos` existe com colunas:
  - `id` BIGINT PK AUTO_INCREMENT
  - `empresa_id` BIGINT FK NOT NULL → `empresas.id`
  - `cliente_id` BIGINT FK NOT NULL → `clientes.id`
  - `profissional_id` BIGINT FK NOT NULL → `profissionais.id`
  - `tipo_atendimento_id` BIGINT FK NOT NULL → `tipos_atendimento.id`
  - `horario_inicio` DATETIME NOT NULL (UTC)
  - `horario_fim` DATETIME NOT NULL (UTC)
  - `status` ENUM('PENDENTE','CONFIRMADO','PRESENTE','CONCLUIDO','CANCELADO','NOSHOW') NOT NULL DEFAULT 'PENDENTE'
  - `public_token` VARCHAR(36) UNIQUE NULL
  - `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
- And índices `idx_agendamentos_empresa_id`, `idx_agendamentos_profissional_id`, `idx_agendamentos_horario_inicio`

**AC-2 — Listagem da agenda com filtros**
- Given usuário autenticado acessa `GET /api/v1/agendamentos?data=2026-06-04`
- When o backend processa
- Then retorna agendamentos da empresa naquela data (FR-015) com os campos: `id`, `horarioInicio`, `horarioFim`, `status`, `cliente` (nome descriptografado + telefone), `profissional` (nome), `tipoAtendimento` (nome)
- And Profissional recebe apenas seus próprios agendamentos — filtro por `profissionalId` do JWT (FR-005)
- And Staff e Admin Empresa recebem todos da empresa
- And aceita filtro opcional `?profissionalId=` (para Staff/Admin filtrarem por profissional específico)

**AC-3 — `AgendamentoCard` com status visual (UX-DR3)**
- Given o frontend renderiza lista de agendamentos
- When os cards são exibidos
- Then cada card exibe: horário início–fim, nome do cliente (text-lg), tipo de atendimento + profissional (text-sm), `StatusBadge` da Story 3.1
- And borda lateral esquerda 4px com cor do status (mesmo token de `STATUS_CONFIG`)
- And toque no card abre o detalhe do agendamento

**AC-4 — Visualizações Dia e Semana com navegação (UX-DR8)**
- Given o usuário está na tela de agenda
- When interage com a navegação
- Then toggle segmentado `[ Dia | Semana ]` no topo alterna entre visualizações (FR-014)
- And swipe horizontal avança/retrocede dias (visualização diária) ou semanas (semanal)
- And chips de profissional roláveis horizontalmente abaixo do toggle filtram a agenda (FR-015, FR-017)
- And botão "Hoje" aparece no header quando data ≠ hoje

**AC-5 — Layout responsivo**
- Given viewport ≤ 767px
- When agenda renderiza
- Then lista vertical ordenada por horário (FR-021)
- Given viewport ≥ 768px
- When agenda renderiza
- Then grade horária com horários em coluna esquerda e cards em colunas de profissional

**AC-6 — Badges especiais (UX-DR12)**
- Given o usuário é Profissional e há agendamentos no dia
- When os cards renderizam
- Then o primeiro agendamento do dia exibe badge `⭐ Primeiro do dia` com fundo `brand-50` (FR-018)
- Given um paciente tem status PRESENTE e seu horário é posterior a outro card PENDENTE/CONFIRMADO
- When o frontend avalia a ordem dos cards
- Then o card exibe badge `⚠️ Chegou antes do horário` (FR-020)

**AC-7 — Polling automático (FR-019)**
- Given a tela de agenda está visível
- When o TanStack Query está configurado
- Then `refetchInterval: 30000` mantém a agenda atualizada silenciosamente
- And se novos itens aparecerem após refetch, exibe `toast.info("Agenda atualizada")` com duração de 2s

## Tasks / Subtasks

- [ ] **Task 1 — Migration V8** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__create_agendamentos.sql`
  - [ ] Usar timestamp real no momento da implementação
  - [ ] SQL com tabela + todos os índices + FKs nomeadas

- [ ] **Task 2 — Enum `StatusAgendamento`** (AC-1)
  - [ ] Criar `domain/enums/StatusAgendamento.java`: `PENDENTE, CONFIRMADO, PRESENTE, CONCLUIDO, CANCELADO, NOSHOW`
  - [ ] `@JsonProperty` para serialização correta

- [ ] **Task 3 — Entidade `Agendamento`** (AC-1)
  - [ ] Criar `domain/entities/Agendamento.java`
  - [ ] Campos: `id`, `empresa` (ManyToOne lazy), `cliente` (ManyToOne lazy), `profissional` (ManyToOne lazy), `tipoAtendimento` (ManyToOne lazy), `horarioInicio` (Instant), `horarioFim` (Instant), `status` (Enum), `publicToken` (nullable), `createdAt` (Instant)
  - [ ] `@Enumerated(EnumType.STRING)` em `status`
  - [ ] `@PrePersist` para `createdAt`

- [ ] **Task 4 — `AgendamentoRepository`** (AC-2)
  - [ ] Criar `domain/repositories/AgendamentoRepository.java`
  - [ ] `List<Agendamento> findAllByEmpresaIdAndHorarioInicioBetween(Long empresaId, Instant inicio, Instant fim)` — listagem do dia/semana
  - [ ] `List<Agendamento> findAllByProfissionalIdAndHorarioInicioBetween(Long profissionalId, Instant inicio, Instant fim)` — filtro para Profissional
  - [ ] `Optional<Agendamento> findByIdAndEmpresaId(Long id, Long empresaId)` — isolamento

- [ ] **Task 5 — DTO de response enriquecido** (AC-2)
  - [ ] Criar `api/dtos/response/AgendamentoResponse.java`:
    ```java
    record AgendamentoResponse(
        Long id,
        Instant horarioInicio,
        Instant horarioFim,
        String status,
        ClienteResumo cliente,        // id, nome (decriptado), telefone
        ProfissionalResumo profissional, // id, nome
        TipoAtendimentoResumo tipoAtendimento // id, nome, duracaoMinutos
    ) {}
    ```
  - [ ] Criar inner records `ClienteResumo`, `ProfissionalResumo`, `TipoAtendimentoResumo`

- [ ] **Task 6 — `AgendaService`** (AC-2)
  - [ ] Criar `domain/services/AgendaService.java`
  - [ ] `listar(Long empresaId, Long profissionalIdJwt, String perfil, LocalDate data, Long profissionalIdFiltro)`:
    - Calcular `inicio = data.atStartOfDay(UTC)` e `fim = inicio + 1 dia`
    - Se `perfil == PROFISSIONAL`: usar `findAllByProfissionalIdAndHorarioInicioBetween(profissionalIdJwt, ...)`
    - Se `perfil == STAFF ou ADMIN_EMPRESA`:
      - Se `profissionalIdFiltro != null`: filtrar por profissional
      - Senão: usar `findAllByEmpresaIdAndHorarioInicioBetween(empresaId, ...)`
    - Mapear para `AgendamentoResponse` — descriptografar `cliente.nome` e `cliente.telefone` via entidade (automático pelo converter)
    - Ordenar por `horarioInicio` ASC

- [ ] **Task 7 — `AgendamentoController` (novo, sem stub anterior)** (AC-2)
  - [ ] Criar `api/controllers/AgendamentoController.java`
  - [ ] `GET /api/v1/agendamentos` → `@PreAuthorize("hasAnyRole('STAFF','PROFISSIONAL','ADMIN_EMPRESA')")`
    - Query params: `data` (ISO `yyyy-MM-dd`, obrigatório), `profissionalId` (opcional)
  - [ ] Usar `SecurityUtils.getEmpresaId()`, `SecurityUtils.getUserId()`, `SecurityUtils.getPerfil()`

- [ ] **Task 8 — `SecurityUtils` — adicionar `getPerfil()`** (AC-2)
  - [ ] Adicionar método `getPerfil()` ao `SecurityUtils.java` (Story 2.1) — lê de `auth.getDetails()` da mesma forma que `getEmpresaId()`

- [ ] **Task 9 — `AgendaPage.tsx`** (AC-3, AC-4, AC-5, AC-6, AC-7)
  - [ ] Criar `src/pages/AgendaPage.tsx`
  - [ ] Estado local: `dataSelecionada` (default: hoje), `visualizacao: 'dia' | 'semana'`, `profissionalFiltro`
  - [ ] `useQuery` com `queryKey: ['agendamentos', dataSelecionada, profissionalFiltro]` e `refetchInterval: 30000`
  - [ ] Detecção de novos itens após refetch: comparar com snapshot anterior via `useRef`; se diferente → `toast.info('Agenda atualizada')` com `autoClose: 2000`
  - [ ] Skeleton 3 cards durante carregamento inicial

- [ ] **Task 10 — `AgendamentoCard.tsx`** (AC-3, AC-6)
  - [ ] Criar `src/shared/components/AgendamentoCard.tsx`
  - [ ] Props: `agendamento: AgendamentoResponse`, `isPrimeiroHoje?: boolean`, `chegouAntes?: boolean`
  - [ ] Estrutura:
    - Borda lateral 4px com `STATUS_CONFIG[status].cor`
    - Fundo `STATUS_CONFIG[status].fundo`
    - Linha 1: horário `HH:mm – HH:mm` (via `useFusoHorario`) + `StatusBadge`
    - Linha 2: nome do cliente (`text-lg font-medium`)
    - Linha 3: tipo atendimento · profissional (`text-sm text-secondary`)
    - Se `isPrimeiroHoje`: badge `⭐ Primeiro do dia` com `bg-brand-50 text-brand-600`
    - Se `chegouAntes`: badge `⚠️ Chegou antes do horário` com `bg-amber-50 text-amber-600`

- [ ] **Task 11 — Navegação por data (swipe + toggle)** (AC-4)
  - [ ] Criar `src/shared/hooks/useSwipeNavigation.ts` com `onSwipeLeft` e `onSwipeRight` via `touchstart`/`touchend`
  - [ ] `AgendaPage` usa o hook para avançar/retroceder dia/semana
  - [ ] Toggle segmentado: dois botões com active state `bg-brand-500 text-white`, inativo `bg-[var(--bg-subtle)]`

- [ ] **Task 12 — Chips de profissional** (AC-4)
  - [ ] Carregar lista via `GET /api/v1/profissionais` (só ativos)
  - [ ] Chips roláveis horizontalmente com `overflow-x-auto` sem scrollbar visível
  - [ ] Chip selecionado: `bg-brand-500 text-white rounded-full`; não selecionado: `border border-[var(--border)]`
  - [ ] Chip "Todos" como primeira opção (limpa o filtro)

- [ ] **Task 13 — Testes** (AC-2, AC-7)
  - [ ] Teste unitário `AgendaServiceTest`:
    - Listagem: filtro por data correto (início do dia UTC)
    - Profissional: recebe apenas seus agendamentos
    - Staff: recebe todos ou filtrados por profissionalId
  - [ ] Teste de integração `AgendamentoControllerIT`:
    - GET sem `data`: HTTP 400
    - GET com data: 200, data descriptografada no response
    - PROFISSIONAL: 200 apenas com seus agendamentos

## Dev Notes

### ⚠️ Migration com timestamp (não V8 sequencial) — R6

```sql
-- CORRETO: V202506042000__create_agendamentos.sql
-- ERRADO:  V8__create_agendamentos.sql
```

### Migration SQL

```sql
-- V202506042000__create_agendamentos.sql (usar timestamp real)

CREATE TABLE agendamentos (
    id                  BIGINT   NOT NULL AUTO_INCREMENT,
    empresa_id          BIGINT   NOT NULL,
    cliente_id          BIGINT   NOT NULL,
    profissional_id     BIGINT   NOT NULL,
    tipo_atendimento_id BIGINT   NOT NULL,
    horario_inicio      DATETIME NOT NULL,
    horario_fim         DATETIME NOT NULL,
    status              ENUM('PENDENTE','CONFIRMADO','PRESENTE','CONCLUIDO','CANCELADO','NOSHOW')
                                 NOT NULL DEFAULT 'PENDENTE',
    public_token        VARCHAR(36) NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_agendamentos_public_token UNIQUE (public_token),
    CONSTRAINT fk_agendamentos_empresa       FOREIGN KEY (empresa_id)          REFERENCES empresas(id),
    CONSTRAINT fk_agendamentos_cliente       FOREIGN KEY (cliente_id)           REFERENCES clientes(id),
    CONSTRAINT fk_agendamentos_profissional  FOREIGN KEY (profissional_id)      REFERENCES profissionais(id),
    CONSTRAINT fk_agendamentos_tipo          FOREIGN KEY (tipo_atendimento_id)  REFERENCES tipos_atendimento(id)
);

CREATE INDEX idx_agendamentos_empresa_id       ON agendamentos (empresa_id);
CREATE INDEX idx_agendamentos_profissional_id  ON agendamentos (profissional_id);
CREATE INDEX idx_agendamentos_horario_inicio   ON agendamentos (horario_inicio);
```

> **H2:** `ENUM` — usar mesma estratégia da Story 3.4 (perfil de migration H2 separado ou `@Enumerated(EnumType.STRING)` com VARCHAR no DDL de teste).

### `AgendaService` — Cálculo do Intervalo UTC

```java
// Dado uma data local (ex: 2026-06-04) e o fuso da empresa:
// O backend não conhece o fuso — recebe apenas a data e retorna tudo em UTC.
// O frontend converte UTC → fuso local para exibição.
//
// Estratégia: retornar agendamentos cujo horario_inicio está no dia
// considerado em UTC (suficiente para a maioria dos casos).
// Para fusos como America/Noronha (UTC-2), agendamentos das 22:00-23:59
// do dia anterior UTC podem aparecer no dia local — aceitar essa limitação
// no MVP ou documentar como known limitation.

Instant inicio = data.atStartOfDay(ZoneOffset.UTC).toInstant();
Instant fim    = data.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
```

### `AgendamentoCard.tsx` — Badges Especiais

```tsx
// Lógica de badge "Chegou antes do horário" no AgendaPage:
function calcularBadges(agendamentos: AgendamentoResponse[]) {
  return agendamentos.map((ag, index) => {
    const isPrimeiroHoje = index === 0  // ordenado por horário ASC

    // "Chegou antes": status PRESENTE e há algum card anterior ainda PENDENTE/CONFIRMADO
    const chegouAntes = ag.status === 'PRESENTE' &&
      agendamentos.some(outro =>
        (outro.status === 'PENDENTE' || outro.status === 'CONFIRMADO') &&
        new Date(outro.horarioInicio) > new Date(ag.horarioInicio)
      )

    return { ...ag, isPrimeiroHoje, chegouAntes }
  })
}
```

### Polling com Detecção de Novos Itens

```tsx
const prevCountRef = useRef(0)

const { data } = useQuery({
  queryKey: ['agendamentos', dataSelecionada, profissionalFiltro],
  queryFn: fetchAgendamentos,
  refetchInterval: 30_000,
})

useEffect(() => {
  if (!data) return
  const currentCount = data.length
  if (prevCountRef.current > 0 && currentCount > prevCountRef.current) {
    toast.info('Agenda atualizada')   // duração 2s — configurar no ToastProvider
  }
  prevCountRef.current = currentCount
}, [data])
```

> **Nota:** `toast.info` usa duração de 3s por padrão (Story 3.1). Para a notificação de polling, passar duração customizada de 2s ou configurar um tipo `toast.silent` — à escolha do dev. O comportamento silencioso é mais importante do que a duração exata.

### `SecurityUtils` — Adicionar `getPerfil()`

```java
// Adicionar ao SecurityUtils.java (Story 2.1):
public static String getPerfil() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Map<String, Object> details = (Map<String, Object>) auth.getDetails();
    return (String) details.get("perfil");
}
```

> O `perfil` deve ter sido incluído no `details` Map pelo `JwtAuthenticationFilter` (Story 2.1). Verificar se já existe — se não, adicionar junto com `empresaId` e `userId` no mesmo filtro.

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__create_agendamentos.sql` | NEW | 3 índices + UNIQUE public_token |
| `domain/enums/StatusAgendamento.java` | NEW | 6 status |
| `domain/entities/Agendamento.java` | NEW | `@Enumerated(EnumType.STRING)` |
| `domain/repositories/AgendamentoRepository.java` | NEW | Queries por data + profissional |
| `api/dtos/response/AgendamentoResponse.java` | NEW | Inclui resumos de cliente/profissional/tipo |
| `domain/services/AgendaService.java` | NEW | Lógica de filtro por perfil |
| `api/controllers/AgendamentoController.java` | NEW | GET /api/v1/agendamentos |
| `infrastructure/config/SecurityUtils.java` | **UPDATE** | Adicionar `getPerfil()` |
| `src/pages/AgendaPage.tsx` | NEW | Estado + query + polling |
| `src/shared/components/AgendamentoCard.tsx` | NEW | Card com borda + badges |
| `src/shared/hooks/useSwipeNavigation.ts` | NEW | Touch swipe |
| `test/.../AgendaServiceTest.java` | NEW | Unitários: filtro data, perfil |
| `test/.../AgendamentoControllerIT.java` | NEW | Integração: GET com data |

### Referências

- [Source: epics.md#Story 4.1] — Acceptance Criteria completos e FR-005, FR-014–FR-021
- [Source: DESIGN.md#Cores de Status] — `STATUS_CONFIG` da Story 3.1
- [Source: DESIGN.md#UX-DR3, UX-DR8, UX-DR12] — card layout, toggle, badges
- [Source: architecture.md#Banco de Dados] — UTC, índices em FKs
- [Source: epics.md#R7 gate Epic 4] — Epics 1+2+3 obrigatórios antes deste Epic
- [Source: 3-1-componentes-base-ui.md] — `StatusBadge`, `SkeletonCard`, `useToast`
- [Source: 3-2-configuracoes-clinica.md] — `useFusoHorario` para exibir horários

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
