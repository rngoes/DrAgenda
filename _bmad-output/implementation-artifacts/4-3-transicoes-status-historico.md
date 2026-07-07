# Story 4.3: Transições de Status, Histórico e Detalhe do Agendamento

Status: ready-for-dev

## Story

Como Staff, Profissional ou Admin Empresa,
Quero atualizar o status de agendamentos em até 2 toques e ver o histórico completo de cada agendamento,
Para que o estado da agenda reflita a realidade em tempo real.

## Acceptance Criteria

**AC-1 — Migration V9: tabela `historico_status`**
- Given a migration `V{yyyyMMddHHmm}__create_historico_status.sql` é aplicada
- When o sistema inicializa
- Then a tabela `historico_status` existe com colunas:
  - `id` BIGINT PK AUTO_INCREMENT
  - `agendamento_id` BIGINT FK NOT NULL → `agendamentos.id`
  - `empresa_id` BIGINT FK NOT NULL → `empresas.id`
  - `status_anterior` ENUM (mesmos 6 valores de `agendamentos.status`) NOT NULL
  - `status_novo` ENUM NOT NULL
  - `usuario_id` BIGINT FK NULL → `usuarios.id` (NULL para ações automáticas do sistema)
  - `timestamp` DATETIME NOT NULL (UTC)
  - `justificativa` TEXT NULL
- And índice `idx_historico_agendamento_id` existe (FR-057)

**AC-2 — `AgendamentoStatusValidator` (R2 — state machine no backend)**
- Given `PATCH /api/v1/agendamentos/{id}/status` é chamado com qualquer transição
- When o `AgendamentoStatusValidator` avalia
- Then rejeita com HTTP 409 as seguintes transições inválidas:
  - `NOSHOW → CONCLUIDO`
  - `NOSHOW → CONFIRMADO`
  - `CANCELADO → qualquer status`
  - `CONCLUIDO → qualquer status`
  - Qualquer transição por Profissional exceto `PRESENTE → CONCLUIDO`
- And a validação ocorre **no backend independentemente dos botões da UI** — botões desabilitados são UX, não segurança (R2)

**AC-3 — Transição com 2 toques**
- Given o usuário toca o botão de ação primária de um card (1º toque)
- When o dialog de confirmação é exibido
- Then o 2º toque envia `PATCH /api/v1/agendamentos/{id}/status` com `{ "statusNovo": "..." }` (FR-028)
- And o backend registra em `historico_status` com `timestamp` UTC, `usuario_id` e `justificativa = null`
- And o card atualiza visualmente via `queryClient.invalidateQueries(['agendamentos'])`
- And `toast.success('Status atualizado para [Label] ✓')` é exibido

**AC-4 — Botões contextuais por status**
- Given um card de agendamento é renderizado (FR-027):
  - `PENDENTE` → botão `[Confirmar]` (Staff/Admin)
  - `CONFIRMADO` → botão `[Chegou]` (Staff/Admin)
  - `PRESENTE` → botão `[Concluir]` (Staff/Admin/Profissional)
  - `NOSHOW` → botão `[Reverter]` (Staff apenas)
  - `CONCLUIDO` / `CANCELADO` → sem botão (status terminal)

**AC-5 — Cancelamento com justificativa obrigatória**
- Given o usuário aciona menu `···` → "Cancelar agendamento" em agendamento não-terminal
- When o dialog de cancelamento é exibido
- Then exibe campo de justificativa obrigatório
- And retorna HTTP 409 se justificativa ausente no backend (proteção contra bypass de UI)
- And `historico_status.justificativa` é persistida (FR-057)

**AC-6 — Reverter NoShow**
- Given Staff toca `[Reverter]` em agendamento NOSHOW
- When o dialog exibe `"Reverter NoShow para Presente? O registro de NoShow será mantido no histórico."`
- Then confirmação envia `PATCH ... { "statusNovo": "PRESENTE" }`
- And o registro de NoShow **permanece** no histórico — não excluído (FR-030)

**AC-7 — Tela de detalhe `/agendamento/:id` (FR-055)**
- Given qualquer usuário toca no corpo de um card (fora dos botões)
- When navega para `/agendamento/:id`
- Then exibe: nome + telefone do cliente (descriptografados), tipo de atendimento, profissional, data/hora início e fim (convertidos para o fuso da clínica)
- And lista de `historico_status` com: timestamp formatado, status anterior → status novo, nome do usuário responsável (ou "Sistema" se `usuario_id = null`), justificativa se presente

**AC-8 — Menu `···` com opções contextuais**
- Given Staff ou Admin acessa o menu `···` de agendamento não-terminal
- When o menu é exibido
- Then opções: "Ver detalhes", "Editar agendamento" (Staff), "Gerar link de cancelamento" (Staff/Admin), "Cancelar agendamento" (Staff/Admin)

**AC-9 — Editar agendamento (FR-052)**
- Given Staff acessa "Editar agendamento" de agendamento PENDENTE ou CONFIRMADO
- When submete `PUT /api/v1/agendamentos/{id}` com nova data, hora, tipo ou profissional
- Then as mesmas validações de disponibilidade e double-booking da Story 4.2 são aplicadas
- And `public_token` existente é setado para NULL (invalidado)
- And retorna HTTP 200

## Tasks / Subtasks

- [ ] **Task 1 — Migration V9** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__create_historico_status.sql`
  - [ ] Usar timestamp real no momento da implementação

- [ ] **Task 2 — Entidade `HistoricoStatus`** (AC-1)
  - [ ] Criar `domain/entities/HistoricoStatus.java`
  - [ ] Campos: `id`, `agendamento` (ManyToOne lazy), `empresa` (ManyToOne lazy), `statusAnterior` (Enum), `statusNovo` (Enum), `usuarioId` (Long nullable), `timestamp` (Instant), `justificativa` (String nullable)
  - [ ] `@PrePersist` para `timestamp`

- [ ] **Task 3 — `HistoricoStatusRepository`** (AC-7)
  - [ ] Criar `domain/repositories/HistoricoStatusRepository.java`
  - [ ] `List<HistoricoStatus> findAllByAgendamentoIdOrderByTimestampAsc(Long agendamentoId)`

- [ ] **Task 4 — `AgendamentoStatusValidator` (R2)** (AC-2)
  - [ ] Criar `domain/services/AgendamentoStatusValidator.java`
  - [ ] Mapa de transições válidas por perfil:
    ```
    STAFF/ADMIN_EMPRESA:
      PENDENTE → CONFIRMADO, CANCELADO
      CONFIRMADO → PRESENTE, CANCELADO
      PRESENTE → CONCLUIDO, CANCELADO
      NOSHOW → PRESENTE  (reverter)
      CONCLUIDO → (nenhuma)
      CANCELADO → (nenhuma)

    PROFISSIONAL:
      PRESENTE → CONCLUIDO  (única transição permitida)
    ```
  - [ ] Método `validar(StatusAgendamento atual, StatusAgendamento novo, String perfil, String justificativa)`:
    - Verificar se transição está no mapa → `TransicaoInvalidaException` (409) se não
    - Se `statusNovo == CANCELADO` e `justificativa == null ou blank` → `JustificativaObrigatoriaException` (409)

- [ ] **Task 5 — `AgendamentoService.atualizarStatus()`** (AC-3, AC-5, AC-6)
  - [ ] Adicionar ao `AgendamentoService.java` (Story 4.2):
    `atualizarStatus(Long id, Long empresaId, Long usuarioId, String perfil, AtualizarStatusRequest)` — `@Transactional`:
    1. Carregar agendamento por `id + empresaId` → 404
    2. Chamar `AgendamentoStatusValidator.validar()`
    3. Setar `agendamento.status = statusNovo`
    4. Criar e salvar `HistoricoStatus`
    5. Retornar `AgendamentoResponse` atualizado

- [ ] **Task 6 — `AgendamentoService.editar()`** (AC-9)
  - [ ] Adicionar `editar(Long id, Long empresaId, EditarAgendamentoRequest)` — `@Transactional`:
    - Verificar status PENDENTE ou CONFIRMADO → 409 se terminal
    - Re-executar validações de disponibilidade e double-booking (reutilizar `DisponibilidadeValidator` e `DoubleBookingValidator`)
    - Setar `publicToken = null`
    - Salvar e retornar

- [ ] **Task 7 — Endpoints no `AgendamentoController`** (AC-3, AC-7, AC-9)
  - [ ] `PATCH /api/v1/agendamentos/{id}/status` → `@PreAuthorize("hasAnyRole('STAFF','PROFISSIONAL','ADMIN_EMPRESA')")`
  - [ ] `GET /api/v1/agendamentos/{id}` → detalhe completo com histórico
  - [ ] `PUT /api/v1/agendamentos/{id}` → editar (apenas STAFF)

- [ ] **Task 8 — DTOs** (AC-3, AC-7, AC-9)
  - [ ] Criar `api/dtos/request/AtualizarStatusRequest.java`: `@NotBlank statusNovo`, `justificativa` (nullable)
  - [ ] Criar `api/dtos/response/AgendamentoDetalheResponse.java`: todos campos de `AgendamentoResponse` + `List<HistoricoStatusResponse>`
  - [ ] Criar `api/dtos/response/HistoricoStatusResponse.java`: `statusAnterior`, `statusNovo`, `timestamp`, `usuarioNome` (ou "Sistema"), `justificativa`
  - [ ] Criar `api/dtos/request/EditarAgendamentoRequest.java`: `horarioInicio`, `tipoAtendimentoId`, `profissionalId`

- [ ] **Task 9 — Exception handlers adicionais** (AC-2, AC-5)
  - [ ] Adicionar ao `GlobalExceptionHandler.java`:
    - `TransicaoInvalidaException` → HTTP 409 com mensagem clara
    - `JustificativaObrigatoriaException` → HTTP 409

- [ ] **Task 10 — Frontend — Botões contextuais no `AgendamentoCard`** (AC-3, AC-4)
  - [ ] Atualizar `AgendamentoCard.tsx` (Story 4.1): adicionar botão de ação primária por status
  - [ ] Mapa frontend de ações primárias (mesmo mapa do backend — redundância intencional de UX):
    ```ts
    const ACAO_PRIMARIA: Partial<Record<AgendamentoStatus, { label: string; statusNovo: AgendamentoStatus; perfis: string[] }>> = {
      PENDENTE:   { label: 'Confirmar', statusNovo: 'CONFIRMADO', perfis: ['STAFF', 'ADMIN_EMPRESA'] },
      CONFIRMADO: { label: 'Chegou',    statusNovo: 'PRESENTE',   perfis: ['STAFF', 'ADMIN_EMPRESA'] },
      PRESENTE:   { label: 'Concluir',  statusNovo: 'CONCLUIDO',  perfis: ['STAFF', 'ADMIN_EMPRESA', 'PROFISSIONAL'] },
      NOSHOW:     { label: 'Reverter',  statusNovo: 'PRESENTE',   perfis: ['STAFF'] },
    }
    ```
  - [ ] Dialog de confirmação: `"Confirmar [Label]? [Paciente] — [Horário]"`
  - [ ] Menu `···` como `DropdownMenu` Shadcn com opções contextuais

- [ ] **Task 11 — `DialogCancelarAgendamento.tsx`** (AC-5)
  - [ ] Criar `src/pages/agenda/DialogCancelarAgendamento.tsx`
  - [ ] Campo de justificativa obrigatório (`z.string().min(10, 'Mínimo 10 caracteres')`)
  - [ ] Botões: `[Cancelar]` (ghost) e `[Confirmar cancelamento]` (destructive)

- [ ] **Task 12 — Tela de detalhe `AgendamentoDetalhePage.tsx`** (AC-7, AC-8)
  - [ ] Criar `src/pages/AgendamentoDetalhePage.tsx`
  - [ ] `useQuery` para `GET /api/v1/agendamentos/{id}` — skeleton durante carregamento
  - [ ] Seção de dados do agendamento + `StatusBadge`
  - [ ] Lista de histórico ordenada por timestamp ASC com ícone de transição `→`
  - [ ] "Sistema" quando `usuarioNome` é null

- [ ] **Task 13 — Testes** (AC-2, AC-5, AC-7)
  - [ ] Teste unitário `AgendamentoStatusValidatorTest`:
    - Todas as transições válidas por perfil: sem exceção
    - Todas as transições inválidas da lista (R2): lança `TransicaoInvalidaException`
    - `CANCELADO` sem justificativa: lança `JustificativaObrigatoriaException`
    - Profissional tenta `PENDENTE → CONFIRMADO`: lança exceção
  - [ ] Teste de integração `AgendamentoStatusIT`:
    - PATCH happy path: status atualizado + historico criado
    - PATCH transição inválida: 409
    - PATCH cancelamento sem justificativa: 409

## Dev Notes

### ⚠️ Migration com timestamp (não V9 sequencial) — R6

```sql
-- CORRETO: V202506042100__create_historico_status.sql
-- ERRADO:  V9__create_historico_status.sql
```

### Migration SQL

```sql
-- V202506042100__create_historico_status.sql (usar timestamp real)

CREATE TABLE historico_status (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    agendamento_id  BIGINT   NOT NULL,
    empresa_id      BIGINT   NOT NULL,
    status_anterior ENUM('PENDENTE','CONFIRMADO','PRESENTE','CONCLUIDO','CANCELADO','NOSHOW') NOT NULL,
    status_novo     ENUM('PENDENTE','CONFIRMADO','PRESENTE','CONCLUIDO','CANCELADO','NOSHOW') NOT NULL,
    usuario_id      BIGINT   NULL,
    timestamp       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    justificativa   TEXT     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historico_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id),
    CONSTRAINT fk_historico_empresa     FOREIGN KEY (empresa_id)     REFERENCES empresas(id),
    CONSTRAINT fk_historico_usuario     FOREIGN KEY (usuario_id)     REFERENCES usuarios(id)
);

CREATE INDEX idx_historico_agendamento_id ON historico_status (agendamento_id);
```

### `AgendamentoStatusValidator.java` — Mapa Completo (R2)

```java
@Component
public class AgendamentoStatusValidator {

    // Transições válidas para STAFF e ADMIN_EMPRESA
    private static final Map<StatusAgendamento, Set<StatusAgendamento>> TRANSICOES_STAFF =
        Map.of(
            PENDENTE,   Set.of(CONFIRMADO, CANCELADO),
            CONFIRMADO, Set.of(PRESENTE, CANCELADO),
            PRESENTE,   Set.of(CONCLUIDO, CANCELADO),
            NOSHOW,     Set.of(PRESENTE),
            CONCLUIDO,  Set.of(),
            CANCELADO,  Set.of()
        );

    // Transições válidas para PROFISSIONAL (apenas uma)
    private static final Map<StatusAgendamento, Set<StatusAgendamento>> TRANSICOES_PROFISSIONAL =
        Map.of(
            PRESENTE, Set.of(CONCLUIDO)
            // todos os outros: Set vazio implícito
        );

    public void validar(StatusAgendamento atual, StatusAgendamento novo,
                        String perfil, String justificativa) {
        Map<StatusAgendamento, Set<StatusAgendamento>> mapa =
            "PROFISSIONAL".equals(perfil) ? TRANSICOES_PROFISSIONAL : TRANSICOES_STAFF;

        Set<StatusAgendamento> permitidos = mapa.getOrDefault(atual, Set.of());

        if (!permitidos.contains(novo)) {
            throw new TransicaoInvalidaException(
                String.format("Transição de %s para %s não é permitida.", atual, novo));
        }

        if (novo == CANCELADO && (justificativa == null || justificativa.isBlank())) {
            throw new JustificativaObrigatoriaException(
                "Justificativa é obrigatória para cancelar agendamento.");
        }
    }
}
```

> **R2 — Segurança:** Esta validação roda no backend para TODA chamada ao endpoint `PATCH /status`. Botões desabilitados na UI são apenas UX — um usuário mal-intencionado pode chamar a API diretamente. O `AgendamentoStatusValidator` é a última linha de defesa.

### Registro em `historico_status`

```java
// AgendamentoService.atualizarStatus() — após setar o novo status:
HistoricoStatus historico = new HistoricoStatus();
historico.setAgendamento(agendamento);
historico.setEmpresa(agendamento.getEmpresa());
historico.setStatusAnterior(statusAtual);     // antes de setar o novo
historico.setStatusNovo(statusNovo);
historico.setUsuarioId(usuarioId);            // null para ações do sistema (NoShow job)
historico.setJustificativa(req.getJustificativa());
historicoRepository.save(historico);
```

### `AgendamentoDetalheResponse` — Resolução de `usuarioNome`

```java
// Para cada HistoricoStatus, resolver nome do usuário:
private HistoricoStatusResponse toHistoricoResponse(HistoricoStatus h) {
    String nomeUsuario = h.getUsuarioId() != null
        ? usuarioRepository.findById(h.getUsuarioId())
            .map(Usuario::getNome).orElse("Usuário desconhecido")
        : "Sistema";
    return new HistoricoStatusResponse(
        h.getStatusAnterior().name(),
        h.getStatusNovo().name(),
        h.getTimestamp(),
        nomeUsuario,
        h.getJustificativa()
    );
}
```

### Frontend — Label dos Status para Toasts

```ts
// src/shared/types/agendamento.ts — adicionar labels de transição:
export const LABEL_STATUS: Record<AgendamentoStatus, string> = {
  PENDENTE:   'Pendente',
  CONFIRMADO: 'Confirmado',
  PRESENTE:   'Presente',
  CONCLUIDO:  'Concluído',
  CANCELADO:  'Cancelado',
  NOSHOW:     'NoShow',
}
// toast.success(`Status atualizado para ${LABEL_STATUS[statusNovo]} ✓`)
```

### Editar Agendamento — Invalidação do `public_token`

```java
// AgendamentoService.editar():
agendamento.setHorarioInicio(req.getHorarioInicio());
agendamento.setHorarioFim(req.getHorarioInicio().plus(tipo.getDuracaoMinutos(), MINUTES));
agendamento.setProfissional(novoProfissional);
agendamento.setTipoAtendimento(novoTipo);
agendamento.setPublicToken(null);   // ⚠️ link de autoatendimento invalidado
agendamentoRepository.save(agendamento);
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__create_historico_status.sql` | NEW | `usuario_id` nullable |
| `domain/entities/HistoricoStatus.java` | NEW | |
| `domain/repositories/HistoricoStatusRepository.java` | NEW | |
| `domain/services/AgendamentoStatusValidator.java` | NEW | **R2 — state machine backend** |
| `domain/exceptions/TransicaoInvalidaException.java` | NEW | HTTP 409 |
| `domain/exceptions/JustificativaObrigatoriaException.java` | NEW | HTTP 409 |
| `api/dtos/request/AtualizarStatusRequest.java` | NEW | |
| `api/dtos/request/EditarAgendamentoRequest.java` | NEW | |
| `api/dtos/response/AgendamentoDetalheResponse.java` | NEW | Com histórico |
| `api/dtos/response/HistoricoStatusResponse.java` | NEW | `usuarioNome` resolvido |
| `domain/services/AgendamentoService.java` | **UPDATE** | Adicionar `atualizarStatus` e `editar` |
| `api/controllers/AgendamentoController.java` | **UPDATE** | PATCH status, GET detalhe, PUT editar |
| `api/exceptions/GlobalExceptionHandler.java` | **UPDATE** | `TransicaoInvalidaException`, `JustificativaObrigatoriaException` |
| `src/shared/components/AgendamentoCard.tsx` | **UPDATE** | Botões contextuais + menu `···` |
| `src/pages/agenda/DialogCancelarAgendamento.tsx` | NEW | Justificativa obrigatória |
| `src/pages/AgendamentoDetalhePage.tsx` | NEW | Detalhe + histórico |
| `src/shared/types/agendamento.ts` | **UPDATE** | Adicionar `LABEL_STATUS` |
| `test/.../AgendamentoStatusValidatorTest.java` | NEW | Todas as transições |
| `test/.../AgendamentoStatusIT.java` | NEW | Integração |

### Referências

- [Source: epics.md#Story 4.3] — Acceptance Criteria completos e FR-027, FR-028, FR-030, FR-052, FR-055, FR-057; R2
- [Source: epics.md#R2] — `AgendamentoStatusValidator` obrigatório no backend
- [Source: 4-2-criar-agendamento.md] — `DisponibilidadeValidator`, `DoubleBookingValidator` reutilizados em editar
- [Source: 3-1-componentes-base-ui.md] — `StatusBadge`, `useToast`
- [Source: 3-2-configuracoes-clinica.md] — `useFusoHorario` para timestamps no histórico

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
