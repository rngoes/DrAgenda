# Story 2.3: Admin Sistema — Gestão de Clínicas e Criação de Admin Empresa

Status: ready-for-dev

## Story

Como Admin Sistema,
Quero cadastrar clínicas e criar o primeiro Admin Empresa automaticamente,
Para que cada nova clínica tenha acesso imediato ao sistema com isolamento total de dados.

## Acceptance Criteria

**AC-1 — Criar clínica com Admin Empresa simultâneo**
- Given o Admin Sistema envia `POST /api/v1/admin/empresas` com `{ "nomeClinica": "...", "adminNome": "...", "adminEmail": "...", "adminSenhaTemporaria": "..." }`
- When o backend processa
- Then a empresa é criada com `ativo = true`
- And o usuário Admin Empresa é criado com `perfil = ADMIN_EMPRESA`, `empresa_id` vinculado, `senha_temporaria = true` (FR-056)
- And `adminSenhaTemporaria` é validada: ≥ 8 chars, ao menos 1 maiúscula, 1 minúscula e 1 caractere especial — retorna HTTP 400 se inválida
- And a senha é armazenada com BCrypt (nunca em texto plano)
- And o response exibe a senha temporária **uma única vez** e retorna HTTP 201 com header `Location: /api/v1/admin/empresas/{id}`

**AC-2 — Listar clínicas com paginação**
- Given o Admin Sistema acessa `GET /api/v1/admin/empresas`
- When a listagem é retornada
- Then inclui todas as clínicas com envelope de paginação: `content`, `page`, `size`, `totalElements`, `totalPages`
- And page size padrão = 20
- And cada item exibe: `id`, `nome`, `ativo`, `createdAt`

**AC-3 — Editar nome da clínica**
- Given o Admin Sistema envia `PUT /api/v1/admin/empresas/{id}` com `{ "nome": "Novo Nome" }`
- When a operação é realizada
- Then os dados são atualizados e retorna HTTP 200

**AC-4 — Inativar clínica**
- Given o Admin Sistema envia `PATCH /api/v1/admin/empresas/{id}/inativar`
- When a operação é confirmada
- Then `empresa.ativo = false` (FR-051)
- And qualquer usuário dessa empresa recebe HTTP 401 na próxima requisição autenticada (o `JwtAuthenticationFilter` da Story 2.1 já verifica `empresa.ativo`)

**AC-5 — Reativar clínica**
- Given o Admin Sistema envia `PATCH /api/v1/admin/empresas/{id}/reativar`
- When a operação é realizada
- Then `empresa.ativo = true` e os usuários voltam a ter acesso normalmente

**AC-6 — Acesso restrito ao Admin Sistema**
- Given qualquer usuário não-ADMIN_SISTEMA tenta acessar `/api/v1/admin/**`
- When o `@PreAuthorize` avalia
- Then retorna HTTP 403

**AC-7 — Operação atômica (empresa + usuário)**
- Given a criação do Admin Empresa falha (ex: email duplicado) após a empresa ser criada
- When o backend processa
- Then a empresa também NÃO é criada (transação única — `@Transactional`)

## Tasks / Subtasks

- [ ] **Task 1 — `PasswordValidator` utilitário** (AC-1)
  - [ ] Criar `domain/services/PasswordValidator.java` com método estático `validate(String senha)`
  - [ ] Retorna lista de erros: `["Mínimo 8 caracteres", "Requer maiúscula", "Requer minúscula", "Requer caractere especial"]`
  - [ ] Regex para especial: `[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]`
  - [ ] Usado nesta story e reutilizado nas Stories 2.4, 2.5, 2.6

- [ ] **Task 2 — DTOs de request/response** (AC-1, AC-2, AC-3)
  - [ ] Criar `api/dtos/request/CriarEmpresaRequest.java`: `@NotBlank nomeClinica`, `@NotBlank adminNome`, `@Email @NotBlank adminEmail`, `@NotBlank adminSenhaTemporaria`
  - [ ] Criar `api/dtos/request/AtualizarEmpresaRequest.java`: `@NotBlank nome`
  - [ ] Criar `api/dtos/response/EmpresaResponse.java`: `id`, `nome`, `ativo`, `createdAt`
  - [ ] Criar `api/dtos/response/CriarEmpresaResponse.java`: `empresaId`, `nome`, `adminEmail`, `senhaTemporariaExibida` (campo presente apenas no response de criação)

- [ ] **Task 3 — `AdminEmpresaService`** (AC-1, AC-3, AC-4, AC-5, AC-7)
  - [ ] Criar `domain/services/AdminEmpresaService.java`
  - [ ] Método `criarEmpresaComAdmin(CriarEmpresaRequest)` anotado com `@Transactional`:
    - Validar senha via `PasswordValidator` → lançar `ValidationException` se inválida
    - Verificar email único (`usuarioRepository.existsByEmail(email)`) → HTTP 409 se duplicado
    - Criar e salvar `Empresa`
    - Criar e salvar `Usuario` com `BCryptPasswordEncoder.encode(senhaTemporaria)`, `perfil = ADMIN_EMPRESA`, `senhaTemporaria = true`
    - Retornar `CriarEmpresaResponse` com `senhaTemporariaExibida` = senha original (plain text exibido uma vez)
  - [ ] Método `listar(Pageable)` → `Page<EmpresaResponse>`
  - [ ] Método `atualizar(Long id, AtualizarEmpresaRequest)` → `EmpresaResponse`
  - [ ] Método `inativar(Long id)` → seta `ativo = false` e salva
  - [ ] Método `reativar(Long id)` → seta `ativo = true` e salva

- [ ] **Task 4 — `AdminEmpresaController` (substituir stub da Story 2.2)** (AC-1–AC-6)
  - [ ] Substituir stub criado na Story 2.2 com implementação completa
  - [ ] `@RestController @RequestMapping("/api/v1/admin") @PreAuthorize("hasRole('ADMIN_SISTEMA')")`
  - [ ] `POST /api/v1/admin/empresas` → 201 + Location header
  - [ ] `GET /api/v1/admin/empresas` → 200 paginado (aceita `?page=0&size=20`)
  - [ ] `PUT /api/v1/admin/empresas/{id}` → 200
  - [ ] `PATCH /api/v1/admin/empresas/{id}/inativar` → 204
  - [ ] `PATCH /api/v1/admin/empresas/{id}/reativar` → 204

- [ ] **Task 5 — `EmpresaRepository` atualizar** (AC-2)
  - [ ] Adicionar ao `EmpresaRepository.java` (stub da Story 1.2): `Page<Empresa> findAll(Pageable pageable)` (já fornecido por `JpaRepository`)
  - [ ] Sem alteração necessária — `JpaRepository` já provê `findAll(Pageable)`

- [ ] **Task 6 — Testes** (AC-1, AC-4, AC-6, AC-7)
  - [ ] Teste unitário `AdminEmpresaServiceTest`:
    - Criar empresa: empresa + admin criados, senha no response, hash no banco
    - Senha inválida: retorna erros de validação, nada criado no banco
    - Email duplicado: lança exceção, empresa não criada (atomicidade)
    - Inativar/reativar: `ativo` atualizado corretamente
  - [ ] Teste de integração `AdminEmpresaControllerIT`:
    - `POST /api/v1/admin/empresas` com credenciais de Admin Sistema → 201
    - `POST /api/v1/admin/empresas` com senha fraca → 400
    - `GET /api/v1/admin/empresas` com STAFF → 403
    - Inativar empresa → usuário da empresa recebe 401 em endpoint autenticado

## Dev Notes

### ⚠️ Sem nova migration nesta story

As tabelas `empresas` e `usuarios` já existem (migrations V1 e V2 da Story 1.2). Esta story apenas adiciona endpoints sobre o schema existente. Nenhum arquivo SQL novo.

### `PasswordValidator.java` — Implementação

```java
// domain/services/PasswordValidator.java
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE  = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE  = Pattern.compile("[a-z]");
    private static final Pattern SPECIAL    = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

    /** Retorna lista de mensagens de erro. Vazia = senha válida. */
    public static List<String> validate(String senha) {
        List<String> erros = new ArrayList<>();
        if (senha == null || senha.length() < MIN_LENGTH)
            erros.add("Mínimo " + MIN_LENGTH + " caracteres");
        if (senha != null && !UPPERCASE.matcher(senha).find())
            erros.add("Requer ao menos uma letra maiúscula");
        if (senha != null && !LOWERCASE.matcher(senha).find())
            erros.add("Requer ao menos uma letra minúscula");
        if (senha != null && !SPECIAL.matcher(senha).find())
            erros.add("Requer ao menos um caractere especial");
        return erros;
    }

    public static void validateOrThrow(String senha) {
        List<String> erros = validate(senha);
        if (!erros.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", erros));
        }
    }
}
```

> **Reutilização:** Este validador é usado em todas as stories que lidam com senhas (2.3, 2.4, 2.5, 2.6). Centraliza a regra em um único lugar.

### `AdminEmpresaService.java` — Método Principal

```java
@Service
@RequiredArgsConstructor
public class AdminEmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public CriarEmpresaResponse criarEmpresaComAdmin(CriarEmpresaRequest req) {
        // 1. Validar senha antes de qualquer persistência
        PasswordValidator.validateOrThrow(req.getAdminSenhaTemporaria());

        // 2. Verificar unicidade do email
        if (usuarioRepository.existsByEmail(req.getAdminEmail())) {
            throw new EmailDuplicadoException("Email já cadastrado: " + req.getAdminEmail());
        }

        // 3. Criar empresa
        Empresa empresa = new Empresa();
        empresa.setNome(req.getNomeClinica());
        empresa.setAtivo(true);
        empresa = empresaRepository.save(empresa);

        // 4. Criar Admin Empresa (dentro da mesma transação)
        Usuario admin = new Usuario();
        admin.setNome(req.getAdminNome());
        admin.setEmail(req.getAdminEmail());
        admin.setSenhaHash(passwordEncoder.encode(req.getAdminSenhaTemporaria()));
        admin.setPerfil(PerfilUsuario.ADMIN_EMPRESA);
        admin.setEmpresa(empresa);
        admin.setAtivo(true);
        admin.setSenhaTemporaria(true);
        usuarioRepository.save(admin);

        // 5. Retornar response com senha exibida uma vez
        return new CriarEmpresaResponse(
            empresa.getId(),
            empresa.getNome(),
            admin.getEmail(),
            req.getAdminSenhaTemporaria()   // plain text, exibido APENAS no response
        );
    }

    // ... listar, atualizar, inativar, reativar
}
```

### `AdminEmpresaController.java` — Endpoints

```java
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN_SISTEMA')")
@RequiredArgsConstructor
public class AdminEmpresaController {

    private final AdminEmpresaService service;

    @PostMapping("/empresas")
    public ResponseEntity<CriarEmpresaResponse> criar(
            @Valid @RequestBody CriarEmpresaRequest req) {
        CriarEmpresaResponse resp = service.criarEmpresaComAdmin(req);
        URI location = URI.create("/api/v1/admin/empresas/" + resp.getEmpresaId());
        return ResponseEntity.created(location).body(resp);
    }

    @GetMapping("/empresas")
    public Page<EmpresaResponse> listar(
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable) {
        return service.listar(pageable);
    }

    @PutMapping("/empresas/{id}")
    public EmpresaResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody AtualizarEmpresaRequest req) {
        return service.atualizar(id, req);
    }

    @PatchMapping("/empresas/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @PatchMapping("/empresas/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@PathVariable Long id) {
        service.reativar(id);
    }
}
```

### Tratamento de `EmailDuplicadoException` no `GlobalExceptionHandler`

```java
// Adicionar ao GlobalExceptionHandler.java (criado na Story 2.1):
@ExceptionHandler(EmailDuplicadoException.class)
public ProblemDetail handleEmailDuplicado(EmailDuplicadoException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Email já cadastrado");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    return pd;
}

@ExceptionHandler(IllegalArgumentException.class)
public ProblemDetail handleValidacao(IllegalArgumentException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Dados inválidos");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    return pd;
}
```

### Resposta de Criação — Senha Exibida Uma Vez

```json
POST /api/v1/admin/empresas
201 Created
Location: /api/v1/admin/empresas/42

{
  "empresaId": 42,
  "nome": "Clínica Bem Estar",
  "adminEmail": "andre@bemestar.com.br",
  "senhaTemporariaExibida": "Admin@2026!"
}
```

> **Segurança:** `senhaTemporariaExibida` existe APENAS no `CriarEmpresaResponse`. Após o response, a senha plain text não existe em nenhuma camada do sistema — apenas o BCrypt hash está no banco. O frontend deve exibir essa senha em um dialog copiável e orientar o Admin Sistema a repassá-la ao novo Admin Empresa.

### Paginação com Spring Data

```java
// Spring Data Page<T> serializa automaticamente para o envelope correto quando
// o controller retorna Page<T> diretamente:
{
  "content": [...],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 5,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

### Nota sobre Frontend — Admin Sistema

O EXPERIENCE.md e DESIGN.md não especificam UI para o Admin Sistema (apenas para Staff, Profissional e Admin Empresa). A sugestão de implementação mínima:
- Tela simples de listagem de clínicas em `src/pages/AdminEmpresasPage.tsx` (stub — não há UX spec)
- Formulário de criação exibindo a `senhaTemporariaExibida` em um campo readonly copiável
- Esta tela é acessível apenas internamente (Admin Sistema usa o sistema diretamente)
- **Não bloquear a story por falta de design do Admin Sistema** — backend é o entregável principal desta story

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `domain/services/PasswordValidator.java` | NEW | Reutilizado nas stories 2.4, 2.5, 2.6 |
| `api/dtos/request/CriarEmpresaRequest.java` | NEW | Bean Validation |
| `api/dtos/request/AtualizarEmpresaRequest.java` | NEW | |
| `api/dtos/response/EmpresaResponse.java` | NEW | |
| `api/dtos/response/CriarEmpresaResponse.java` | NEW | Inclui `senhaTemporariaExibida` |
| `domain/services/AdminEmpresaService.java` | NEW | `@Transactional` na criação |
| `domain/exceptions/EmailDuplicadoException.java` | NEW | Runtime exception |
| `api/controllers/AdminEmpresaController.java` | **UPDATE** | Substituir stub da Story 2.2 |
| `api/exceptions/GlobalExceptionHandler.java` | **UPDATE** | Adicionar handler para EmailDuplicado e IllegalArgument |
| `test/.../AdminEmpresaServiceTest.java` | NEW | Unitários: criação, senha fraca, email duplicado, atomicidade |
| `test/.../AdminEmpresaControllerIT.java` | NEW | Integração: POST 201, senha fraca 400, STAFF → 403 |

### Referências

- [Source: epics.md#Story 2.3] — Acceptance Criteria completos
- [Source: architecture.md#Autenticação e Segurança] — BCrypt, senha temporária rules
- [Source: architecture.md#API e Comunicação] — HTTP 201 + Location, RFC 7807 ProblemDetail
- [Source: architecture.md#Regras de Processo] — empresaId do JWT, nunca de request
- [Source: epics.md#FR-056] — criação simultânea empresa + Admin Empresa
- [Source: epics.md#FR-051] — inativar empresa → todos os usuários perdem acesso

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
