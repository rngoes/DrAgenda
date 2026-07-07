# Story 4.2: Criar Agendamento — FAB, Formulário, Validações e Double-Booking

Status: ready-for-dev

## Story

Como Staff ou Profissional,
Quero criar agendamentos com validação automática de disponibilidade e suporte a double-booking intencional,
Para que a agenda nunca tenha conflitos acidentais e o sistema mostre vagas reais.

## Acceptance Criteria

**AC-1 — FAB 56×56px (UX-DR5)**
- Given o usuário autenticado é Staff ou Profissional e está na tela de agenda
- When a tela renderiza
- Then o FAB `[ + ]` de 56×56px é exibido no canto inferior direito, acima da BottomTabBar (UX-DR5) (FR-022)
- And o FAB não é exibido para Admin Empresa (apenas Staff e Profissional criam agendamentos)

**AC-2 — Bottom Sheet (mobile) / Drawer (desktop)**
- Given o usuário toca no FAB
- When o gesto é detectado
- Then mobile (≤767px): Bottom Sheet sobe com formulário (UX-DR6)
- And desktop (≥768px): drawer lateral 380px desliza da direita, com a agenda visível ao fundo
- And campos do formulário: Cliente (autocomplete), Tipo de Atendimento (seletor — apenas ativos), Profissional (seletor para Staff; pré-preenchido e bloqueado para Profissional), Data, Hora

**AC-3 — Duração calculada (FR-025)**
- Given o Staff ou Profissional seleciona um Tipo de Atendimento
- When o tipo é selecionado
- Then exibe abaixo do seletor: `"Duração: 30 min"` (usa `duracaoMinutos` do tipo escolhido)

**AC-4 — Feedback de disponibilidade inline (FR-023)**
- Given o formulário tem Profissional, Data e Hora preenchidos
- When qualquer desses campos muda
- Then consulta `GET /api/v1/profissionais/{id}/disponibilidade` e exibe:
  - `✅ "[Nome] disponível às 14:00"` se horário está dentro da disponibilidade
  - `❌ "[Nome] não atende às terças. Próximo disponível: quarta 09:00"` se não disponível

**AC-5 — Criar agendamento (happy path)**
- Given o Staff ou Profissional submete `POST /api/v1/agendamentos` com dados válidos
- When o backend valida (FR-023):
  1. Dia da semana com disponibilidade cadastrada para o profissional
  2. Horário dentro da faixa `horarioInicio`–`horarioFim`
  3. Janela `[horaInicio, horaInicio + duracaoMinutos]` não intersecta o intervalo de almoço
- Then cria agendamento com `status = PENDENTE`, `horario_fim = horario_inicio + duracao_minutos` (FR-025)
- And retorna HTTP 201 com `Location: /api/v1/agendamentos/{id}`
- And frontend: `toast.success('Agendamento criado ✓')`, Bottom Sheet fecha, agenda atualiza via `queryClient.invalidateQueries`

**AC-6 — Double-booking: segundo agendamento (FR-024)**
- Given o backend detecta que seria o **segundo** agendamento com janelas sobrepostas para o mesmo profissional no mesmo horário
- When a validação ocorre
- Then retorna HTTP 409 com body: `{ "type": "double-booking", "agendamentoExistente": { "id", "clienteNome", "horarioInicio", "horarioFim", "tipoNome" } }`

**AC-7 — Dialog de double-booking (UX-DR7)**
- Given o frontend recebe o HTTP 409 com `type: "double-booking"`
- When o `DialogDoubleBooking` é exibido
- Then mostra: nome do paciente existente, horário e tipo do conflito
- And dois botões: `[Cancelar]` (ghost) e `[Agendar mesmo assim]` (âmbar/warning)
- And `[Agendar mesmo assim]` reenvia `POST /api/v1/agendamentos` com header `X-Confirm-Double-Booking: true`
- And backend aceita a criação com esse header mesmo com sobreposição

**AC-8 — Terceiro double-booking bloqueado**
- Given seria o terceiro ou mais agendamento com sobreposição para o mesmo profissional
- When o backend valida
- Then retorna HTTP 409 com mensagem `"Terceiro agendamento com sobreposição não é permitido"` sem `type: "double-booking"` — erro inline (FR-024)

**AC-9 — Mensagens de erro em linguagem natural (FR-026)**
- Given validação falha
- When o backend retorna HTTP 400 ou 409
- Then a mensagem usa linguagem natural, ex:
  - `"[Profissional] não atende às sextas-feiras."`
  - `"Intervalo de descanso: 12:00–13:00. Escolha outro horário."`
  - `"Horário fora da faixa de atendimento: atende das 08:00 às 17:00."`

## Tasks / Subtasks

- [ ] **Task 1 — `CriarAgendamentoRequest` DTO** (AC-5)
  - [ ] Criar `api/dtos/request/CriarAgendamentoRequest.java`:
    - `@NotNull clienteId` (Long)
    - `@NotNull profissionalId` (Long)
    - `@NotNull tipoAtendimentoId` (Long)
    - `@NotNull horarioInicio` (Instant — ISO 8601 UTC)
  - [ ] Criar `api/dtos/response/CriarAgendamentoResponse.java`: `id`, `horarioInicio`, `horarioFim`, `status`

- [ ] **Task 2 — `DisponibilidadeValidator`** (AC-5, AC-9)
  - [ ] Criar `domain/services/DisponibilidadeValidator.java`
  - [ ] Método `validar(Profissional, Instant horarioInicio, int duracaoMinutos)`:
    1. Verificar `DiaSemana` do `horarioInicio` — lançar exceção se sem disponibilidade
    2. Verificar que `horarioInicio` e `horarioFim` (início + duração) estão dentro de `disponibilidade.horarioInicio–horarioFim`
    3. Se há intervalo configurado: verificar que a janela não intersecta `[inicioIntervalo, fimIntervalo]`
  - [ ] Lançar `DisponibilidadeException` com mensagem em linguagem natural para cada caso

- [ ] **Task 3 — `DoubleBookingValidator`** (AC-6, AC-7, AC-8)
  - [ ] Criar `domain/services/DoubleBookingValidator.java`
  - [ ] Método `validar(Long profissionalId, Instant inicio, Instant fim, boolean confirmarDoubleBooking)`:
    - Buscar agendamentos sobrepostos: `status IN (PENDENTE, CONFIRMADO, PRESENTE)` e janelas que intersectam `[inicio, fim]`
    - 0 sobrepostos: OK
    - 1 sobreposto e `confirmarDoubleBooking = false`: lançar `DoubleBookingException` com dados do agendamento existente
    - 1 sobreposto e `confirmarDoubleBooking = true`: OK (permite criação)
    - 2+ sobrepostos: lançar `TriploBookingException` ("Terceiro agendamento com sobreposição não é permitido")
  - [ ] Adicionar ao `AgendamentoRepository`: `List<Agendamento> findSobrepostos(Long profissionalId, Instant inicio, Instant fim, List<StatusAgendamento> statusAtivos)`

- [ ] **Task 4 — `AgendamentoService.criar()`** (AC-5–AC-8)
  - [ ] Criar `domain/services/AgendamentoService.java`
  - [ ] `criar(CriarAgendamentoRequest, Long empresaId, boolean confirmarDoubleBooking)` — `@Transactional`:
    1. Carregar `Cliente`, `Profissional`, `TipoAtendimento` — verificar que pertencem à empresa do JWT (404 se não)
    2. Calcular `horarioFim = horarioInicio + tipoAtendimento.duracaoMinutos`
    3. Chamar `DisponibilidadeValidator.validar()` → `DisponibilidadeException` (400)
    4. Chamar `DoubleBookingValidator.validar()` → `DoubleBookingException` (409) ou `TriploBookingException` (409)
    5. Criar e salvar `Agendamento` com `status = PENDENTE`
    6. Retornar `CriarAgendamentoResponse`

- [ ] **Task 5 — Atualizar `AgendamentoController`** (AC-5–AC-9)
  - [ ] Adicionar `POST /api/v1/agendamentos` ao `AgendamentoController.java` (Story 4.1)
  - [ ] `@PreAuthorize("hasAnyRole('STAFF','PROFISSIONAL')")`
  - [ ] Ler header `X-Confirm-Double-Booking` → converter para `boolean confirmarDoubleBooking`
  - [ ] Usar `SecurityUtils.getEmpresaId()`

- [ ] **Task 6 — Exception handlers no `GlobalExceptionHandler`** (AC-6, AC-9)
  - [ ] `DoubleBookingException` → HTTP 409 com `{ "type": "double-booking", "agendamentoExistente": {...} }`
  - [ ] `TriploBookingException` → HTTP 409 com mensagem simples (sem `type`)
  - [ ] `DisponibilidadeException` → HTTP 400 com mensagem em linguagem natural

- [ ] **Task 7 — FAB no `AgendaPage.tsx`** (AC-1)
  - [ ] Adicionar ao `AgendaPage.tsx` (Story 4.1): FAB `<button>` 56×56px, `fixed bottom-20 right-4 z-40` (acima da BottomTabBar que tem `bottom-0 h-12`)
  - [ ] Exibido apenas se `perfil !== 'ADMIN_EMPRESA'`
  - [ ] Ao clicar: abrir Bottom Sheet ou Drawer dependendo de `isDesktop`

- [ ] **Task 8 — `BottomSheet.tsx` / `DrawerFormulario.tsx`** (AC-2, AC-3, AC-4)
  - [ ] Criar `src/shared/components/BottomSheet.tsx`: painel que desliza do fundo, `fixed inset-x-0 bottom-0`, `rounded-t-2xl`, backdrop opaco, max-height `85vh`, overflow-y scroll
  - [ ] Criar `src/pages/agenda/FormularioAgendamento.tsx`: formulário usado dentro do BottomSheet (mobile) e Drawer (desktop)
  - [ ] Campos no formulário:
    - **Cliente**: `Combobox` com autocomplete → usa `GET /api/v1/clientes/autocomplete?busca=`
    - **Tipo de Atendimento**: `Select` com `GET /api/v1/tipos-atendimento/ativos`; ao selecionar, exibe `"Duração: X min"`
    - **Profissional**: `Select` com lista de profissionais ativos; bloqueado para Profissional (pré-preenchido)
    - **Data**: `<input type="date">` com min = hoje
    - **Hora**: `<input type="time">` com step = 900 (15 min)
  - [ ] Feedback de disponibilidade: `useEffect` que valida quando profissional + data + hora mudam (consulta `GET /api/v1/profissionais/{id}/disponibilidade`)

- [ ] **Task 9 — `DialogDoubleBooking.tsx`** (AC-7)
  - [ ] Criar `src/pages/agenda/DialogDoubleBooking.tsx` usando Shadcn `Dialog`
  - [ ] Props: `agendamentoExistente`, `onCancelar`, `onConfirmar`
  - [ ] Exibe: "Já existe um agendamento neste horário: [nome paciente] — [horário] — [tipo]"
  - [ ] Botão `[Cancelar]` — variant ghost
  - [ ] Botão `[Agendar mesmo assim]` — variant warning (âmbar: `bg-amber-500 text-white`)

- [ ] **Task 10 — Testes** (AC-5, AC-6, AC-8, AC-9)
  - [ ] Teste unitário `DisponibilidadeValidatorTest`:
    - Dia sem disponibilidade → exceção com mensagem correta
    - Horário fora da faixa → exceção
    - Dentro do intervalo de almoço → exceção
    - Horário válido → sem exceção
  - [ ] Teste unitário `DoubleBookingValidatorTest`:
    - 0 sobrepostos → OK
    - 1 sobreposto sem header → `DoubleBookingException` com dados do existente
    - 1 sobreposto com header → OK
    - 2 sobrepostos → `TriploBookingException`
  - [ ] Teste de integração `AgendamentoControllerIT`:
    - POST happy path → 201
    - POST dia sem disponibilidade → 400
    - POST double-booking sem header → 409 com `type: "double-booking"`
    - POST double-booking com header → 201
    - POST triplo → 409 sem dialog

## Dev Notes

### `DisponibilidadeValidator` — Lógica de Interseção com Intervalo

```java
public class DisponibilidadeValidator {

    private final DisponibilidadeRepository disponibilidadeRepository;

    public void validar(Long profissionalId, Instant horarioInicio, int duracaoMinutos,
                        ZoneId fusoEmpresa) {
        Instant horarioFim = horarioInicio.plus(duracaoMinutos, ChronoUnit.MINUTES);

        // Converter para hora local para comparar com disponibilidade (TIME)
        ZonedDateTime inicioLocal = horarioInicio.atZone(fusoEmpresa);
        ZonedDateTime fimLocal    = horarioFim.atZone(fusoEmpresa);

        DiaSemana dia = mapDayOfWeek(inicioLocal.getDayOfWeek());
        LocalTime horaInicio = inicioLocal.toLocalTime();
        LocalTime horaFim    = fimLocal.toLocalTime();

        // 1. Verificar se há disponibilidade no dia
        Disponibilidade disp = disponibilidadeRepository
            .findByProfissionalIdAndDiaSemana(profissionalId, dia)
            .orElseThrow(() -> new DisponibilidadeException(
                String.format("Profissional não atende às %ss.", dia.getNomePtBr())));

        // 2. Verificar faixa de atendimento
        if (horaInicio.isBefore(disp.getHorarioInicio()) ||
            horaFim.isAfter(disp.getHorarioFim())) {
            throw new DisponibilidadeException(
                String.format("Fora do horário de atendimento: %s às %s.",
                    disp.getHorarioInicio(), disp.getHorarioFim()));
        }

        // 3. Verificar intersecção com intervalo (se configurado)
        if (disp.getInicioIntervalo() != null) {
            boolean intersecta = horaInicio.isBefore(disp.getFimIntervalo()) &&
                                 horaFim.isAfter(disp.getInicioIntervalo());
            if (intersecta) {
                throw new DisponibilidadeException(
                    String.format("Intervalo de descanso: %s–%s. Escolha outro horário.",
                        disp.getInicioIntervalo(), disp.getFimIntervalo()));
            }
        }
    }
}
```

> **Nota:** o fuso da empresa é necessário para converter `Instant` UTC → `LocalTime` para comparar com a disponibilidade (que está em `TIME` local). O `AgendamentoService` deve carregar `empresa.fusoHorario` e passar para o validator.

### `DoubleBookingValidator` — Query JPQL

```java
// AgendamentoRepository — adicionar:
@Query("""
    SELECT a FROM Agendamento a
    WHERE a.profissional.id = :profissionalId
      AND a.status IN :statusAtivos
      AND a.horarioInicio < :fim
      AND a.horarioFim > :inicio
    """)
List<Agendamento> findSobrepostos(
    @Param("profissionalId") Long profissionalId,
    @Param("inicio") Instant inicio,
    @Param("fim") Instant fim,
    @Param("statusAtivos") List<StatusAgendamento> statusAtivos
);
```

```java
// DoubleBookingValidator.validar():
List<StatusAgendamento> ativos = List.of(PENDENTE, CONFIRMADO, PRESENTE);
List<Agendamento> sobrepostos = repository.findSobrepostos(profissionalId, inicio, fim, ativos);

if (sobrepostos.isEmpty()) return;  // OK

if (sobrepostos.size() == 1 && !confirmarDoubleBooking) {
    Agendamento existente = sobrepostos.get(0);
    throw new DoubleBookingException(existente);
}

if (sobrepostos.size() == 1 && confirmarDoubleBooking) return;  // Confirmado — OK

// 2+ sobrepostos
throw new TriploBookingException("Terceiro agendamento com sobreposição não é permitido");
```

### Response HTTP 409 Double-Booking

```json
HTTP 409 Conflict
{
  "type": "double-booking",
  "title": "Conflito de agendamento",
  "status": 409,
  "agendamentoExistente": {
    "id": 42,
    "clienteNome": "Maria Silva",
    "horarioInicio": "2026-06-05T14:00:00Z",
    "horarioFim":    "2026-06-05T14:30:00Z",
    "tipoNome": "Consulta"
  }
}
```

> O campo `agendamentoExistente.clienteNome` deve ser descriptografado antes de incluir no response (o `AgendamentoController` ou o `GlobalExceptionHandler` deve descriptografar via converter ou chamar `cliente.getNome()`).

### FAB Posicionamento

```tsx
// AgendaPage.tsx — FAB acima da BottomTabBar (h-12 = 48px):
{perfil !== 'ADMIN_EMPRESA' && (
  <button
    onClick={() => setFormularioAberto(true)}
    className={`
      fixed z-40 flex items-center justify-center
      w-14 h-14 rounded-full shadow-elevation-3
      bg-brand-500 text-white
      ${isDesktop ? 'bottom-6 right-6' : 'bottom-16 right-4'}
    `}
    aria-label="Novo agendamento"
  >
    <Plus size={28} />
  </button>
)}
```

> `bottom-16` (64px) no mobile: BottomTabBar tem `h-12` (48px) + 16px de margem. No desktop não há BottomTabBar, então `bottom-6`.

### Validação de Disponibilidade Inline no Frontend

```tsx
// FormularioAgendamento.tsx — feedback inline:
const { data: disponibilidade } = useQuery({
  queryKey: ['disponibilidade', profissionalId, data, hora],
  queryFn: () => fetchDisponibilidade(profissionalId),
  enabled: !!(profissionalId && data && hora),
  staleTime: 60_000,
})

const feedbackDisponibilidade = useMemo(() => {
  if (!disponibilidade || !data || !hora) return null
  const dia = getDiaSemana(data)  // ex: 'TERCA'
  const slot = disponibilidade.find(s => s.diaSemana === dia)
  if (!slot) return { ok: false, msg: `${profissionalNome} não atende às ${labelDia(dia)}s.` }
  if (hora < slot.horarioInicio || hora > slot.horarioFim)
    return { ok: false, msg: `Fora do horário: ${slot.horarioInicio}–${slot.horarioFim}.` }
  return { ok: true, msg: `${profissionalNome} disponível às ${hora}.` }
}, [disponibilidade, data, hora, profissionalNome])
```

### `DiaSemana.getNomePtBr()` — Enum Helper

```java
// Adicionar ao enum DiaSemana.java (Story 3.4):
public String getNomePtBr() {
    return switch (this) {
        case SEGUNDA -> "segunda-feira";
        case TERCA   -> "terça-feira";
        case QUARTA  -> "quarta-feira";
        case QUINTA  -> "quinta-feira";
        case SEXTA   -> "sexta-feira";
        case SABADO  -> "sábado";
        case DOMINGO -> "domingo";
    };
}
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `api/dtos/request/CriarAgendamentoRequest.java` | NEW | `horarioInicio` como `Instant` |
| `api/dtos/response/CriarAgendamentoResponse.java` | NEW | |
| `domain/services/DisponibilidadeValidator.java` | NEW | Mensagens em linguagem natural |
| `domain/services/DoubleBookingValidator.java` | NEW | Lógica de 1 vs 2+ sobrepostos |
| `domain/services/AgendamentoService.java` | NEW | Orquestra validators |
| `domain/repositories/AgendamentoRepository.java` | **UPDATE** | Adicionar `findSobrepostos()` JPQL |
| `domain/exceptions/DoubleBookingException.java` | NEW | Carrega dados do agendamento existente |
| `domain/exceptions/TriploBookingException.java` | NEW | |
| `domain/exceptions/DisponibilidadeException.java` | NEW | |
| `api/controllers/AgendamentoController.java` | **UPDATE** | Adicionar POST, ler header `X-Confirm-Double-Booking` |
| `api/exceptions/GlobalExceptionHandler.java` | **UPDATE** | Handlers para 3 novas exceptions |
| `domain/enums/DiaSemana.java` | **UPDATE** | Adicionar `getNomePtBr()` |
| `src/pages/AgendaPage.tsx` | **UPDATE** | Adicionar FAB + controle de Bottom Sheet/Drawer |
| `src/shared/components/BottomSheet.tsx` | NEW | Painel mobile |
| `src/pages/agenda/FormularioAgendamento.tsx` | NEW | Formulário com autocomplete, select, feedback |
| `src/pages/agenda/DialogDoubleBooking.tsx` | NEW | Dialog UX-DR7 |
| `test/.../DisponibilidadeValidatorTest.java` | NEW | Unitários |
| `test/.../DoubleBookingValidatorTest.java` | NEW | Unitários |
| `test/.../AgendamentoControllerIT.java` | **UPDATE** | POST happy path + 409 casos |

### Referências

- [Source: epics.md#Story 4.2] — Acceptance Criteria completos e FR-022–FR-026
- [Source: DESIGN.md#UX-DR5, UX-DR6, UX-DR7] — FAB, Bottom Sheet, Dialog double-booking
- [Source: 3-4-disponibilidade-profissionais.md] — `DisponibilidadeRepository`, `DiaSemana`
- [Source: 3-6-listagem-busca-clientes.md] — `GET /api/v1/clientes/autocomplete`
- [Source: 4-1-agenda-dashboard.md] — `AgendamentoController` base, `AgendaPage`

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
