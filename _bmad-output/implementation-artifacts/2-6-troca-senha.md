# Story 2.6: Troca de Senha Obrigatória, Redefinição e Alteração de Senha

Status: ready-for-dev

## Story

Como usuário do DrAgenda,
Quero poder trocar minha senha temporária, alterar minha senha atual e ter minha senha redefinida pelo Admin Empresa,
Para que minha conta seja segura e eu tenha controle sobre meu acesso.

## Acceptance Criteria

**AC-1 — Bloqueio de rota para senha temporária**
- Given um usuário com `senha_temporaria = true` faz login com sucesso
- When o frontend recebe o response de login (que inclui `senhaTemporaria: true` no payload JWT ou response body)
- Then redireciona obrigatoriamente para `/trocar-senha` independente da rota solicitada (FR-044)
- And todas as outras rotas protegidas redirecionam para `/trocar-senha` enquanto `senhaTemporaria = true`
- And o usuário NÃO pode navegar para `/agenda`, `/clientes` nem qualquer outra rota protegida sem antes trocar a senha

**AC-2 — Troca de senha temporária obrigatória**
- Given o usuário está na tela `/trocar-senha`
- When preenche senha atual correta + nova senha válida + confirmação da nova senha (devem ser iguais) e submete via `POST /api/v1/auth/trocar-senha`
- Then `senha_hash` é atualizado com BCrypt da nova senha
- And `senha_temporaria = false`
- And o frontend redireciona para `/agenda`
- And retorna HTTP 204

**AC-3 — Validação de complexidade de senha**
- Given o usuário submete nova senha que não atende os critérios (< 8 chars, sem maiúscula, sem minúscula ou sem especial)
- When o backend valida via `PasswordValidator`
- Then retorna HTTP 400 com mensagem indicando qual critério falhou (RFC 7807)
- And o frontend exibe o erro inline no campo de nova senha

**AC-4 — Senha atual incorreta**
- Given o usuário submete senha atual incorreta
- When o backend verifica com `BCryptPasswordEncoder.matches()`
- Then retorna HTTP 400 com mensagem "Senha atual incorreta"
- And nenhuma alteração é persistida

**AC-5 — Alteração voluntária de senha**
- Given um usuário autenticado com `senha_temporaria = false` acessa a tela de alteração
- When preenche senha atual correta + nova senha válida e confirma via `PUT /api/v1/auth/minha-senha` (FR-048)
- Then `senha_hash` é atualizado com novo hash BCrypt
- And retorna HTTP 204

**AC-6 — Redefinição de senha pelo Admin Empresa**
- Given o Admin Empresa envia `POST /api/v1/usuarios/{id}/redefinir-senha` com `{ "novaSenhaTemporaria": "..." }` (FR-043)
- When o backend processa
- Then verifica que o usuário alvo pertence à empresa do JWT — HTTP 403 se não pertencer
- And `novaSenhaTemporaria` validada via `PasswordValidator` → HTTP 400 se inválida
- And `senha_hash` atualizado com BCrypt e `senha_temporaria = true`
- And a nova senha temporária é retornada no response uma única vez (`senhaTemporariaExibida`)
- And o usuário alvo será forçado a trocar na próxima sessão

## Tasks / Subtasks

- [ ] **Task 1 — DTOs de request/response** (AC-2, AC-5, AC-6)
  - [ ] Criar `api/dtos/request/TrocarSenhaRequest.java`: `@NotBlank senhaAtual`, `@NotBlank novaSenha`, `@NotBlank confirmacaoNovaSenha`
  - [ ] Criar `api/dtos/request/AlterarSenhaRequest.java`: `@NotBlank senhaAtual`, `@NotBlank novaSenha`
  - [ ] Criar `api/dtos/request/RedefinirSenhaRequest.java`: `@NotBlank novaSenhaTemporaria`
  - [ ] Criar `api/dtos/response/RedefinirSenhaResponse.java`: `usuarioId`, `email`, `senhaTemporariaExibida`

- [ ] **Task 2 — `SenhaService`** (AC-2–AC-6)
  - [ ] Criar `domain/services/SenhaService.java`
  - [ ] `trocarSenhaTemporaria(Long usuarioId, TrocarSenhaRequest)` — `@Transactional`:
    - Buscar usuário por id, verificar `senhaAtual` com `BCryptPasswordEncoder.matches()`
    - Validar que `novaSenha == confirmacaoNovaSenha` → `IllegalArgumentException` se diferentes
    - Validar complexidade via `PasswordValidator.validateOrThrow()`
    - Salvar novo hash e `senhaTemporaria = false`
  - [ ] `alterarMinhaSenha(Long usuarioId, AlterarSenhaRequest)` — `@Transactional`:
    - Verificar senha atual, validar nova senha, salvar
  - [ ] `redefinirSenha(Long usuarioIdAlvo, Long empresaIdAdmin, RedefinirSenhaRequest)` — `@Transactional`:
    - Buscar usuário alvo verificando que `usuario.empresaId == empresaIdAdmin` → `AcessoNegadoException` se diferente
    - Validar `novaSenhaTemporaria` via `PasswordValidator`
    - Salvar hash BCrypt e `senhaTemporaria = true`
    - Retornar `RedefinirSenhaResponse` com senha exibida uma vez

- [ ] **Task 3 — Endpoints de senha no `AuthController`** (AC-2, AC-5)
  - [ ] Adicionar ao `AuthController.java` (criado na Story 2.1):
    - `POST /api/v1/auth/trocar-senha` — requer autenticação, qualquer perfil
    - `PUT /api/v1/auth/minha-senha` — requer autenticação, qualquer perfil
  - [ ] Verificar que ambos já estão na whitelist do `SecurityConfig` (Story 2.1 whitelistou `trocar-senha`)
  - [ ] `PUT /api/v1/auth/minha-senha` deve estar em rota autenticada (não whitelistada)

- [ ] **Task 4 — Endpoint de redefinição no `UsuarioController`** (AC-6)
  - [ ] Criar `api/controllers/UsuarioController.java` (novo, não havia stub)
  - [ ] `POST /api/v1/usuarios/{id}/redefinir-senha` com `@PreAuthorize("hasRole('ADMIN_EMPRESA')")`
  - [ ] Usar `SecurityUtils.getEmpresaId()` para verificar isolamento — nunca confiar em parâmetro de request

- [ ] **Task 5 — Frontend — Tela `/trocar-senha`** (AC-1, AC-2, AC-3, AC-4)
  - [ ] Criar `src/pages/TrocarSenhaPage.tsx` com React Hook Form + Zod
  - [ ] Schema Zod:
    ```ts
    z.object({
      senhaAtual: z.string().min(1, 'Obrigatório'),
      novaSenha: z.string().min(8, 'Mínimo 8 caracteres')
        .regex(/[A-Z]/, 'Requer maiúscula')
        .regex(/[a-z]/, 'Requer minúscula')
        .regex(/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>?]/, 'Requer caractere especial'),
      confirmacaoNovaSenha: z.string(),
    }).refine(data => data.novaSenha === data.confirmacaoNovaSenha, {
      message: 'As senhas não coincidem',
      path: ['confirmacaoNovaSenha'],
    })
    ```
  - [ ] Todos os campos tipo `password` com toggle show/hide
  - [ ] Exibir erros inline por campo
  - [ ] Após sucesso: limpar `senhaTemporaria` do estado de auth e navegar para `/agenda`

- [ ] **Task 6 — Bloqueio de rota no frontend** (AC-1)
  - [ ] Atualizar `AppLayout.tsx` (Story 2.2): se `senhaTemporaria === true`, renderizar `<Navigate to="/trocar-senha" replace />` antes do `<Outlet />`
  - [ ] Atualizar `useAuth.ts`: o campo `senhaTemporaria` deve ser lido do localStorage (salvo no login junto com o token)
  - [ ] Atualizar `AuthController` response de login (Story 2.1): incluir `senhaTemporaria: boolean` no response body para que o frontend possa armazenar

- [ ] **Task 7 — Tela de alteração voluntária `/menu` → "Alterar senha"** (AC-5)
  - [ ] Criar `src/pages/AlterarSenhaPage.tsx` — formulário simplificado: senha atual + nova senha (sem campo de confirmação separado, ou com confirmação — decisão do dev)
  - [ ] Rota `/alterar-senha` já existente no stub do App.tsx (ou adicionar)

- [ ] **Task 8 — Testes** (AC-2, AC-4, AC-6)
  - [ ] Teste unitário `SenhaServiceTest`:
    - Troca bem-sucedida: `senhaTemporaria = false`, novo hash no banco
    - Senha atual incorreta: HTTP 400, nenhuma alteração
    - Senhas não coincidem: HTTP 400
    - Nova senha fraca: HTTP 400
    - Redefinição admin: hash atualizado, `senhaTemporaria = true`
    - Redefinição cross-empresa: HTTP 403
  - [ ] Teste de integração `SenhaControllerIT`:
    - Fluxo completo: login com senha temporária → troca → login com nova senha → OK

## Dev Notes

### ⚠️ `senhaTemporaria` no Response de Login e no `useAuth`

Para que o frontend possa bloquear rotas, o campo `senhaTemporaria` deve estar no response do login **e** no estado do `useAuth`:

```java
// AuthController — response de login (atualizar LoginResponse da Story 2.1):
public record LoginResponse(String token, String perfil, boolean senhaTemporaria) {}

// AuthService.login() — incluir campo no response:
return new LoginResponse(token, usuario.getPerfil().name(), usuario.isSenhaTemporaria());
```

```ts
// useAuth.ts — ler senhaTemporaria do localStorage:
export function useAuth() {
  const token = localStorage.getItem('token')
  const perfil = localStorage.getItem('perfil') as Perfil | null
  const senhaTemporaria = localStorage.getItem('senhaTemporaria') === 'true'
  const isAuthenticated = !!token
  return { token, perfil, senhaTemporaria, isAuthenticated }
}

// axiosInstance.ts interceptor de login — salvar no localStorage:
localStorage.setItem('token', data.token)
localStorage.setItem('perfil', data.perfil)
localStorage.setItem('senhaTemporaria', String(data.senhaTemporaria))
```

### Bloqueio de Rota em `AppLayout.tsx`

```tsx
// Adicionar ANTES do return principal em AppLayout.tsx:
const { isAuthenticated, senhaTemporaria } = useAuth()

if (!isAuthenticated) return <Navigate to="/login" replace />
if (senhaTemporaria) return <Navigate to="/trocar-senha" replace />
// ...resto do render
```

> Desta forma, qualquer tentativa de navegar para `/agenda`, `/clientes` etc. enquanto `senhaTemporaria = true` redireciona automaticamente para `/trocar-senha`. A rota `/trocar-senha` deve estar FORA do `AppLayout` (rota pública autenticada ou rota separada).

### Estrutura de Rotas para `/trocar-senha`

```tsx
// App.tsx — /trocar-senha NÃO fica dentro do <Route element={<AppLayout/>}>:
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route path="/politica" element={<PoliticaPage />} />
  <Route path="/trocar-senha" element={<TrocarSenhaPage />} />  {/* sem AppLayout */}
  <Route path="/autoatendimento/:token" element={<AutoatendimentoPage />} />
  <Route element={<AppLayout />}>
    <Route path="/agenda" element={<AgendaPage />} />
    {/* ... outras rotas protegidas */}
  </Route>
</Routes>
```

> `/trocar-senha` precisa de autenticação (token válido) mas não de AppLayout (sem navbar/sidebar). `TrocarSenhaPage` deve redirecionar para `/login` se não houver token.

### `SenhaService` — Método de Troca

```java
@Service
@RequiredArgsConstructor
public class SenhaService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public void trocarSenhaTemporaria(Long usuarioId, TrocarSenhaRequest req) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(RecursoNaoEncontradoException::new);

        // Verificar senha atual
        if (!passwordEncoder.matches(req.getSenhaAtual(), usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }

        // Verificar confirmação
        if (!req.getNovaSenha().equals(req.getConfirmacaoNovaSenha())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        // Validar complexidade
        PasswordValidator.validateOrThrow(req.getNovaSenha());

        // Persistir
        usuario.setSenhaHash(passwordEncoder.encode(req.getNovaSenha()));
        usuario.setSenhaTemporaria(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public RedefinirSenhaResponse redefinirSenha(Long usuarioIdAlvo,
                                                  Long empresaIdAdmin,
                                                  RedefinirSenhaRequest req) {
        Usuario alvo = usuarioRepository.findById(usuarioIdAlvo)
            .orElseThrow(RecursoNaoEncontradoException::new);

        // Isolamento multi-tenant
        if (!alvo.getEmpresaId().equals(empresaIdAdmin)) {
            throw new AcessoNegadoException("Usuário não pertence à sua empresa");
        }

        PasswordValidator.validateOrThrow(req.getNovaSenhaTemporaria());

        alvo.setSenhaHash(passwordEncoder.encode(req.getNovaSenhaTemporaria()));
        alvo.setSenhaTemporaria(true);
        usuarioRepository.save(alvo);

        return new RedefinirSenhaResponse(
            alvo.getId(),
            alvo.getEmail(),
            req.getNovaSenhaTemporaria()   // plain text, exibido uma vez
        );
    }
}
```

### Atualização do `SecurityConfig` (verificar da Story 2.1)

```java
// Confirmar que /api/v1/auth/trocar-senha está na whitelist:
.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/auth/trocar-senha").authenticated()  // precisa de token mas não de senhaTemporaria=false
// /api/v1/auth/minha-senha deve ser .authenticated() (já coberto pelo padrão)
```

> Importante: `/trocar-senha` não deve ser `permitAll()` — requer token válido. Mas também não pode ser bloqueado pelo fato do `senhaTemporaria = true` (seria loop infinito). O filtro de bloqueio é apenas no **frontend** via `AppLayout`. O backend aceita o token válido em qualquer estado de `senhaTemporaria`.

### Limpeza do `senhaTemporaria` no localStorage após troca

```ts
// Após POST /api/v1/auth/trocar-senha retornar 204:
localStorage.setItem('senhaTemporaria', 'false')
navigate('/agenda', { replace: true })
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `api/dtos/request/TrocarSenhaRequest.java` | NEW | |
| `api/dtos/request/AlterarSenhaRequest.java` | NEW | |
| `api/dtos/request/RedefinirSenhaRequest.java` | NEW | |
| `api/dtos/response/RedefinirSenhaResponse.java` | NEW | Com `senhaTemporariaExibida` |
| `domain/services/SenhaService.java` | NEW | |
| `api/controllers/AuthController.java` | **UPDATE** | Adicionar `trocar-senha` + `minha-senha` + incluir `senhaTemporaria` no `LoginResponse` |
| `api/controllers/UsuarioController.java` | NEW | `POST /{id}/redefinir-senha` |
| `api/dtos/response/LoginResponse.java` | **UPDATE** | Adicionar campo `senhaTemporaria: boolean` |
| `src/pages/TrocarSenhaPage.tsx` | NEW | RHF + Zod + bloqueio de rota |
| `src/pages/AlterarSenhaPage.tsx` | NEW | Alteração voluntária |
| `src/shared/hooks/useAuth.ts` | **UPDATE** | Ler `senhaTemporaria` do localStorage |
| `src/shared/components/AppLayout.tsx` | **UPDATE** | Redirect para `/trocar-senha` se `senhaTemporaria = true` |
| `src/App.tsx` | **UPDATE** | Mover `/trocar-senha` para fora do `AppLayout` |
| `test/.../SenhaServiceTest.java` | NEW | Unitários |
| `test/.../SenhaControllerIT.java` | NEW | Fluxo completo login → troca → re-login |

### Referências

- [Source: epics.md#Story 2.6] — Acceptance Criteria completos e FR-043, FR-044, FR-048
- [Source: 2-1-login-jwt-logout.md] — `AuthController`, `LoginResponse`, `SecurityConfig` whitelist
- [Source: 2-2-multitenant-autorizacao-navegacao.md] — `AppLayout` a ser atualizado
- [Source: 2-3-admin-sistema-gestao-clinicas.md] — `PasswordValidator` reutilizado

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
