# Story 3.4: Disponibilidade dos Profissionais por Dia da Semana

Status: ready-for-dev

## Story

Como Admin Empresa,
Quero configurar os dias e horários de atendimento de cada profissional,
Para que o sistema valide automaticamente a disponibilidade ao criar agendamentos.

## Acceptance Criteria

**AC-1 — Migration V6: tabela `disponibilidades`**
- Given a migration `V{yyyyMMddHHmm}__create_disponibilidades.sql` é aplicada
- When o sistema inicializa
- Then a tabela `disponibilidades` existe com colunas:
  - `id` BIGINT PK AUTO_INCREMENT
  - `profissional_id` BIGINT FK NOT NULL → `profissionais.id`
  - `empresa_id` BIGINT FK NOT NULL → `empresas.id`
  - `dia_semana` ENUM('SEGUNDA','TERCA','QUARTA','QUINTA','SEXTA','SABADO','DOMINGO') NOT NULL
  - `horario_inicio` TIME NOT NULL
  - `horario_fim` TIME NOT NULL
  - `inicio_intervalo` TIME NULL
  - `fim_intervalo` TIME NULL
- And índice `idx_disponibilidades_profissional_id` existe
- And constraint UNIQUE em `(profissional_id, dia_semana)`

**AC-2 — Consultar disponibilidade de um profissional**
- Given o Admin Empresa acessa `GET /api/v1/profissionais/{id}/disponibilidade`
- When a tela carrega
- Then retorna array com todos os slots de disponibilidade do profissional (0 a 7 dias)
- And apenas profissional da empresa do JWT pode ser consultado — HTTP 404 se de outra empresa

**AC-3 — Salvar disponibilidade (upsert completo)**
- Given o Admin Empresa envia `PUT /api/v1/profissionais/{id}/disponibilidade` com array de dias/horários
- When o backend persiste
- Then substitui TODA a disponibilidade do profissional pelo novo array (delete + insert) (FR-013)
- And `horario_fim` deve ser posterior a `horario_inicio` — HTTP 400 se inválido
- And se `inicioIntervalo` e `fimIntervalo` são fornecidos, ambos devem estar dentro da faixa `horarioInicio`–`horarioFim` — HTTP 400 se inválido
- And retorna HTTP 200 com a disponibilidade salva

**AC-4 — Remover dia da disponibilidade**
- Given o Admin Empresa envia `PUT` omitindo um dia que existia anteriormente
- When o backend persiste
- Then a disponibilidade desse dia é excluída (consequência natural do upsert completo)

**AC-5 — Indisponibilidade bloqueia agendamento**
- Given um profissional não tem disponibilidade cadastrada para um dia específico
- When o sistema tenta criar um agendamento nesse dia (Epic 4 — Story 4.2)
- Then o backend retorna HTTP 409 `"Profissional sem disponibilidade cadastrada"` (FR-023)
- And esta validação é responsabilidade da Story 4.2 — a Story 3.4 apenas estabelece a estrutura de dados que a Story 4.2 consultará

**AC-6 — Grade semanal no frontend**
- Given o Admin Empresa acessa `/configuracoes/disponibilidade`
- When seleciona um profissional da empresa
- Then exibe grade semanal com os 7 dias, cada um com toggle ativo/inativo e campos de horário
- And dias sem disponibilidade mostram o toggle desligado
- And formulário usa `react-hook-form` com validação de horários

## Tasks / Subtasks

- [ ] **Task 1 — Migration V6** (AC-1)
  - [ ] Criar `resources/db/migration/V{yyyyMMddHHmm}__create_disponibilidades.sql`
  - [ ] Usar timestamp real no momento da implementação
  - [ ] SQL com tabela + constraint UNIQUE + índice

- [ ] **Task 2 — Enum `DiaSemana`** (AC-1)
  - [ ] Criar `domain/enums/DiaSemana.java`: `SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA, SABADO, DOMINGO`
  - [ ] Anotado com `@JsonProperty` para serialização correta no JSON

- [ ] **Task 3 — Entidade `Disponibilidade`** (AC-1)
  - [ ] Criar `domain/entities/Disponibilidade.java` com anotações JPA
  - [ ] Campos: `id`, `profissional` (ManyToOne lazy), `empresa` (ManyToOne lazy), `diaSemana` (Enum), `horarioInicio` (LocalTime), `horarioFim` (LocalTime), `inicioIntervalo` (LocalTime, nullable), `fimIntervalo` (LocalTime, nullable)
  - [ ] `@Enumerated(EnumType.STRING)` em `diaSemana`
  - [ ] `@Column(columnDefinition = "TIME")` em campos de horário

- [ ] **Task 4 — `DisponibilidadeRepository`** (AC-2, AC-3)
  - [ ] Criar `domain/repositories/DisponibilidadeRepository.java`
  - [ ] `List<Disponibilidade> findAllByProfissionalId(Long profissionalId)`
  - [ ] `void deleteAllByProfissionalId(Long profissionalId)` — usado no upsert
  - [ ] `boolean existsByProfissionalIdAndDiaSemana(Long profissionalId, DiaSemana dia)` — usado na Story 4.2

- [ ] **Task 5 — DTOs** (AC-2, AC-3)
  - [ ] Criar `api/dtos/request/DisponibilidadeSlotRequest.java`: `@NotNull diaSemana`, `@NotNull horarioInicio`, `@NotNull horarioFim`, `inicioIntervalo` (nullable), `fimIntervalo` (nullable)
  - [ ] Criar `api/dtos/response/DisponibilidadeSlotResponse.java`: mesmos campos + `id`
  - [ ] Array de slots como `List<DisponibilidadeSlotRequest>` no request

- [ ] **Task 6 — `DisponibilidadeService`** (AC-2–AC-4)
  - [ ] Criar `domain/services/DisponibilidadeService.java`
  - [ ] `buscar(Long profissionalId, Long empresaId)` → `List<DisponibilidadeSlotResponse>`
    - Verifica que profissional pertence à empresa do JWT via `profissionalRepository.findByIdAndEmpresaId()`
  - [ ] `salvar(Long profissionalId, Long empresaId, List<DisponibilidadeSlotRequest>)` — `@Transactional`:
    - Validar cada slot: `horarioFim > horarioInicio`; se intervalo fornecido, `inicioIntervalo >= horarioInicio` e `fimIntervalo <= horarioFim`
    - `deleteAllByProfissionalId(profissionalId)` — limpa tudo
    - Salvar todos os novos slots em batch (`saveAll`)
    - Retornar lista salva

- [ ] **Task 7 — Endpoints em `ProfissionalController`** (AC-2, AC-3)
  - [ ] Adicionar ao `ProfissionalController.java` (Story 2.4):
    - `GET /{id}/disponibilidade` → `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` → 200
    - `PUT /{id}/disponibilidade` → `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` → 200
  - [ ] Também expor `GET /{id}/disponibilidade` para `STAFF` e `PROFISSIONAL` (consultado ao criar agendamentos):
    `@PreAuthorize("hasAnyRole('ADMIN_EMPRESA','STAFF','PROFISSIONAL')")`

- [ ] **Task 8 — Frontend — `DisponibilidadeTab.tsx`** (AC-6)
  - [ ] Criar `src/pages/configuracoes/DisponibilidadeTab.tsx`
  - [ ] Seletor de profissional no topo (usa `GET /api/v1/profissionais` — lista da empresa)
  - [ ] Grade de 7 dias: cada linha com toggle ativo + campos `horario_inicio`, `horario_fim`, `inicio_intervalo`, `fim_intervalo`
  - [ ] Campos de intervalo visíveis apenas quando toggle de intervalo está ativo
  - [ ] Validação frontend: `horarioFim > horarioInicio`; intervalo dentro da faixa
  - [ ] Botão "Salvar Disponibilidade" que dispara `PUT` com array completo
  - [ ] Skeleton durante carregamento; `toast.success('Disponibilidade salva ✓')`
  - [ ] Adicionar tab a `ConfiguracoesPage.tsx`

- [ ] **Task 9 — Testes** (AC-2, AC-3)
  - [ ] Teste unitário `DisponibilidadeServiceTest`:
    - Salvar: delete + insert, retorna slots salvos
    - `horarioFim <= horarioInicio`: HTTP 400
    - Intervalo fora da faixa: HTTP 400
    - Profissional de outra empresa: HTTP 404
    - Remover dia: não presente no resultado após salvar sem aquele dia
  - [ ] Teste de integração `DisponibilidadeControllerIT`:
    - PUT → 200; GET → retorna o que foi salvo; STAFF → 403 em PUT (apenas consulta GET)

## Dev Notes

### ⚠️ Migration com timestamp (não V6 sequencial) — R6

```sql
-- CORRETO: V202506041800__create_disponibilidades.sql
-- ERRADO:  V6__create_disponibilidades.sql
```

### Migration SQL

```sql
-- V202506041800__create_disponibilidades.sql (usar timestamp real)

CREATE TABLE disponibilidades (
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    profissional_id   BIGINT   NOT NULL,
    empresa_id        BIGINT   NOT NULL,
    dia_semana        ENUM('SEGUNDA','TERCA','QUARTA','QUINTA','SEXTA','SABADO','DOMINGO') NOT NULL,
    horario_inicio    TIME     NOT NULL,
    horario_fim       TIME     NOT NULL,
    inicio_intervalo  TIME     NULL,
    fim_intervalo     TIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_disponibilidade_profissional_dia UNIQUE (profissional_id, dia_semana),
    CONSTRAINT fk_disponibilidades_profissional FOREIGN KEY (profissional_id) REFERENCES profissionais(id),
    CONSTRAINT fk_disponibilidades_empresa      FOREIGN KEY (empresa_id)      REFERENCES empresas(id)
);

CREATE INDEX idx_disponibilidades_profissional_id ON disponibilidades (profissional_id);
```

> **H2:** `ENUM` não é suportado nativamente no H2. Para testes CI com H2, usar `VARCHAR(10)` com `CHECK` constraint. Alternativa: usar `@Enumerated(EnumType.STRING)` no JPA e deixar o Hibernate criar o DDL no H2 com `VARCHAR`. Para o Flyway em ambiente de teste, criar perfil `src/test/resources/db/migration/` com a migration adaptada para H2, ou configurar `spring.flyway.locations=classpath:db/migration,classpath:db/migration-h2` no `application-test.properties`.

### Entidade `Disponibilidade.java`

```java
@Entity
@Table(name = "disponibilidades",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_disponibilidade_profissional_dia",
           columnNames = {"profissional_id", "dia_semana"}))
public class Disponibilidade {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DiaSemana diaSemana;

    @Column(name = "horario_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "horario_fim", nullable = false)
    private LocalTime horarioFim;

    @Column(name = "inicio_intervalo")
    private LocalTime inicioIntervalo;

    @Column(name = "fim_intervalo")
    private LocalTime fimIntervalo;
}
```

### `DisponibilidadeService.salvar()` — Upsert Completo

```java
@Transactional
public List<DisponibilidadeSlotResponse> salvar(Long profissionalId,
                                                 Long empresaId,
                                                 List<DisponibilidadeSlotRequest> slots) {
    // 1. Verificar isolamento
    Profissional profissional = profissionalRepository
        .findByIdAndEmpresaId(profissionalId, empresaId)
        .orElseThrow(RecursoNaoEncontradoException::new);

    Empresa empresa = profissional.getEmpresa();

    // 2. Validar cada slot
    for (var slot : slots) {
        if (!slot.getHorarioFim().isAfter(slot.getHorarioInicio())) {
            throw new IllegalArgumentException(
                "horarioFim deve ser posterior a horarioInicio no dia " + slot.getDiaSemana());
        }
        if (slot.getInicioIntervalo() != null || slot.getFimIntervalo() != null) {
            if (slot.getInicioIntervalo() == null || slot.getFimIntervalo() == null) {
                throw new IllegalArgumentException(
                    "inicioIntervalo e fimIntervalo devem ser fornecidos juntos");
            }
            if (slot.getInicioIntervalo().isBefore(slot.getHorarioInicio()) ||
                slot.getFimIntervalo().isAfter(slot.getHorarioFim())) {
                throw new IllegalArgumentException(
                    "Intervalo fora da faixa de atendimento no dia " + slot.getDiaSemana());
            }
        }
    }

    // 3. Upsert: delete all + insert all
    disponibilidadeRepository.deleteAllByProfissionalId(profissionalId);

    List<Disponibilidade> entidades = slots.stream().map(slot -> {
        Disponibilidade d = new Disponibilidade();
        d.setProfissional(profissional);
        d.setEmpresa(empresa);
        d.setDiaSemana(slot.getDiaSemana());
        d.setHorarioInicio(slot.getHorarioInicio());
        d.setHorarioFim(slot.getHorarioFim());
        d.setInicioIntervalo(slot.getInicioIntervalo());
        d.setFimIntervalo(slot.getFimIntervalo());
        return d;
    }).toList();

    return disponibilidadeRepository.saveAll(entidades)
        .stream().map(this::toResponse).toList();
}
```

### Grade Semanal no Frontend

```tsx
// Ordem dos dias e labels em pt-BR
const DIAS: { value: DiaSemana; label: string }[] = [
  { value: 'SEGUNDA',  label: 'Segunda-feira' },
  { value: 'TERCA',    label: 'Terça-feira'   },
  { value: 'QUARTA',   label: 'Quarta-feira'  },
  { value: 'QUINTA',   label: 'Quinta-feira'  },
  { value: 'SEXTA',    label: 'Sexta-feira'   },
  { value: 'SABADO',   label: 'Sábado'        },
  { value: 'DOMINGO',  label: 'Domingo'       },
]

// Estado local: mapa de dia → slot (undefined = dia desativado)
type SlotForm = {
  horarioInicio: string  // 'HH:mm'
  horarioFim: string
  temIntervalo: boolean
  inicioIntervalo?: string
  fimIntervalo?: string
}
```

### Nota: Uso na Story 4.2 (Criar Agendamento)

A Story 4.2 consultará a disponibilidade via:
```java
boolean existsByProfissionalIdAndDiaSemana(Long profissionalId, DiaSemana dia)
```
e também validará que o horário solicitado está dentro da faixa `horarioInicio`–`horarioFim` (considerando o intervalo). Essa lógica pertence à Story 4.2 — esta story apenas fornece a estrutura de dados.

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `resources/db/migration/V{yyyyMMddHHmm}__create_disponibilidades.sql` | NEW | ENUM + UNIQUE constraint |
| `domain/enums/DiaSemana.java` | NEW | 7 dias |
| `domain/entities/Disponibilidade.java` | NEW | `@Enumerated(EnumType.STRING)` |
| `domain/repositories/DisponibilidadeRepository.java` | NEW | `deleteAllByProfissionalId`, `existsByProfissionalIdAndDiaSemana` |
| `api/dtos/request/DisponibilidadeSlotRequest.java` | NEW | |
| `api/dtos/response/DisponibilidadeSlotResponse.java` | NEW | |
| `domain/services/DisponibilidadeService.java` | NEW | Upsert completo |
| `api/controllers/ProfissionalController.java` | **UPDATE** | Adicionar GET/PUT `/{id}/disponibilidade` |
| `src/pages/configuracoes/DisponibilidadeTab.tsx` | NEW | Grade semanal com toggles |
| `src/pages/ConfiguracoesPage.tsx` | **UPDATE** | Adicionar tab de Disponibilidade |
| `test/.../DisponibilidadeServiceTest.java` | NEW | Unitários |
| `test/.../DisponibilidadeControllerIT.java` | NEW | Integração |

### Referências

- [Source: epics.md#Story 3.4] — Acceptance Criteria completos e FR-013, FR-023
- [Source: epics.md#Story 1.2 AC-R6] — Flyway timestamp naming
- [Source: architecture.md#Banco de Dados] — Constraints nomeadas, UTC
- [Source: architecture.md#Regras de Processo] — empresaId do JWT
- [Source: 3-1-componentes-base-ui.md] — skeleton + `useToast`
- [Source: 3-3-tipos-atendimento.md] — padrão de sub-tabs em ConfiguracoesPage

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
