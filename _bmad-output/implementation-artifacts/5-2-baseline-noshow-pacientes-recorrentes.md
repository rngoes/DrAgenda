# Story 5.2: Baseline de No-Show e Lista de Pacientes Recorrentes

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero configurar uma taxa de no-show de referência e ver quais pacientes mais faltam no período,
Para que eu possa comparar a melhora real e agir preventivamente com os pacientes problemáticos.

## Acceptance Criteria

**AC-1 — Migration V10: coluna `baseline_noshow_pct` em `empresas` (FR-045)**
- Given a migration `V{yyyyMMddHHmm}__add_baseline_noshow.sql` é aplicada
- When o sistema inicializa
- Then a coluna `baseline_noshow_pct` existe na tabela `empresas` com tipo `DECIMAL(5,2) NULL`
- And valor padrão é NULL para todas as empresas existentes

**AC-2 — Configurar baseline via tela de configurações (FR-045)**
- Given Admin Empresa acessa `/configuracoes/clinica` (já implementado na Story 3.2)
- When edita o campo "Taxa de no-show estimada (%)" (campo numérico 0–100) e salva
- Then `PUT /api/v1/configuracoes/clinica` persiste `baseline_noshow_pct`
- And validação: valor deve ser entre 0 e 100 inclusive — HTTP 400 se fora do intervalo
- And campo é opcional — enviar `null` limpa o baseline
- And retorna HTTP 200

**AC-3 — Exibir baseline no relatório (FR-045)**
- Given Admin Empresa tem `baseline_noshow_pct` configurado E a empresa tem `created_at` com ≥ 60 dias de operação
- When o relatório `/relatorios` é exibido
- Then exibe linha de referência: `"Baseline configurado: X% · Atual: Y%"` onde Y = `noShows / agendados × 100` do período selecionado
- And se a empresa tem < 60 dias de operação, exibe apenas `"Baseline configurado: X% · Operação iniciada há N dias — comparação disponível após 60 dias"`

**AC-4 — Endpoint de pacientes recorrentes com no-show (FR-046, FR-049)**
- Given Admin Empresa acessa `GET /api/v1/relatorios/noshow-pacientes?dataInicio=&dataFim=` com parâmetro opcional `profissionalId=`
- When o backend processa
- Then retorna lista paginada (page size 20) de clientes com ≥ 1 NoShow no período
- And cada item contém: `clienteId`, `nomeCliente` (descriptografado do AES), `quantidadeNoShows`, `dataUltimoNoShow`
- And lista ordenada por `quantidadeNoShows` DESC, `dataUltimoNoShow` DESC como critério de desempate
- And isolamento por empresa — apenas clientes da empresa do Admin autenticado

**AC-5 — Paginação da lista de no-shows (FR-046)**
- Given o endpoint `/relatorios/noshow-pacientes` retorna dados
- When renderiza
- Then usa envelope padrão de paginação: `{ content, page, size, totalElements, totalPages }`
- And a `RelatoriosPage.tsx` exibe tabela com controles de paginação: `← Anterior` / `Próxima →`
- And indicador: `"Página N de T (X pacientes)"`
- And estado vazio: `"Nenhum paciente com no-show no período selecionado."`

**AC-6 — Campo `created_at` em `empresas` para calcular dias de operação (AC-3)**
- Given a tabela `empresas` é consultada para verificar dias de operação
- When o backend calcula
- Then usa `empresas.created_at` (coluna já existente desde V1) para determinar quantos dias se passaram desde a criação da empresa
- And se `created_at` ausente, assume que a empresa tem ≥ 60 dias

## Tasks / Subtasks

- [ ] **Task 1 — Migration V10** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__add_baseline_noshow.sql`:
    ```sql
    ALTER TABLE empresas ADD COLUMN baseline_noshow_pct DECIMAL(5,2) NULL;
    ```
  - [ ] Usar timestamp real no momento da implementação (R6 — nunca V10 sequencial)

- [ ] **Task 2 — Atualizar entidade `Empresa`** (AC-1, AC-3, AC-6)
  - [ ] Adicionar `BigDecimal baselineNoshowPct` com `@Column(nullable = true, precision = 5, scale = 2)` em `Empresa.java`
  - [ ] Verificar se `createdAt` já existe na entidade — se não, adicionar `Instant createdAt` com `@CreationTimestamp` / `@Column(updatable = false)`

- [ ] **Task 3 — Atualizar `ConfiguracaoClinicaService` e DTO** (AC-2)
  - [ ] Adicionar `baselineNoshowPct` ao `ConfiguracaoClinicaRequest.java` (Story 3.2) com `@DecimalMin("0.0") @DecimalMax("100.0")` (nullable)
  - [ ] Atualizar `ConfiguracaoClinicaService.atualizar()` para persistir o novo campo
  - [ ] Atualizar `ConfiguracaoClinicaResponse.java` para incluir `baselineNoshowPct`

- [ ] **Task 4 — `RelatorioNoshowService.java`** (AC-4, AC-5)
  - [ ] Criar `domain/services/RelatorioNoshowService.java`
  - [ ] Método `listarPacientesComNoshow(Long empresaId, LocalDate dataInicio, LocalDate dataFim, Long profissionalId, Pageable pageable)` → `Page<NoshowPacienteResponse>`
  - [ ] Query JPQL agrupada por cliente:
    ```java
    @Query("""
        SELECT a.cliente.id,
               COUNT(a.id),
               MAX(a.horarioInicio)
        FROM Agendamento a
        WHERE a.empresa.id = :empresaId
          AND a.status = 'NOSHOW'
          AND a.horarioInicio >= :inicio
          AND a.horarioInicio < :fim
          AND (:profissionalId IS NULL OR a.profissional.id = :profissionalId)
        GROUP BY a.cliente.id
        ORDER BY COUNT(a.id) DESC, MAX(a.horarioInicio) DESC
        """)
    Page<Object[]> findPacientesComNoshow(
        @Param("empresaId") Long empresaId,
        @Param("inicio") Instant inicio,
        @Param("fim") Instant fim,
        @Param("profissionalId") Long profissionalId,
        Pageable pageable
    );
    ```
  - [ ] Para cada `Object[]` resultado: carregar cliente por id, descriptografar `nome` via `AesAttributeConverter` (já ocorre automaticamente ao carregar a entidade via JPA — `@Convert`)
  - [ ] Montar `NoshowPacienteResponse`

- [ ] **Task 5 — `RelatorioController` — novo endpoint** (AC-4, AC-5)
  - [ ] Adicionar ao `RelatorioController.java` (criado na Story 5.1):
    - `GET /api/v1/relatorios/noshow-pacientes` → `@PreAuthorize("hasRole('ADMIN_EMPRESA')")`
    - Parâmetros: `dataInicio`, `dataFim`, `profissionalId` (optional), `page` (default 0), `size` (default 20)

- [ ] **Task 6 — DTOs** (AC-4, AC-5)
  - [ ] Criar `api/dtos/response/NoshowPacienteResponse.java`:
    ```java
    record NoshowPacienteResponse(
        Long clienteId,
        String nomeCliente,
        long quantidadeNoShows,
        Instant dataUltimoNoShow
    ) {}
    ```

- [ ] **Task 7 — Integrar baseline ao `RelatorioAgendamentosService`** (AC-3)
  - [ ] Atualizar `RelatorioAgendamentosService.gerar()` (Story 5.1) para:
    - Carregar `empresa.baselineNoshowPct`
    - Calcular `diasDeOperacao = ChronoUnit.DAYS.between(empresa.getCreatedAt(), Instant.now())`
    - Retornar `BaselineInfo` com `baselineNoshowPct`, `taxaAtualNoshow` e `diasDeOperacao`

- [ ] **Task 8 — Atualizar `RelatoriosPage.tsx`** (AC-3, AC-5)
  - [ ] Adicionar seção de baseline no relatório de agendamentos:
    - Se `baseline` não null e `diasDeOperacao >= 60`: exibir `"Baseline: X% · Atual: Y%"`
    - Se `baseline` não null e `diasDeOperacao < 60`: exibir aviso de operação recente
    - Se `baseline` null: exibir nota de configuração
  - [ ] Adicionar aba/seção "Pacientes com No-Show" na `RelatoriosPage`:
    - Tabela paginada com colunas: Nome, Quantidade, Último No-Show
    - Controles de paginação
    - Estados de loading, vazio e erro

- [ ] **Task 9 — Campo baseline na `ConfiguracaoClinicaPage`** (AC-2)
  - [ ] Adicionar campo `"Taxa de no-show estimada (%)"` na `ConfiguracaoClinicaPage.tsx` (Story 3.2)
  - [ ] Tipo `number`, placeholder `"Ex: 15"`, validação `0–100` via Zod: `z.number().min(0).max(100).nullable().optional()`
  - [ ] Pré-preencher com valor existente (pode ser null)

- [ ] **Task 10 — Testes** (AC-1, AC-2, AC-4)
  - [ ] Teste unitário `RelatorioNoshowServiceTest`:
    - Período sem NoShows: lista vazia
    - Múltiplos NoShows por cliente: ordenação por quantidade DESC
    - `nome` do cliente é descriptografado (não retorna bytes criptografados)
    - Isolamento: cliente de outra empresa não aparece
  - [ ] Teste de integração `RelatorioNoshowControllerIT`:
    - ADMIN_EMPRESA → 200 com dados paginados
    - STAFF → 403
    - Paginação: `page=0&size=5` retorna apenas 5 registros

## Dev Notes

### ⚠️ Migration com timestamp (R6)

```sql
-- CORRETO: V202506042300__add_baseline_noshow.sql
-- ERRADO:  V10__add_baseline_noshow.sql
```

### Migration SQL

```sql
-- V202506042300__add_baseline_noshow.sql (usar timestamp real)
ALTER TABLE empresas ADD COLUMN baseline_noshow_pct DECIMAL(5,2) NULL;
```

### Descriptografia automática via `@Convert` (R3)

O campo `nome` do cliente é anotado com `@Convert(converter = AesAttributeConverter.class)` — a descriptografia ocorre **automaticamente** quando o JPA carrega a entidade `Cliente`. Não é necessário chamar nenhum método manual de descriptografia.

```java
// ClienteRepository — busca por id já retorna nome descriptografado:
Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
String nome = cliente.getNome();  // ← já descriptografado
```

> Não reimplementar AES. Não criar conversor adicional (R3).

### Cálculo da Taxa Atual de No-Show

```java
double taxaAtualNoshow = (noShows == 0 || agendados == 0)
    ? 0.0
    : Math.round((double) noShows / agendados * 100.0 * 10) / 10.0;
```

### Verificação de `created_at` na entidade `Empresa`

```java
// Se createdAt não existir na entidade, adicionar:
@Column(name = "created_at", nullable = false, updatable = false)
@CreationTimestamp
private Instant createdAt;

// Ou: verificar se V1 (Story 1.2) já inclui a coluna created_at em empresas
// Se sim: apenas mapear na entidade
```

### Envelope de Paginação Padrão (FR-046)

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 92,
  "totalPages": 5
}
```

Este envelope é o mesmo para todos os endpoints paginados (Story 5.3 vai garantir consistência). A `RelatoriosPage` deve esperar este formato.

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__add_baseline_noshow.sql` | NEW | Apenas `ALTER TABLE empresas` |
| `domain/entities/Empresa.java` | **UPDATE** | Adicionar `baselineNoshowPct`, verificar `createdAt` |
| `api/dtos/request/ConfiguracaoClinicaRequest.java` | **UPDATE** | Adicionar `baselineNoshowPct` |
| `api/dtos/response/ConfiguracaoClinicaResponse.java` | **UPDATE** | Adicionar `baselineNoshowPct` |
| `domain/services/ConfiguracaoClinicaService.java` | **UPDATE** | Persistir `baselineNoshowPct` |
| `domain/services/RelatorioNoshowService.java` | NEW | |
| `domain/services/RelatorioAgendamentosService.java` | **UPDATE** | Integrar `BaselineInfo` |
| `api/controllers/RelatorioController.java` | **UPDATE** | `GET /relatorios/noshow-pacientes` |
| `api/dtos/response/NoshowPacienteResponse.java` | NEW | |
| `domain/repositories/AgendamentoRepository.java` | **UPDATE** | Query agrupada por cliente/NoShow |
| `src/pages/RelatoriosPage.tsx` | **UPDATE** | Seção baseline + tabela no-shows |
| `src/pages/configuracoes/ConfiguracaoClinicaPage.tsx` | **UPDATE** | Campo baseline |
| `test/.../RelatorioNoshowServiceTest.java` | NEW | |
| `test/.../RelatorioNoshowControllerIT.java` | NEW | |

### Referências

- [Source: epics.md#Story 5.2] — Acceptance Criteria completos e FR-045, FR-046, FR-049
- [Source: 3-2-configuracoes-clinica.md] — `ConfiguracaoClinicaService`, DTO e Page já existentes
- [Source: 3-5-cadastro-clientes-lgpd.md] — `AesAttributeConverter`, descriptografia automática por JPA (R3)
- [Source: 5-1-relatorio-agendamentos-contadores-ocupacao.md] — `RelatorioController`, envelope de paginação

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
