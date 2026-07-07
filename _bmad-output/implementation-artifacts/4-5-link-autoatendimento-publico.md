# Story 4.5: Link de Autoatendimento Público

Status: ready-for-dev

## Story

Como Staff ou Admin Empresa,
Quero gerar um link de cancelamento/confirmação para o paciente sem precisar que ele crie conta,
Para que o paciente gerencie seu agendamento de forma autônoma e a clínica reduza faltas.

## Acceptance Criteria

**AC-1 — Gerar link de cancelamento (FR-031)**
- Given Staff ou Admin acessa menu `···` → "Gerar link de cancelamento" de agendamento PENDENTE ou CONFIRMADO
- When `POST /api/v1/agendamentos/{id}/gerar-link` é chamado
- Then o backend gera `public_token = UUID.randomUUID().toString()` (UUID v4, 122 bits de entropia — R4)
- And persiste `public_token` no registro do agendamento
- And retorna `{ "link": "{APP_FRONTEND_URL}/cancelar/{token}" }`
- And o frontend auto-copia o link para o clipboard (`navigator.clipboard.writeText(link)`)
- And exibe toast `"Link copiado para área de transferência ✓"`

**AC-2 — Link inválido — HTTP 410 Gone**
- Given qualquer um acessa `GET /api/v1/cancelar/{token}`
- When o token não existe, foi invalidado (NULL no banco) ou o agendamento já passou (`horario_fim < agora`)
- Then retorna HTTP 410 Gone com body `{ "status": 410, "title": "Link expirado ou inválido" }`
- And a `AutoatendimentoPage.tsx` exibe estado de erro com mensagem "Este link expirou ou já foi utilizado."

**AC-3 — Carregar agendamento pelo token (público)**
- Given um paciente acessa `{APP_FRONTEND_URL}/cancelar/{token}`
- When `GET /api/v1/cancelar/{token}` retorna HTTP 200
- Then a `AutoatendimentoPage.tsx` exibe: nome do paciente (descriptografado), tipo de atendimento, profissional, data e hora (no fuso da clínica), status atual
- And o endpoint é **público** — sem `Authorization` header; configurado em `SecurityConfig.java` como `permitAll()`

**AC-4 — Confirmar presença pelo token**
- Given o agendamento está em status PENDENTE
- When o paciente toca `[Confirmar presença]` e `POST /api/v1/cancelar/{token}/confirmar` é chamado
- Then o status muda para CONFIRMADO
- And registra em `historico_status` com `usuario_id = null` e `justificativa = 'Confirmado pelo paciente via link'`
- And o token **NÃO é invalidado** (paciente pode confirmar e depois cancelar)
- And retorna HTTP 200 com o agendamento atualizado

**AC-5 — Cancelar pelo token**
- Given o agendamento está em status PENDENTE ou CONFIRMADO
- When o paciente toca `[Cancelar agendamento]` e `POST /api/v1/cancelar/{token}/cancelar` é chamado
- Then o status muda para CANCELADO
- And registra em `historico_status` com `usuario_id = null` e `justificativa = 'Cancelado pelo paciente via link'`
- And `public_token` é setado para NULL (token invalidado — não pode ser reutilizado)
- And retorna HTTP 200

**AC-6 — Tentativa de ação em status não permitido**
- Given o paciente acessa link de agendamento já CANCELADO ou CONCLUIDO
- When `GET /api/v1/cancelar/{token}` é chamado
- Then retorna HTTP 200 com dados do agendamento, mas `disponivel = false`
- And a `AutoatendimentoPage.tsx` exibe o status atual sem botões de ação ("Agendamento já cancelado" ou "Agendamento concluído")

**AC-7 — Invalidação do token em edição e NoShow (integração)**
- Given um link foi gerado para um agendamento
- When o agendamento é editado (Story 4.2/4.3) OU marcado como NOSHOW pelo job (Story 4.4)
- Then `public_token` é setado para NULL
- And o link antigo retorna HTTP 410 (validação já implementada em AC-2)

**AC-8 — Segurança: endpoint público isolado**
- Given o endpoint `/api/v1/cancelar/**` é configurado
- When `SecurityConfig.java` é atualizado
- Then apenas as rotas `/api/v1/cancelar/**` têm `permitAll()` — todas as outras rotas permanecem protegidas
- And o endpoint NÃO expõe dados de outros agendamentos — consulta sempre por `public_token` único

## Tasks / Subtasks

- [ ] **Task 1 — Campo `public_token` no `Agendamento`** (AC-1)
  - [ ] Verificar se `public_token` já existe na entidade `Agendamento.java` (Stories 4.2/4.3 referenciam o campo)
  - [ ] Se não existir: adicionar campo `String publicToken` com `@Column(unique = true, nullable = true)`
  - [ ] Criar migration `V{yyyyMMddHHmm}__add_public_token_agendamentos.sql` — APENAS se o campo não foi incluído na V8 (Story 4.1)
  > ⚠️ Verificar se V8 já inclui `public_token` antes de criar nova migration

- [ ] **Task 2 — `AgendamentoRepository` — busca por token** (AC-2, AC-3)
  - [ ] Adicionar ao `AgendamentoRepository.java`:
    ```java
    Optional<Agendamento> findByPublicToken(String publicToken);
    ```

- [ ] **Task 3 — `AutoatendimentoService.java`** (AC-2, AC-3, AC-4, AC-5, AC-6)
  - [ ] Criar `domain/services/AutoatendimentoService.java` com métodos:
    - `carregar(String token)` → `AutoatendimentoResponse` ou HTTP 410
    - `confirmar(String token)` → `AutoatendimentoResponse` ou HTTP 409/410
    - `cancelar(String token)` → `AutoatendimentoResponse` ou HTTP 409/410
  - [ ] Lógica de validação de token:
    ```java
    private Agendamento buscarPorToken(String token) {
        Agendamento ag = agendamentoRepository.findByPublicToken(token)
            .orElseThrow(() -> new LinkExpiradoException());
        if (ag.getHorarioFim().isBefore(Instant.now())) {
            throw new LinkExpiradoException();
        }
        return ag;
    }
    ```

- [ ] **Task 4 — `AgendamentoService.gerarLink()`** (AC-1, AC-7)
  - [ ] Adicionar ao `AgendamentoService.java`:
    ```java
    @Transactional
    public GerarLinkResponse gerarLink(Long id, Long empresaId) {
        Agendamento ag = agendamentoRepository.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new AgendamentoNotFoundException(id));
        if (ag.getStatus() == CANCELADO || ag.getStatus() == CONCLUIDO) {
            throw new TransicaoInvalidaException("Não é possível gerar link para status " + ag.getStatus());
        }
        String token = UUID.randomUUID().toString();  // R4: UUID v4 — 122 bits entropia
        ag.setPublicToken(token);
        agendamentoRepository.save(ag);
        String link = frontendBaseUrl + "/cancelar/" + token;
        return new GerarLinkResponse(link);
    }
    ```
  - [ ] Injetar `${app.frontend.base-url}` via `@Value`; configurar em `application.properties`

- [ ] **Task 5 — `AutoatendimentoController.java`** (AC-3, AC-4, AC-5, AC-8)
  - [ ] Criar `api/controllers/AutoatendimentoController.java`
  - [ ] Endpoints:
    - `GET /api/v1/cancelar/{token}` — público, sem `@PreAuthorize`
    - `POST /api/v1/cancelar/{token}/confirmar` — público
    - `POST /api/v1/cancelar/{token}/cancelar` — público
  - [ ] Adicionar ao `AgendamentoController.java`:
    - `POST /api/v1/agendamentos/{id}/gerar-link` → `@PreAuthorize("hasAnyRole('STAFF','ADMIN_EMPRESA')")`

- [ ] **Task 6 — `SecurityConfig.java` — rota pública** (AC-8)
  - [ ] Adicionar `.requestMatchers("/api/v1/cancelar/**").permitAll()` **antes** do `.anyRequest().authenticated()`
  - [ ] Garantir que apenas `/api/v1/cancelar/**` é público — nenhuma outra rota é afetada

- [ ] **Task 7 — Exception `LinkExpiradoException`** (AC-2)
  - [ ] Criar `domain/exceptions/LinkExpiradoException.java`
  - [ ] Adicionar ao `GlobalExceptionHandler.java`:
    ```java
    @ExceptionHandler(LinkExpiradoException.class)
    @ResponseStatus(HttpStatus.GONE)  // 410
    public ProblemDetail handleLinkExpirado(LinkExpiradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(410);
        pd.setTitle("Link expirado ou inválido");
        pd.setDetail(ex.getMessage());
        return pd;
    }
    ```

- [ ] **Task 8 — DTOs** (AC-1, AC-3)
  - [ ] `GerarLinkResponse.java`: `record GerarLinkResponse(String link) {}`
  - [ ] `AutoatendimentoResponse.java`: `agendamentoId`, `nomeCliente` (descriptografado), `tipoAtendimento`, `profissionalNome`, `horarioInicio`, `horarioFim`, `fusoHorario`, `status`, `disponivel` (boolean — false se terminal)

- [ ] **Task 9 — `AutoatendimentoPage.tsx`** (AC-2, AC-3, AC-4, AC-5, AC-6)
  - [ ] Atualizar o stub criado na Story 1.3 em `src/pages/AutoatendimentoPage.tsx`
  - [ ] Rota `/cancelar/:token` já deve estar em `AppRouter.tsx` (Story 1.3) — verificar e adicionar se ausente, **fora do `AppLayout`**
  - [ ] Estados da página:
    - `loading`: Skeleton
    - `erro 410`: `<AlertCircle>` + "Este link expirou ou já foi utilizado."
    - `disponivel = false`: exibir dados + status sem botões + mensagem contextual
    - `PENDENTE`: botões `[Confirmar presença]` e `[Cancelar agendamento]`
    - `CONFIRMADO`: apenas botão `[Cancelar agendamento]`
    - `após ação bem-sucedida`: recarregar dados e atualizar UI
  - [ ] Sem autenticação — não usa `useAuth`, não usa `axios` com interceptor de JWT; usa instância `axiosPublico` sem header `Authorization`

- [ ] **Task 10 — `axiosPublico` — instância sem autenticação** (AC-8, AC-9)
  - [ ] Criar `src/lib/axiosPublico.ts`:
    ```ts
    import axios from 'axios'
    const axiosPublico = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL })
    // SEM interceptor de Authorization
    export default axiosPublico
    ```

- [ ] **Task 11 — Testes** (AC-1, AC-2, AC-4, AC-5, AC-7)
  - [ ] Teste unitário `AutoatendimentoServiceTest`:
    - Token válido PENDENTE: `carregar()` retorna response com `disponivel = true`
    - Token expirado (horario_fim passado): `buscarPorToken()` lança `LinkExpiradoException`
    - Token nulo (não existe no banco): 410
    - `confirmar()` em PENDENTE: status → CONFIRMADO, historico criado com `usuarioId=null`
    - `cancelar()` em CONFIRMADO: status → CANCELADO, token invalidado
    - Tentativa de `confirmar()` em CANCELADO: `TransicaoInvalidaException` (409)
  - [ ] Teste de integração `AutoatendimentoControllerIT`:
    - GET token válido → 200 sem Authorization header
    - GET token inválido → 410
    - POST confirmar → 200, historico persiste
    - POST cancelar → 200, `public_token = null`
    - GET token após cancelar → 410

## Dev Notes

### ⚠️ Verificar migration V8 antes de criar nova (R6)

Antes de criar `V{yyyyMMddHHmm}__add_public_token_agendamentos.sql`, verificar se a Story 4.1 incluiu `public_token` na V8. Se já incluído: não criar nova migration.

```sql
-- Apenas se NÃO existir na V8:
-- V202506042200__add_public_token_agendamentos.sql
ALTER TABLE agendamentos ADD COLUMN public_token VARCHAR(36) NULL UNIQUE;
CREATE UNIQUE INDEX idx_agendamento_public_token ON agendamentos (public_token);
```

### Segurança — R4: UUID v4

```java
// CORRETO — UUID v4 com 122 bits de entropia aleatória
String token = UUID.randomUUID().toString();

// ERRADO — sequencial ou previsível
String token = String.valueOf(id);       // trivialmente previsível
String token = DigestUtils.md5(id+"");   // colisão + previsível
```

O UUID v4 tem 122 bits de entropia efetiva — impossível de forçar brute-force (2^122 combinações).

### `AutoatendimentoService` — Confirmar e Cancelar

```java
@Transactional
public AutoatendimentoResponse confirmar(String token) {
    Agendamento ag = buscarPorToken(token);  // lança 410 se inválido

    if (ag.getStatus() != PENDENTE) {
        throw new TransicaoInvalidaException(
            "Confirmação disponível apenas para agendamentos Pendentes. Status atual: " + ag.getStatus());
    }

    ag.setStatus(CONFIRMADO);

    HistoricoStatus h = new HistoricoStatus();
    h.setAgendamento(ag);
    h.setEmpresa(ag.getEmpresa());
    h.setStatusAnterior(PENDENTE);
    h.setStatusNovo(CONFIRMADO);
    h.setUsuarioId(null);   // ação do paciente via link público
    h.setJustificativa("Confirmado pelo paciente via link");
    historicoRepository.save(h);

    agendamentoRepository.save(ag);
    return toAutoatendimentoResponse(ag);
}

@Transactional
public AutoatendimentoResponse cancelar(String token) {
    Agendamento ag = buscarPorToken(token);

    if (ag.getStatus() != PENDENTE && ag.getStatus() != CONFIRMADO) {
        throw new TransicaoInvalidaException(
            "Cancelamento não disponível para status: " + ag.getStatus());
    }

    StatusAgendamento anterior = ag.getStatus();
    ag.setStatus(CANCELADO);
    ag.setPublicToken(null);  // link invalidado após cancelamento

    HistoricoStatus h = new HistoricoStatus();
    h.setAgendamento(ag);
    h.setEmpresa(ag.getEmpresa());
    h.setStatusAnterior(anterior);
    h.setStatusNovo(CANCELADO);
    h.setUsuarioId(null);
    h.setJustificativa("Cancelado pelo paciente via link");
    historicoRepository.save(h);

    agendamentoRepository.save(ag);
    return toAutoatendimentoResponse(ag);
}
```

### `SecurityConfig.java` — Adicionar rota pública

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/v1/cancelar/**").permitAll()   // ← ADICIONAR
    .anyRequest().authenticated()
)
```

> **Isolamento:** A rota `/api/v1/cancelar/**` é a ÚNICA rota pública além de `/auth/**`. O endpoint consulta apenas pelo `public_token` único — não aceita `empresaId` ou `agendamentoId` como parâmetro, prevenindo enumeração.

### `application.properties`

```properties
# URL do frontend (para geração do link)
app.frontend.base-url=${APP_FRONTEND_URL:http://localhost:5173}
```

### `AutoatendimentoPage.tsx` — Instância Axios Pública

```tsx
// src/pages/AutoatendimentoPage.tsx
import axiosPublico from '@/lib/axiosPublico'
import { useParams } from 'react-router-dom'

export function AutoatendimentoPage() {
  const { token } = useParams<{ token: string }>()

  const { data, isLoading, error } = useQuery({
    queryKey: ['autoatendimento', token],
    queryFn: () => axiosPublico.get(`/api/v1/cancelar/${token}`).then(r => r.data),
    retry: 0,  // não tentar novamente em 410
  })

  if (isLoading) return <SkeletonCard />

  if (error) {
    const status = (error as AxiosError)?.response?.status
    if (status === 410) {
      return <ErroLinkExpirado />
    }
    return <ErroPadrao />
  }

  return <AutoatendimentoConteudo dados={data} token={token!} />
}
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__add_public_token.sql` | NEW (se necessário) | Verificar V8 primeiro |
| `domain/services/AutoatendimentoService.java` | NEW | |
| `domain/exceptions/LinkExpiradoException.java` | NEW | HTTP 410 |
| `api/dtos/response/GerarLinkResponse.java` | NEW | |
| `api/dtos/response/AutoatendimentoResponse.java` | NEW | `disponivel` boolean |
| `api/controllers/AutoatendimentoController.java` | NEW | `permitAll()` |
| `api/controllers/AgendamentoController.java` | **UPDATE** | `POST /gerar-link` |
| `domain/services/AgendamentoService.java` | **UPDATE** | Adicionar `gerarLink()` |
| `domain/repositories/AgendamentoRepository.java` | **UPDATE** | `findByPublicToken()` |
| `api/exceptions/GlobalExceptionHandler.java` | **UPDATE** | `LinkExpiradoException` → 410 |
| `infrastructure/security/SecurityConfig.java` | **UPDATE** | `/api/v1/cancelar/**` permitAll |
| `application.properties` | **UPDATE** | `app.frontend.base-url` |
| `src/lib/axiosPublico.ts` | NEW | Sem interceptor JWT |
| `src/pages/AutoatendimentoPage.tsx` | **UPDATE** | Substituir stub por implementação |
| `src/AppRouter.tsx` | **UPDATE** (verificar) | Rota `/cancelar/:token` fora do AppLayout |
| `test/.../AutoatendimentoServiceTest.java` | NEW | |
| `test/.../AutoatendimentoControllerIT.java` | NEW | Sem Authorization header |

### Referências

- [Source: epics.md#Story 4.5] — Acceptance Criteria completos e FR-031, R4
- [Source: epics.md#R4] — UUID v4 via `UUID.randomUUID()` — 122 bits entropia
- [Source: 4-3-transicoes-status-historico.md] — `HistoricoStatus`, `usuario_id = null` para ações automáticas
- [Source: 4-4-noshow-automatico-proximos-disponiveis.md] — `public_token = null` já implementado no NoShowJob
- [Source: 1-3-frontend-design-tokens-routing.md] — `AutoatendimentoPage` stub criado, rota a verificar
- [Source: 2-1-login-jwt-logout.md] — `SecurityConfig.java` existente; adicionar rota pública

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
