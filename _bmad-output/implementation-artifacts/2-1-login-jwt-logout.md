# Story 2.1: Login, JWT Multi-Tenant e Logout

Status: ready-for-dev

## Story

Como usuário do DrAgenda,
Quero fazer login com email e senha e receber um token que identifica minha empresa e meu perfil,
Para que eu possa acessar somente os dados da minha clínica com as permissões do meu perfil.

## Acceptance Criteria

**AC-1 — Schema pré-existente (Story 1.2)**
- Given as migrations V1 (empresas) e V2 (usuarios) foram aplicadas na Story 1.2
- When esta story é iniciada
- Then a tabela `usuarios` já existe com todas as colunas e índices corretos — nenhuma migration é criada aqui

**AC-2 — Endpoint de login**
- Given um usuário ativo envia `POST /api/v1/auth/login` com `{ "email": "...", "senha": "..." }`
- When o backend processa
- Then retorna HTTP 200 com `{ "token": "...", "perfil": "STAFF", "nome": "Maria", "empresaId": 1, "senhaTemporaria": false }`
- And o JWT contém claims: `sub` (userId como String), `empresaId` (Long, null para ADMIN_SISTEMA), `perfil` (String), `exp` (configurável via `JWT_EXPIRATION_MS`)
- And o token é assinado com HMAC-SHA256 usando `JWT_SECRET` via JJWT 0.12.x

**AC-3 — Credenciais inválidas**
- Given um usuário envia email inexistente ou senha errada
- When o backend processa
- Then retorna HTTP 401 com ProblemDetail `{ "title": "Credenciais inválidas", "status": 401 }`
- And a mensagem é IDÊNTICA para email inexistente e senha errada (proteção contra user enumeration)

**AC-4 — Usuário inativo**
- Given um usuário com `ativo = false` tenta fazer login
- When o backend processa
- Then retorna HTTP 401 com ProblemDetail `"Credenciais inválidas"` (mesma mensagem — não revela que a conta existe)

**AC-5 — Empresa inativa bloqueada no filtro JWT**
- Given um usuário de uma empresa com `ativo = false` tenta acessar qualquer endpoint autenticado
- When o `JwtAuthenticationFilter` valida o token
- Then retorna HTTP 401 (token válido, mas empresa inativa)

**AC-6 — Senha temporária bloqueada**
- Given um usuário autenticado com `senha_temporaria = true` acessa qualquer rota protegida exceto `POST /api/v1/auth/trocar-senha`
- When o middleware avalia o token
- Then retorna HTTP 403 com ProblemDetail `"Troca de senha obrigatória antes de prosseguir"` (FR-044)

**AC-7 — Logout no frontend**
- Given o usuário está logado e toca "Sair" no menu
- When o frontend processa o logout (FR-047)
- Then remove `token`, `perfil`, `nome`, `empresaId` do `localStorage`
- And redireciona para `/login`
- And nenhuma chamada ao backend é feita (JWT stateless no MVP)

**AC-8 — Tela de login funcional**
- Given o usuário acessa `/login`
- When preenche email e senha e submete
- Then o frontend chama `POST /api/v1/auth/login`
- And em sucesso: salva `token`, `perfil`, `nome`, `empresaId`, `senhaTemporaria` no `localStorage` e redireciona para `/agenda` (ou `/trocar-senha` se `senhaTemporaria = true`)
- And em erro 401: exibe mensagem "Email ou senha incorretos" abaixo do formulário
- And o link "Política de Privacidade" permanece visível no rodapé (implementado na Story 1.4)

**AC-9 — SecurityConfig atualizado**
- Given o backend está rodando
- When qualquer endpoint `/api/v1/auth/**` é acessado sem JWT
- Then é permitido (rota pública)
- And qualquer outro endpoint `/api/v1/**` exige JWT válido

## Tasks / Subtasks

- [ ] **Task 1 — Dependência JJWT no `pom.xml`** (AC-2)
  - [ ] Adicionar ao `pom.xml`:
    ```xml
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    ```

- [ ] **Task 2 — `JwtService`** (AC-2)
  - [ ] Criar `infrastructure/security/JwtService.java`
  - [ ] Método `gerarToken(Usuario usuario)` → String JWT assinado com HMAC-SHA256
  - [ ] Método `extrairClaims(String token)` → `Claims`
  - [ ] Método `extrairEmpresaId(String token)` → `Long` (null para ADMIN_SISTEMA)
  - [ ] Método `extrairPerfil(String token)` → `String`
  - [ ] Método `extrairUserId(String token)` → `Long`
  - [ ] Método `isTokenValido(String token)` → `boolean`
  - [ ] Configuração via `@Value("${jwt.secret}")` e `@Value("${jwt.expiration-ms}")`

- [ ] **Task 3 — `JwtAuthenticationFilter`** (AC-5, AC-6)
  - [ ] Criar `infrastructure/security/JwtAuthenticationFilter.java` extendendo `OncePerRequestFilter`
  - [ ] Extrair token do header `Authorization: Bearer {token}`
  - [ ] Validar token via `JwtService`
  - [ ] Verificar `empresa.ativo` — rejeitar com 401 se inativa (AC-5)
  - [ ] Verificar `usuario.ativo` — rejeitar com 401 se inativo
  - [ ] Criar `UsernamePasswordAuthenticationToken` com `empresaId` e `perfil` nos detalhes
  - [ ] Injetar no `SecurityContextHolder`
  - [ ] Verificar `senhaTemporaria = true` — lançar 403 para rotas que não sejam `/api/v1/auth/trocar-senha` (AC-6)

- [ ] **Task 4 — `SecurityConfig` atualizado** (AC-9)
  - [ ] Atualizar `SecurityConfig.java` (criado na Story 1.1, bean BCrypt adicionado na 1.2)
  - [ ] Configurar `SecurityFilterChain`:
    - `.csrf(csrf -> csrf.disable())`
    - `.sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))`
    - `.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**", "/api/v1/cancelar/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll().anyRequest().authenticated())`
    - `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`

- [ ] **Task 5 — `AuthController` e `AuthService`** (AC-2, AC-3, AC-4)
  - [ ] Criar `api/dtos/request/LoginRequest.java` com `email` e `senha` (validados com `@NotBlank`)
  - [ ] Criar `api/dtos/response/LoginResponse.java` com `token`, `perfil`, `nome`, `empresaId`, `senhaTemporaria`
  - [ ] Criar `domain/services/AuthService.java` com método `login(LoginRequest)`:
    - `usuarioRepository.findByEmail(email)` — se não existir → `"Credenciais inválidas"` (HTTP 401)
    - `!passwordEncoder.matches(senha, usuario.getSenhaHash())` → mesma mensagem genérica
    - `!usuario.isAtivo()` → mesma mensagem genérica (AC-4)
    - Sucesso → `jwtService.gerarToken(usuario)` → retornar `LoginResponse`
  - [ ] Criar `api/controllers/AuthController.java` com `POST /api/v1/auth/login`

- [ ] **Task 6 — `GlobalExceptionHandler`** (AC-3, AC-6)
  - [ ] Criar `api/exceptions/GlobalExceptionHandler.java` com `@RestControllerAdvice`
  - [ ] Tratar `AuthenticationException` → HTTP 401 com `ProblemDetail`
  - [ ] Tratar `AccessDeniedException` → HTTP 403 com `ProblemDetail`
  - [ ] Tratar `MethodArgumentNotValidException` → HTTP 400 com erros por campo
  - [ ] ProblemDetail com `type`, `title`, `status`, `detail`, `instance` (RFC 7807)

- [ ] **Task 7 — Tela de login no frontend** (AC-8)
  - [ ] Substituir stub `LoginPage.tsx` (Story 1.3) com implementação completa usando React Hook Form + Zod
  - [ ] Instalar: `npm install react-hook-form zod @hookform/resolvers`
  - [ ] Schema Zod: `email` (string email) + `senha` (string min 1)
  - [ ] Chamar `POST /api/v1/auth/login` via `axios` (não TanStack Query — mutation de auth)
  - [ ] Em sucesso: salvar no `localStorage` → `token`, `perfil`, `nome`, `empresaId`, `senhaTemporaria`
  - [ ] Redirecionar para `/trocar-senha` se `senhaTemporaria === true`, senão `/agenda`
  - [ ] Em erro 401: exibir `"Email ou senha incorretos"` abaixo do botão
  - [ ] Manter link `/politica` no rodapé (já implementado na Story 1.4)

- [ ] **Task 8 — Botão Sair e `useAuth` atualizado** (AC-7)
  - [ ] Criar `src/shared/hooks/useLogout.ts` que limpa localStorage e redireciona para `/login`
  - [ ] Atualizar `MenuPage.tsx` (stub) para incluir botão "Sair" que chama `useLogout`
  - [ ] Verificar que `useAuth.ts` (Story 1.3) lê corretamente todos os campos do localStorage

- [ ] **Task 9 — Testes backend** (AC-2, AC-3, AC-4, AC-5)
  - [ ] Teste unitário `AuthServiceTest`: login válido, email inválido, senha inválida, conta inativa — todos retornam resposta correta sem vazar informação
  - [ ] Teste unitário `JwtServiceTest`: gerar token, extrair claims, token expirado
  - [ ] Teste de integração `AuthControllerIT`: `POST /api/v1/auth/login` com credenciais válidas e inválidas
  - [ ] Teste de integração: endpoint autenticado sem token → 401; com token inválido → 401; com token válido → 200

## Dev Notes

### ⚠️ JJWT 0.12.x — API mudou significativamente vs versões anteriores

JJWT 0.12.x (lançado em 2023) tem API completamente diferente das versões 0.9.x/0.11.x encontradas na maioria dos tutoriais. **Usar apenas a API nova:**

```java
// ✅ CORRETO — JJWT 0.12.x
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

// Gerar chave a partir do secret
SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

// Gerar token
String token = Jwts.builder()
    .subject(String.valueOf(usuario.getId()))
    .claim("empresaId", usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null)
    .claim("perfil", usuario.getPerfil().name())
    .claim("nome", usuario.getNome())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + expirationMs))
    .signWith(key)
    .compact();

// Extrair claims
Claims claims = Jwts.parser()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

```java
// ❌ ERRADO — API 0.9.x (não usar)
Jwts.parser().setSigningKey(secret).parseClaimsJws(token)  // deprecated
Jwts.builder().setSubject(...)                              // deprecated
```

### `JwtService.java` — Implementação Completa

```java
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String gerarToken(Usuario usuario) {
        return Jwts.builder()
            .subject(String.valueOf(usuario.getId()))
            .claim("empresaId", usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null)
            .claim("perfil", usuario.getPerfil().name())
            .claim("nome", usuario.getNome())
            .claim("senhaTemporaria", usuario.isSenhaTemporaria())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Long extrairEmpresaId(String token) {
        Object val = extrairClaims(token).get("empresaId");
        return val != null ? ((Number) val).longValue() : null;
    }

    public String extrairPerfil(String token) {
        return extrairClaims(token).get("perfil", String.class);
    }

    public Long extrairUserId(String token) {
        return Long.valueOf(extrairClaims(token).getSubject());
    }

    public boolean isTokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### `JwtAuthenticationFilter.java` — Estrutura

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValido(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Long userId = jwtService.extrairUserId(token);
        Long empresaId = jwtService.extrairEmpresaId(token);
        String perfil = jwtService.extrairPerfil(token);

        // Verificar empresa ativa (se não for ADMIN_SISTEMA)
        if (empresaId != null) {
            Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
            if (empresa == null || !empresa.isAtivo()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // Verificar usuário ativo
        Usuario usuario = usuarioRepository.findById(userId).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Verificar senha temporária — bloquear todas as rotas exceto trocar-senha
        if (usuario.isSenhaTemporaria()
                && !request.getRequestURI().equals("/api/v1/auth/trocar-senha")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                """
                {"title":"Troca de senha obrigatória antes de prosseguir","status":403}
                """
            );
            return;
        }

        // Criar contexto de autenticação com empresaId nos details
        var auth = new UsernamePasswordAuthenticationToken(
            userId, null,
            List.of(new SimpleGrantedAuthority("ROLE_" + perfil))
        );
        auth.setDetails(Map.of("empresaId", empresaId, "perfil", perfil));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
```

### Extraindo `empresaId` nos Serviços

Para que os serviços acessem o `empresaId` sem parâmetros de request, criar um helper:

```java
// infrastructure/security/SecurityUtils.java
@Component
public class SecurityUtils {

    @SuppressWarnings("unchecked")
    public static Long getEmpresaId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        var details = (Map<String, Object>) auth.getDetails();
        Object val = details.get("empresaId");
        return val != null ? ((Number) val).longValue() : null;
    }

    public static Long getUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
```

> **Regra arquitetural inviolável (R1):** Todo `Repository` multi-tenant DEVE chamar `SecurityUtils.getEmpresaId()` internamente — nunca aceitar `empresaId` como parâmetro de request. Este `SecurityUtils` é o único ponto de entrada. Qualquer query que omita o filtro por `empresaId` é um bug de segurança.

### `LoginPage.tsx` — Implementação Completa

```tsx
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import api from '../shared/lib/axios'

const schema = z.object({
  email: z.string().email('Email inválido'),
  senha: z.string().min(1, 'Informe a senha'),
})
type LoginForm = z.infer<typeof schema>

export default function LoginPage() {
  const navigate = useNavigate()
  const [erro, setErro] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: LoginForm) => {
    setErro(null)
    try {
      const res = await api.post('/api/v1/auth/login', {
        email: data.email,
        senha: data.senha,
      })
      const { token, perfil, nome, empresaId, senhaTemporaria } = res.data
      localStorage.setItem('token', token)
      localStorage.setItem('perfil', perfil)
      localStorage.setItem('nome', nome)
      localStorage.setItem('empresaId', String(empresaId ?? ''))
      localStorage.setItem('senhaTemporaria', String(senhaTemporaria))
      navigate(senhaTemporaria ? '/trocar-senha' : '/agenda', { replace: true })
    } catch (e: any) {
      if (e.response?.status === 401) {
        setErro('Email ou senha incorretos')
      } else {
        setErro('Erro ao conectar ao servidor. Tente novamente.')
      }
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[var(--bg-base)] px-4">
      <div className="w-full max-w-sm">
        {/* Wordmark */}
        <h1 className="font-brand font-medium text-3xl text-center mb-8">
          <span className="font-light text-[var(--text-primary)]">Dr</span>
          <span className="text-brand-500">Agenda</span>
        </h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-[var(--text-primary)] mb-1">
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              aria-required="true"
              className="w-full rounded-radius-md border border-[var(--border)] bg-[var(--bg-base)] px-3 py-2 text-base text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-brand-500"
              {...register('email')}
            />
            {errors.email && (
              <p className="mt-1 text-xs text-error">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="senha" className="block text-sm font-medium text-[var(--text-primary)] mb-1">
              Senha
            </label>
            <input
              id="senha"
              type="password"
              autoComplete="current-password"
              aria-required="true"
              className="w-full rounded-radius-md border border-[var(--border)] bg-[var(--bg-base)] px-3 py-2 text-base text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-brand-500"
              {...register('senha')}
            />
            {errors.senha && (
              <p className="mt-1 text-xs text-error">{errors.senha.message}</p>
            )}
          </div>

          {erro && (
            <p className="text-sm text-error text-center" role="alert">{erro}</p>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-brand-500 hover:bg-brand-400 text-white font-medium py-2 rounded-radius-md transition-colors disabled:opacity-50"
          >
            {isSubmitting ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </div>

      <footer className="mt-8 text-center">
        <a href="/politica" className="text-sm text-[var(--text-secondary)] underline">
          Política de Privacidade
        </a>
      </footer>
    </div>
  )
}
```

### `useLogout.ts`

```typescript
import { useNavigate } from 'react-router-dom'

export function useLogout() {
  const navigate = useNavigate()

  return () => {
    localStorage.removeItem('token')
    localStorage.removeItem('perfil')
    localStorage.removeItem('nome')
    localStorage.removeItem('empresaId')
    localStorage.removeItem('senhaTemporaria')
    navigate('/login', { replace: true })
  }
}
```

### JWT_SECRET — Formato Obrigatório

O `JWT_SECRET` deve ser uma string Base64 com entropia ≥ 256 bits. Gerar com:
```bash
openssl rand -base64 32
```
O `JwtService` usa `Decoders.BASE64.decode(jwtSecret)` — a chave deve ser Base64 válida.

### `application.yml` — Configuração JWT

```yaml
jwt:
  secret: ${JWT_SECRET}              # sem default — falha se ausente
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}  # 24h default
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `backend/pom.xml` | **UPDATE** | Adicionar 3 dependências JJWT 0.12.6 |
| `infrastructure/security/JwtService.java` | NEW | Gerar/validar/extrair claims |
| `infrastructure/security/JwtAuthenticationFilter.java` | NEW | Filtro OncePerRequestFilter |
| `infrastructure/security/SecurityUtils.java` | NEW | Helper para extrair empresaId/userId do contexto |
| `infrastructure/config/SecurityConfig.java` | **UPDATE** | Adicionar filtro JWT ao SecurityFilterChain |
| `api/dtos/request/LoginRequest.java` | NEW | `@NotBlank email`, `@NotBlank senha` |
| `api/dtos/response/LoginResponse.java` | NEW | token, perfil, nome, empresaId, senhaTemporaria |
| `domain/services/AuthService.java` | NEW | Lógica de login com mensagem genérica |
| `api/controllers/AuthController.java` | NEW | `POST /api/v1/auth/login` |
| `api/exceptions/GlobalExceptionHandler.java` | NEW | RFC 7807 ProblemDetail |
| `test/.../AuthServiceTest.java` | NEW | Unitários: login, credenciais inválidas, conta inativa |
| `test/.../JwtServiceTest.java` | NEW | Unitários: gerar, extrair, token expirado |
| `test/.../AuthControllerIT.java` | NEW | Integração: login HTTP, token inválido |
| `frontend/src/pages/LoginPage.tsx` | **UPDATE** | Substituir stub — form completo com RHF+Zod |
| `frontend/src/shared/hooks/useLogout.ts` | NEW | Limpa localStorage + navega para /login |
| `frontend/src/pages/MenuPage.tsx` | **UPDATE** | Adicionar botão Sair usando useLogout |
| `frontend/package.json` | **UPDATE** | Adicionar react-hook-form, zod, @hookform/resolvers |

### Referências

- [Source: epics.md#Story 2.1] — Acceptance Criteria completos
- [Source: architecture.md#Autenticação e Segurança] — JJWT 0.12.x, multi-tenancy via empresaId
- [Source: architecture.md#Regras de Processo] — empresaId nunca de parâmetro de request
- [Source: architecture.md#API e Comunicação] — RFC 7807 ProblemDetail, status HTTP canônicos
- [Source: architecture.md#Nomenclatura Java] — PascalCase classes, camelCase campos
- [Source: architecture.md#Estratégia de Testes] — JUnit 5 + Mockito (80% services), MockMvc (controllers)
- [Source: epics.md#R1] — Teste de isolamento multi-tenant obrigatório (implementado em Story 2.2)

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
