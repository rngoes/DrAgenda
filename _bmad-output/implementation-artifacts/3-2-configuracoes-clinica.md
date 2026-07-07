# Story 3.2: Configurações da Clínica — Dados Básicos e Fuso Horário

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero configurar os dados básicos e o fuso horário da minha clínica,
Para que o sistema exiba horários corretos para todos os usuários e a clínica esteja identificada corretamente.

## Acceptance Criteria

**AC-1 — Migration V4: colunas adicionais em `empresas`**
- Given a migration `V{yyyyMMddHHmm}__add_configuracoes_empresa.sql` é aplicada
- When o sistema inicializa
- Then as colunas existem na tabela `empresas`:
  - `telefone` VARCHAR(20) NULL
  - `endereco` VARCHAR(500) NULL
  - `fuso_horario` VARCHAR(50) NOT NULL DEFAULT `'America/Sao_Paulo'`
  - `updated_at` DATETIME NULL

**AC-2 — Formulário de configurações**
- Given o Admin Empresa acessa `/configuracoes/clinica`
- When a tela carrega via `GET /api/v1/configuracoes/clinica`
- Then exibe formulário pré-preenchido com: Nome da Clínica, Telefone de Contato, Endereço e Fuso Horário
- And o seletor de Fuso Horário oferece exatamente as opções:
  - `America/Sao_Paulo` — "Brasília (UTC-3)"
  - `America/Manaus` — "Manaus (UTC-4)"
  - `America/Belem` — "Belém (UTC-3)"
  - `America/Fortaleza` — "Fortaleza (UTC-3)"
  - `America/Noronha` — "Fernando de Noronha (UTC-2)"
- And enquanto carrega: skeleton screen substitui o formulário (padrão Story 3.1)

**AC-3 — Salvar configurações**
- Given o Admin Empresa submete `PUT /api/v1/configuracoes/clinica` com dados válidos
- When o backend persiste
- Then retorna HTTP 200 com os dados atualizados
- And `fuso_horario` é armazenado como string IANA (ex: `"America/Sao_Paulo"`) (FR-041)
- And `updated_at` é atualizado para o momento da persistência
- And o frontend exibe `toast.success('Configurações salvas ✓')` (padrão Story 3.1)

**AC-4 — Conversão de fuso horário no frontend**
- Given o fuso configurado é `America/Manaus` (UTC-4)
- When qualquer horário é exibido na interface
- Then o frontend converte o UTC recebido da API para o fuso configurado antes de exibir
- And todos os horários são armazenados em UTC no banco de dados

**AC-5 — Isolamento multi-tenant**
- Given o Admin Empresa tenta acessar `/api/v1/configuracoes/clinica` de outra empresa via token forjado
- When o backend verifica `empresaId` do JWT
- Then retorna apenas os dados da empresa do token — nunca dados de outra empresa

## Tasks / Subtasks

- [ ] **Task 1 — Migration V4** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__add_configuracoes_empresa.sql`
  - [ ] Usar timestamp real no momento da implementação (ex: `V202506041600`)
  - [ ] Adicionar colunas: `telefone`, `endereco`, `fuso_horario`, `updated_at`
  - [ ] `fuso_horario` com DEFAULT `'America/Sao_Paulo'` (deve funcionar em H2 e MySQL)

- [ ] **Task 2 — Atualizar entidade `Empresa`** (AC-1)
  - [ ] Adicionar campos ao `domain/entities/Empresa.java`: `telefone`, `endereco`, `fusoHorario`, `updatedAt`
  - [ ] `@PreUpdate` para setar `updatedAt` automaticamente
  - [ ] `fusoHorario` com valor default `"America/Sao_Paulo"` na entidade

- [ ] **Task 3 — DTOs** (AC-2, AC-3)
  - [ ] Criar `api/dtos/request/ConfiguracoesClinicaRequest.java`: `@NotBlank nome`, `telefone` (nullable), `endereco` (nullable), `@NotBlank fusoHorario` com validação de valor permitido
  - [ ] Criar `api/dtos/response/ConfiguracoesClinicaResponse.java`: `id`, `nome`, `telefone`, `endereco`, `fusoHorario`, `updatedAt`
  - [ ] Criar `domain/enums/FusoHorarioBrasileiro.java` com as 5 opções IANA válidas — validar no DTO

- [ ] **Task 4 — `ConfiguracoesService`** (AC-3, AC-5)
  - [ ] Criar `domain/services/ConfiguracoesService.java`
  - [ ] `buscarConfiguracoes(Long empresaId)` → `ConfiguracoesClinicaResponse`
  - [ ] `salvarConfiguracoes(Long empresaId, ConfiguracoesClinicaRequest)` → `ConfiguracoesClinicaResponse` — `@Transactional`
  - [ ] Verifica que a empresa existe e pertence ao usuário (via `empresaId` do JWT — já garantido pelo filtro)

- [ ] **Task 5 — `ConfiguracoesController` (substituir stub da Story 2.2)** (AC-2, AC-3, AC-5)
  - [ ] Substituir stub com implementação real
  - [ ] `@RestController @RequestMapping("/api/v1/configuracoes") @PreAuthorize("hasRole('ADMIN_EMPRESA')")`
  - [ ] `GET /clinica` → 200 com dados da empresa do JWT
  - [ ] `PUT /clinica` → 200 com dados atualizados
  - [ ] Usar `SecurityUtils.getEmpresaId()` — nunca path param

- [ ] **Task 6 — Frontend — `ConfiguracoesPage.tsx`** (AC-2, AC-3, AC-4)
  - [ ] Criar `src/pages/ConfiguracoesPage.tsx` — layout com sub-navegação lateral (ou tabs)
  - [ ] Criar `src/pages/configuracoes/ClinicaTab.tsx` com React Hook Form + Zod
  - [ ] Usar `useQuery` para carregar via `GET /api/v1/configuracoes/clinica` com skeleton enquanto `isLoading`
  - [ ] Usar `useMutation` para `PUT /api/v1/configuracoes/clinica` com `toast.success('Configurações salvas ✓')` no `onSuccess`
  - [ ] Seletor de fuso horário: `<select>` com as 5 opções mapeadas
  - [ ] Armazenar `fusoHorario` no contexto global (localStorage ou React Context) para uso na conversão de horários (AC-4)

- [ ] **Task 7 — Utilitário de conversão de fuso horário** (AC-4)
  - [ ] Criar `src/shared/utils/datetime.ts` com função:
    ```ts
    export function formatarHorario(utcString: string, fusoHorario: string): string {
      return new Intl.DateTimeFormat('pt-BR', {
        timeZone: fusoHorario,
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(utcString))
    }

    export function formatarDataHora(utcString: string, fusoHorario: string): string {
      return new Intl.DateTimeFormat('pt-BR', {
        timeZone: fusoHorario,
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      }).format(new Date(utcString))
    }
    ```
  - [ ] Usar `Intl.DateTimeFormat` — sem dependência de biblioteca externa (disponível nativamente)
  - [ ] Criar `src/shared/hooks/useFusoHorario.ts` que lê o fuso do contexto/localStorage e retorna as funções de formatação já configuradas

- [ ] **Task 8 — Testes** (AC-3, AC-4, AC-5)
  - [ ] Teste unitário `ConfiguracoesServiceTest`: buscar e salvar; fuso inválido retorna 400
  - [ ] Teste unitário `datetime.test.ts`: conversão de UTC para fusos brasileiros
  - [ ] Teste de integração `ConfiguracoesControllerIT`: GET/PUT → 200; STAFF → 403

## Dev Notes

### ⚠️ Migration com timestamp (não V4 sequencial) — R6

```sql
-- CORRETO:
-- V202506041600__add_configuracoes_empresa.sql (timestamp real)

-- ERRADO:
-- V4__add_configuracoes_empresa.sql
```

### Migration SQL

```sql
-- V202506041600__add_configuracoes_empresa.sql (usar timestamp real)

ALTER TABLE empresas
  ADD COLUMN telefone    VARCHAR(20)  NULL,
  ADD COLUMN endereco    VARCHAR(500) NULL,
  ADD COLUMN fuso_horario VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
  ADD COLUMN updated_at  DATETIME     NULL;
```

> **H2 compatibility:** H2 aceita `DEFAULT 'America/Sao_Paulo'` em `VARCHAR`. Sem problemas para testes de CI.

### `FusoHorarioBrasileiro.java` — Enum de Validação

```java
// domain/enums/FusoHorarioBrasileiro.java
public enum FusoHorarioBrasileiro {
    BRASILIA("America/Sao_Paulo"),
    MANAUS("America/Manaus"),
    BELEM("America/Belem"),
    FORTALEZA("America/Fortaleza"),
    NORONHA("America/Noronha");

    private final String zoneId;

    FusoHorarioBrasileiro(String zoneId) { this.zoneId = zoneId; }
    public String getZoneId() { return zoneId; }

    public static boolean isValido(String zoneId) {
        return Arrays.stream(values()).anyMatch(f -> f.zoneId.equals(zoneId));
    }
}
```

### `ConfiguracoesClinicaRequest.java` — Validação do Fuso

```java
// Validação customizada para aceitar apenas fusos brasileiros:
public class ConfiguracoesClinicaRequest {

    @NotBlank
    private String nome;

    private String telefone;
    private String endereco;

    @NotBlank
    private String fusoHorario;

    // Validação no service (não via Bean Validation para simplicidade):
    // ConfiguracoesService chama FusoHorarioBrasileiro.isValido(req.getFusoHorario())
    // e lança IllegalArgumentException se inválido
}
```

### `ConfiguracoesService.java`

```java
@Service
@RequiredArgsConstructor
public class ConfiguracoesService {

    private final EmpresaRepository empresaRepository;

    public ConfiguracoesClinicaResponse buscarConfiguracoes(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        return toResponse(empresa);
    }

    @Transactional
    public ConfiguracoesClinicaResponse salvarConfiguracoes(Long empresaId,
                                                             ConfiguracoesClinicaRequest req) {
        if (!FusoHorarioBrasileiro.isValido(req.getFusoHorario())) {
            throw new IllegalArgumentException("Fuso horário inválido: " + req.getFusoHorario());
        }

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(RecursoNaoEncontradoException::new);

        empresa.setNome(req.getNome());
        empresa.setTelefone(req.getTelefone());
        empresa.setEndereco(req.getEndereco());
        empresa.setFusoHorario(req.getFusoHorario());
        // updatedAt setado pelo @PreUpdate da entidade

        return toResponse(empresaRepository.save(empresa));
    }

    private ConfiguracoesClinicaResponse toResponse(Empresa e) {
        return new ConfiguracoesClinicaResponse(
            e.getId(), e.getNome(), e.getTelefone(),
            e.getEndereco(), e.getFusoHorario(), e.getUpdatedAt()
        );
    }
}
```

### `useFusoHorario.ts` — Hook de Conversão

```ts
// src/shared/hooks/useFusoHorario.ts
import { formatarHorario, formatarDataHora } from '../utils/datetime'

const FUSO_STORAGE_KEY = 'fusoHorario'
const FUSO_DEFAULT = 'America/Sao_Paulo'

export function useFusoHorario() {
  const fuso = localStorage.getItem(FUSO_STORAGE_KEY) ?? FUSO_DEFAULT
  return {
    fuso,
    formatarHorario:  (utc: string) => formatarHorario(utc, fuso),
    formatarDataHora: (utc: string) => formatarDataHora(utc, fuso),
  }
}
```

> **Persistência do fuso:** após `PUT /api/v1/configuracoes/clinica`, salvar `localStorage.setItem('fusoHorario', data.fusoHorario)`. O hook `useFusoHorario` lê deste storage e é usado em todas as telas com horários a partir do Epic 4.

### Seletor de Fuso Horário no Frontend

```tsx
const FUSOS = [
  { value: 'America/Sao_Paulo', label: 'Brasília (UTC-3)' },
  { value: 'America/Manaus',    label: 'Manaus (UTC-4)' },
  { value: 'America/Belem',     label: 'Belém (UTC-3)' },
  { value: 'America/Fortaleza', label: 'Fortaleza (UTC-3)' },
  { value: 'America/Noronha',   label: 'Fernando de Noronha (UTC-2)' },
]

// No JSX:
<select {...register('fusoHorario')}>
  {FUSOS.map(f => (
    <option key={f.value} value={f.value}>{f.label}</option>
  ))}
</select>
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__add_configuracoes_empresa.sql` | NEW | Timestamp real; ALTER TABLE |
| `domain/entities/Empresa.java` | **UPDATE** | Adicionar campos + `@PreUpdate` |
| `domain/enums/FusoHorarioBrasileiro.java` | NEW | 5 zonas IANA válidas |
| `api/dtos/request/ConfiguracoesClinicaRequest.java` | NEW | |
| `api/dtos/response/ConfiguracoesClinicaResponse.java` | NEW | |
| `domain/services/ConfiguracoesService.java` | NEW | |
| `api/controllers/ConfiguracoesController.java` | **UPDATE** | Substituir stub da Story 2.2 |
| `src/pages/ConfiguracoesPage.tsx` | NEW | Layout com sub-navegação |
| `src/pages/configuracoes/ClinicaTab.tsx` | NEW | Formulário RHF + Zod |
| `src/shared/utils/datetime.ts` | NEW | `Intl.DateTimeFormat` — sem deps externas |
| `src/shared/hooks/useFusoHorario.ts` | NEW | Lê fuso do localStorage |
| `test/.../ConfiguracoesServiceTest.java` | NEW | Unitários |
| `test/.../ConfiguracoesControllerIT.java` | NEW | Integração: GET/PUT, 403 para STAFF |
| `src/shared/utils/datetime.test.ts` | NEW | Conversão UTC → fusos brasileiros |

### Referências

- [Source: epics.md#Story 3.2] — Acceptance Criteria completos e FR-041, FR-058
- [Source: epics.md#Story 1.2 AC-R6] — Flyway timestamp naming
- [Source: architecture.md#Banco de Dados] — UTC como padrão de armazenamento
- [Source: architecture.md#Regras de Processo] — empresaId do JWT
- [Source: 3-1-componentes-base-ui.md] — skeleton screens e `useToast` obrigatórios

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
