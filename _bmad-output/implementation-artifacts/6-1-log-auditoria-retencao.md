# Story 6.1: Log de Auditoria com Retenção Configurável

Status: ready-for-dev

## Story

Como sistema,
Quero registrar automaticamente todo acesso e modificação de dados de pacientes em um log de auditoria retido por 5 anos,
Para que a clínica esteja em conformidade com a LGPD e tenha rastreabilidade completa de operações sensíveis.

## Acceptance Criteria

**AC-1 — Migration V11: tabela `audit_log` (FR-038)**
- Given a migration `V{yyyyMMddHHmm}__create_audit_log.sql` é aplicada
- When o sistema inicializa
- Then a tabela `audit_log` existe com colunas:
  - `id` BIGINT PK AUTO_INCREMENT
  - `empresa_id` BIGINT FK nullable → `empresas.id`
  - `usuario_id` BIGINT FK nullable → `usuarios.id`
  - `acao` VARCHAR(100) NOT NULL — ex: `CLIENTE_CRIADO`, `CLIENTE_EDITADO`, `CLIENTE_ANONIMIZADO`, `CLIENTE_LIDO`
  - `recurso_tipo` VARCHAR(50) NOT NULL — ex: `CLIENTE`
  - `recurso_id` BIGINT nullable
  - `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
  - `detalhes` TEXT nullable — JSON com campos alterados, sem valores sensíveis
- And índices `idx_audit_empresa_id` e `idx_audit_timestamp`

**AC-2 — Registro automático via `AuditService` (FR-038, R5)**
- Given qualquer operação nos métodos de `ClienteService` é completada com sucesso:
  - `criar()` → `acao = CLIENTE_CRIADO`
  - `atualizar()` → `acao = CLIENTE_EDITADO`
  - `buscarPorId()` → `acao = CLIENTE_LIDO`
  - `listar()` → NÃO registrar (operação de listagem não auditada — apenas acesso individual)
- When o `AuditService.registrar()` é chamado
- Then uma entrada é inserida com `empresa_id`, `usuario_id` (de `SecurityUtils.getUsuarioId()`), `recurso_tipo = "CLIENTE"`, `recurso_id = cliente.getId()` e `timestamp` UTC
- And `detalhes` para `CLIENTE_EDITADO` contém JSON com campos alterados mas **NUNCA os valores**: `{"camposAlterados": ["nome", "telefone"]}`
- And `detalhes` para `CLIENTE_CRIADO` e `CLIENTE_LIDO`: `null`

**AC-3 — R5: Proibição absoluta de PII no `audit_log`**
- Given uma operação de auditoria é registrada
- When o `audit_log` é inspecionado
- Then `recurso_id` armazena apenas o `id` (BIGINT) do cliente — nome, telefone, data de nascimento ou qualquer campo PII **nunca** é gravado em nenhuma coluna do `audit_log`
- And teste de integração desta story verifica: após criar e editar cliente com nome `"Auditoria Teste Paciente"`, nenhuma linha em `audit_log` com `recurso_id = clienteId` contém a string `"Auditoria Teste Paciente"` em nenhuma coluna

**AC-4 — Job de retenção de 5 anos (FR-038)**
- Given a constante `auditoria.retencao-anos` está configurada em `application.properties` com valor `5`
- When o `AuditRetencaoJob` executa diariamente via `@Scheduled(cron = "0 0 2 * * *")` (02:00 UTC)
- Then registros com `timestamp < NOW() - INTERVAL {retencao-anos} YEAR` são excluídos
- And a constante é configurável sem redeploy (`@Value("${auditoria.retencao-anos:5}")`)

**AC-5 — Endpoint `GET /api/v1/audit-log` retorna 403 para todos**
- Given qualquer usuário autenticado (qualquer perfil) acessa `GET /api/v1/audit-log`
- When o `@PreAuthorize` avalia
- Then retorna HTTP 403 — log é interno sem UI de consulta no MVP

**AC-6 — Independência de falhas de auditoria**
- Given o `AuditService.registrar()` lança qualquer exceção
- When ocorre durante operação de `ClienteService`
- Then a operação principal (criar/editar/ler cliente) **não é revertida** — auditoria usa `@Async` ou try/catch para não afetar a transação principal
- And falha de auditoria é logada em `ERROR` level mas não propagada ao usuário

## Tasks / Subtasks

- [ ] **Task 1 — Migration V11** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__create_audit_log.sql`
  - [ ] Usar timestamp real no momento da implementação (R6 — nunca V11 sequencial)

- [ ] **Task 2 — Entidade `AuditLog`** (AC-1)
  - [ ] Criar `domain/entities/AuditLog.java`:
    ```java
    @Entity @Table(name = "audit_log")
    public class AuditLog {
        @Id @GeneratedValue(strategy = IDENTITY)
        private Long id;
        private Long empresaId;
        private Long usuarioId;
        @Column(nullable = false, length = 100)
        private String acao;
        @Column(nullable = false, length = 50)
        private String recursoTipo;
        private Long recursoId;
        @Column(nullable = false)
        private Instant timestamp;
        @Column(columnDefinition = "TEXT")
        private String detalhes;  // JSON string — nunca PII

        @PrePersist
        void prePersist() { if (timestamp == null) timestamp = Instant.now(); }
    }
    ```

- [ ] **Task 3 — `AuditLogRepository`** (AC-2, AC-5)
  - [ ] Criar `domain/repositories/AuditLogRepository.java`:
    ```java
    public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
        void deleteByTimestampBefore(Instant limite);
    }
    ```

- [ ] **Task 4 — `AuditService`** (AC-2, AC-3, AC-6)
  - [ ] Criar `domain/services/AuditService.java`:
    ```java
    @Service
    @RequiredArgsConstructor
    public class AuditService {

        private final AuditLogRepository auditLogRepository;

        // Chamada assíncrona — falha não reverte a transação principal
        @Async
        public void registrar(String acao, String recursoTipo, Long recursoId,
                              Long empresaId, Long usuarioId, String detalhesJson) {
            try {
                AuditLog log = new AuditLog();
                log.setAcao(acao);
                log.setRecursoTipo(recursoTipo);
                log.setRecursoId(recursoId);
                log.setEmpresaId(empresaId);
                log.setUsuarioId(usuarioId);
                log.setDetalhes(detalhesJson);  // null ou JSON sem PII
                auditLogRepository.save(log);
            } catch (Exception e) {
                log.error("Falha ao registrar audit log: {}", e.getMessage());
            }
        }
    }
    ```
  - [ ] Adicionar `@EnableAsync` em classe `@Configuration` ou em `Application.java`

- [ ] **Task 5 — Constantes de ação de auditoria** (AC-2)
  - [ ] Criar `domain/constants/AuditAcao.java`:
    ```java
    public final class AuditAcao {
        public static final String CLIENTE_CRIADO     = "CLIENTE_CRIADO";
        public static final String CLIENTE_EDITADO    = "CLIENTE_EDITADO";
        public static final String CLIENTE_LIDO       = "CLIENTE_LIDO";
        public static final String CLIENTE_ANONIMIZADO = "CLIENTE_ANONIMIZADO";
        private AuditAcao() {}
    }
    ```

- [ ] **Task 6 — Integrar `AuditService` no `ClienteService`** (AC-2, AC-3)
  - [ ] Injetar `AuditService` em `ClienteService.java` (Story 3.5/3.6)
  - [ ] Após `criar()` bem-sucedido:
    ```java
    auditService.registrar(CLIENTE_CRIADO, "CLIENTE", cliente.getId(),
        empresaId, SecurityUtils.getUsuarioId(), null);
    ```
  - [ ] Após `atualizar()` bem-sucedido — montar `detalhes` com campos alterados (sem valores):
    ```java
    List<String> camposAlterados = new ArrayList<>();
    if (!Objects.equals(request.getNome(), clienteExistente.getNome()))
        camposAlterados.add("nome");
    if (!Objects.equals(request.getTelefone(), clienteExistente.getTelefone()))
        camposAlterados.add("telefone");
    // ... demais campos
    String detalhes = camposAlterados.isEmpty() ? null
        : "{\"camposAlterados\":" + camposAlterados.stream()
            .map(s -> "\"" + s + "\"")
            .collect(Collectors.joining(",", "[", "]")) + "}";
    auditService.registrar(CLIENTE_EDITADO, "CLIENTE", cliente.getId(),
        empresaId, SecurityUtils.getUsuarioId(), detalhes);
    ```
  - [ ] Após `buscarPorId()` bem-sucedido:
    ```java
    auditService.registrar(CLIENTE_LIDO, "CLIENTE", id,
        empresaId, SecurityUtils.getUsuarioId(), null);
    ```
  - [ ] ⚠️ `listar()` e `autocomplete()` NÃO são auditados

- [ ] **Task 7 — `AuditRetencaoJob`** (AC-4)
  - [ ] Criar `infrastructure/jobs/AuditRetencaoJob.java`:
    ```java
    @Component
    @RequiredArgsConstructor
    public class AuditRetencaoJob {

        private final AuditLogRepository auditLogRepository;

        @Value("${auditoria.retencao-anos:5}")
        private int retencaoAnos;

        @Scheduled(cron = "0 0 2 * * *")  // 02:00 UTC diariamente
        @Transactional
        public void executar() {
            Instant limite = Instant.now()
                .minus(retencaoAnos * 365L, ChronoUnit.DAYS);  // aproximação segura
            auditLogRepository.deleteByTimestampBefore(limite);
        }
    }
    ```

- [ ] **Task 8 — `application.properties`** (AC-4)
  - [ ] Adicionar: `auditoria.retencao-anos=5`

- [ ] **Task 9 — Endpoint `GET /api/v1/audit-log` retorna 403** (AC-5)
  - [ ] Criar `api/controllers/AuditLogController.java` com único endpoint:
    ```java
    @RestController
    @RequestMapping("/api/v1/audit-log")
    @PreAuthorize("denyAll()")  // 403 para todos no MVP
    public class AuditLogController {
        @GetMapping
        public void listar() {}
    }
    ```

- [ ] **Task 10 — Testes** (AC-2, AC-3, AC-6)
  - [ ] Teste de integração `AuditLogIT` — o teste mais importante desta story (R5):
    1. Criar cliente com `nome = "Auditoria Teste Paciente"`
    2. Editar cliente (alterar nome e telefone)
    3. Consultar `SELECT * FROM audit_log WHERE recurso_id = :clienteId`
    4. **Afirmar que nenhuma linha contém "Auditoria Teste Paciente" em nenhuma coluna** (id, empresa_id, usuario_id, acao, recurso_tipo, recurso_id, timestamp, detalhes)
    5. Afirmar que existem entradas `CLIENTE_CRIADO` e `CLIENTE_EDITADO`
    6. Afirmar que `detalhes` de `CLIENTE_EDITADO` contém `camposAlterados` sem os valores
  - [ ] Teste unitário `AuditServiceTest`:
    - `registrar()` com exceção no repository: não propaga exceção, loga ERROR
    - `registrar()` bem-sucedido: persiste com campos corretos

## Dev Notes

### ⚠️ Migration com timestamp (R6)

```sql
-- CORRETO: V202506042400__create_audit_log.sql
-- ERRADO:  V11__create_audit_log.sql
```

### Migration SQL

```sql
-- V202506042400__create_audit_log.sql (usar timestamp real)

CREATE TABLE audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    empresa_id   BIGINT       NULL,
    usuario_id   BIGINT       NULL,
    acao         VARCHAR(100) NOT NULL,
    recurso_tipo VARCHAR(50)  NOT NULL,
    recurso_id   BIGINT       NULL,
    timestamp    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalhes     TEXT         NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_empresa  FOREIGN KEY (empresa_id)  REFERENCES empresas(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_usuario  FOREIGN KEY (usuario_id)  REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_empresa_id ON audit_log (empresa_id);
CREATE INDEX idx_audit_timestamp  ON audit_log (timestamp);
```

> FKs com `ON DELETE SET NULL`: se empresa ou usuário for deletado, o registro de auditoria permanece (retenção LGPD) com o campo setado para NULL.

### R5 — Garantia de Ausência de PII

O princípio central desta story: **o `audit_log` nunca armazena dados pessoais**. Apenas:
- `recurso_id` (BIGINT) — referência numérica ao cliente
- `camposAlterados` (lista de nomes de campos, sem valores) — em `detalhes`

```java
// CORRETO — apenas IDs e nomes de campo:
auditService.registrar("CLIENTE_EDITADO", "CLIENTE", cliente.getId(), ...
    "{\"camposAlterados\":[\"nome\",\"telefone\"]}");

// ERRADO — nunca gravar valores PII:
auditService.registrar("CLIENTE_EDITADO", "CLIENTE", cliente.getId(), ...
    "{\"nomeAnterior\":\"João Silva\",\"nomeNovo\":\"João Santos\"}");
```

### `@Async` — Isolamento de Falha

O `@Async` em `AuditService.registrar()` executa em thread separada do pool de tasks do Spring. Se o audit falhar, a transação do `ClienteService` já commitou e o usuário recebeu a resposta — a falha de auditoria é tratada internamente.

> **Atenção:** `@Async` requer `@EnableAsync` configurado. Pode ser adicionado ao mesmo `@Configuration` que `@EnableScheduling` (Story 4.4).

### `SecurityUtils.getUsuarioId()`

Este método precisa ser adicionado a `SecurityUtils.java` (Story 2.2 adicionou `getEmpresaId()` e `getPerfil()`):

```java
public static Long getUsuarioId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) return null;
    return user.getUsuarioId();
}
```

### Cálculo de `camposAlterados` — Comparação Segura

Como os campos `nome`, `telefone` e `dataNascimento` são descriptografados pelo JPA ao carregar a entidade, a comparação é entre strings plaintext — segura e precisa:

```java
// Antes de atualizar, capturar estado anterior:
String nomeAnterior = clienteExistente.getNome();  // já descriptografado
// ... após aplicar o request ao objeto
// comparar e listar campos que mudaram
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__create_audit_log.sql` | NEW | FK com `ON DELETE SET NULL` |
| `domain/entities/AuditLog.java` | NEW | Sem `@Convert` — nunca PII |
| `domain/repositories/AuditLogRepository.java` | NEW | `deleteByTimestampBefore()` |
| `domain/services/AuditService.java` | NEW | `@Async` + try/catch |
| `domain/constants/AuditAcao.java` | NEW | Constantes tipadas |
| `domain/services/ClienteService.java` | **UPDATE** | Injetar `AuditService`, 3 pontos de chamada |
| `infrastructure/jobs/AuditRetencaoJob.java` | NEW | `@Scheduled cron 02:00 UTC` |
| `api/controllers/AuditLogController.java` | NEW | `denyAll()` no MVP |
| `Application.java` | **UPDATE** (verificar) | `@EnableAsync` se não presente |
| `application.properties` | **UPDATE** | `auditoria.retencao-anos=5` |
| `application-test.properties` | **UPDATE** | `auditoria.retencao-anos=5` (ou menor para testes) |
| `domain/security/SecurityUtils.java` | **UPDATE** | Adicionar `getUsuarioId()` |
| `test/.../AuditLogIT.java` | NEW | **Teste crítico R5** |
| `test/.../AuditServiceTest.java` | NEW | |

### Referências

- [Source: epics.md#Story 6.1] — Acceptance Criteria completos e FR-038, R5
- [Source: epics.md#R5] — `recurso_id` apenas — nenhum PII em logs
- [Source: 3-5-cadastro-clientes-lgpd.md] — `ClienteService`, `AesAttributeConverter`, descriptografia automática
- [Source: 3-6-listagem-busca-clientes.md] — `ClienteService.buscarPorId()`, `ClienteService.atualizar()`
- [Source: 4-4-noshow-automatico-proximos-disponiveis.md] — `@EnableScheduling` já em `Application.java`
- [Source: 2-2-multitenant-autorizacao-navegacao.md] — `SecurityUtils.getEmpresaId()` como referência para `getUsuarioId()`

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
