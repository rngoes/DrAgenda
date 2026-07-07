# Story 3.3: Tipos de Atendimento — Cadastro, Edição e Inativação

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero cadastrar e gerenciar os tipos de atendimento com suas durações,
Para que o sistema calcule automaticamente o tempo de cada consulta ao criar agendamentos.

## Acceptance Criteria

**AC-1 — Migration V5: tabela `tipos_atendimento`**
- Given a migration `V{yyyyMMddHHmm}__create_tipos_atendimento.sql` é aplicada
- When o sistema inicializa
- Then a tabela `tipos_atendimento` existe com colunas:
  - `id` BIGINT PK AUTO_INCREMENT
  - `empresa_id` BIGINT FK NOT NULL → `empresas.id`
  - `nome` VARCHAR(255) NOT NULL
  - `duracao_minutos` INT NOT NULL
  - `ativo` BOOLEAN NOT NULL DEFAULT TRUE
  - `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
- And índice `idx_tipos_atendimento_empresa_id` existe

**AC-2 — Criar tipo de atendimento**
- Given o Admin Empresa envia `POST /api/v1/tipos-atendimento` com `{ "nome": "Consulta", "duracaoMinutos": 30 }`
- When o backend persiste
- Then o tipo é criado com `ativo = true` e `empresa_id` do JWT (FR-012)
- And `duracaoMinutos` deve ser inteiro positivo entre 5 e 480 — retorna HTTP 400 se inválido
- And retorna HTTP 201

**AC-3 — Listar tipos (todos)**
- Given o Admin Empresa acessa `GET /api/v1/tipos-atendimento`
- When a listagem é retornada
- Then exibe todos os tipos da empresa (ativos e inativos) com paginação server-side
- And page size padrão = 20

**AC-4 — Listar tipos ativos (para formulário de agendamento)**
- Given qualquer usuário autenticado acessa `GET /api/v1/tipos-atendimento/ativos`
- When a listagem é retornada
- Then exibe apenas tipos com `ativo = true` da empresa do JWT
- And sem paginação (lista completa — usada em seletores de formulário)

**AC-5 — Editar tipo de atendimento**
- Given o Admin Empresa envia `PUT /api/v1/tipos-atendimento/{id}` com novos `nome` e/ou `duracaoMinutos`
- When o backend persiste
- Then os dados são atualizados e retorna HTTP 200
- And agendamentos já criados com esse tipo NÃO têm duração recalculada retroativamente

**AC-6 — Inativar tipo**
- Given o Admin Empresa envia `PATCH /api/v1/tipos-atendimento/{id}/inativar`
- When a operação é realizada
- Then `ativo = false` (FR-053)
- And `GET /api/v1/tipos-atendimento/ativos` não retorna esse tipo

**AC-7 — Reativar tipo**
- Given o Admin Empresa envia `PATCH /api/v1/tipos-atendimento/{id}/reativar`
- When a operação é realizada
- Then `ativo = true`

**AC-8 — Isolamento multi-tenant**
- Given o Admin Empresa tenta acessar tipo de outra empresa
- When o backend verifica
- Then retorna HTTP 404

## Tasks / Subtasks

- [ ] **Task 1 — Migration V5** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__create_tipos_atendimento.sql`
  - [ ] Usar timestamp real no momento da implementação
  - [ ] SQL com tabela + índice `idx_tipos_atendimento_empresa_id`

- [ ] **Task 2 — Entidade `TipoAtendimento`** (AC-1)
  - [ ] Criar `domain/entities/TipoAtendimento.java` com anotações JPA
  - [ ] Campos: `id`, `empresa` (ManyToOne lazy), `nome`, `duracaoMinutos`, `ativo`, `createdAt`
  - [ ] `@PrePersist` para `createdAt`

- [ ] **Task 3 — `TipoAtendimentoRepository`** (AC-3, AC-4, AC-8)
  - [ ] Criar `domain/repositories/TipoAtendimentoRepository.java`
  - [ ] `Page<TipoAtendimento> findAllByEmpresaId(Long empresaId, Pageable pageable)`
  - [ ] `List<TipoAtendimento> findAllByEmpresaIdAndAtivoTrue(Long empresaId)` — sem paginação, para seletor
  - [ ] `Optional<TipoAtendimento> findByIdAndEmpresaId(Long id, Long empresaId)` — isolamento

- [ ] **Task 4 — DTOs** (AC-2, AC-3, AC-5)
  - [ ] Criar `api/dtos/request/CriarTipoAtendimentoRequest.java`: `@NotBlank nome`, `@Min(5) @Max(480) @NotNull duracaoMinutos`
  - [ ] Criar `api/dtos/request/AtualizarTipoAtendimentoRequest.java`: mesmos campos
  - [ ] Criar `api/dtos/response/TipoAtendimentoResponse.java`: `id`, `nome`, `duracaoMinutos`, `ativo`, `createdAt`

- [ ] **Task 5 — `TipoAtendimentoService`** (AC-2–AC-8)
  - [ ] Criar `domain/services/TipoAtendimentoService.java`
  - [ ] `criar(CriarTipoAtendimentoRequest, Long empresaId)` — `@Transactional` → HTTP 201
  - [ ] `listarTodos(Long empresaId, Pageable)` → `Page<TipoAtendimentoResponse>`
  - [ ] `listarAtivos(Long empresaId)` → `List<TipoAtendimentoResponse>` (sem paginação)
  - [ ] `atualizar(Long id, Long empresaId, AtualizarTipoAtendimentoRequest)` → HTTP 200
  - [ ] `inativar(Long id, Long empresaId)` → `ativo = false`
  - [ ] `reativar(Long id, Long empresaId)` → `ativo = true`
  - [ ] Todos os métodos de escrita: `findByIdAndEmpresaId()` → 404 se não encontrado

- [ ] **Task 6 — `TipoAtendimentoController`** (AC-2–AC-8)
  - [ ] Criar `api/controllers/TipoAtendimentoController.java`
  - [ ] `POST /` — `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` → 201
  - [ ] `GET /` — `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` → 200 paginado
  - [ ] `GET /ativos` — `@PreAuthorize("hasAnyRole('ADMIN_EMPRESA','STAFF','PROFISSIONAL')")` → 200 lista completa
  - [ ] `PUT /{id}` — `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` → 200
  - [ ] `PATCH /{id}/inativar` — `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` → 204
  - [ ] `PATCH /{id}/reativar` — `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` → 204
  - [ ] `SecurityUtils.getEmpresaId()` em todos os métodos

- [ ] **Task 7 — Frontend** (AC-3, AC-4, AC-6)
  - [ ] Criar `src/pages/configuracoes/TiposAtendimentoTab.tsx` (sub-tab de `/configuracoes`)
  - [ ] Listagem com coluna: Nome · Duração · Status (ativo/inativo) · Ações
  - [ ] Formulário inline ou dialog: Nome + Duração em minutos
  - [ ] Validação frontend: duração entre 5 e 480 com mensagem "Entre 5 e 480 minutos"
  - [ ] Skeleton durante carregamento; `toast.success` / `toast.error` em ações
  - [ ] Botão "Inativar"/"Reativar" por linha com confirmação (`window.confirm` ou Dialog)

- [ ] **Task 8 — Testes** (AC-2, AC-4, AC-6, AC-8)
  - [ ] Teste unitário `TipoAtendimentoServiceTest`:
    - Criar: tipo salvo, `ativo = true`
    - Duração inválida (4 e 481): HTTP 400
    - Inativar: `ativo = false`; `listarAtivos` não retorna
    - Isolamento: id de outra empresa → 404
  - [ ] Teste de integração `TipoAtendimentoControllerIT`:
    - POST → 201; GET /ativos → apenas ativos da empresa; STAFF → 403 em POST

## Dev Notes

### ⚠️ Migration com timestamp (não V5 sequencial) — R6

```sql
-- CORRETO: V202506041700__create_tipos_atendimento.sql
-- ERRADO:  V5__create_tipos_atendimento.sql
```

### Migration SQL

```sql
-- V202506041700__create_tipos_atendimento.sql (usar timestamp real)

CREATE TABLE tipos_atendimento (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    empresa_id       BIGINT       NOT NULL,
    nome             VARCHAR(255) NOT NULL,
    duracao_minutos  INT          NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tipos_atendimento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

CREATE INDEX idx_tipos_atendimento_empresa_id ON tipos_atendimento (empresa_id);
```

### Entidade `TipoAtendimento.java`

```java
@Entity
@Table(name = "tipos_atendimento")
public class TipoAtendimento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String nome;

    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}
```

### `TipoAtendimentoService.java` — Detalhe da Validação de Duração

```java
@Transactional
public TipoAtendimentoResponse criar(CriarTipoAtendimentoRequest req, Long empresaId) {
    // Bean Validation (@Min(5) @Max(480)) já rejeita valores inválidos com HTTP 400
    // Se usar validação no service diretamente:
    if (req.getDuracaoMinutos() < 5 || req.getDuracaoMinutos() > 480) {
        throw new IllegalArgumentException("Duração deve ser entre 5 e 480 minutos");
    }

    Empresa empresa = empresaRepository.findById(empresaId)
        .orElseThrow(RecursoNaoEncontradoException::new);

    TipoAtendimento tipo = new TipoAtendimento();
    tipo.setEmpresa(empresa);
    tipo.setNome(req.getNome());
    tipo.setDuracaoMinutos(req.getDuracaoMinutos());
    tipo.setAtivo(true);

    return toResponse(tipoAtendimentoRepository.save(tipo));
}
```

> **Preferência:** usar `@Min(5) @Max(480)` no DTO com Bean Validation — a mensagem de erro padrão do `GlobalExceptionHandler` (Story 2.1) já formata corretamente como HTTP 400 RFC 7807. Não reimplementar a validação no service.

### `GET /ativos` — Endpoint Sem Paginação

```java
@GetMapping("/ativos")
@PreAuthorize("hasAnyRole('ADMIN_EMPRESA','STAFF','PROFISSIONAL')")
public List<TipoAtendimentoResponse> listarAtivos() {
    Long empresaId = SecurityUtils.getEmpresaId();
    return service.listarAtivos(empresaId);
}
```

> Este endpoint é usado no formulário de criação de agendamentos (Epic 4). Staff e Profissional precisam acessá-lo — por isso `hasAnyRole(...)`. A listagem gerencial completa (com inativos e paginação) é exclusiva do Admin Empresa.

### Nota sobre Agendamentos Existentes (AC-5)

A regra "agendamentos existentes não têm duração recalculada" é naturalmente garantida porque:
- A duração é calculada no momento da criação do agendamento (Epic 4) e armazenada diretamente no registro do agendamento
- Alterar `tipos_atendimento.duracao_minutos` não retroage porque o agendamento já tem seu próprio campo de duração
- Nenhuma lógica especial é necessária aqui — apenas documentar o comportamento esperado

### Sub-navegação em `ConfiguracoesPage`

A Story 3.2 criou `ConfiguracoesPage.tsx` com sub-navegação. Esta story adiciona `TiposAtendimentoTab.tsx` à mesma página. Sugestão de estrutura:

```tsx
// ConfiguracoesPage.tsx — adicionar tab
const tabs = [
  { path: 'clinica',           label: 'Dados da Clínica' },
  { path: 'tipos-atendimento', label: 'Tipos de Atendimento' },
  // Story 3.4 adicionará: { path: 'disponibilidade', label: 'Disponibilidade' }
]
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__create_tipos_atendimento.sql` | NEW | Timestamp real |
| `domain/entities/TipoAtendimento.java` | NEW | |
| `domain/repositories/TipoAtendimentoRepository.java` | NEW | 3 métodos de query |
| `api/dtos/request/CriarTipoAtendimentoRequest.java` | NEW | `@Min(5) @Max(480)` |
| `api/dtos/request/AtualizarTipoAtendimentoRequest.java` | NEW | |
| `api/dtos/response/TipoAtendimentoResponse.java` | NEW | |
| `domain/services/TipoAtendimentoService.java` | NEW | |
| `api/controllers/TipoAtendimentoController.java` | NEW | Autorização diferenciada por endpoint |
| `src/pages/configuracoes/TiposAtendimentoTab.tsx` | NEW | Sub-tab de ConfiguracoesPage |
| `src/pages/ConfiguracoesPage.tsx` | **UPDATE** | Adicionar tab de Tipos de Atendimento |
| `test/.../TipoAtendimentoServiceTest.java` | NEW | Unitários |
| `test/.../TipoAtendimentoControllerIT.java` | NEW | Integração |

### Referências

- [Source: epics.md#Story 3.3] — Acceptance Criteria completos e FR-012, FR-053
- [Source: epics.md#Story 1.2 AC-R6] — Flyway timestamp naming
- [Source: architecture.md#Regras de Processo] — empresaId do JWT
- [Source: 3-1-componentes-base-ui.md] — skeleton + `useToast` obrigatórios
- [Source: 3-2-configuracoes-clinica.md] — `ConfiguracoesPage` sub-navegação

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
