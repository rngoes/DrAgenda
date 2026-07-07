# Story 6.2: Direito ao Esquecimento — Anonimização de Pacientes

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero poder anonimizar os dados pessoais de um paciente mediante confirmação explícita,
Para que a clínica possa cumprir o direito ao esquecimento previsto na LGPD.

## Acceptance Criteria

**AC-1 — Botão "Remover dados pessoais" na tela de detalhe do cliente (FR-039)**
- Given Admin Empresa acessa a tela de detalhe de um cliente via `/clientes/:id`
- When a tela renderiza para perfil `ADMIN_EMPRESA`
- Then exibe botão `"Remover dados pessoais"` em cor `error` (`bg-red-600`) no rodapé da tela
- And Staff NÃO vê o botão (perfil insuficiente)

**AC-2 — Dialog de confirmação explícita**
- Given Admin Empresa toca "Remover dados pessoais"
- When o dialog de confirmação é exibido
- Then mostra: `"Esta ação é irreversível. Os dados pessoais de [nome atual do paciente] serão substituídos por [PACIENTE REMOVIDO]. O histórico de agendamentos será preservado."`
- And dois botões: `[Cancelar]` (ghost) e `[Confirmar remoção]` (destructive / `bg-red-600`)
- And `[Confirmar remoção]` chama `DELETE /api/v1/clientes/{id}/anonimizar`

**AC-3 — Backend: anonimização dos campos PII (FR-039)**
- Given Admin Empresa chama `DELETE /api/v1/clientes/{id}/anonimizar`
- When o backend processa
- Then substitui os valores AES-256 criptografados de `nome_enc`, `telefone_enc` e `data_nascimento_enc` pelos valores criptografados da string `[PACIENTE REMOVIDO]`
- And marca `anonimizado = true` no registro do cliente
- And preserva intactos todos os registros de `agendamentos` e `historico_status` vinculados — apenas os campos PII são sobrescritos
- And registra `CLIENTE_ANONIMIZADO` no `audit_log` via `AuditService` (Story 6.1)
- And retorna HTTP 204 No Content

**AC-4 — Segurança: apenas Admin Empresa pode anonimizar**
- Given qualquer outro perfil (STAFF, PROFISSIONAL) chama `DELETE /api/v1/clientes/{id}/anonimizar`
- When o `@PreAuthorize` avalia
- Then retorna HTTP 403

**AC-5 — R5: Teste de integridade LGPD pós-anonimização**
- Given a anonimização é concluída
- When o `audit_log` é inspecionado para o `recurso_id` desse cliente
- Then nenhuma linha contém o nome original do paciente em nenhuma coluna — o nome nunca foi gravado no log, apenas o `cliente_id` numérico (R5)
- And o teste de integração desta story executa a sequência completa: (1) criar cliente com nome conhecido, (2) executar anonimização, (3) varrer todas as colunas de `audit_log` para `recurso_id` desse cliente e afirmar que nenhuma contém o nome original

**AC-6 — Exibição de dados anonimizados em listagens e agendamentos**
- Given um cliente foi anonimizado (`anonimizado = true`)
- When aparece em qualquer listagem (`/clientes`), card de agendamento ou tela de detalhe
- Then exibe `[PACIENTE REMOVIDO]` no lugar de nome, telefone e data de nascimento
- And o badge `StatusBadge` com cor warning `#D97706` e texto `"Dados removidos"` é exibido na linha/card do cliente

**AC-7 — Impedir novo agendamento para cliente anonimizado**
- Given um cliente está anonimizado
- When Staff tenta criar agendamento para esse cliente via `GET /api/v1/clientes/autocomplete`
- Then o cliente anonimizado NÃO aparece nos resultados de autocomplete
- And se o `clienteId` de um cliente anonimizado for enviado diretamente no `POST /api/v1/agendamentos`, retorna HTTP 409 com `"Não é possível agendar para cliente com dados removidos"`

## Tasks / Subtasks

- [ ] **Task 1 — Coluna `anonimizado` em `clientes`** (AC-3)
  - [ ] Verificar se a coluna `anonimizado` foi incluída na migration V7 (Story 3.5)
  - [ ] Se não: criar `resources/db/migration/V{yyyyMMddHHmm}__add_anonimizado_clientes.sql`:
    ```sql
    ALTER TABLE clientes ADD COLUMN anonimizado TINYINT(1) NOT NULL DEFAULT 0;
    CREATE INDEX idx_clientes_anonimizado ON clientes (anonimizado);
    ```
  - [ ] Adicionar campo `boolean anonimizado = false` à entidade `Cliente.java` se não presente

- [ ] **Task 2 — `ClienteService.anonimizar()` (AC-3, AC-4)**
  - [ ] Adicionar ao `ClienteService.java`:
    ```java
    @Transactional
    public void anonimizar(Long id, Long empresaId) {
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new ClienteNotFoundException(id));

        if (cliente.isAnonimizado()) {
            throw new ClienteJaAnonimizadoException("Cliente já foi anonimizado.");
        }

        // Sobrescrever campos PII com valor de anonimização
        // O AesAttributeConverter criptografa automaticamente ao salvar
        cliente.setNome("[PACIENTE REMOVIDO]");
        cliente.setTelefone("[PACIENTE REMOVIDO]");
        cliente.setDataNascimento(null);  // ou "[PACIENTE REMOVIDO]" se campo não-null
        cliente.setAnonimizado(true);
        clienteRepository.save(cliente);

        // Registrar no audit_log (Story 6.1) — apenas recurso_id, sem PII
        auditService.registrar(
            AuditAcao.CLIENTE_ANONIMIZADO, "CLIENTE", id,
            empresaId, SecurityUtils.getUsuarioId(), null);
    }
    ```

- [ ] **Task 3 — Endpoint `DELETE /api/v1/clientes/{id}/anonimizar`** (AC-3, AC-4)
  - [ ] Adicionar ao `ClienteController.java`:
    ```java
    @DeleteMapping("/{id}/anonimizar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public void anonimizar(@PathVariable Long id) {
        Long empresaId = SecurityUtils.getEmpresaId();
        clienteService.anonimizar(id, empresaId);
    }
    ```

- [ ] **Task 4 — Exception `ClienteJaAnonimizadoException`** (AC-3)
  - [ ] Criar `domain/exceptions/ClienteJaAnonimizadoException.java`
  - [ ] Adicionar ao `GlobalExceptionHandler.java` → HTTP 409 com mensagem `"Cliente já foi anonimizado anteriormente."`

- [ ] **Task 5 — Filtrar anonimizados no autocomplete** (AC-7)
  - [ ] Atualizar query de autocomplete em `ClienteRepository.java` (Story 3.6) para adicionar `AND c.anonimizado = false`
  - [ ] Verificar também query de listagem `GET /api/v1/clientes` — decidir se clientes anonimizados aparecem na lista (com `[PACIENTE REMOVIDO]`) ou são filtrados

- [ ] **Task 6 — Validação no `AgendamentoService.criar()`** (AC-7)
  - [ ] Adicionar verificação em `AgendamentoService.criar()` (Story 4.2):
    ```java
    if (cliente.isAnonimizado()) {
        throw new ClienteAnonimizadoException(
            "Não é possível agendar para cliente com dados removidos.");
    }
    ```
  - [ ] Adicionar ao `GlobalExceptionHandler.java` → HTTP 409

- [ ] **Task 7 — Frontend: botão e dialog na `ClienteDetalhePage.tsx`** (AC-1, AC-2)
  - [ ] Criar (ou atualizar) `src/pages/ClienteDetalhePage.tsx`:
    - Exibir botão `"Remover dados pessoais"` apenas quando `perfil === 'ADMIN_EMPRESA'`
    - Badge `"Dados removidos"` se `cliente.anonimizado = true`
  - [ ] Criar `src/pages/clientes/DialogAnonimizarCliente.tsx`:
    ```tsx
    <Dialog>
      <DialogTitle>Remover dados pessoais</DialogTitle>
      <DialogDescription>
        Esta ação é irreversível. Os dados pessoais de <strong>{nomeCliente}</strong> serão
        substituídos por [PACIENTE REMOVIDO]. O histórico de agendamentos será preservado.
      </DialogDescription>
      <Button variant="ghost">Cancelar</Button>
      <Button variant="destructive" onClick={confirmar} loading={isPending}>
        Confirmar remoção
      </Button>
    </Dialog>
    ```
  - [ ] Ao confirmar: `useMutation` → `DELETE /api/v1/clientes/{id}/anonimizar` → HTTP 204
  - [ ] Após sucesso: `queryClient.invalidateQueries(['clientes'])` + toast `"Dados pessoais removidos com sucesso"`

- [ ] **Task 8 — Badge em `ClientesPage` e `AgendamentoCard`** (AC-6)
  - [ ] Atualizar `ClientesPage.tsx` (Story 3.6): se `cliente.anonimizado = true`, exibir badge warning e nome `[PACIENTE REMOVIDO]`
  - [ ] Atualizar `AgendamentoCard.tsx` (Story 4.1): se `agendamento.cliente.anonimizado = true`, exibir `[PACIENTE REMOVIDO]` no lugar do nome

- [ ] **Task 9 — Testes** (AC-3, AC-4, AC-5)
  - [ ] Teste de integração `AnonimizacaoIT` — teste crítico R5:
    1. Criar cliente com nome `"LGPD Teste Silva"`
    2. Editar cliente (para gerar entrada `CLIENTE_EDITADO` no audit_log)
    3. Chamar `DELETE /api/v1/clientes/{id}/anonimizar`
    4. Afirmar HTTP 204
    5. Carregar cliente do banco: `nome = "[PACIENTE REMOVIDO]"`, `anonimizado = true`
    6. Afirmar que `agendamentos` do cliente ainda existem (integridade preservada)
    7. **Varrer audit_log**: `SELECT * FROM audit_log WHERE recurso_id = :clienteId` → nenhuma coluna contém `"LGPD Teste Silva"` (R5)
    8. Afirmar que existe entrada `CLIENTE_ANONIMIZADO` no audit_log
  - [ ] Teste de integração `AnonimizacaoSegurancaIT`:
    - Staff → DELETE 403
    - Segundo DELETE (já anonimizado) → 409
    - POST agendamento com cliente anonimizado → 409
    - Autocomplete não retorna cliente anonimizado
  - [ ] Teste unitário `ClienteServiceAnonimizarTest`:
    - Cliente anonimizado: campos corretos após anonimização
    - AuditService é chamado com `CLIENTE_ANONIMIZADO` e sem PII

## Dev Notes

### ⚠️ Migration com timestamp (R6)

```sql
-- CORRETO: V202506042500__add_anonimizado_clientes.sql
-- ERRADO:  V12__add_anonimizado_clientes.sql
```

### `AesAttributeConverter` ao sobrescrever PII (R3)

O `@Convert(converter = AesAttributeConverter.class)` nos campos `nome`, `telefone` e `dataNascimento` da entidade `Cliente` cuida automaticamente da criptografia ao salvar. Basta setar o valor plaintext:

```java
cliente.setNome("[PACIENTE REMOVIDO]");
// JPA aplica AesAttributeConverter.convertToDatabaseColumn("[PACIENTE REMOVIDO]")
// e grava o ciphertext na coluna nome_enc
```

> **R3:** NÃO criar outro conversor AES. O `AesAttributeConverter` existente cobre este caso automaticamente.

### R5 — Integridade do Log Pós-Anonimização

A garantia do R5 é que o nome do paciente **nunca foi gravado** no `audit_log` — não que ele seja removido de lá após a anonimização. A Story 6.1 garantiu que apenas `recurso_id` (BIGINT) é gravado. A Story 6.2 confirma que isso se aplica também à entrada `CLIENTE_ANONIMIZADO`.

```
audit_log após anonimização de clienteId=42:
┌─────────────────────────────────────────────────────────────────────────┐
│ acao                  │ recurso_id │ detalhes                           │
├─────────────────────────────────────────────────────────────────────────┤
│ CLIENTE_CRIADO        │ 42         │ null                               │
│ CLIENTE_EDITADO       │ 42         │ {"camposAlterados":["telefone"]}   │
│ CLIENTE_ANONIMIZADO   │ 42         │ null                               │
└─────────────────────────────────────────────────────────────────────────┘
Nenhuma linha contém "João Silva" ou qualquer nome de paciente ✓
```

### `dataNascimento` — Tratamento de Campo Nullable

Se `dataNascimento` for nullable na entidade, setar `null` é mais simples que criptografar `"[PACIENTE REMOVIDO]"` — e semanticamente correto (dado removido):

```java
cliente.setNome("[PACIENTE REMOVIDO]");
cliente.setTelefone("[PACIENTE REMOVIDO]");
cliente.setDataNascimento(null);  // null = dado removido
```

Verificar a constraint da coluna `data_nascimento_enc` na migration V7.

### Exibição no Frontend

```tsx
// Utilitário reutilizável para exibir dados de cliente:
function exibirNomeCliente(cliente: { nome: string; anonimizado: boolean }): string {
  return cliente.anonimizado ? '[PACIENTE REMOVIDO]' : cliente.nome
}
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__add_anonimizado_clientes.sql` | NEW (se necessário) | Verificar V7 primeiro |
| `domain/entities/Cliente.java` | **UPDATE** | `boolean anonimizado` se ausente |
| `domain/services/ClienteService.java` | **UPDATE** | Método `anonimizar()` |
| `domain/exceptions/ClienteJaAnonimizadoException.java` | NEW | HTTP 409 |
| `domain/exceptions/ClienteAnonimizadoException.java` | NEW | HTTP 409 no agendamento |
| `api/controllers/ClienteController.java` | **UPDATE** | `DELETE /{id}/anonimizar` |
| `domain/services/AgendamentoService.java` | **UPDATE** | Verificar `anonimizado` ao criar |
| `domain/repositories/ClienteRepository.java` | **UPDATE** | Filtro `anonimizado=false` no autocomplete |
| `api/exceptions/GlobalExceptionHandler.java` | **UPDATE** | Novas exceptions |
| `src/pages/ClienteDetalhePage.tsx` | NEW/UPDATE | Botão + badge |
| `src/pages/clientes/DialogAnonimizarCliente.tsx` | NEW | Dialog de confirmação |
| `src/pages/ClientesPage.tsx` | **UPDATE** | Badge warning para anonimizados |
| `src/shared/components/AgendamentoCard.tsx` | **UPDATE** | `[PACIENTE REMOVIDO]` |
| `test/.../AnonimizacaoIT.java` | NEW | **Teste crítico R5** |
| `test/.../AnonimizacaoSegurancaIT.java` | NEW | |
| `test/.../ClienteServiceAnonimizarTest.java` | NEW | |

### Referências

- [Source: epics.md#Story 6.2] — Acceptance Criteria completos e FR-039, R5
- [Source: epics.md#R5] — PII nunca gravado em logs — garantia de anonimização completa
- [Source: 3-5-cadastro-clientes-lgpd.md] — `AesAttributeConverter`, campos `nome_enc`, `telefone_enc`, `data_nascimento_enc`
- [Source: 6-1-log-auditoria-retencao.md] — `AuditService`, `AuditAcao.CLIENTE_ANONIMIZADO`
- [Source: 4-2-criar-agendamento.md] — `AgendamentoService.criar()` — ponto de guarda para cliente anonimizado

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
