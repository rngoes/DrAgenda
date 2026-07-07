# Story 3.5: Cadastro de Clientes com Consentimento LGPD e Criptografia

Status: ready-for-dev

## Story

Como Staff,
Quero cadastrar clientes com coleta obrigatória do termo de consentimento LGPD e ter seus dados sensíveis protegidos por criptografia,
Para que a clínica esteja em conformidade com a LGPD desde o primeiro cadastro.

## Acceptance Criteria

**AC-1 — Migration V7: tabela `clientes`**
- Given a migration `V{yyyyMMddHHmm}__create_clientes.sql` é aplicada
- When o sistema inicializa
- Then a tabela `clientes` existe com colunas:
  - `id` BIGINT PK AUTO_INCREMENT
  - `empresa_id` BIGINT FK NOT NULL → `empresas.id`
  - `nome_enc` TEXT NOT NULL (campo criptografado AES-256)
  - `telefone_enc` TEXT NULL (campo criptografado AES-256)
  - `data_nascimento_enc` TEXT NULL (campo criptografado AES-256)
  - `consentimento_lgpd` BOOLEAN NOT NULL DEFAULT FALSE
  - `consentimento_timestamp` DATETIME NULL
  - `consentimento_versao_termo` VARCHAR(50) NULL
  - `consentimento_usuario_id` BIGINT FK NULL → `usuarios.id`
  - `anonimizado` BOOLEAN NOT NULL DEFAULT FALSE
  - `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
- And índice `idx_clientes_empresa_id` existe
- And **sem colunas de diagnóstico, procedimento clínico ou saúde** (FR-040)

**AC-2 — `AesAttributeConverter` como componente único de criptografia (R3)**
- Given a entidade `Cliente` é persistida
- When JPA grava no banco
- Then `nome_enc`, `telefone_enc` e `data_nascimento_enc` são criptografados com AES-256 usando `ENCRYPTION_KEY` do ambiente
- And na leitura, os campos são descriptografados automaticamente
- And `AesAttributeConverter` é o **único** componente de criptografia AES-256 em todo o projeto — nenhuma outra classe reimplementa AES
- And todas as entidades presentes e futuras com campos sensíveis reutilizam este converter via `@Convert(converter = AesAttributeConverter.class)` (R3)

**AC-3 — Formulário de cadastro de cliente**
- Given o Staff acessa `/clientes/novo`
- When a tela renderiza
- Then exibe campos: Nome (obrigatório), Telefone (opcional), Data de Nascimento (opcional)
- And exibe checkbox obrigatório: "Li e aceito o Termo de Consentimento LGPD" com link para `/politica`
- And o botão "Cadastrar" é desabilitado enquanto o checkbox não estiver marcado

**AC-4 — Criar cliente com consentimento**
- Given o Staff submete `POST /api/v1/clientes` com `consentimentoLgpd: true`
- When o backend persiste
- Then `consentimento_lgpd = true`, `consentimento_timestamp` = instante atual UTC, `consentimento_versao_termo` = valor de `TERMO_VERSAO_VIGENTE` (constante do sistema, `"v1.0"`), `consentimento_usuario_id` = `SecurityUtils.getUserId()` (FR-037)
- And retorna HTTP 201

**AC-5 — Consentimento obrigatório**
- Given o Staff submete com `consentimentoLgpd: false` ou sem o campo
- When o backend valida
- Then retorna HTTP 400 com mensagem `"Consentimento LGPD é obrigatório para cadastrar cliente"`
- And o frontend bloqueia a submissão antes de chegar ao backend (validação dupla)

**AC-6 — Isolamento multi-tenant**
- Given qualquer usuário autenticado acessa ou cria clientes
- When o backend processa
- Then `empresa_id` vem exclusivamente de `SecurityUtils.getEmpresaId()` — nunca do request body
- And `GET /api/v1/clientes/{id}` de cliente de outra empresa retorna HTTP 404

## Tasks / Subtasks

- [ ] **Task 1 — Migration V7** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__create_clientes.sql`
  - [ ] Usar timestamp real no momento da implementação
  - [ ] SQL com tabela + índice + FKs nomeadas

- [ ] **Task 2 — `AesAttributeConverter` (R3 — componente único)** (AC-2)
  - [ ] Criar `infrastructure/persistence/AesAttributeConverter.java` que implementa `JPA AttributeConverter<String, String>`
  - [ ] Lê `ENCRYPTION_KEY` via `@Value("${ENCRYPTION_KEY}")` no construtor/campo
  - [ ] Algoritmo: `AES/GCM/NoPadding` com IV aleatório de 12 bytes por operação de escrita
  - [ ] `convertToDatabaseColumn(String attr)`: Base64( IV + ciphertext ) — retorna `null` se `attr == null`
  - [ ] `convertToEntityAttribute(String dbData)`: extrai IV + decifra — retorna `null` se `dbData == null`
  - [ ] Anotado com `@Component` para injeção de `@Value`; registrado como converter JPA via `@Converter`
  - [ ] **Não criar nenhuma outra classe com lógica AES — este é o único ponto de criptografia do projeto**

- [ ] **Task 3 — Entidade `Cliente`** (AC-1, AC-2)
  - [ ] Criar `domain/entities/Cliente.java`
  - [ ] Campos criptografados com `@Convert(converter = AesAttributeConverter.class)`:
    - `@Column(name = "nome_enc") private String nome`
    - `@Column(name = "telefone_enc") private String telefone`
    - `@Column(name = "data_nascimento_enc") private String dataNascimento`
  - [ ] Campos de consentimento: `consentimentoLgpd`, `consentimentoTimestamp`, `consentimentoVersaoTermo`, `consentimentoUsuarioId`
  - [ ] Campo `anonimizado` (boolean, default false) — usado na Story 6.2
  - [ ] `@PrePersist` para `createdAt`

- [ ] **Task 4 — `ClienteRepository`** (AC-4, AC-6)
  - [ ] Criar `domain/repositories/ClienteRepository.java`
  - [ ] `Page<Cliente> findAllByEmpresaIdAndAnonimizadoFalse(Long empresaId, Pageable pageable)`
  - [ ] `Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId)` — isolamento multi-tenant
  - [ ] **Nota:** busca por nome/telefone não usa query SQL — é feita em memória no service (AC-2: campos criptografados não são pesquisáveis em SQL)

- [ ] **Task 5 — DTOs** (AC-3, AC-4)
  - [ ] Criar `api/dtos/request/CriarClienteRequest.java`: `@NotBlank nome`, `telefone` (nullable), `dataNascimento` (nullable, String `"YYYY-MM-DD"`), `@AssertTrue(message = "Consentimento LGPD é obrigatório") consentimentoLgpd`
  - [ ] Criar `api/dtos/response/ClienteResponse.java`: `id`, `nome`, `telefone`, `dataNascimento`, `consentimentoLgpd`, `consentimentoTimestamp`, `anonimizado`, `createdAt`

- [ ] **Task 6 — `ClienteService`** (AC-4, AC-5, AC-6)
  - [ ] Criar `domain/services/ClienteService.java`
  - [ ] `criar(CriarClienteRequest, Long empresaId, Long usuarioId)` — `@Transactional`:
    - Verificar `consentimentoLgpd == true` → `IllegalArgumentException` se false
    - Criar `Cliente` com `empresaId`, consentimento preenchido (`timestamp` = now UTC, `versaoTermo` = `TERMO_VERSAO_VIGENTE`, `usuarioId`)
    - Os campos `nome`, `telefone`, `dataNascimento` são armazenados na entidade em texto plano — o `AesAttributeConverter` criptografa automaticamente ao persistir
    - Retornar `ClienteResponse`
  - [ ] `buscarPorId(Long id, Long empresaId)` → `ClienteResponse` ou 404

- [ ] **Task 7 — `ClienteController`** (AC-3–AC-6)
  - [ ] Substituir stub da Story 2.2 com implementação real
  - [ ] `@RestController @RequestMapping("/api/v1/clientes") @PreAuthorize("hasRole('STAFF')")`
  - [ ] `POST /` → 201
  - [ ] `GET /{id}` → 200 ou 404
  - [ ] Usar `SecurityUtils.getEmpresaId()` e `SecurityUtils.getUserId()`

- [ ] **Task 8 — Frontend — Formulário de Cadastro** (AC-3, AC-4, AC-5)
  - [ ] Criar `src/pages/ClienteNovoPage.tsx` com React Hook Form + Zod
  - [ ] Schema Zod:
    ```ts
    z.object({
      nome: z.string().min(1, 'Nome é obrigatório'),
      telefone: z.string().optional(),
      dataNascimento: z.string().optional(),
      consentimentoLgpd: z.literal(true, {
        errorMap: () => ({ message: 'Consentimento LGPD é obrigatório' })
      }),
    })
    ```
  - [ ] Checkbox com link para `/politica` em nova aba
  - [ ] Botão "Cadastrar" desabilitado enquanto checkbox desmarcado
  - [ ] Após criação: `toast.success('Cliente cadastrado com sucesso')` + navegar para `/clientes`

- [ ] **Task 9 — `ENCRYPTION_KEY` no ambiente** (AC-2)
  - [ ] Verificar que `ENCRYPTION_KEY` está listado em `application.properties` como `${ENCRYPTION_KEY}`
  - [ ] Adicionar à documentação de variáveis de ambiente no README ou `.env.example`:
    ```
    ENCRYPTION_KEY=<gerado com openssl rand -hex 32>
    ```
  - [ ] Para testes: adicionar `ENCRYPTION_KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef` no `application-test.properties` (64 hex chars = 32 bytes = AES-256)

- [ ] **Task 10 — Testes** (AC-2, AC-4, AC-5, AC-6)
  - [ ] Teste unitário `AesAttributeConverterTest`:
    - Round-trip: `encrypt → decrypt` retorna string original
    - IVs diferentes em cada chamada (dois `convertToDatabaseColumn(mesmoTexto)` retornam strings diferentes)
    - `null` entra → `null` sai
  - [ ] Teste unitário `ClienteServiceTest`:
    - Criar com consentimento: campos preenchidos corretamente
    - `consentimentoLgpd = false`: lança exceção
    - Isolamento: buscar id de outra empresa → 404
  - [ ] Teste de integração `ClienteControllerIT`:
    - POST → 201; campos no banco estão criptografados (valor diferente do original); GET descriptografa corretamente
    - `PROFISSIONAL` → 403

## Dev Notes

### ⚠️ Migration com timestamp (não V7 sequencial) — R6

```sql
-- CORRETO: V202506041900__create_clientes.sql
-- ERRADO:  V7__create_clientes.sql
```

### Migration SQL

```sql
-- V202506041900__create_clientes.sql (usar timestamp real)

CREATE TABLE clientes (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    empresa_id                  BIGINT       NOT NULL,
    nome_enc                    TEXT         NOT NULL,
    telefone_enc                TEXT         NULL,
    data_nascimento_enc         TEXT         NULL,
    consentimento_lgpd          BOOLEAN      NOT NULL DEFAULT FALSE,
    consentimento_timestamp     DATETIME     NULL,
    consentimento_versao_termo  VARCHAR(50)  NULL,
    consentimento_usuario_id    BIGINT       NULL,
    anonimizado                 BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_clientes_empresa  FOREIGN KEY (empresa_id)               REFERENCES empresas(id),
    CONSTRAINT fk_clientes_usuario  FOREIGN KEY (consentimento_usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_clientes_empresa_id ON clientes (empresa_id);
```

### `AesAttributeConverter.java` — Implementação Completa (R3)

```java
// infrastructure/persistence/AesAttributeConverter.java
@Component
@Converter
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;

    public AesAttributeConverter(@Value("${ENCRYPTION_KEY}") String hexKey) {
        byte[] keyBytes = HexFormat.of().parseHex(hexKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                        new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            // Formato: Base64( IV(12 bytes) + ciphertext )
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Erro ao criptografar campo", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                        new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Erro ao descriptografar campo", e);
        }
    }
}
```

> **Segurança:**
> - `AES/GCM/NoPadding` provê autenticidade além de confidencialidade (authenticated encryption)
> - IV aleatório de 12 bytes por operação garante que dois clientes com mesmo nome produzem ciphertexts diferentes (sem padrões identificáveis)
> - `ENCRYPTION_KEY` deve ser gerado com `openssl rand -hex 32` e armazenado como secret no Railway/GitHub Actions — nunca hardcoded

### Entidade `Cliente.java`

```java
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // Campos criptografados — AesAttributeConverter aplicado automaticamente pelo JPA
    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "nome_enc", nullable = false, columnDefinition = "TEXT")
    private String nome;

    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "telefone_enc", columnDefinition = "TEXT")
    private String telefone;

    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "data_nascimento_enc", columnDefinition = "TEXT")
    private String dataNascimento;

    // Consentimento LGPD
    @Column(name = "consentimento_lgpd", nullable = false)
    private boolean consentimentoLgpd = false;

    @Column(name = "consentimento_timestamp")
    private Instant consentimentoTimestamp;

    @Column(name = "consentimento_versao_termo", length = 50)
    private String consentimentoVersaoTermo;

    @Column(name = "consentimento_usuario_id")
    private Long consentimentoUsuarioId;

    @Column(nullable = false)
    private boolean anonimizado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { this.createdAt = Instant.now(); }
}
```

### `TERMO_VERSAO_VIGENTE` — Constante do Sistema

```java
// domain/constants/SistemaConstants.java
public final class SistemaConstants {
    private SistemaConstants() {}
    public static final String TERMO_VERSAO_VIGENTE = "v1.0";
}
```

> Esta é a constante Java correspondente à constante TypeScript `TERMO_VERSAO_VIGENTE = 'v1.0'` criada na Story 1.4. Ambas devem ter o mesmo valor. Ao atualizar o termo de privacidade no futuro, atualizar as duas.

### `ClienteService.criar()` — Preenchimento de Consentimento

```java
@Transactional
public ClienteResponse criar(CriarClienteRequest req, Long empresaId, Long usuarioId) {
    if (!req.isConsentimentoLgpd()) {
        throw new IllegalArgumentException("Consentimento LGPD é obrigatório para cadastrar cliente");
    }

    Empresa empresa = empresaRepository.findById(empresaId)
        .orElseThrow(RecursoNaoEncontradoException::new);

    Cliente cliente = new Cliente();
    cliente.setEmpresa(empresa);

    // Campos em texto plano — AesAttributeConverter criptografa ao persistir
    cliente.setNome(req.getNome());
    cliente.setTelefone(req.getTelefone());
    cliente.setDataNascimento(req.getDataNascimento());

    // Consentimento LGPD
    cliente.setConsentimentoLgpd(true);
    cliente.setConsentimentoTimestamp(Instant.now());
    cliente.setConsentimentoVersaoTermo(SistemaConstants.TERMO_VERSAO_VIGENTE);
    cliente.setConsentimentoUsuarioId(usuarioId);

    return toResponse(clienteRepository.save(cliente));
}
```

### Busca por Nome — Em Memória (não em SQL)

```java
// ClienteService.listar() — busca em memória (Story 3.6 implementa isso):
public Page<ClienteResponse> listar(Long empresaId, String busca, Pageable pageable) {
    // Carregar todos os registros não-anonimizados da empresa
    List<Cliente> todos = clienteRepository.findAllByEmpresaIdAndAnonimizadoFalse(empresaId);

    // Filtrar em memória (campos são descriptografados pelo converter ao ler)
    Stream<Cliente> filtrados = todos.stream();
    if (busca != null && !busca.isBlank()) {
        String termoBusca = busca.toLowerCase().strip();
        filtrados = filtrados.filter(c ->
            (c.getNome() != null && c.getNome().toLowerCase().contains(termoBusca)) ||
            (c.getTelefone() != null && c.getTelefone().contains(termoBusca))
        );
    }

    // Paginação manual após filtro
    List<ClienteResponse> resultado = filtrados.map(this::toResponse).toList();
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), resultado.size());
    return new PageImpl<>(resultado.subList(start, end), pageable, resultado.size());
}
```

> **Nota de performance:** a busca em memória é aceitável para o volume esperado de clínicas pequenas/médias (centenas de clientes por empresa). Não usar `LIKE` em SQL — os valores estão criptografados no banco. Esta lógica é implementada na Story 3.6; a Story 3.5 apenas prepara a estrutura.

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__create_clientes.sql` | NEW | Timestamp real; sem colunas de saúde |
| `infrastructure/persistence/AesAttributeConverter.java` | NEW | **Único** componente AES do projeto (R3) |
| `domain/entities/Cliente.java` | NEW | `@Convert` nos 3 campos sensíveis |
| `domain/repositories/ClienteRepository.java` | NEW | |
| `domain/constants/SistemaConstants.java` | NEW | `TERMO_VERSAO_VIGENTE = "v1.0"` |
| `api/dtos/request/CriarClienteRequest.java` | NEW | `@AssertTrue` para consentimento |
| `api/dtos/response/ClienteResponse.java` | NEW | |
| `domain/services/ClienteService.java` | NEW | |
| `api/controllers/ClienteController.java` | **UPDATE** | Substituir stub da Story 2.2 |
| `src/pages/ClienteNovoPage.tsx` | NEW | RHF + Zod + checkbox LGPD |
| `application.properties` | **UPDATE** | Adicionar `ENCRYPTION_KEY=${ENCRYPTION_KEY}` |
| `application-test.properties` | **UPDATE** | Adicionar chave de teste (64 hex chars) |
| `test/.../AesAttributeConverterTest.java` | NEW | Round-trip, IVs únicos, null-safe |
| `test/.../ClienteServiceTest.java` | NEW | Unitários |
| `test/.../ClienteControllerIT.java` | NEW | Criptografia end-to-end |

### Referências

- [Source: epics.md#Story 3.5] — Acceptance Criteria completos e FR-011, FR-035, FR-037, FR-040; R3
- [Source: architecture.md#Segurança e LGPD] — AES-256 via JPA AttributeConverter; `ENCRYPTION_KEY` como secret
- [Source: epics.md#Story 1.2 AC-R6] — Flyway timestamp naming
- [Source: 1-4-politica-privacidade.md] — `TERMO_VERSAO_VIGENTE = 'v1.0'` (versão TS)
- [Source: architecture.md#Regras de Processo] — empresaId do JWT; getUserId()

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
