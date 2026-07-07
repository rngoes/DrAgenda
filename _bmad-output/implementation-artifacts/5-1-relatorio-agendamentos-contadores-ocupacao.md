# Story 5.1: Relatório de Agendamentos — Contadores e Taxa de Ocupação

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero visualizar relatórios de agendamentos por período e profissional com contadores e taxa de ocupação,
Para que eu possa medir o desempenho da clínica e identificar padrões de uso.

## Acceptance Criteria

**AC-1 — Endpoint `GET /api/v1/relatorios/agendamentos` (FR-032, FR-033)**
- Given Admin Empresa chama `GET /api/v1/relatorios/agendamentos?dataInicio=2026-06-01&dataFim=2026-06-30` com parâmetro opcional `profissionalId=`
- When o backend processa
- Then retorna contadores por status no período:
  - `agendados` (total de agendamentos no período)
  - `confirmados` (status CONFIRMADO)
  - `concluidos` (status CONCLUIDO)
  - `cancelados` (status CANCELADO)
  - `noShows` (status NOSHOW)
- And retorna duas taxas de ocupação:
  - `ocupacaoConfirmada` = (CONFIRMADO + PRESENTE + CONCLUIDO + NOSHOW) / total slots disponíveis × 100
  - `ocupacaoTotal` = (todos exceto CANCELADO) / total slots disponíveis × 100
- And retorna bloco consolidado e, se `profissionalId` não fornecido, bloco por profissional
- And apenas Admin Empresa da própria empresa acessa — HTTP 403 para Staff e Profissional (FR-032)

**AC-2 — Cálculo de total slots disponíveis (FR-034)**
- Given o cálculo de `totalSlotsDisponiveis` é necessário para a taxa de ocupação
- When o backend computa para o período
- Then soma todos os slots de `duracao_minutos` de cada tipo de atendimento dentro das faixas de `disponibilidades` configuradas para cada dia do período
- And exclui intervalos (`inicio_intervalo` e `fim_intervalo`) no cálculo
- And considera apenas profissionais com `ativo = true` e apenas os tipos de atendimento com `ativo = true`
- And se `totalSlotsDisponiveis = 0`, retorna taxas como `null` (evita divisão por zero)

**AC-3 — Segurança: isolamento por empresa**
- Given qualquer chamada ao endpoint
- When o `SecurityUtils.getEmpresaId()` é usado no service
- Then apenas agendamentos, disponibilidades e tipos da empresa do Admin autenticado são considerados
- And `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` no controller garante que Staff e Profissional recebem 403

**AC-4 — Baseline: exibição quando não configurado**
- Given o Admin Empresa ainda não configurou `baseline_noshow_pct` (Story 5.2)
- When o relatório é exibido
- Then `baseline` no response é `null`
- And a `RelatoriosPage.tsx` exibe `"—"` com nota `"Configure o baseline em Configurações para comparação após 60 dias"`

**AC-5 — Tela `/relatorios` — formulário de filtros**
- Given Admin Empresa acessa a rota `/relatorios`
- When a tela renderiza
- Then exibe: date pickers `Data início` e `Data fim` (padrão: primeiro e último dia do mês corrente)
- And select `Profissional` (opcional — "Todos os profissionais" como padrão)
- And botão `[Gerar Relatório]` que dispara a query

**AC-6 — Tela `/relatorios` — exibição dos contadores (FR-033)**
- Given o backend retorna response com sucesso
- When a `RelatoriosPage.tsx` renderiza
- Then exibe cards de contadores: Agendados, Confirmados, Concluídos, Cancelados, No-Shows
- And exibe as duas taxas de ocupação em formato percentual: `"Ocupação confirmada: 72%" / "Ocupação total: 85%"` ou `"—"` se `totalSlotsDisponiveis = 0`
- And se `profissionalId` não filtrado, exibe tabela por profissional com contadores individuais
- And estado vazio quando não há dados: "Nenhum agendamento encontrado no período selecionado."

## Tasks / Subtasks

- [ ] **Task 1 — `RelatorioAgendamentosService.java`** (AC-1, AC-2, AC-3)
  - [ ] Criar `domain/services/RelatorioAgendamentosService.java`
  - [ ] Método `gerar(Long empresaId, LocalDate dataInicio, LocalDate dataFim, Long profissionalIdFiltro)` → `RelatorioAgendamentosResponse`
  - [ ] Converter datas para Instant UTC usando fuso da empresa (`FusoHorarioBrasileiro` da Story 3.2)
  - [ ] Query de contadores por status no período:
    ```java
    List<Object[]> findContadoresPorStatus(Long empresaId, Instant inicio, Instant fim, Long profissionalId)
    ```
  - [ ] Cálculo de `totalSlotsDisponiveis` — ver AC-2 e Dev Notes

- [ ] **Task 2 — Queries no `AgendamentoRepository`** (AC-1)
  - [ ] Adicionar:
    ```java
    @Query("""
        SELECT a.status, COUNT(a)
        FROM Agendamento a
        WHERE a.empresa.id = :empresaId
          AND a.horarioInicio >= :inicio
          AND a.horarioInicio < :fim
          AND (:profissionalId IS NULL OR a.profissional.id = :profissionalId)
        GROUP BY a.status
        """)
    List<Object[]> countPorStatus(
        @Param("empresaId") Long empresaId,
        @Param("inicio") Instant inicio,
        @Param("fim") Instant fim,
        @Param("profissionalId") Long profissionalId
    );
    ```
  - [ ] Query similar agrupada por profissional para o bloco por profissional

- [ ] **Task 3 — Cálculo de slots disponíveis** (AC-2)
  - [ ] Criar método `calcularTotalSlots(Long empresaId, LocalDate dataInicio, LocalDate dataFim, Long profissionalIdFiltro)` em `RelatorioAgendamentosService`:
    - Para cada dia no período
    - Para cada profissional ativo (filtrado se `profissionalId` fornecido)
    - Buscar `Disponibilidade` para o `DiaSemana` do dia
    - Para cada tipo de atendimento ativo da empresa
    - Calcular slots de `duracaoMinutos` em `duracaoMinutos` dentro de `horarioInicio–horarioFim`, excluindo `inicioIntervalo–fimIntervalo`
    - Somar total de slots possíveis
  - [ ] Retornar `0` se nenhuma disponibilidade configurada

- [ ] **Task 4 — `RelatorioController.java`** (AC-1, AC-3)
  - [ ] Criar `api/controllers/RelatorioController.java`
  - [ ] `GET /api/v1/relatorios/agendamentos` → `@PreAuthorize("hasRole('ADMIN_EMPRESA')")`
  - [ ] Parâmetros: `@RequestParam LocalDate dataInicio`, `@RequestParam LocalDate dataFim`, `@RequestParam(required = false) Long profissionalId`
  - [ ] Validação: `dataFim` não pode ser anterior a `dataInicio` → HTTP 400

- [ ] **Task 5 — DTOs** (AC-1, AC-4)
  - [ ] Criar `api/dtos/response/RelatorioAgendamentosResponse.java`:
    ```java
    record RelatorioAgendamentosResponse(
        ConsolidadoResponse consolidado,
        List<ConsolidadoPorProfissionalResponse> porProfissional,
        Double baseline   // null se não configurado
    ) {}
    ```
  - [ ] Criar `api/dtos/response/ConsolidadoResponse.java`:
    ```java
    record ConsolidadoResponse(
        long agendados,
        long confirmados,
        long concluidos,
        long cancelados,
        long noShows,
        Integer totalSlotsDisponiveis,
        Double ocupacaoConfirmada,  // null se totalSlotsDisponiveis = 0
        Double ocupacaoTotal        // null se totalSlotsDisponiveis = 0
    ) {}
    ```
  - [ ] Criar `api/dtos/response/ConsolidadoPorProfissionalResponse.java`: `profissionalId`, `profissionalNome`, + campos de `ConsolidadoResponse`

- [ ] **Task 6 — `RelatoriosPage.tsx`** (AC-5, AC-6)
  - [ ] Criar `src/pages/RelatoriosPage.tsx`
  - [ ] DatePickers com padrão: primeiro/último dia do mês corrente
  - [ ] Select de profissional (populado com `GET /api/v1/profissionais`)
  - [ ] `useQuery` acionado por botão `[Gerar Relatório]` (não automático)
  - [ ] Cards de contadores: usar `Skeleton` durante loading
  - [ ] Taxas de ocupação: formatar como `"72%"` ou `"—"` se null
  - [ ] Tabela por profissional: somente se `profissionalId` não filtrado
  - [ ] Estado vazio e estado de erro

- [ ] **Task 7 — Registrar rota no Router** (AC-5)
  - [ ] Adicionar rota `/relatorios` em `AppRouter.tsx` dentro do `AppLayout`
  - [ ] BottomTabBar: adicionar aba "Relatórios" com ícone `<BarChart2>` visível apenas para `ADMIN_EMPRESA`

- [ ] **Task 8 — Testes** (AC-1, AC-2, AC-3)
  - [ ] Teste unitário `RelatorioAgendamentosServiceTest`:
    - Período sem agendamentos: todos contadores = 0, taxas = null
    - Mix de status: contadores corretos
    - `totalSlotsDisponiveis = 0`: `ocupacaoConfirmada = null`, `ocupacaoTotal = null`
    - Filtro por profissional: contadores apenas do profissional filtrado
  - [ ] Teste de integração `RelatorioControllerIT`:
    - ADMIN_EMPRESA → 200
    - STAFF → 403
    - PROFISSIONAL → 403
    - `dataFim < dataInicio` → 400

## Dev Notes

### Conversão de Datas LocalDate → Instant UTC

```java
// Usar o fuso da empresa para converter datas de filtro em Instant UTC
ZoneId fuso = ZoneId.of(empresa.getFusoHorario());  // FusoHorarioBrasileiro.IANA
Instant inicio = dataInicio.atStartOfDay(fuso).toInstant();
Instant fim    = dataFim.plusDays(1).atStartOfDay(fuso).toInstant();  // exclusive
```

> Isso garante que "1º de junho" inclui todos os agendamentos do dia no fuso da clínica, independente de UTC.

### Cálculo de Slots Disponíveis — Exemplo

```
Profissional A — Sexta-feira
Disponibilidade: 09:00–12:00 (sem intervalo)
Tipo A: 30 min → 6 slots
Tipo B: 60 min → 3 slots
Total para este profissional/dia: 6 + 3 = 9 slots

Profissional A — Sexta-feira COM intervalo
Disponibilidade: 09:00–18:00, intervalo 12:00–13:00
Tipo A: 30 min → 9h - 1h = 8h → 16 slots (09:00–12:00 = 6 slots + 13:00–18:00 = 10 slots)
```

```java
private int calcularSlotsNoDia(Disponibilidade disp, TipoAtendimento tipo) {
    int slots = 0;
    LocalTime cursor = disp.getHorarioInicio();
    int duracaoMin = tipo.getDuracaoMinutos();

    while (!cursor.plusMinutes(duracaoMin).isAfter(disp.getHorarioFim())) {
        LocalTime fim = cursor.plusMinutes(duracaoMin);

        // Pular se intersecta intervalo
        boolean intersectaIntervalo = disp.getInicioIntervalo() != null
            && cursor.isBefore(disp.getFimIntervalo())
            && fim.isAfter(disp.getInicioIntervalo());

        if (intersectaIntervalo) {
            cursor = disp.getFimIntervalo();  // avançar para após o intervalo
            continue;
        }

        slots++;
        cursor = fim;
    }
    return slots;
}
```

### Formula de Ocupação

```
ocupacaoConfirmada = (CONFIRMADO + PRESENTE + CONCLUIDO + NOSHOW) / totalSlots × 100
ocupacaoTotal      = (PENDENTE + CONFIRMADO + PRESENTE + CONCLUIDO + NOSHOW) / totalSlots × 100
                   = (agendados - cancelados) / totalSlots × 100

// Arredondar para 1 casa decimal:
double pct = Math.round(valor * 10.0) / 10.0;
```

### Nota sobre Baseline (Story 5.2)

O campo `baseline_noshow_pct` é adicionado pela Story 5.2. Nesta story, ao montar o response:

```java
// RelatorioAgendamentosService.gerar():
Double baseline = empresa.getBaselineNoshowPct();  // null se não configurado
return new RelatorioAgendamentosResponse(consolidado, porProfissional, baseline);
```

A Story 5.1 simplesmente passa `null` se o campo não existir ainda — a migration da Story 5.2 adicionará a coluna. Se a 5.1 for implementada antes da 5.2, o campo não existe na entidade e `baseline = null` é o comportamento correto.

### `RelatoriosPage.tsx` — Formato de Exibição

```tsx
// Formatar taxa de ocupação:
function formatarOcupacao(valor: number | null): string {
  if (valor === null) return '—'
  return `${valor.toFixed(1)}%`
}

// Nota quando baseline é null:
{response.baseline === null && (
  <p className="text-xs text-muted-foreground mt-1">
    Configure o baseline em Configurações para comparação após 60 dias
  </p>
)}
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `domain/services/RelatorioAgendamentosService.java` | NEW | Inclui cálculo de slots |
| `api/controllers/RelatorioController.java` | NEW | `@PreAuthorize ADMIN_EMPRESA` |
| `api/dtos/response/RelatorioAgendamentosResponse.java` | NEW | |
| `api/dtos/response/ConsolidadoResponse.java` | NEW | `ocupacaoConfirmada` nullable |
| `api/dtos/response/ConsolidadoPorProfissionalResponse.java` | NEW | |
| `domain/repositories/AgendamentoRepository.java` | **UPDATE** | `countPorStatus()` JPQL |
| `src/pages/RelatoriosPage.tsx` | NEW | |
| `src/AppRouter.tsx` | **UPDATE** | Rota `/relatorios` |
| `src/shared/components/BottomTabBar.tsx` | **UPDATE** | Aba "Relatórios" só para ADMIN_EMPRESA |
| `test/.../RelatorioAgendamentosServiceTest.java` | NEW | |
| `test/.../RelatorioControllerIT.java` | NEW | |

### Referências

- [Source: epics.md#Story 5.1] — Acceptance Criteria completos e FR-032, FR-033, FR-034
- [Source: 3-2-configuracoes-clinica.md] — `FusoHorarioBrasileiro`, campo `fusoHorario` na empresa
- [Source: 3-3-tipos-atendimento.md] — `TipoAtendimento.duracaoMinutos`, `TipoAtendimento.ativo`
- [Source: 3-4-disponibilidade-profissionais.md] — `Disponibilidade`, `DiaSemana`, intervalos
- [Source: 4-4-noshow-automatico-proximos-disponiveis.md] — algoritmo de geração de slots (reutilizar lógica de `calcularSlotsNoDia`)

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
