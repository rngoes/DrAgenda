# Story 2.5: Admin Empresa — Cadastro e Gestão de Usuários Staff

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero cadastrar e gerenciar os usuários de recepção (Staff) da minha clínica,
Para que eles possam operar o sistema de agendamento.

## Acceptance Criteria

**AC-1 — Criar usuário Staff**
- Given o Admin Empresa envia `POST /api/v1/staff` com `{ "nome": "...", "email": "...", "senhaTemporaria": "..." }`
- When o backend processa
- Then cria `usuarios` com `perfil = STAFF`, `empresa_id` do JWT, `senha_temporaria = true`, `ativo = true` (FR-010)
- And `senhaTemporaria` validada via `PasswordValidator` (≥ 8 chars, maiúscula, minúscula, especial) → HTTP 400 se inválida (FR-007)
- And email único verificado → HTTP 409 se duplicado
- And a senha é armazenada com BCrypt (nunca em texto plano)
- And retorna HTTP 201 com `senhaTemporariaExibida` no response (exibida uma única vez)

**AC-2 — Listar Staff da empresa**
- Given o Admin Empresa acessa `GET /api/v1/staff`
- When a listagem é retornada
- Then exibe apenas usuários com `perfil = STAFF` da empresa do JWT
- And suporta paginação server-side com envelope `content`, `totalElements`, `totalPages`, `page`, `size`
- And page size padrão = 20

**AC-3 — Inativar Staff**
- Given o Admin Empresa envia `PATCH /api/v1/staff/{id}/inativar`
- When a operação é realizada
- Then `usuario.ativo = false`
- And o Staff perde acesso imediatamente na próxima requisição autenticada (verificado pelo `JwtAuthenticationFilter` da Story 2.1)

**AC-4 — Reativar Staff**
- Given o Admin Empresa envia `PATCH /api/v1/staff/{id}/reativar`
- When a operação é realizada
- Then `usuario.ativo = true` e o Staff volta a ter acesso

**AC-5 — Isolamento multi-tenant**
- Given o Admin Empresa tenta acessar Staff de outra empresa via path param
- When o backend verifica o `empresaId` do JWT contra o `empresa_id` do usuário alvo
- Then retorna HTTP 404 (não expõe existência do recurso de outra empresa)

## Tasks / Subtasks

- [ ] **Task 1 — DTOs** (AC-1, AC-2)
  - [ ] Criar `api/dtos/request/CriarStaffRequest.java`: `@NotBlank nome`, `@Email @NotBlank email`, `@NotBlank senhaTemporaria`
  - [ ] Criar `api/dtos/response/StaffResponse.java`: `id`, `nome`, `email`, `ativo`, `createdAt`
  - [ ] Criar `api/dtos/response/CriarStaffResponse.java`: campos do `StaffResponse` + `senhaTemporariaExibida`

- [ ] **Task 2 — `StaffService`** (AC-1–AC-5)
  - [ ] Criar `domain/services/StaffService.java`
  - [ ] `criar(CriarStaffRequest req, Long empresaId)` — `@Transactional`:
    - Validar senha via `PasswordValidator.validateOrThrow()`
    - Verificar email único (`usuarioRepository.existsByEmail()`) → `EmailDuplicadoException` se duplicado
    - Criar `Usuario` com `perfil = STAFF`, `empresa_id`, `senha_temporaria = true`, hash BCrypt
    - Retornar `CriarStaffResponse` com `senhaTemporariaExibida` = senha original (plain text, exibido uma vez)
  - [ ] `listar(Long empresaId, Pageable)` → usa `usuarioRepository.findAllByEmpresaIdAndPerfil(empresaId, STAFF, pageable)`
  - [ ] `buscarPorId(Long id, Long empresaId)` → `StaffResponse` ou 404 se outra empresa
  - [ ] `inativar(Long id, Long empresaId)` → seta `usuario.ativo = false`
  - [ ] `reativar(Long id, Long empresaId)` → seta `usuario.ativo = true`

- [ ] **Task 3 — `UsuarioRepository` — métodos adicionais** (AC-2, AC-5)
  - [ ] Verificar que `UsuarioRepository` já possui (da Story 2.2):
    `Page<Usuario> findAllByEmpresaIdAndPerfil(Long empresaId, PerfilUsuario perfil, Pageable pageable)`
  - [ ] Adicionar se ausente: `Optional<Usuario> findByIdAndEmpresaIdAndPerfil(Long id, Long empresaId, PerfilUsuario perfil)` — garante isolamento sem expor recursos de outras empresas

- [ ] **Task 4 — `StaffController` (substituir stub da Story 2.2)** (AC-1–AC-5)
  - [ ] `@RestController @RequestMapping("/api/v1/staff") @PreAuthorize("hasRole('ADMIN_EMPRESA')")`
  - [ ] `POST /` → 201
  - [ ] `GET /` → 200 paginado
  - [ ] `GET /{id}` → 200 ou 404
  - [ ] `PATCH /{id}/inativar` → 204
  - [ ] `PATCH /{id}/reativar` → 204
  - [ ] Usar `SecurityUtils.getEmpresaId()` em todos os métodos — nunca receber `empresaId` do request

- [ ] **Task 5 — Frontend — Tela de Gestão de Staff** (AC-1, AC-2)
  - [ ] Criar `src/pages/StaffPage.tsx` com listagem (tabela) + botão "Novo Staff"
  - [ ] Formulário de criação: nome, email, senha temporária (campo tipo `password` com toggle show/hide)
  - [ ] Após criação: modal/dialog exibindo `senhaTemporariaExibida` com botão copiar (mesmo padrão da Story 2.4)
  - [ ] Botão "Inativar"/"Reativar" por linha com confirmação

- [ ] **Task 6 — Testes** (AC-1, AC-3, AC-5)
  - [ ] Teste unitário `StaffServiceTest`:
    - Criar: usuário STAFF criado, senha no response, hash no banco
    - Senha inválida: 400, nada criado
    - Email duplicado: 409, atomicidade
    - Inativar: `ativo = false`; reativar: `ativo = true`
    - Isolamento: buscar id de outra empresa → 404
  - [ ] Teste de integração `StaffControllerIT`:
    - POST → 201 como ADMIN_EMPRESA
    - GET: staff de empresa B não aparece na listagem da empresa A
    - PROFISSIONAL → 403 em qualquer endpoint

## Dev Notes

### ⚠️ Sem nova migration nesta story

Toda a persistência usa a tabela `usuarios` já existente (migrations V1+V2 da Story 1.2). Apenas o `perfil` diferencia Staff de Profissional.

### `StaffService.criar()` — Fluxo

```java
@Service
@RequiredArgsConstructor
public class StaffService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public CriarStaffResponse criar(CriarStaffRequest req, Long empresaId) {
        // 1. Validações
        PasswordValidator.validateOrThrow(req.getSenhaTemporaria());
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new EmailDuplicadoException("Email já cadastrado: " + req.getEmail());
        }

        // 2. Criar usuário STAFF
        Usuario usuario = new Usuario();
        usuario.setNome(req.getNome());
        usuario.setEmail(req.getEmail());
        usuario.setSenhaHash(passwordEncoder.encode(req.getSenhaTemporaria()));
        usuario.setPerfil(PerfilUsuario.STAFF);
        usuario.setEmpresaId(empresaId);
        usuario.setAtivo(true);
        usuario.setSenhaTemporaria(true);
        usuario = usuarioRepository.save(usuario);

        return new CriarStaffResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            true,
            usuario.getCreatedAt(),
            req.getSenhaTemporaria()   // plain text, exibido APENAS no response
        );
    }

    public Page<StaffResponse> listar(Long empresaId, Pageable pageable) {
        return usuarioRepository
            .findAllByEmpresaIdAndPerfil(empresaId, PerfilUsuario.STAFF, pageable)
            .map(u -> new StaffResponse(u.getId(), u.getNome(), u.getEmail(),
                                        u.isAtivo(), u.getCreatedAt()));
    }

    @Transactional
    public void inativar(Long id, Long empresaId) {
        Usuario u = usuarioRepository
            .findByIdAndEmpresaIdAndPerfil(id, empresaId, PerfilUsuario.STAFF)
            .orElseThrow(RecursoNaoEncontradoException::new);
        u.setAtivo(false);
        usuarioRepository.save(u);
    }

    @Transactional
    public void reativar(Long id, Long empresaId) {
        Usuario u = usuarioRepository
            .findByIdAndEmpresaIdAndPerfil(id, empresaId, PerfilUsuario.STAFF)
            .orElseThrow(RecursoNaoEncontradoException::new);
        u.setAtivo(true);
        usuarioRepository.save(u);
    }
}
```

### `StaffController.java`

```java
@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService service;

    @PostMapping
    public ResponseEntity<CriarStaffResponse> criar(@Valid @RequestBody CriarStaffRequest req) {
        Long empresaId = SecurityUtils.getEmpresaId();
        CriarStaffResponse resp = service.criar(req, empresaId);
        URI location = URI.create("/api/v1/staff/" + resp.getId());
        return ResponseEntity.created(location).body(resp);
    }

    @GetMapping
    public Page<StaffResponse> listar(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.listar(SecurityUtils.getEmpresaId(), pageable);
    }

    @GetMapping("/{id}")
    public StaffResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id, SecurityUtils.getEmpresaId());
    }

    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id, SecurityUtils.getEmpresaId());
    }

    @PatchMapping("/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@PathVariable Long id) {
        service.reativar(id, SecurityUtils.getEmpresaId());
    }
}
```

### Comparativo Stories 2.4 vs 2.5

| Aspecto | Story 2.4 (Profissionais) | Story 2.5 (Staff) |
|---|---|---|
| Tabelas afetadas | `usuarios` + `profissionais` | Apenas `usuarios` |
| Nova migration | Sim (V3) | Não |
| Perfil criado | `PROFISSIONAL` | `STAFF` |
| Campo extra | `especialidade` | — |
| Inativação | Sincroniza `profissional.ativo` + `usuario.ativo` | Apenas `usuario.ativo` |
| Rota base | `/api/v1/profissionais` | `/api/v1/staff` |
| Repository query | `ProfissionalRepository.findAllByEmpresaId()` | `UsuarioRepository.findAllByEmpresaIdAndPerfil(..., STAFF, ...)` |

> A maior simplificação da Story 2.5 em relação à 2.4: não há entidade separada `Staff` — tudo vive na tabela `usuarios`. O `perfil = STAFF` é o único discriminador.

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `api/dtos/request/CriarStaffRequest.java` | NEW | |
| `api/dtos/response/StaffResponse.java` | NEW | |
| `api/dtos/response/CriarStaffResponse.java` | NEW | Com `senhaTemporariaExibida` |
| `domain/services/StaffService.java` | NEW | Simples — só `usuarios` |
| `domain/repositories/UsuarioRepository.java` | **UPDATE** | Adicionar `findByIdAndEmpresaIdAndPerfil` |
| `api/controllers/StaffController.java` | **UPDATE** | Substituir stub da Story 2.2 |
| `src/pages/StaffPage.tsx` | NEW | Listagem + formulário + modal de senha |
| `test/.../StaffServiceTest.java` | NEW | Unitários |
| `test/.../StaffControllerIT.java` | NEW | Integração |

### Referências

- [Source: epics.md#Story 2.5] — Acceptance Criteria completos e FR-007, FR-010
- [Source: architecture.md#Regras de Processo] — empresaId do JWT, nunca do request
- [Source: 2-3-admin-sistema-gestao-clinicas.md] — `PasswordValidator` e `EmailDuplicadoException` reutilizados
- [Source: 2-4-admin-empresa-profissionais.md] — padrão de modal de senha temporária no frontend

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
