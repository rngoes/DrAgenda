# Story 2.4: Admin Empresa — Cadastro e Gestão de Profissionais

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero cadastrar e gerenciar os profissionais de saúde da minha clínica,
Para que eles possam fazer login no sistema e ter seus agendamentos gerenciados.

## Acceptance Criteria

**AC-1 — Migration V3: tabela `profissionais`**
- Given a migration `V{yyyyMMddHHmm}__create_profissionais.sql` é aplicada (timestamp real na implementação)
- When o sistema inicializa
- Then a tabela `profissionais` existe com colunas:
  - `id` BIGINT PK AUTO_INCREMENT
  - `usuario_id` BIGINT FK UNIQUE NOT NULL → `usuarios.id`
  - `empresa_id` BIGINT FK NOT NULL → `empresas.id`
  - `especialidade` VARCHAR(255) NULL
  - `ativo` BOOLEAN NOT NULL DEFAULT TRUE
  - `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
- And o índice `idx_profissionais_empresa_id` existe na coluna `empresa_id`

**AC-2 — Criar profissional (usuário + registro vinculado)**
- Given o Admin Empresa envia `POST /api/v1/profissionais` com `{ "nome": "...", "email": "...", "especialidade": "...", "senhaTemporaria": "..." }`
- When o backend processa
- Then cria `usuarios` com `perfil = PROFISSIONAL`, `empresa_id` do JWT, `senha_temporaria = true` (FR-007)
- And cria `profissionais` vinculado ao `usuarios.id`
- And `senhaTemporaria` validada via `PasswordValidator` (≥ 8 chars, maiúscula, minúscula, especial) → HTTP 400 se inválida
- And email único verificado → HTTP 409 se duplicado
- And a senha é armazenada com BCrypt (nunca em texto plano)
- And retorna HTTP 201 com `senhaTemporariaExibida` no response (exibida uma única vez)

**AC-3 — Listar profissionais da empresa**
- Given o Admin Empresa acessa `GET /api/v1/profissionais`
- When a listagem é retornada
- Then exibe apenas profissionais da empresa do JWT (`empresaId` de `SecurityUtils.getEmpresaId()`)
- And suporta paginação server-side com envelope `content`, `totalElements`, `totalPages`, `page`, `size` (FR-046)
- And page size padrão = 20

**AC-4 — Editar profissional**
- Given o Admin Empresa envia `PUT /api/v1/profissionais/{id}` com `{ "nome": "...", "especialidade": "..." }`
- When o backend processa
- Then `nome` e `especialidade` são atualizados
- And email **não pode ser alterado** (imutável após criação) — campo ignorado se enviado
- And retorna HTTP 200

**AC-5 — Inativar profissional**
- Given o Admin Empresa envia `PATCH /api/v1/profissionais/{id}/inativar`
- When a operação é realizada
- Then `profissional.ativo = false` (FR-009)
- And o `JwtAuthenticationFilter` (Story 2.1) bloqueia o acesso do profissional na próxima requisição (`usuario.ativo` verificado no filtro — **atenção:** `profissional.ativo` deve refletir em `usuario.ativo` ou o filtro deve verificar ambos)

**AC-6 — Reativar profissional**
- Given o Admin Empresa envia `PATCH /api/v1/profissionais/{id}/reativar`
- When a operação é realizada
- Then `profissional.ativo = true` e o profissional volta a poder fazer login

**AC-7 — Isolamento multi-tenant**
- Given o Admin Empresa tenta acessar profissional de outra empresa via `GET /api/v1/profissionais/{id}` ou `PUT /api/v1/profissionais/{id}`
- When o backend verifica
- Then retorna HTTP 404 (não expõe existência do recurso de outra empresa)

## Tasks / Subtasks

- [ ] **Task 1 — Migration V3** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__create_profissionais.sql`
  - [ ] Usar timestamp real no momento da implementação (formato `yyyyMMddHHmm`, ex: `V202506041500`)
  - [ ] Incluir criação da tabela + índice `idx_profissionais_empresa_id`
  - [ ] Testar aplicação da migration em H2 (CI) e MySQL local

- [ ] **Task 2 — Entidade `Profissional`** (AC-1)
  - [ ] Criar `domain/entities/Profissional.java` com anotações JPA
  - [ ] Campos: `id`, `usuario` (ManyToOne lazy), `empresa` (ManyToOne lazy), `especialidade`, `ativo`, `createdAt`
  - [ ] `@PrePersist` para `createdAt`
  - [ ] `usuario_id` com `@Column(unique = true)` — sem `@OneToOne` bidirecional para manter simplidade

- [ ] **Task 3 — `ProfissionalRepository`** (AC-3, AC-7)
  - [ ] Criar/substituir `domain/repositories/ProfissionalRepository.java` (stub da Story 2.2 vira implementação real)
  - [ ] Adicionar: `Page<Profissional> findAllByEmpresaId(Long empresaId, Pageable pageable)`
  - [ ] Adicionar: `Optional<Profissional> findByIdAndEmpresaId(Long id, Long empresaId)` — usado para isolamento (AC-7)
  - [ ] Adicionar: `boolean existsByUsuarioId(Long usuarioId)`

- [ ] **Task 4 — DTOs** (AC-2, AC-3, AC-4)
  - [ ] Criar `api/dtos/request/CriarProfissionalRequest.java`: `@NotBlank nome`, `@Email @NotBlank email`, `especialidade` (nullable), `@NotBlank senhaTemporaria`
  - [ ] Criar `api/dtos/request/AtualizarProfissionalRequest.java`: `@NotBlank nome`, `especialidade` (nullable) — sem campo email
  - [ ] Criar `api/dtos/response/ProfissionalResponse.java`: `id`, `nome`, `email`, `especialidade`, `ativo`, `createdAt`
  - [ ] Criar `api/dtos/response/CriarProfissionalResponse.java`: campos do `ProfissionalResponse` + `senhaTemporariaExibida`

- [ ] **Task 5 — `ProfissionalService`** (AC-2–AC-7)
  - [ ] Criar `domain/services/ProfissionalService.java`
  - [ ] `criar(CriarProfissionalRequest, Long empresaId)` — `@Transactional`:
    - Validar senha via `PasswordValidator.validateOrThrow()`
    - Verificar email único
    - Criar `Usuario` (PROFISSIONAL, `senhaTemporaria = true`) + salvar
    - Criar `Profissional` vinculado + salvar
    - Retornar `CriarProfissionalResponse` com senha exibida uma vez
  - [ ] `listar(Long empresaId, Pageable)` → `Page<ProfissionalResponse>`
  - [ ] `buscarPorId(Long id, Long empresaId)` → `ProfissionalResponse` ou 404 se outra empresa
  - [ ] `atualizar(Long id, Long empresaId, AtualizarProfissionalRequest)` → `ProfissionalResponse`
  - [ ] `inativar(Long id, Long empresaId)` → seta `profissional.ativo = false` **e** `usuario.ativo = false`
  - [ ] `reativar(Long id, Long empresaId)` → seta ambos como `true`

- [ ] **Task 6 — `ProfissionalController` (substituir stub da Story 2.2)** (AC-2–AC-7)
  - [ ] `@RestController @RequestMapping("/api/v1/profissionais") @PreAuthorize("hasRole('ADMIN_EMPRESA')")`
  - [ ] `POST /` → 201
  - [ ] `GET /` → 200 paginado
  - [ ] `GET /{id}` → 200 ou 404
  - [ ] `PUT /{id}` → 200
  - [ ] `PATCH /{id}/inativar` → 204
  - [ ] `PATCH /{id}/reativar` → 204
  - [ ] Usar `SecurityUtils.getEmpresaId()` em todos os métodos de serviço — nunca receber `empresaId` do request

- [ ] **Task 7 — Frontend — Tela de Gestão de Profissionais** (AC-2, AC-3)
  - [ ] Criar `src/pages/ProfissionaisPage.tsx` com listagem (tabela) + botão "Novo Profissional"
  - [ ] Formulário de criação: nome, email, especialidade, senha temporária (campo tipo `password` com toggle show/hide)
  - [ ] Após criação: modal/dialog exibindo `senhaTemporariaExibida` com botão copiar — orientar Admin a repassar ao profissional
  - [ ] Botão "Inativar"/"Reativar" por linha com confirmação

- [ ] **Task 8 — Testes** (AC-2, AC-5, AC-7)
  - [ ] Teste unitário `ProfissionalServiceTest`:
    - Criar: profissional + usuário criados, senha no response, hash no banco
    - Senha inválida: 400, nada criado
    - Email duplicado: 409, atomicidade mantida
    - Inativar: `profissional.ativo = false` + `usuario.ativo = false`
    - Isolamento: buscar id de outra empresa → 404
  - [ ] Teste de integração `ProfissionalControllerIT`:
    - POST → 201 como ADMIN_EMPRESA
    - GET filtrado: profissional de empresa B não aparece na listagem da empresa A
    - STAFF → 403 em qualquer endpoint

## Dev Notes

### ⚠️ Naming da migration: timestamp, NÃO sequencial (R6)

```sql
-- CORRETO — usar timestamp real do momento da implementação:
-- V202506041500__create_profissionais.sql

-- ERRADO — não usar:
-- V3__create_profissionais.sql
```

> O epics.md referencia como "V3 migration" apenas para facilitar leitura. O arquivo real **deve** usar o formato `V{yyyyMMddHHmm}__` conforme documentado na Story 1.2 (R6).

### Migration SQL

```sql
-- V202506041500__create_profissionais.sql (usar timestamp real)

CREATE TABLE profissionais (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT NOT NULL,
    empresa_id  BIGINT NOT NULL,
    especialidade VARCHAR(255),
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_profissionais_usuario_id UNIQUE (usuario_id),
    CONSTRAINT fk_profissionais_usuario  FOREIGN KEY (usuario_id)  REFERENCES usuarios(id),
    CONSTRAINT fk_profissionais_empresa  FOREIGN KEY (empresa_id)  REFERENCES empresas(id)
);

CREATE INDEX idx_profissionais_empresa_id ON profissionais (empresa_id);
```

### ⚠️ Inativação: sincronizar `profissional.ativo` com `usuario.ativo`

O `JwtAuthenticationFilter` (Story 2.1) verifica `usuario.ativo` para bloquear acesso. Logo, inativar o `Profissional` deve **também** setar `usuario.ativo = false`. O service deve atualizar ambas as entidades na mesma transação:

```java
@Transactional
public void inativar(Long profissionalId, Long empresaId) {
    Profissional p = profissionalRepository
        .findByIdAndEmpresaId(profissionalId, empresaId)
        .orElseThrow(RecursoNaoEncontradoException::new);
    p.setAtivo(false);
    p.getUsuario().setAtivo(false);   // bloqueia login imediatamente
    profissionalRepository.save(p);
    // usuario é salvo em cascata se configurado, ou salvar explicitamente:
    usuarioRepository.save(p.getUsuario());
}
```

### `ProfissionalService.criar()` — Fluxo Completo

```java
@Transactional
public CriarProfissionalResponse criar(CriarProfissionalRequest req, Long empresaId) {
    // 1. Validações
    PasswordValidator.validateOrThrow(req.getSenhaTemporaria());
    if (usuarioRepository.existsByEmail(req.getEmail())) {
        throw new EmailDuplicadoException("Email já cadastrado: " + req.getEmail());
    }

    // 2. Criar usuário
    Usuario usuario = new Usuario();
    usuario.setNome(req.getNome());
    usuario.setEmail(req.getEmail());
    usuario.setSenhaHash(passwordEncoder.encode(req.getSenhaTemporaria()));
    usuario.setPerfil(PerfilUsuario.PROFISSIONAL);
    usuario.setEmpresaId(empresaId);
    usuario.setAtivo(true);
    usuario.setSenhaTemporaria(true);
    usuario = usuarioRepository.save(usuario);

    // 3. Criar profissional
    Profissional profissional = new Profissional();
    profissional.setUsuario(usuario);
    profissional.setEmpresaId(empresaId);
    profissional.setEspecialidade(req.getEspecialidade());
    profissional.setAtivo(true);
    profissionalRepository.save(profissional);

    return new CriarProfissionalResponse(
        profissional.getId(),
        usuario.getNome(),
        usuario.getEmail(),
        profissional.getEspecialidade(),
        true,
        profissional.getCreatedAt(),
        req.getSenhaTemporaria()   // exibida uma vez
    );
}
```

### Isolamento Multi-Tenant — Regra Fundamental

Todos os métodos do `ProfissionalService` recebem `empresaId` de `SecurityUtils.getEmpresaId()` no controller — **nunca** do request body ou path param. Exemplo:

```java
// Controller — CORRETO:
@GetMapping
public Page<ProfissionalResponse> listar(
        @PageableDefault(size = 20) Pageable pageable) {
    Long empresaId = SecurityUtils.getEmpresaId();  // do JWT
    return service.listar(empresaId, pageable);
}

// Controller — ERRADO (nunca fazer):
@GetMapping
public Page<ProfissionalResponse> listar(
        @RequestParam Long empresaId,  // NUNCA!
        Pageable pageable) { ... }
```

### Modal de Senha Temporária (Frontend)

```tsx
// Após POST /api/v1/profissionais bem-sucedido:
{isCreated && (
  <Dialog open={isCreated} onOpenChange={() => setIsCreated(false)}>
    <DialogContent>
      <DialogHeader>
        <DialogTitle>Profissional criado com sucesso</DialogTitle>
        <DialogDescription>
          Copie a senha temporária e repasse ao profissional.
          Ela não será exibida novamente.
        </DialogDescription>
      </DialogHeader>
      <div className="flex items-center gap-2 p-3 bg-[var(--bg-subtle)] rounded-radius-md">
        <code className="flex-1 text-sm font-mono">{senhaExibida}</code>
        <Button variant="ghost" size="sm" onClick={() => navigator.clipboard.writeText(senhaExibida)}>
          Copiar
        </Button>
      </div>
      <DialogFooter>
        <Button onClick={() => setIsCreated(false)}>Entendido</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
)}
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__create_profissionais.sql` | NEW | Timestamp real |
| `domain/entities/Profissional.java` | NEW | JPA entity |
| `domain/repositories/ProfissionalRepository.java` | **UPDATE** | Substituir stub da Story 2.2 |
| `api/dtos/request/CriarProfissionalRequest.java` | NEW | |
| `api/dtos/request/AtualizarProfissionalRequest.java` | NEW | Sem campo email |
| `api/dtos/response/ProfissionalResponse.java` | NEW | |
| `api/dtos/response/CriarProfissionalResponse.java` | NEW | Com `senhaTemporariaExibida` |
| `domain/services/ProfissionalService.java` | NEW | `@Transactional`, inativação sincronizada |
| `api/controllers/ProfissionalController.java` | **UPDATE** | Substituir stub da Story 2.2 |
| `src/pages/ProfissionaisPage.tsx` | NEW | Listagem + formulário + modal de senha |
| `test/.../ProfissionalServiceTest.java` | NEW | Unitários |
| `test/.../ProfissionalControllerIT.java` | NEW | Integração: criação, isolamento, 403 |

### Referências

- [Source: epics.md#Story 2.4] — Acceptance Criteria completos e FR-007, FR-009, FR-046
- [Source: epics.md#Story 1.2 AC-R6] — Flyway timestamp naming obrigatório
- [Source: architecture.md#Regras de Processo] — empresaId do JWT, nunca do request
- [Source: architecture.md#Banco de Dados] — Índices nas FKs, constraints nomeadas
- [Source: 2-3-admin-sistema-gestao-clinicas.md] — `PasswordValidator` reutilizado aqui

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
