---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments:
  - "_bmad-output/planning-artifacts/prds/prd-DrAgenda-2026-06-03/prd.md"
  - "_bmad-output/planning-artifacts/ux-designs/ux-DrAgenda-2026-06-04/DESIGN.md"
  - "_bmad-output/planning-artifacts/ux-designs/ux-DrAgenda-2026-06-04/EXPERIENCE.md"
  - "docs/brief.md"
workflowType: 'architecture'
lastStep: 8
status: 'complete'
completedAt: '2026-06-04'
project_name: 'DrAgenda'
user_name: 'Rodrigo Navarro'
date: '2026-06-04'
---

# Architecture Decision Document

_Este documento é construído colaborativamente através de descoberta passo a passo. Seções são adicionadas conforme trabalhamos cada decisão arquitetural juntos._

---

## Análise de Contexto do Projeto

**Projeto:** DrAgenda — Sistema de agendamento para consultórios médicos e odontológicos de pequeno porte.

**Escala:** Complexidade **Média**. Sem IA, sem streaming, sem integrações externas no MVP. Full-stack web (PWA + REST API).

**Requisitos analisados:** 42 FRs, 8 NFRs, 19 decisões registradas no PRD v1.2. UX completo (DESIGN.md + EXPERIENCE.md).

**Preocupações transversais identificadas:**
- Multi-tenancy via Row-level Security (`empresa_id` em toda query via middleware JWT)
- LGPD: anonimização, log de auditoria, consentimento explícito, AES-256 em repouso
- Real-time soft: polling periódico 30–60s (não WebSocket)
- Job assíncrono crítico: NoShow automático +30min por agendamento (FR-030)
- Tela pública sem autenticação: link de autoatendimento do paciente com token UUID (FR-042)
- Fuso horário: storage em UTC, exibição convertida para fuso da clínica (FR-041)
- PWA mobile-first: viewport ≥ 320px, Bottom Tab Bar, FAB, Bottom Sheet, swipe gestures

---

## Starter Templates

**Estrutura de Repositório:** Monorepo simples — `/frontend` + `/backend` em um único repositório Git.

**Frontend — Vite + React + TypeScript:**
```bash
npm create vite@latest frontend -- --template react-ts
```
Adicionar: `vite-plugin-pwa` para Service Worker + PWA Manifest.

**Backend — Spring Initializr:**
```
https://start.spring.io
Projeto: Maven | Java 21 | Spring Boot 3.x
Dependências: Spring Web, Spring Data JPA, Spring Security,
              MySQL Driver, Validation, Lombok,
              Spring Scheduler, Actuator
```

---

## Decisões Arquiteturais Principais

### Análise de Prioridade

**Decisões Críticas (bloqueiam implementação):**
- Multi-tenancy: middleware JWT + `empresa_id` em toda query
- Autenticação: JJWT + Spring Security
- Token público: UUID v4 no banco (link de autoatendimento)
- Job de NoShow: `@Scheduled` Spring

**Decisões Importantes (moldam a arquitetura):**
- Migrations: Flyway
- Estado frontend: TanStack Query
- Componentes UI: Shadcn/ui + Tailwind CSS
- Deploy: dois serviços separados (API + frontend estático)

**Decisões Diferidas (pós-MVP):**
- Cache: sem cache no MVP; Caffeine/Redis quando volume justificar
- Observabilidade avançada: Datadog/New Relic pós escala
- Schema por empresa: migrar de row-level para schema separado ao atingir 100 clínicas ativas

---

### Arquitetura de Dados

| Decisão | Escolha | Versão | Justificativa |
|---------|---------|--------|--------------|
| Migrations | **Flyway** | 9.x (bundled com Spring Boot 3.x) | Controle explícito versionado em SQL puro. Auditável em Git. Padrão com Spring Boot 3.x. |
| Cache | **Sem cache no MVP** | — | Volume inicial (<100 clínicas) não justifica. MySQL com índices adequados é suficiente. Caffeine adicionado como otimização quando volume justificar. |
| ORM | **Spring Data JPA + Hibernate** | Spring Boot 3.x (Hibernate 6.x) | Padrão da stack definida. |
| Banco | **MySQL 8.0+** | 8.0+ | Definido no Brief. Row-level security via `empresa_id`. |

---

### Autenticação e Segurança

| Decisão | Escolha | Versão | Justificativa |
|---------|---------|--------|--------------|
| JWT Library | **JJWT (io.jsonwebtoken)** | 0.12.x | Mais usada com Spring Security para JWT customizado. Sem dependência de Identity Provider externo. |
| Token público (FR-042) | **UUID v4 armazenado no banco** | — | `UUID.randomUUID()` — 122 bits de entropia. Coluna `public_token` na tabela `agendamentos`. Invalida ao cancelar/concluir/noshow. Stateful — permite invalidação imediata. |
| Multi-tenancy | **Row-level Security via `empresa_id`** | — | JWT carrega `empresa_id`. Middleware Spring Security injeta filtro em toda query JPA via `@Filter` do Hibernate ou método de repositório. Sem acesso cross-tenant possível por design. |
| Criptografia em repouso | **AES-256** | — | Dados sensíveis de pacientes. Implementado via JPA AttributeConverter. |
| HTTPS | **SSL/TLS obrigatório** | — | Heroku/Railway provisionam automaticamente. Redirecionar HTTP → HTTPS na configuração. |

---

### API e Comunicação

| Decisão | Escolha | Justificativa |
|---------|---------|--------------|
| Versionamento | **Prefixo `/api/v1/`** | Explícito, sem custo de implementação, evita refatoração futura. Ex.: `GET /api/v1/agendamentos` |
| Formato de erro | **RFC 7807 Problem Details** | Padrão Spring Boot 3.x (`ProblemDetail`). Nativo, sem configuração adicional. |
| Estilo REST | **Resources no plural, snake_case** | Ex.: `/api/v1/agendamentos`, `/api/v1/tipos-atendimento` |
| Polling | **Client-side com TanStack Query `refetchInterval`** | 30–60s conforme FR-019. Sem WebSocket — complexidade desnecessária para o volume do MVP. |
| Documentação API | **Springdoc OpenAPI (Swagger UI)** | Gerado automaticamente a partir das anotações. Facilita integração frontend-backend. |

---

### Arquitetura Frontend

| Decisão | Escolha | Versão | Justificativa |
|---------|---------|--------|--------------|
| Estado de servidor | **TanStack Query** | 5.x | Polling (FR-019), cache, invalidação, loading/error states. Resolve o core do problema com mínimo boilerplate. |
| Roteamento | **React Router** | v6 | Padrão de fato para SPA React. Maduro, amplamente documentado. |
| Componentes UI | **Shadcn/ui + Tailwind CSS** | Shadcn (latest) + Tailwind 3.x | Componentes copiados para o projeto (não são dependência). Customizáveis para o Design System da Sally. Bottom Sheet, Dialog, Toast, Dropdown disponíveis. |
| Design tokens | **CSS variables via Tailwind config** | — | Cores de marca, status, light/dark definidos como variáveis CSS. `prefers-color-scheme` via classe `dark` no `<html>`. |
| PWA | **vite-plugin-pwa** | latest | Service Worker, manifest, ícones. Offline básico (agenda do dia em cache). |
| Formulários | **React Hook Form + Zod** | RHF 7.x + Zod 3.x | Validação client-side tipada. Integra com Shadcn form components. |

---

### Infraestrutura e Deploy

| Decisão | Escolha | Justificativa |
|---------|---------|--------------|
| Frontend deploy | **Vercel ou Netlify (free tier)** | CDN global, build automático via Git push. Zero custo para o MVP. |
| Backend deploy | **Railway** | Suporte nativo a Java/Maven, MySQL como add-on, deploy via Git. Mais previsível que Heroku free tier. |
| Job de NoShow (FR-030) | **Spring `@Scheduled`** | Stateless por design — busca `status IN ('PENDENTE','CONFIRMADO') AND horario_fim <= NOW()`. Restart seguro. Zero dependência adicional. |
| Logging | **Logback (padrão Spring Boot) + JSON output** | Configurado com `logback-spring.xml` para output JSON estruturado. Railway agrega logs automaticamente. |
| Backup | **MySQL backups automáticos do Railway** | Daily backups incluídos no plano. Suficiente para o MVP. |

---

## Padrões de Implementação e Regras de Consistência

### Nomenclatura — Banco de Dados (MySQL)

| Elemento | Padrão | Exemplo |
|---------|--------|---------|
| Tabelas | `snake_case`, plural | `agendamentos`, `tipos_atendimento` |
| Colunas | `snake_case` | `empresa_id`, `horario_inicio` |
| PKs | `id` (BIGINT AUTO_INCREMENT) | `id` |
| FKs | `{tabela_singular}_id` | `profissional_id`, `empresa_id` |
| Índices | `idx_{tabela}_{coluna}` | `idx_agendamentos_empresa_id` |
| Migrations Flyway | `V{N}__{descricao}.sql` | `V1__create_empresas.sql` |

### Nomenclatura — API REST (Spring Boot)

| Elemento | Padrão | Exemplo |
|---------|--------|---------|
| Endpoints | `/api/v1/{recurso-plural}` | `/api/v1/agendamentos` |
| Path params | `/{id}` | `/api/v1/agendamentos/{id}` |
| Query params | `snake_case` | `?profissional_id=1&data=2026-06-04` |
| JSON request | `camelCase` | `{ "profissionalId": 1 }` |
| JSON response | `camelCase` | `{ "horarioInicio": "14:00" }` |
| Datas em JSON | ISO 8601 | `"2026-06-04T14:00:00Z"` |

### Nomenclatura — Java (Spring Boot)

| Elemento | Padrão | Exemplo |
|---------|--------|---------|
| Classes | `PascalCase` | `AgendamentoService` |
| Métodos | `camelCase` | `buscarProximoDisponivel()` |
| Variáveis | `camelCase` | `empresaId` |
| Constantes | `UPPER_SNAKE_CASE` | `MAX_DOUBLE_BOOKING = 2` |
| Pacotes | `lowercase` | `com.dragenda.agendamentos` |

### Nomenclatura — React/TypeScript (Frontend)

| Elemento | Padrão | Exemplo |
|---------|--------|---------|
| Componentes | `PascalCase` + `.tsx` | `AgendamentoCard.tsx` |
| Hooks | `use` + PascalCase + `.ts` | `useAgendamentos.ts` |
| Utils | `camelCase` + `.ts` | `formatarHorario.ts` |
| CSS | Tailwind utilitário | `"bg-brand-500 text-white"` |

### Formato de API

**Response de sucesso (lista):**
```json
[{ "id": 1, "status": "PENDENTE", "horarioInicio": "14:00:00" }]
```

**Response de sucesso (item único):**
```json
{ "id": 1, "status": "PENDENTE", "horarioInicio": "14:00:00" }
```

**Response de erro (RFC 7807):**
```json
{
  "type": "https://dragenda.com.br/errors/conflict",
  "title": "Conflito de horário",
  "status": 409,
  "detail": "Dr. Carlos não atende às terças-feiras.",
  "instance": "/api/v1/agendamentos"
}
```

**Status HTTP canônicos:**

| Código | Uso |
|--------|-----|
| `200 OK` | GET, PUT/PATCH bem-sucedido |
| `201 Created` | POST bem-sucedido (com `Location` header) |
| `204 No Content` | DELETE, status update sem body |
| `400 Bad Request` | Validação de campos (Bean Validation) |
| `401 Unauthorized` | Token ausente ou inválido |
| `403 Forbidden` | Perfil sem permissão para a ação |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Regra de negócio violada (horário, double-booking, transição inválida) |
| `410 Gone` | Link de autoatendimento expirado |

### Regras de Processo (Invioláveis)

**Multi-tenancy:**
- Todo endpoint autenticado DEVE extrair `empresaId` do JWT — nunca de parâmetro da request
- Todo `Repository` DEVE filtrar por `empresaId` — validado em code review obrigatório
- `GET /api/v1/cancelar/{token}` é a única rota que bypassa o filtro de tenant (acesso público)

**Transições de status:**
- Backend valida a transição antes de persistir (FR-027) — frontend envia o novo status, backend decide se é válido
- Estados terminais (`CONCLUIDO`, `CANCELADO`) retornam `409` se transição for tentada
- NoShow é revertível somente para `PRESENTE`, somente por `Staff`, com registro no histórico

**Tratamento de erros no frontend:**
- `400` → erro inline no campo via React Hook Form
- `409` → Toast `warning` com `detail` do ProblemDetail
- `401/403` → redirect para `/login` ou Toast `error`
- Erro de rede → Toast `error` + retry automático via TanStack Query (`retry: 2`)

---

## Estrutura do Projeto e Fronteiras Arquiteturais

### Estrutura do Monorepo

```
dragenda/
├── README.md
├── .gitignore
├── .github/
│   └── workflows/
│       ├── ci-frontend.yml      → build + lint + test no push
│       └── ci-backend.yml       → build + test Maven no push
│
├── frontend/                    → Vite + React + TypeScript (PWA)
│   ├── package.json
│   ├── vite.config.ts           → vite-plugin-pwa configurado
│   ├── tailwind.config.ts       → tokens de cor brand + status + dark mode
│   ├── tsconfig.json
│   ├── .env.example             → VITE_API_URL=
│   ├── index.html
│   └── src/
│       ├── main.tsx             → entry point, QueryClientProvider, RouterProvider
│       ├── App.tsx              → rotas protegidas por perfil
│       ├── features/
│       │   ├── agenda/
│       │   │   ├── components/
│       │   │   │   ├── AgendaView.tsx
│       │   │   │   ├── AgendamentoCard.tsx
│       │   │   │   ├── FiltrosProfissional.tsx
│       │   │   │   └── AlertaReordenacao.tsx     → FR-020
│       │   │   ├── hooks/
│       │   │   │   ├── useAgenda.ts             → TanStack Query, refetchInterval
│       │   │   │   └── useStatusUpdate.ts
│       │   │   └── types.ts
│       │   ├── agendamentos/
│       │   │   ├── components/
│       │   │   │   ├── NovoAgendamentoSheet.tsx  → bottom sheet / drawer
│       │   │   │   ├── DialogDoubleBooking.tsx   → FR-024
│       │   │   │   └── BuscarDisponivelView.tsx  → FR-029
│       │   │   ├── hooks/
│       │   │   │   ├── useNovoAgendamento.ts
│       │   │   │   └── useBuscarDisponivel.ts
│       │   │   └── types.ts
│       │   ├── clientes/
│       │   │   ├── components/
│       │   │   │   ├── ClientesList.tsx
│       │   │   │   └── ClienteForm.tsx           → coleta LGPD consent
│       │   │   ├── hooks/
│       │   │   │   └── useClientes.ts
│       │   │   └── types.ts
│       │   ├── autoatendimento/
│       │   │   └── AutoatendimentoPage.tsx       → /cancelar/:token (público)
│       │   └── configuracoes/
│       │       ├── components/
│       │       │   ├── ProfissionaisConfig.tsx
│       │       │   ├── DisponibilidadeConfig.tsx
│       │       │   ├── TiposAtendimentoConfig.tsx
│       │       │   └── ClinicaConfig.tsx         → fuso horário FR-041
│       │       └── hooks/
│       │           └── useConfiguracoes.ts
│       ├── shared/
│       │   ├── components/
│       │   │   ├── ui/                          → Shadcn/ui (copiados)
│       │   │   │   ├── button.tsx
│       │   │   │   ├── dialog.tsx
│       │   │   │   ├── sheet.tsx
│       │   │   │   ├── toast.tsx
│       │   │   │   └── badge.tsx
│       │   │   ├── StatusBadge.tsx              → cor + ícone + rótulo (WCAG)
│       │   │   ├── BottomTabBar.tsx             → navegação por perfil
│       │   │   └── Fab.tsx
│       │   ├── hooks/
│       │   │   ├── useAuth.ts
│       │   │   └── useToast.ts
│       │   ├── lib/
│       │   │   ├── queryClient.ts
│       │   │   ├── axios.ts                     → interceptor JWT
│       │   │   └── utils.ts
│       │   └── types/
│       │       └── index.ts
│       └── pages/
│           ├── LoginPage.tsx
│           ├── AgendaPage.tsx
│           ├── BuscarPage.tsx
│           ├── ClientesPage.tsx
│           ├── ConfiguracoesPage.tsx
│           └── AutoatendimentoPage.tsx          → rota pública
│
└── backend/                     → Spring Boot 3.x + Java 21 + Maven
    ├── pom.xml
    ├── .env.example
    ├── Procfile                  → Railway deploy
    └── src/
        ├── main/
        │   ├── java/com/dragenda/
        │   │   ├── DrAgendaApplication.java
        │   │   ├── api/
        │   │   │   ├── controllers/
        │   │   │   │   ├── AuthController.java
        │   │   │   │   ├── AgendamentoController.java
        │   │   │   │   ├── ClienteController.java
        │   │   │   │   ├── ProfissionalController.java
        │   │   │   │   ├── DisponibilidadeController.java
        │   │   │   │   ├── TipoAtendimentoController.java
        │   │   │   │   ├── EmpresaController.java
        │   │   │   │   └── AutoatendimentoController.java
        │   │   │   ├── dtos/
        │   │   │   │   ├── request/
        │   │   │   │   └── response/
        │   │   │   └── exceptions/
        │   │   │       └── GlobalExceptionHandler.java  → RFC 7807
        │   │   ├── domain/
        │   │   │   ├── entities/
        │   │   │   │   ├── Empresa.java
        │   │   │   │   ├── Usuario.java
        │   │   │   │   ├── Profissional.java
        │   │   │   │   ├── Cliente.java           → AES-256
        │   │   │   │   ├── Agendamento.java       → public_token, status
        │   │   │   │   ├── TipoAtendimento.java
        │   │   │   │   └── Disponibilidade.java
        │   │   │   ├── enums/
        │   │   │   │   ├── StatusAgendamento.java
        │   │   │   │   └── PerfilUsuario.java
        │   │   │   ├── repositories/
        │   │   │   │   └── *Repository.java       → filtros por empresaId
        │   │   │   └── services/
        │   │   │       ├── AgendamentoService.java
        │   │   │       ├── DisponibilidadeService.java
        │   │   │       ├── AutoatendimentoService.java
        │   │   │       └── ClienteService.java
        │   │   └── infrastructure/
        │   │       ├── config/
        │   │       │   ├── SecurityConfig.java
        │   │       │   ├── JwtConfig.java
        │   │       │   └── OpenApiConfig.java
        │   │       ├── jobs/
        │   │       │   └── NoShowJob.java          → @Scheduled FR-030
        │   │       └── security/
        │   │           ├── JwtFilter.java
        │   │           └── TenantFilter.java
        │   └── resources/
        │       ├── application.yml
        │       ├── application-dev.yml
        │       ├── application-prod.yml
        │       ├── logback-spring.xml
        │       └── db/migration/
        │           ├── V1__create_empresas.sql
        │           ├── V2__create_usuarios.sql
        │           ├── V3__create_profissionais.sql
        │           ├── V4__create_clientes.sql
        │           ├── V5__create_tipos_atendimento.sql
        │           ├── V6__create_disponibilidades.sql
        │           └── V7__create_agendamentos.sql
        └── test/
            └── java/com/dragenda/
                ├── services/
                └── api/
```

### Adições à Estrutura — Gaps Resolvidos

**`infrastructure/config/CorsConfig.java`** (adicionado):
```java
// Origens permitidas via variável de ambiente APP_FRONTEND_URL
// Permite múltiplas origens separadas por vírgula para dev + prod
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Value("${app.frontend.url}")
    private String[] allowedOrigins;
    // addCorsMappings: /api/** com allowedOrigins, GET/POST/PUT/PATCH/DELETE
}
```
Adicionar ao `SecurityConfig.java`: `.cors(cors -> cors.configurationSource(corsConfigurationSource()))`.

**`backend/src/test/java/com/dragenda/`** (estrutura expandida):
```
test/java/com/dragenda/
├── services/
│   ├── AgendamentoServiceTest.java    → FR-022–031: validações, double-booking, NoShow
│   ├── DisponibilidadeServiceTest.java → FR-023: janelas de tempo, intervalos
│   └── AutoatendimentoServiceTest.java → FR-042: token, expiração, transições
└── api/
    ├── AgendamentoControllerIT.java   → integração: auth, tenant isolation, CRUD
    └── AutoatendimentoControllerIT.java → integração: rota pública, token inválido/expirado
```

### Mapeamento de Requisitos → Estrutura

| Grupo de FRs | Backend | Frontend |
|-------------|---------|---------|
| Auth / Perfis (FR-001–007) | `infrastructure/security/` + `AuthController` | `shared/hooks/useAuth.ts` + `LoginPage` |
| Cadastros Base (FR-008–013, FR-041) | `domain/` entities + controllers de config | `features/configuracoes/` |
| Dashboard / Agenda (FR-014–021) | `AgendamentoController` GET filtrados | `features/agenda/` |
| Gestão Agendamentos (FR-022–031) | `AgendamentoService` + validações | `features/agendamentos/` |
| Link Autoatendimento (FR-042) | `AutoatendimentoController` + `AutoatendimentoService` | `features/autoatendimento/` |
| LGPD (FR-037–040) | `ClienteService` (anonimização) + `AuditLog` | `features/clientes/` (consentimento) |
| NoShow Job (FR-030) | `infrastructure/jobs/NoShowJob` | — (backend only) |

### Fronteiras de Integração

**Comunicação Frontend → Backend:**
- Todas as chamadas via `shared/lib/axios.ts` com interceptor JWT automático
- `401` → limpa sessão + redirect `/login`
- Base URL via `VITE_API_URL`

**Rota pública (sem auth):**
- `GET /api/v1/cancelar/{token}` bypassa `TenantFilter` e `JwtFilter`
- `AutoatendimentoPage` não usa `useAuth`, sem Bottom Tab Bar

**Fluxo NoShow:**
```
@Scheduled (a cada 1 min) → NoShowJob
  → busca: status IN (PENDENTE, CONFIRMADO) AND horario + 30min ≤ NOW() UTC
  → valida status no momento da execução (condição de corrida segura)
  → persiste NOSHOW
  → TanStack Query invalida cache na próxima poll (30–60s)
```

---

## Variáveis de Ambiente

### Backend (`application-prod.yml` / Railway environment)

| Variável | Descrição | Exemplo |
|---------|-----------|---------|
| `DB_URL` | JDBC URL do MySQL Railway | `jdbc:mysql://containers-us-west-XX.railway.app:6578/railway` |
| `DB_USERNAME` | Usuário do banco | `root` |
| `DB_PASSWORD` | Senha do banco | *(secret)* |
| `JWT_SECRET` | Chave HMAC-SHA256 para assinar JWTs (≥ 256 bits) | *(secret, gerado com `openssl rand -hex 32`)* |
| `JWT_EXPIRATION_MS` | Expiração do token em ms | `86400000` (24h) |
| `ENCRYPTION_KEY` | Chave AES-256 para dados sensíveis de pacientes | *(secret, gerado com `openssl rand -hex 32`)* |
| `APP_FRONTEND_URL` | URL(s) do frontend permitidas no CORS | `https://dragenda.vercel.app,http://localhost:5173` |
| `SERVER_PORT` | Porta da aplicação | `8080` |

### Frontend (`frontend/.env` / Vercel environment)

| Variável | Descrição | Exemplo |
|---------|-----------|---------|
| `VITE_API_URL` | URL base da API backend | `https://dragenda-api.railway.app` |

> **Regra:** Nenhuma variável de ambiente acima pode ser hardcoded no código-fonte. Usar `@Value("${...}")` no Spring e `import.meta.env.VITE_*` no Vite. Os arquivos `.env.example` em `/backend` e `/frontend` devem listar todas as variáveis acima com valores de placeholder.

---

## Estratégia de Testes

### Cobertura Mínima do MVP

| Camada | Ferramenta | Meta de cobertura | Foco |
|--------|-----------|------------------|------|
| Services (backend) | JUnit 5 + Mockito | **80%** | Regras de negócio críticas |
| Controllers (backend) | `@SpringBootTest` + MockMvc | Endpoints críticos | Auth, tenant isolation, erros |
| Frontend hooks | Vitest + Testing Library | Hooks de mutação | Status update, novo agendamento |
| E2E | — | Não no MVP | Adicionado pós-launch |

### Testes Obrigatórios por Regra de Negócio

| Teste | FR coberto | Tipo |
|-------|-----------|------|
| Validação de disponibilidade (dia, horário, intervalo) | FR-023 | Unitário |
| Double-booking: 1° OK, 2° OK com confirmação, 3° bloqueado | FR-024 | Unitário |
| Transições de status válidas e inválidas | FR-027 | Unitário |
| NoShow não aplicado se status já for CANCELADO/PRESENTE | FR-030 | Unitário |
| Token de autoatendimento expira no horário do agendamento | FR-042 | Unitário |
| Token inválido retorna 410 Gone | FR-042 | Integração |
| Endpoint autenticado com `empresa_id` diferente retorna 403 | Multi-tenancy | Integração |
| `GET /api/v1/cancelar/{token}` acessível sem JWT | FR-042 | Integração |
