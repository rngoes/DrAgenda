# Story 4.4: NoShow Automático e Busca de Próximo Disponível

Status: ready-for-dev

## Story

Como sistema e como Staff,
Quero que agendamentos sem check-in sejam marcados automaticamente como NoShow e que o Staff possa encontrar vagas rapidamente,
Para que a agenda reflita os ausentes sem intervenção manual e a criação de agendamentos seja ágil.

## Acceptance Criteria

**AC-1 — `NoShowJob` marca ausentes automaticamente (FR-030)**
- Given o `NoShowJob` executa com `@Scheduled(fixedDelay = 60000)` (a cada 1 minuto)
- When a query encontra agendamentos com `status IN ('PENDENTE', 'CONFIRMADO') AND horario_fim <= NOW() - INTERVAL 30 MINUTE` (UTC)
- Then para cada agendamento encontrado: `status = NOSHOW`
- And registra em `historico_status` com `usuario_id = null` e `justificativa = 'NoShow automático'`
- And invalida `public_token` (set NULL) — link de autoatendimento expira junto

**AC-2 — Double-booking e NoShow independentes (FR-030)**
- Given dois agendamentos no mesmo slot (double-booking confirmado) e um deles já tem status PRESENTE
- When o `NoShowJob` executa após `horario_fim + 30 min`
- Then apenas o agendamento ainda PENDENTE/CONFIRMADO recebe NoShow — cada agendamento é avaliado independentemente

**AC-3 — Banner de profissional inativo (FR-009)**
- Given um agendamento futuro pertence a um profissional com `ativo = false`
- When o `AgendaPage` renderiza o card
- Then o `AgendamentoCard` exibe banner `⚠️ Profissional inativo — redistribuir manualmente` em cor warning (`#D97706`)
- And o agendamento NÃO é cancelado automaticamente

**AC-4 — Tela `/buscar` — seletores de filtro (UX-DR9, FR-029)**
- Given Staff acessa a aba "Buscar" na BottomTabBar
- When a tela `/buscar` renderiza
- Then exibe seletores: `[ Todos os profissionais ▾ ]` e `[ Qualquer tipo ▾ ]`
- And botão `[Buscar próximos horários]` dispara `GET /api/v1/agendamentos/proximos-disponiveis`

**AC-5 — Endpoint de próximos disponíveis (FR-029)**
- Given Staff acessa `GET /api/v1/agendamentos/proximos-disponiveis?profissionalId=&tipoId=`
- When o backend calcula vagas
- Then retorna no máximo 20 slots disponíveis nos próximos 30 dias, ordenados por data/horário
- And cada slot contém: `data`, `horarioInicio`, `horarioFim`, `profissional` (id + nome), `tipoAtendimento` (id + nome + duracaoMinutos)
- And considera: disponibilidade de cada profissional, ausência de agendamento existente (PENDENTE/CONFIRMADO/PRESENTE) no slot
- And se não houver vagas, retorna array vazio

**AC-6 — Tela `/buscar` — lista de vagas e agendamento rápido (UX-DR9)**
- Given o backend retorna slots disponíveis
- When a lista renderiza
- Then cada item exibe: data, horário, profissional, tipo de atendimento
- And botão `[Agendar]` por slot
- And ao tocar `[Agendar]`: Bottom Sheet abre com Data, Hora, Profissional e Tipo pré-preenchidos — apenas campo Cliente fica em branco

## Tasks / Subtasks

- [ ] **Task 1 — `@EnableScheduling` e `NoShowJob`** (AC-1, AC-2)
  - [ ] Adicionar `@EnableScheduling` ao `Application.java` principal (ou a uma `@Configuration`)
  - [ ] Criar `infrastructure/jobs/NoShowJob.java` com `@Component` + `@Scheduled(fixedDelay = 60000)`
  - [ ] Método `executar()` — `@Transactional`:
    - Query: `agendamentos` com `status IN (PENDENTE, CONFIRMADO)` e `horario_fim <= Instant.now().minus(30, MINUTES)`
    - Para cada agendamento: setar `status = NOSHOW` e `public_token = null`
    - Criar `HistoricoStatus` com `usuarioId = null` e `justificativa = 'NoShow automático'`
    - Salvar em batch: `agendamentoRepository.saveAll(...)` + `historicoRepository.saveAll(...)`

- [ ] **Task 2 — Query do NoShowJob no `AgendamentoRepository`** (AC-1)
  - [ ] Adicionar ao `AgendamentoRepository.java`:
    ```java
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.status IN :statusAlvo
          AND a.horarioFim <= :limite
        """)
    List<Agendamento> findParaNoShow(
        @Param("statusAlvo") List<StatusAgendamento> statusAlvo,
        @Param("limite") Instant limite
    );
    ```

- [ ] **Task 3 — Calcular próximos disponíveis** (AC-5)
  - [ ] Criar `domain/services/ProximosDisponiveisService.java`
  - [ ] Método `calcular(Long empresaId, Long profissionalIdFiltro, Long tipoIdFiltro)` → `List<SlotDisponivelResponse>`:
    1. Carregar profissionais ativos da empresa (filtrado por `profissionalIdFiltro` se fornecido)
    2. Carregar tipo de atendimento (por `tipoIdFiltro` se fornecido, senão iterar todos os tipos ativos)
    3. Para cada profissional, para cada dia nos próximos 30 dias:
       - Buscar `Disponibilidade` para aquele `DiaSemana`
       - Gerar slots de `duracaoMinutos` em `duracaoMinutos` dentro de `horarioInicio`–`horarioFim` (respeitando intervalo)
       - Para cada slot gerado: verificar ausência de agendamento sobreposto (`findSobrepostos()` da Story 4.2)
       - Slot livre → adicionar à lista de resultado
       - Parar quando 20 slots encontrados
    4. Ordenar resultado por data/horário ASC
    5. Retornar lista (vazia se nenhum encontrado)

- [ ] **Task 4 — DTO `SlotDisponivelResponse`** (AC-5)
  - [ ] Criar `api/dtos/response/SlotDisponivelResponse.java`:
    ```java
    record SlotDisponivelResponse(
        Instant horarioInicio,
        Instant horarioFim,
        ProfissionalResumo profissional,
        TipoAtendimentoResumo tipoAtendimento
    ) {}
    ```

- [ ] **Task 5 — Endpoint no `AgendamentoController`** (AC-5)
  - [ ] Adicionar `GET /api/v1/agendamentos/proximos-disponiveis` ao `AgendamentoController.java`
  - [ ] `@PreAuthorize("hasRole('STAFF')")`
  - [ ] Query params opcionais: `profissionalId`, `tipoId`
  - [ ] Usar `SecurityUtils.getEmpresaId()`

- [ ] **Task 6 — Banner de profissional inativo no `AgendamentoCard`** (AC-3)
  - [ ] Adicionar prop `profissionalInativo?: boolean` ao `AgendamentoCard.tsx` (Story 4.1)
  - [ ] Se `profissionalInativo = true`: renderizar banner abaixo do card:
    ```tsx
    <div className="px-3 py-1.5 text-xs flex items-center gap-1.5
                    bg-amber-50 text-amber-700 border-t border-amber-200 rounded-b-radius-md">
      <AlertTriangle size={12} />
      Profissional inativo — redistribuir manualmente
    </div>
    ```
  - [ ] `AgendaPage` calcula `profissionalInativo` comparando `agendamento.profissional.ativo` com os dados do profissional

- [ ] **Task 7 — `BuscarPage.tsx`** (AC-4, AC-6)
  - [ ] Criar `src/pages/BuscarPage.tsx`
  - [ ] Seletores:
    - Profissional: `<select>` populado com `GET /api/v1/profissionais` (ativos, todos)
    - Tipo: `<select>` populado com `GET /api/v1/tipos-atendimento/ativos`
    - Opção "Todos" como primeira opção em cada seletor
  - [ ] Botão `[Buscar próximos horários]` com loading state
  - [ ] Lista de slots retornados: data + horário + profissional + tipo + botão `[Agendar]`
  - [ ] Ao tocar `[Agendar]`: setar estado do formulário de agendamento com dados pré-preenchidos e abrir Bottom Sheet (reutilizar `FormularioAgendamento` da Story 4.2)

- [ ] **Task 8 — Testes** (AC-1, AC-5)
  - [ ] Teste unitário `NoShowJobTest`:
    - Agendamento PENDENTE com `horario_fim` há 31 min → marca NOSHOW
    - Agendamento PRESENTE: ignorado
    - Agendamento CANCELADO: ignorado
    - Double-booking: apenas o PENDENTE vira NOSHOW, o PRESENTE permanece
    - Cria `HistoricoStatus` com `usuarioId = null` e justificativa correta
  - [ ] Teste unitário `ProximosDisponiveisServiceTest`:
    - Profissional sem disponibilidade: nenhum slot
    - Profissional com disponibilidade e sem agendamentos: slots gerados
    - Slot com agendamento existente: slot pulado
    - Máximo 20 slots retornados
  - [ ] Teste de integração `ProximosDisponiveisControllerIT`:
    - GET → 200 com slots; Staff → 200; ADMIN_EMPRESA → 403

## Dev Notes

### `NoShowJob.java` — Implementação Completa

```java
@Component
@RequiredArgsConstructor
public class NoShowJob {

    private final AgendamentoRepository agendamentoRepository;
    private final HistoricoStatusRepository historicoRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void executar() {
        Instant limite = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<StatusAgendamento> alvo = List.of(PENDENTE, CONFIRMADO);

        List<Agendamento> paraNoShow = agendamentoRepository.findParaNoShow(alvo, limite);
        if (paraNoShow.isEmpty()) return;

        List<HistoricoStatus> historicos = new ArrayList<>();

        for (Agendamento ag : paraNoShow) {
            StatusAgendamento statusAnterior = ag.getStatus();
            ag.setStatus(NOSHOW);
            ag.setPublicToken(null);  // invalidar link

            HistoricoStatus h = new HistoricoStatus();
            h.setAgendamento(ag);
            h.setEmpresa(ag.getEmpresa());
            h.setStatusAnterior(statusAnterior);
            h.setStatusNovo(NOSHOW);
            h.setUsuarioId(null);    // ação automática do sistema
            h.setJustificativa("NoShow automático");
            historicos.add(h);
        }

        agendamentoRepository.saveAll(paraNoShow);
        historicoRepository.saveAll(historicos);
    }
}
```

### `ProximosDisponiveisService` — Algoritmo de Geração de Slots

```java
// Pseudocódigo simplificado:
List<SlotDisponivelResponse> slots = new ArrayList<>();

for (int dia = 0; dia < 30 && slots.size() < 20; dia++) {
    LocalDate data = LocalDate.now(ZoneOffset.UTC).plusDays(dia);
    DiaSemana diaSemana = mapDayOfWeek(data.getDayOfWeek());

    for (Profissional profissional : profissionais) {
        Optional<Disponibilidade> dispOpt =
            disponibilidadeRepository.findByProfissionalIdAndDiaSemana(
                profissional.getId(), diaSemana);
        if (dispOpt.isEmpty()) continue;
        Disponibilidade disp = dispOpt.get();

        for (TipoAtendimento tipo : tipos) {
            // Gerar slots de duracaoMinutos em duracaoMinutos
            LocalTime cursor = disp.getHorarioInicio();
            while (!cursor.plusMinutes(tipo.getDuracaoMinutos()).isAfter(disp.getHorarioFim())) {
                LocalTime slotFim = cursor.plusMinutes(tipo.getDuracaoMinutos());

                // Pular se intersecta intervalo
                if (disp.getInicioIntervalo() != null &&
                    cursor.isBefore(disp.getFimIntervalo()) &&
                    slotFim.isAfter(disp.getInicioIntervalo())) {
                    cursor = disp.getFimIntervalo();
                    continue;
                }

                // Converter para Instant UTC
                ZoneId fuso = ZoneId.of(profissional.getEmpresa().getFusoHorario());
                Instant inicio = data.atTime(cursor).atZone(fuso).toInstant();
                Instant fim    = data.atTime(slotFim).atZone(fuso).toInstant();

                // Pular se há agendamento sobreposto
                List<StatusAgendamento> ativos = List.of(PENDENTE, CONFIRMADO, PRESENTE);
                if (agendamentoRepository.findSobrepostos(
                        profissional.getId(), inicio, fim, ativos).isEmpty()) {
                    slots.add(new SlotDisponivelResponse(inicio, fim,
                        new ProfissionalResumo(profissional.getId(),
                                               profissional.getUsuario().getNome()),
                        new TipoAtendimentoResumo(tipo.getId(), tipo.getNome(),
                                                   tipo.getDuracaoMinutos())));
                    if (slots.size() >= 20) return slots;
                }

                cursor = slotFim;
            }
        }
    }
}
return slots;
```

> **Performance:** O algoritmo é O(30 dias × profissionais × tipos × slots por dia). Para clínicas com 5 profissionais, 5 tipos e ~20 slots/dia, são ~150.000 combinações no pior caso — aceitável para o MVP. Se necessário, pode ser otimizado com índices ou cache.

### Nota sobre `@EnableScheduling`

```java
// Application.java — adicionar anotação:
@SpringBootApplication
@EnableScheduling   // ← necessário para @Scheduled funcionar
public class DrAgendaApplication {
    public static void main(String[] args) {
        SpringApplication.run(DrAgendaApplication.class, args);
    }
}
```

> **Testes CI:** Para evitar que o job execute durante testes de integração, desabilitar em `application-test.properties`:
> ```properties
> spring.task.scheduling.enabled=false
> ```
> Ou usar `@ConditionalOnProperty` no `NoShowJob`.

### `BuscarPage.tsx` — Pré-preenchimento do Formulário

```tsx
// Estado compartilhado entre BuscarPage e FormularioAgendamento:
const [formularioPreenchido, setFormularioPreenchido] = useState<Partial<FormularioValues> | null>(null)
const [formularioAberto, setFormularioAberto] = useState(false)

function agendarSlot(slot: SlotDisponivelResponse) {
  setFormularioPreenchido({
    profissionalId: slot.profissional.id,
    tipoAtendimentoId: slot.tipoAtendimento.id,
    data: formatDate(slot.horarioInicio),  // YYYY-MM-DD no fuso local
    hora: formatTime(slot.horarioInicio),  // HH:mm
    // clienteId: undefined — deixar em branco
  })
  setFormularioAberto(true)
}
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `Application.java` | **UPDATE** | Adicionar `@EnableScheduling` |
| `infrastructure/jobs/NoShowJob.java` | NEW | `@Scheduled(fixedDelay = 60000)` |
| `domain/repositories/AgendamentoRepository.java` | **UPDATE** | Adicionar `findParaNoShow()` JPQL |
| `domain/services/ProximosDisponiveisService.java` | NEW | Algoritmo de slots disponíveis |
| `api/dtos/response/SlotDisponivelResponse.java` | NEW | |
| `api/controllers/AgendamentoController.java` | **UPDATE** | Adicionar `GET /proximos-disponiveis` |
| `src/shared/components/AgendamentoCard.tsx` | **UPDATE** | Adicionar banner de profissional inativo |
| `src/pages/BuscarPage.tsx` | NEW | Seletores + lista de slots + pré-preenchimento |
| `application-test.properties` | **UPDATE** | `spring.task.scheduling.enabled=false` |
| `test/.../NoShowJobTest.java` | NEW | Unitários + casos de double-booking |
| `test/.../ProximosDisponiveisServiceTest.java` | NEW | Algoritmo de slots |
| `test/.../ProximosDisponiveisControllerIT.java` | NEW | Integração |

### Referências

- [Source: epics.md#Story 4.4] — Acceptance Criteria completos e FR-009, FR-029, FR-030
- [Source: 4-3-transicoes-status-historico.md] — `HistoricoStatus`, `historico_status.usuario_id = null`
- [Source: 4-2-criar-agendamento.md] — `findSobrepostos()`, `FormularioAgendamento` reutilizado
- [Source: 3-4-disponibilidade-profissionais.md] — `DisponibilidadeRepository`
- [Source: architecture.md#Banco de Dados] — UTC; processamento em UTC com conversão de exibição no fuso

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
