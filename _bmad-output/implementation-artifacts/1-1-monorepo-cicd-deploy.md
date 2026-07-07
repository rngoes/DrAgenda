---
baseline_commit: NO_VCS
---

# Story 1.1: Monorepo com CI/CD e Deploy Base

Status: review

## Story

Como desenvolvedor,
Quero um monorepo configurado com pipeline de CI/CD e deploy automático no Vercel (frontend) e Railway (backend + MySQL),
Para que cada push entregue código validado em produção sem intervenção manual e o sistema seja acessível via HTTPS desde o primeiro deploy.

## Acceptance Criteria

**AC-1 — Estrutura do monorepo**
- Given o repositório Git existe
- When a estrutura é verificada
- Then existe `/frontend` (Vite + React + TypeScript) e `/backend` (Spring Boot 3.x + Java 21 + Maven) na raiz

**AC-2 — CI/CD GitHub Actions**
- Given um push é feito na branch principal
- When o GitHub Actions dispara
- Then `ci-frontend.yml` executa: build + lint (ESLint) + testes (vitest) sem erros
- And `ci-backend.yml` executa: `mvn verify` (build + testes Maven) sem erros
- And ambos pipelines rodam em paralelo

**AC-3 — Deploy automático**
- Given o CI passa
- When o pipeline completa
- Then o frontend é deployed no Vercel automaticamente via integração nativa (sem script manual)
- And o backend é deployed no Railway com MySQL add-on via Git push

**AC-4 — HTTPS obrigatório**
- Given o backend está em execução no Railway
- When qualquer endpoint é acessado via HTTP
- Then a comunicação ocorre exclusivamente via HTTPS (FR-036)
- And CORS aceita apenas origens definidas em `APP_FRONTEND_URL`

**AC-5 — Variáveis de ambiente documentadas**
- Given o projeto é clonado em uma nova máquina
- When o desenvolvedor lê os arquivos de exemplo
- Then `backend/.env.example` lista todas as variáveis obrigatórias: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `ENCRYPTION_KEY`, `APP_FRONTEND_URL`, `SERVER_PORT`
- And `frontend/.env.example` lista: `VITE_API_URL`
- And a aplicação Spring falha com mensagem clara se qualquer variável obrigatória estiver ausente

**AC-6 — Springdoc OpenAPI e logging JSON**
- Given o backend está rodando
- When `GET /v3/api-docs` é acessado
- Then retorna a especificação OpenAPI em JSON válido
- And todos os logs do backend são emitidos em formato JSON estruturado via Logback (compatível com Railway log drain)

**AC-7 — Backup automático do banco**
- Given o MySQL add-on está ativo no Railway
- When o ambiente de produção está configurado
- Then o backup automático diário está habilitado no Railway com retenção mínima de 30 dias (NFR-008)
- And o procedimento de restore manual está documentado no `README.md` de operações

**AC-8 — PWA manifest**
- Given o frontend está em build de produção
- When `npm run build` é executado
- Then `manifest.json` está presente com: `name: "DrAgenda"`, `display: "standalone"`, ícone 192px e 512px
- And o build completa sem erros de TypeScript e sem warnings de lint

## Tasks / Subtasks

- [x] **Task 1 — Inicializar o monorepo** (AC-1)
  - [x] Criar repositório Git com `.gitignore` cobrindo `node_modules/`, `target/`, `.env`
  - [x] Scaffold frontend: `npm create vite@latest frontend -- --template react-ts`
  - [x] Scaffold backend: Spring Initializr com dependências: Spring Web, Spring Data JPA, Spring Security, MySQL Driver, Validation, Lombok, Spring Scheduler, Actuator, Flyway
  - [x] Criar `README.md` na raiz com visão geral do projeto e instrução de convenção de migrations Flyway (`V{yyyyMMddHHmm}__descricao.sql`)

- [x] **Task 2 — Configurar variáveis de ambiente** (AC-5)
  - [x] Criar `backend/.env.example` com todas as 8 variáveis obrigatórias e valores placeholder
  - [x] Criar `frontend/.env.example` com `VITE_API_URL=http://localhost:8080`
  - [x] Configurar `application.yml` para falhar explicitamente se variável obrigatória ausente (Spring `@Value` sem default em campos críticos)
  - [x] Configurar `application-dev.yml` para ambiente local (MySQL local em `localhost:3306/dragenda_dev`)
  - [x] Configurar `application-prod.yml` usando variáveis de ambiente Railway

- [x] **Task 3 — Configurar HTTPS, CORS e segurança básica** (AC-4)
  - [x] Criar `CorsConfig.java` em `infrastructure/config/` lendo `APP_FRONTEND_URL` como array (split por vírgula para múltiplas origens dev+prod)
  - [x] Configurar redirect HTTP → HTTPS via `SecurityConfig.java` (`.requiresChannel().anyRequest().requiresSecure()` ou via Railway)
  - [x] Criar `SecurityConfig.java` esqueleto com CSRF desabilitado para API stateless e placeholder para JWT filter (implementado na Story 2.1)

- [x] **Task 4 — Configurar Springdoc OpenAPI e Logback** (AC-6)
  - [x] Adicionar dependência `springdoc-openapi-starter-webmvc-ui` ao `pom.xml`
  - [x] Criar `OpenApiConfig.java` com informações básicas da API (título "DrAgenda API", versão)
  - [x] Criar `logback-spring.xml` com encoder JSON (usando `logstash-logback-encoder`) para saída JSON em produção, padrão legível em dev

- [x] **Task 5 — Configurar GitHub Actions CI/CD** (AC-2, AC-3)
  - [x] Criar `.github/workflows/ci-frontend.yml`: trigger `push` + `pull_request`, steps: `npm ci`, `npm run lint`, `npm run build`, `npm run test -- --run`
  - [x] Criar `.github/workflows/ci-backend.yml`: trigger `push` + `pull_request`, steps: setup Java 21, `mvn verify`
  - [x] Configurar integração Vercel via `vercel.json` na raiz de `/frontend` apontando para o repositório (ou via Vercel dashboard)
  - [x] Criar `Procfile` em `/backend` para Railway: `web: java -jar target/dragenda-*.jar`

- [x] **Task 6 — Configurar PWA manifest** (AC-8)
  - [x] Instalar `vite-plugin-pwa`: `npm install -D vite-plugin-pwa`
  - [x] Configurar `vite.config.ts` com `VitePWA({ manifest: { name: "DrAgenda", display: "standalone", icons: [...] } })`
  - [x] Criar ícones de placeholder 192x192 e 512x512 em `public/icons/`

- [x] **Task 7 — Validar e documentar backup** (AC-7)
  - [x] Verificar que Railway MySQL add-on tem backups automáticos diários habilitados
  - [x] Documentar em `README.md` o procedimento para restore manual a partir de um snapshot do Railway

## Dev Notes

### Stack e Versões Obrigatórias

| Tecnologia | Versão | Notas |
|---|---|---|
| Java | 21 | LTS — obrigatório para Spring Boot 3.x |
| Spring Boot | 3.x (latest stable) | Maven project |
| Flyway | 9.x (bundled) | Via `spring-boot-starter-flyway` |
| Node.js | 20+ LTS | Para frontend |
| Vite | 5.x | Via `npm create vite@latest` |
| React | 18.x | Template `react-ts` |
| TypeScript | 5.x | Strict mode habilitado |
| `vite-plugin-pwa` | latest | Para manifest e Service Worker |

### Estrutura de Arquivos a Criar (Story 1.1 — ALL NEW)

```
dragenda/
├── README.md                          → NEW: convenção migrations + restore backup
├── .gitignore                         → NEW
├── .github/
│   └── workflows/
│       ├── ci-frontend.yml            → NEW
│       └── ci-backend.yml             → NEW
├── frontend/
│   ├── package.json                   → NEW (vite, react, ts, vite-plugin-pwa)
│   ├── vite.config.ts                 → NEW (VitePWA configurado)
│   ├── tsconfig.json                  → NEW (strict: true)
│   ├── .env.example                   → NEW (VITE_API_URL)
│   ├── index.html                     → NEW
│   └── src/
│       └── main.tsx                   → NEW (stub — sem lógica de negócio ainda)
└── backend/
    ├── pom.xml                        → NEW (Spring Boot 3.x + todas as deps)
    ├── .env.example                   → NEW (8 variáveis obrigatórias)
    ├── Procfile                       → NEW (Railway)
    └── src/
        └── main/
            ├── java/com/dragenda/
            │   ├── DrAgendaApplication.java → NEW
            │   └── infrastructure/
            │       └── config/
            │           ├── SecurityConfig.java  → NEW (esqueleto básico)
            │           ├── CorsConfig.java      → NEW (APP_FRONTEND_URL)
            │           └── OpenApiConfig.java   → NEW
            └── resources/
                ├── application.yml           → NEW
                ├── application-dev.yml       → NEW
                ├── application-prod.yml      → NEW
                └── logback-spring.xml        → NEW (JSON prod, legível dev)
```

> **Nota:** Nenhum arquivo de migration Flyway é criado nesta story. A primeira migration (`V{timestamp}__create_empresas.sql`) é responsabilidade da Story 1.2. Esta story apenas configura o Flyway no `pom.xml` e `application.yml` sem migrations para executar.

### CorsConfig.java — Padrão de Implementação

```java
// backend/src/main/java/com/dragenda/infrastructure/config/CorsConfig.java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.frontend.url}")
    private String allowedOriginsRaw;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOriginsRaw.split(",");
        registry.addMapping("/api/**")
            .allowedOrigins(origins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

`application.yml`:
```yaml
app:
  frontend:
    url: ${APP_FRONTEND_URL:http://localhost:5173}
```

### Logback JSON — Padrão de Implementação

Dependência no `pom.xml`:
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

`logback-spring.xml` (perfil `prod` usa JSON, demais usam padrão legível):
```xml
<springProfile name="prod">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
</springProfile>
<springProfile name="!prod">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</springProfile>
```

### ci-frontend.yml — Padrão de Implementação

```yaml
name: CI Frontend
on:
  push:
    paths: ['frontend/**']
  pull_request:
    paths: ['frontend/**']
defaults:
  run:
    working-directory: frontend
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm', cache-dependency-path: frontend/package-lock.json }
      - run: npm ci
      - run: npm run lint
      - run: npm run build
      - run: npm run test -- --run
```

### ci-backend.yml — Padrão de Implementação

```yaml
name: CI Backend
on:
  push:
    paths: ['backend/**']
  pull_request:
    paths: ['backend/**']
defaults:
  run:
    working-directory: backend
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: 'maven' }
      - run: mvn verify
```

### Variáveis de Ambiente — Falha Explícita

No Spring, qualquer `@Value` sem default em um `@Component`/`@Configuration` obrigatório causa falha na inicialização se a variável não estiver definida. Para `JWT_SECRET`, `ENCRYPTION_KEY` e `DB_PASSWORD`, nunca adicionar `defaultValue`:

```java
@Value("${jwt.secret}")  // ← sem `:` — falha se ausente
private String jwtSecret;
```

`application.yml` mapeia via:
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

### Convenção de Migrations Flyway (R6)

Documentar no `README.md` e no `.github/CONTRIBUTING.md`:

> Toda nova migration usa timestamp como prefixo: `V{yyyyMMddHHmm}__{descricao}.sql`
> Exemplo: `V202606041430__create_clientes.sql`
> 
> **Nunca use número sequencial** (V1, V2...) — conflito garantido em branches paralelas.
> Antes de criar uma migration, verifique se existe outra migration com timestamp próximo.

### Alerta de Segurança — Sem Segredos em Código

- `JWT_SECRET` e `ENCRYPTION_KEY` NUNCA no `application.yml` ou `application-prod.yml`
- Apenas `${JWT_SECRET}` (referência à variável de ambiente) é permitido
- `.env` real NUNCA commitado (garantido pelo `.gitignore`)
- Para Railway: configurar como Environment Variables no dashboard, não como arquivo

### Notas de Arquitetura

- **`ddl-auto`**: configurar como `validate` em prod e `none` em dev nesta story. A Story 1.2 adicionará as migrations e mudará para `validate` permanentemente.
- **`SecurityConfig.java`**: apenas esqueleto — CSRF desabilitado, todas as rotas permitidas temporariamente. A Story 2.1 implementa JWT filter e regras de autorização por perfil. Documentar `// TODO Story 2.1: adicionar JwtFilter aqui`.
- **Flyway sem migrations**: configurar `spring.flyway.enabled=true` mas o diretório `db/migration/` vazio é válido — Flyway apenas inicializa a tabela `flyway_schema_history`.

### Referências

- [Source: epics.md#Story 1.1] — Acceptance Criteria completos
- [Source: architecture.md#Starter Templates] — comandos exatos de scaffold
- [Source: architecture.md#Infraestrutura e Deploy] — Railway, Vercel, Logback JSON, Backup
- [Source: architecture.md#Variáveis de Ambiente] — tabela completa de variáveis
- [Source: architecture.md#Estrutura do Monorepo] — estrutura de pastas canônica
- [Source: architecture.md#Adições à Estrutura] — `CorsConfig.java` padrão de implementação
- [Source: epics.md#R6] — Convenção de naming de migrations com timestamp

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
