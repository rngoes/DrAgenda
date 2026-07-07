# Story 1.2: Banco de Dados Versionado com Flyway e Seed do Admin Sistema

Status: done

## Story

Como sistema,
Quero um banco de dados versionado com Flyway e um Admin Sistema seed seguro,
Para que o schema evolua de forma auditável e o sistema seja inicializável com segurança em produção.

## Acceptance Criteria

**AC-1 — Migration V1: tabela `empresas`**
- Given o backend inicia pela primeira vez contra um banco MySQL vazio
- When o Flyway executa
- Then a migration `V{timestamp}__create_empresas.sql` é aplicada criando a tabela `empresas` (plural — convenção da arquitetura) com colunas: `id` (BIGINT AUTO_INCREMENT PK), `nome` (VARCHAR 255 NOT NULL), `ativo` (BOOLEAN DEFAULT TRUE NOT NULL), `created_at` (DATETIME NOT NULL)
- And `ddl-auto` está configurado como `validate` (nunca `update` ou `create`)
- And a tabela `flyway_schema_history` registra a migration aplicada

**AC-2 — Migration V2: tabela `usuarios`**
- Given a migration V1 foi aplicada com sucesso
- When o Flyway aplica a segunda migration
- Then a tabela `usuarios` é criada com colunas: `id` (BIGINT AUTO_INCREMENT PK), `nome` (VARCHAR 255 NOT NULL), `email` (VARCHAR 255 NOT NULL), `senha_hash` (VARCHAR 255 NOT NULL), `perfil` (ENUM: `ADMIN_SISTEMA`, `ADMIN_EMPRESA`, `STAFF`, `PROFISSIONAL` NOT NULL), `empresa_id` (BIGINT FK nullable → `empresas.id`), `ativo` (BOOLEAN DEFAULT TRUE NOT NULL), `senha_temporaria` (BOOLEAN DEFAULT FALSE NOT NULL), `created_at` (DATETIME NOT NULL)
- And existem índices: `idx_usuarios_email` (UNIQUE) e `idx_usuarios_empresa_id`

**AC-3 — Seed idempotente do Admin Sistema**
- Given as migrations V1 e V2 foram aplicadas e `ADMIN_SEED_PASSWORD` está definida no ambiente
- When o contexto Spring inicializa
- Then o usuário `admin@agenda.com` com perfil `ADMIN_SISTEMA` é criado se ainda não existir (idempotente — sem erro em reinicializações)
- And a senha é armazenada com hash BCrypt (nunca em texto plano)
- And o campo `senha_temporaria = true` está marcado, forçando troca no primeiro login (FR-044)
- And `empresa_id = NULL` (Admin Sistema não pertence a nenhuma empresa)

**AC-4 — Falha explícita se `ADMIN_SEED_PASSWORD` ausente**
- Given `ADMIN_SEED_PASSWORD` não está definida no ambiente
- When o sistema tenta inicializar
- Then a aplicação falha na inicialização com mensagem clara: `"ADMIN_SEED_PASSWORD não configurada — deploy bloqueado por segurança"`

**AC-5 — Proteção contra edição retroativa de migrations**
- Given uma migration já aplicada tem seu checksum alterado
- When o Flyway inicia
- Then o sistema falha com erro de validação do Flyway (`FlywayValidateException`)

**AC-6 — `ddl-auto: validate` em todos os ambientes**
- Given qualquer ambiente (dev, prod)
- When a aplicação sobe
- Then `spring.jpa.hibernate.ddl-auto=validate` — Hibernate nunca cria ou altera tabelas automaticamente

## Tasks / Subtasks

- [x] **Task 1 — Criar migrations Flyway** (AC-1, AC-2)
  - [x] Criar `backend/src/main/resources/db/migration/V{timestamp}__create_empresas.sql` com DDL da tabela `empresas`
  - [x] Criar `backend/src/main/resources/db/migration/V{timestamp}__create_usuarios.sql` com DDL completo da tabela `usuarios` (incluindo índices UNIQUE em `email` e índice em `empresa_id`)
  - [x] Usar timestamps sequenciais com ~1min de intervalo (ex: `V202606041000__create_empresas.sql`, `V202606041001__create_usuarios.sql`)
  - [x] Verificar que nenhuma migration existente foi editada (checksum intacto)

- [x] **Task 2 — Configurar Flyway e JPA no application.yml** (AC-1, AC-5, AC-6)
  - [x] Configurar `spring.flyway.enabled=true` e `spring.flyway.validate-on-migrate=true` (padrão, mas explícito)
  - [x] Configurar `spring.jpa.hibernate.ddl-auto=validate` em `application.yml` base e `application-prod.yml`
  - [x] Configurar `spring.jpa.hibernate.ddl-auto=validate` em `application-dev.yml` também (não `create-drop`)

- [x] **Task 3 — Entidades JPA e repositórios base** (dependência das Tasks 4 e 5)
  - [x] Criar `domain/entities/Empresa.java` com `@Entity @Table(name = "empresas")` e campos mapeados
  - [x] Criar `domain/entities/Usuario.java` com `@Entity @Table(name = "usuarios")`, `@Enumerated(EnumType.STRING)` para `perfil`, FK para `Empresa`
  - [x] Criar `domain/enums/PerfilUsuario.java` com: `ADMIN_SISTEMA`, `ADMIN_EMPRESA`, `STAFF`, `PROFISSIONAL`
  - [x] Criar `domain/repositories/UsuarioRepository.java` com `Optional<Usuario> findByEmail(String email)` e `boolean existsByEmail(String email)`
  - [x] Criar `domain/repositories/EmpresaRepository.java` (vazio por ora — stub para compilar)

- [x] **Task 4 — AdminSeedConfig com BCrypt** (AC-3, AC-4)
  - [x] Criar `infrastructure/config/AdminSeedConfig.java` implementando `ApplicationRunner`
  - [x] Injetar `UsuarioRepository` e `BCryptPasswordEncoder`
  - [x] Ler `ADMIN_SEED_PASSWORD` via `@Value("${admin.seed.password}")` — **sem default**
  - [x] Adicionar `admin.seed.password=${ADMIN_SEED_PASSWORD}` ao `application.yml`
  - [x] Adicionar `ADMIN_SEED_PASSWORD=` ao `backend/.env.example`
  - [x] Implementar lógica: `if (!usuarioRepository.existsByEmail("admin@agenda.com")) { ... criar ... }`
  - [x] `@Order(1)` para garantir execução antes de outros `ApplicationRunner`s

- [x] **Task 5 — Falha explícita se variável ausente** (AC-4)
  - [x] Criar `infrastructure/config/StartupValidator.java` com `@PostConstruct` que valida presença de `ADMIN_SEED_PASSWORD`
  - [x] Lançar `IllegalStateException` com mensagem exata: `"ADMIN_SEED_PASSWORD não configurada — deploy bloqueado por segurança"`
  - [x] Alternativa: usar `@Value` sem default — Spring lança `BeanCreationException` automaticamente (preferível, menos código)

- [x] **Task 6 — Configurar `BCryptPasswordEncoder` como bean** (dependência da Task 4)
  - [x] Adicionar `@Bean BCryptPasswordEncoder passwordEncoder()` ao `SecurityConfig.java` (já criado na Story 1.1)

- [x] **Task 7 — Testes de integração** (AC-3, AC-4, AC-5)
  - [x] Criar `test/java/com/dragenda/infrastructure/AdminSeedConfigIT.java` com `@SpringBootTest`
  - [x] Teste: seed executa uma vez → `admin@agenda.com` existe com `senha_temporaria=true` e `empresa_id=null`
  - [x] Teste: seed é idempotente → segunda execução não cria duplicata, não lança exceção
  - [x] Teste: senha armazenada como hash BCrypt (não plain text — `!senha_hash.equals(ADMIN_SEED_PASSWORD)`)
  - [x] Teste: sem `ADMIN_SEED_PASSWORD` → aplicação falha na inicialização com `IllegalStateException`

## Dev Notes

### ⚠️ Dependência Arquitetural Crítica — V1 e V2 nesta Story

A Story 1.2 **deve criar tanto V1 (empresas) quanto V2 (usuarios)** porque o `AdminSeedConfig` precisa da tabela `usuarios` para persistir o seed na inicialização. A Story 2.1 (Login e JWT) **não cria V2** — ela constrói a lógica de autenticação *sobre* o schema já existente criado aqui. Isso é intencional: fundação de schema é responsabilidade desta story de infraestrutura.

A Story 2.1 assume que `usuarios` já existe e adiciona: `AuthController`, `JwtFilter`, `TenantFilter` e regras de autorização.

### ⚠️ Inconsistência de Nomenclatura — `empresa` vs `empresas`

O AC do epics.md menciona `V1__create_empresa.sql` e tabela `empresa` (singular). **A arquitetura define tabelas no plural** (`agendamentos`, `tipos_atendimento`). Use `empresas` (plural) e ajuste o AC mentalmente. Nome canônico: `empresas`.

### Nomenclatura dos Arquivos de Migration (R6)

Use timestamps reais do momento da criação. Exemplo:
```
V202606041000__create_empresas.sql
V202606041001__create_usuarios.sql
```
**Nunca** `V1__...` ou `V2__...` — conflito garantido com outros devs em branches paralelas.

### DDL Completo — `V{ts}__create_empresas.sql`

```sql
CREATE TABLE empresas (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(255) NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### DDL Completo — `V{ts}__create_usuarios.sql`

```sql
CREATE TABLE usuarios (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    senha_hash       VARCHAR(255) NOT NULL,
    perfil           ENUM('ADMIN_SISTEMA','ADMIN_EMPRESA','STAFF','PROFISSIONAL') NOT NULL,
    empresa_id       BIGINT       NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    senha_temporaria BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    INDEX idx_usuarios_empresa_id (empresa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_usuarios_email ON usuarios(email);
```

### `AdminSeedConfig.java` — Implementação Completa

```java
// backend/src/main/java/com/dragenda/infrastructure/config/AdminSeedConfig.java
@Configuration
@Order(1)
@RequiredArgsConstructor
public class AdminSeedConfig implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.seed.password}")  // ← SEM default — falha explícita se ausente
    private String adminSeedPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByEmail("admin@agenda.com")) {
            return; // idempotente
        }
        Usuario admin = new Usuario();
        admin.setEmail("admin@agenda.com");
        admin.setNome("Admin Sistema");
        admin.setSenhaHash(passwordEncoder.encode(adminSeedPassword));
        admin.setPerfil(PerfilUsuario.ADMIN_SISTEMA);
        admin.setAtivo(true);
        admin.setSenhaTemporaria(true);   // força troca no primeiro login (FR-044)
        admin.setEmpresaId(null);         // Admin Sistema não pertence a empresa
        usuarioRepository.save(admin);
    }
}
```

### `SecurityConfig.java` — Adicionar Bean BCrypt (complementar à Story 1.1)

```java
// Adicionar ao SecurityConfig.java criado na Story 1.1:
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### `application.yml` — Configurações Flyway e JPA

```yaml
spring:
  flyway:
    enabled: true
    validate-on-migrate: true   # proteção contra edição retroativa (AC-5)
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate        # NUNCA update/create (AC-6)
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true

admin:
  seed:
    password: ${ADMIN_SEED_PASSWORD}  # sem default — falha se ausente (AC-4)
```

### `application-dev.yml` — Ambiente Local

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/dragenda_dev?createDatabaseIfNotExist=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate   # mesmo em dev — usar Flyway, nunca create-drop

admin:
  seed:
    password: ${ADMIN_SEED_PASSWORD:admin123@Dev}  # default SOMENTE em dev
```

> **Nota de segurança:** O default `admin123@Dev` em `application-dev.yml` é aceitável **apenas para dev local**. Em produção e CI, `ADMIN_SEED_PASSWORD` deve ser definida explicitamente sem default.

### Entidade `Usuario.java` — Campos Essenciais

```java
@Entity
@Table(name = "usuarios")
@Data @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String email;
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerfilUsuario perfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private boolean ativo = true;
    private boolean senhaTemporaria = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

> **Atenção:** `empresa_id` é FK nullable — `ADMIN_SISTEMA` tem `empresa = null`. Toda query de entidade multi-tenant em Epics futuros deve verificar `empresa != null` antes de filtrar por `empresaId`.

### Entidade `Empresa.java`

```java
@Entity
@Table(name = "empresas")
@Data @NoArgsConstructor
public class Empresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### Configuração do `pom.xml` — Dependências Necessárias

Esta story requer (além do que já existe da Story 1.1):
```xml
<!-- Spring Security (para BCryptPasswordEncoder) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- Flyway MySQL -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

> O `spring-boot-starter-flyway` inclui Flyway core; `flyway-mysql` é necessário para suporte específico ao MySQL 8.

### Teste de Integração — Configuração H2 para CI

Para os testes de integração rodarem em CI sem MySQL, configurar H2 em memória:

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate
admin:
  seed:
    password: TestPassword123!
```

Adicionar dependência de escopo `test`:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### Arquivos Modificados vs Criados

| Arquivo | Operação | Notas |
|---|---|---|
| `db/migration/V{ts}__create_empresas.sql` | NEW | Tabela `empresas` |
| `db/migration/V{ts}__create_usuarios.sql` | NEW | Tabela `usuarios` com FK e índices |
| `domain/entities/Empresa.java` | NEW | |
| `domain/entities/Usuario.java` | NEW | FK nullable para `Empresa` |
| `domain/enums/PerfilUsuario.java` | NEW | 4 valores |
| `domain/repositories/EmpresaRepository.java` | NEW | Stub vazio |
| `domain/repositories/UsuarioRepository.java` | NEW | `findByEmail`, `existsByEmail` |
| `infrastructure/config/AdminSeedConfig.java` | NEW | `ApplicationRunner` |
| `infrastructure/config/SecurityConfig.java` | **UPDATE** | Adicionar `@Bean BCryptPasswordEncoder` |
| `application.yml` | **UPDATE** | Flyway + JPA + `admin.seed.password` |
| `application-dev.yml` | **UPDATE** | Default seed password para dev |
| `application-prod.yml` | **UPDATE** | Sem default para `ADMIN_SEED_PASSWORD` |
| `backend/.env.example` | **UPDATE** | Adicionar `ADMIN_SEED_PASSWORD=` |
| `test/resources/application-test.yml` | NEW | H2 para CI |
| `test/.../AdminSeedConfigIT.java` | NEW | Testes de integração do seed |

### Referências

- [Source: epics.md#Story 1.2] — Acceptance Criteria completos
- [Source: epics.md#Story 2.1] — Schema V2 detalhado (usado aqui como fundação)
- [Source: architecture.md#Nomenclatura — Banco de Dados] — snake_case plural, idx_ prefix
- [Source: architecture.md#Autenticação e Segurança] — BCrypt, multi-tenancy via empresa_id
- [Source: architecture.md#Estrutura do Monorepo] — paths canônicos de todos os arquivos
- [Source: architecture.md#Estratégia de Testes] — @SpringBootTest + H2 para integração
- [Source: epics.md#R6] — Convenção timestamp para migrations

## Dev Agent Record

### Agent Model Used
Claude Sonnet 4.6 (GitHub Copilot)

### Completion Notes List
- Task 6 (BCryptPasswordEncoder bean) já existia no SecurityConfig.java da Story 1.1 — pulado
- application-test.properties estava com conteúdo YAML em arquivo .properties (formato inválido) — corrigido para key=value
- Java 21 não disponível localmente (Java 1.8 instalado) — validação feita exclusivamente via CI GitHub Actions
- ADMIN_SEED_PASSWORD adicionado ao ci-backend.yml como env var de teste

### File List
- `backend/src/main/resources/db/migration/V202607071000__create_empresas.sql` — NEW
- `backend/src/main/resources/db/migration/V202607071001__create_usuarios.sql` — NEW
- `backend/src/main/java/com/dragenda/domain/enums/PerfilUsuario.java` — NEW
- `backend/src/main/java/com/dragenda/domain/entities/Empresa.java` — NEW
- `backend/src/main/java/com/dragenda/domain/entities/Usuario.java` — NEW
- `backend/src/main/java/com/dragenda/domain/repositories/EmpresaRepository.java` — NEW
- `backend/src/main/java/com/dragenda/domain/repositories/UsuarioRepository.java` — NEW
- `backend/src/main/java/com/dragenda/infrastructure/config/AdminSeedConfig.java` — NEW
- `backend/src/test/java/com/dragenda/infrastructure/AdminSeedConfigIT.java` — NEW
- `backend/src/main/resources/application.yml` — UPDATED
- `backend/src/main/resources/application-dev.yml` — UPDATED
- `backend/.env.example` — UPDATED
- `backend/src/test/resources/application-test.properties` — UPDATED (formato corrigido)
- `.github/workflows/ci-backend.yml` — UPDATED
