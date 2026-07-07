# Story 3.6: Listagem, Busca e Edição de Clientes

Status: ready-for-dev

## Story

Como Staff,
Quero listar, buscar e editar clientes cadastrados,
Para que eu possa encontrar rapidamente um cliente ao criar agendamentos e manter os dados atualizados.

## Acceptance Criteria

**AC-1 — Listagem com paginação server-side**
- Given o Staff acessa `GET /api/v1/clientes`
- When sem parâmetro de busca
- Then retorna clientes não-anonimizados da empresa com envelope paginado (`content`, `totalElements`, `totalPages`, `page`, `size`) (FR-046)
- And page size padrão = 20

**AC-2 — Busca por nome ou telefone**
- Given o Staff acessa `GET /api/v1/clientes?busca=João`
- When o backend processa
- Then descriptografa todos os campos `nome_enc` e `telefone_enc` da empresa em memória
- And filtra pelo termo de busca (case-insensitive, substring)
- And retorna resultado paginado com os clientes correspondentes (FR-050)

**AC-3 — Tela de listagem com busca**
- Given o Staff acessa `/clientes`
- When a lista renderiza
- Then cada item exibe: nome (descriptografado), telefone, data de nascimento formatada (DD/MM/YYYY)
- And campo de busca no topo com debounce de 300ms disparando `GET /api/v1/clientes?busca=...`
- And skeleton durante carregamento inicial (padrão Story 3.1)
- And botão "Novo Cliente" navega para `/clientes/novo`

**AC-4 — Edição de cliente**
- Given o Staff toca em um cliente e acessa `/clientes/{id}/editar`
- When submete `PUT /api/v1/clientes/{id}` com dados válidos
- Then `nome`, `telefone` e `dataNascimento` são atualizados com re-criptografia automática pelo `AesAttributeConverter` (FR-050)
- And retorna HTTP 200
- And `consentimentoLgpd` e dados de consentimento são **imutáveis** após criação — ignorados mesmo se enviados no body

**AC-5 — Controle de acesso**
- Given um Profissional ou Admin Empresa tenta acessar `GET /api/v1/clientes`
- When o `@PreAuthorize` avalia
- Then retorna HTTP 403 (apenas Staff acessa a lista de clientes)
- And `GET /api/v1/clientes/{id}` para uso em agendamentos: acessível por `STAFF` e `PROFISSIONAL` (campo de cliente no formulário de agendamento — Epic 4)

**AC-6 — Autocomplete para agendamentos (Epic 4)**
- Given o campo "Cliente" no formulário de agendamento (Story 4.2) recebe 3+ caracteres
- When dispara `GET /api/v1/clientes?busca=...`
- Then retorna lista de clientes (sem paginação completa, apenas primeiros 10 resultados) para popular o autocomplete
- And este endpoint `GET /api/v1/clientes/autocomplete?busca=...` é acessível por `STAFF` e `PROFISSIONAL`

**AC-7 — Isolamento multi-tenant**
- Given Staff tenta acessar `PUT /api/v1/clientes/{id}` de cliente de outra empresa
- When o backend verifica
- Then retorna HTTP 404

## Tasks / Subtasks

- [ ] **Task 1 — Atualizar `ClienteService`** (AC-1, AC-2, AC-4, AC-6)
  - [ ] Adicionar `listar(Long empresaId, String busca, Pageable pageable)` → `Page<ClienteResponse>` (lógica de busca em memória conforme documentado na Story 3.5)
  - [ ] Adicionar `autocomplete(Long empresaId, String busca)` → `List<ClienteResponse>` (máx. 10 resultados, sem paginação)
  - [ ] Adicionar `atualizar(Long id, Long empresaId, AtualizarClienteRequest)` — `@Transactional`:
    - `findByIdAndEmpresaId()` → 404 se outra empresa
    - Atualizar apenas `nome`, `telefone`, `dataNascimento` — campos de consentimento imutáveis
    - Re-criptografia automática pelo `AesAttributeConverter` ao salvar

- [ ] **Task 2 — DTOs adicionais** (AC-4)
  - [ ] Criar `api/dtos/request/AtualizarClienteRequest.java`: `@NotBlank nome`, `telefone` (nullable), `dataNascimento` (nullable) — **sem campo consentimento**

- [ ] **Task 3 — Atualizar `ClienteController`** (AC-1–AC-7)
  - [ ] Adicionar endpoints ao `ClienteController.java` (Story 3.5):
    - `GET /` — `@PreAuthorize("hasRole('STAFF')")` → listar com busca + paginação
    - `GET /autocomplete` — `@PreAuthorize("hasAnyRole('STAFF','PROFISSIONAL')")` → até 10 resultados
    - `PUT /{id}` — `@PreAuthorize("hasRole('STAFF')")` → 200
  - [ ] Query param `busca` opcional em ambos os GET; quando ausente, retorna todos

- [ ] **Task 4 — Tela de Listagem `ClientesPage.tsx`** (AC-3)
  - [ ] Criar `src/pages/ClientesPage.tsx`
  - [ ] Usar `useQuery` com `['clientes', busca, page]` como query key
  - [ ] Campo de busca controlado com `useDebounce(busca, 300)` — resetar `page` para 0 ao buscar
  - [ ] Tabela/lista com colunas: Nome · Telefone · Data de Nasc. · Ações
  - [ ] Botão "Editar" por linha navega para `/clientes/{id}/editar`
  - [ ] Paginação no rodapé: botões Anterior/Próximo com contador de páginas
  - [ ] Skeleton com 5 linhas durante carregamento

- [ ] **Task 5 — Tela de Edição `ClienteEditarPage.tsx`** (AC-4)
  - [ ] Criar `src/pages/ClienteEditarPage.tsx`
  - [ ] Carregar dados via `GET /api/v1/clientes/{id}` com `useQuery` — skeleton durante carregamento
  - [ ] Formulário pré-preenchido com React Hook Form + Zod (mesma validação da criação, sem checkbox de consentimento)
  - [ ] Campo de consentimento exibido como **readonly** (exibe "Consentimento coletado em [data]") — não editável
  - [ ] `useMutation` para `PUT` com `toast.success('Cliente atualizado com sucesso')`
  - [ ] Após salvar: navegar de volta para `/clientes`

- [ ] **Task 6 — `useDebounce` hook** (AC-3)
  - [ ] Criar `src/shared/hooks/useDebounce.ts`:
    ```ts
    export function useDebounce<T>(value: T, delay: number): T {
      const [debouncedValue, setDebouncedValue] = useState(value)
      useEffect(() => {
        const timer = setTimeout(() => setDebouncedValue(value), delay)
        return () => clearTimeout(timer)
      }, [value, delay])
      return debouncedValue
    }
    ```

- [ ] **Task 7 — Testes** (AC-2, AC-4, AC-5, AC-7)
  - [ ] Teste unitário `ClienteServiceTest` (adicionar aos testes da Story 3.5):
    - Listar: retorna apenas clientes não-anonimizados da empresa
    - Busca: filtra corretamente por substring case-insensitive após descriptografia
    - Busca vazia: retorna todos
    - Atualizar: consentimento permanece inalterado; campos nome/telefone/dataNascimento atualizados
    - Isolamento: atualizar id de outra empresa → 404
  - [ ] Teste de integração `ClienteControllerIT` (adicionar):
    - GET com busca: retorna clientes filtrados
    - PUT: dados atualizados, consentimento inalterado
    - PROFISSIONAL → 403 em GET /; PROFISSIONAL → 200 em GET /autocomplete

## Dev Notes

### Busca em Memória — Detalhe de Performance e Paginação

```java
// ClienteService.listar() — implementação completa
public Page<ClienteResponse> listar(Long empresaId, String busca, Pageable pageable) {
    // 1. Carregar todos os não-anonimizados da empresa (criptografia transparente pelo converter)
    List<Cliente> todos = clienteRepository
        .findAllByEmpresaIdAndAnonimizadoFalse(empresaId);
        // Nota: para empresas com muitos clientes, considerar stream lazy.
        // No escopo do projeto (clínicas pequenas), este approach é aceitável.

    // 2. Filtrar em memória
    Stream<Cliente> stream = todos.stream();
    if (busca != null && !busca.isBlank()) {
        String termo = busca.strip().toLowerCase();
        stream = stream.filter(c ->
            (c.getNome() != null     && c.getNome().toLowerCase().contains(termo)) ||
            (c.getTelefone() != null && c.getTelefone().contains(termo))
        );
    }

    // 3. Mapear e paginar manualmente
    List<ClienteResponse> lista = stream.map(this::toResponse).toList();
    int start = (int) pageable.getOffset();
    int end   = Math.min(start + pageable.getPageSize(), lista.size());
    List<ClienteResponse> paginado = (start >= lista.size())
        ? List.of()
        : lista.subList(start, end);

    return new PageImpl<>(paginado, pageable, lista.size());
}

// ClienteService.autocomplete() — máx 10 resultados
public List<ClienteResponse> autocomplete(Long empresaId, String busca) {
    if (busca == null || busca.strip().length() < 3) return List.of();

    return clienteRepository
        .findAllByEmpresaIdAndAnonimizadoFalse(empresaId)
        .stream()
        .filter(c -> c.getNome() != null &&
                     c.getNome().toLowerCase().contains(busca.strip().toLowerCase()))
        .limit(10)
        .map(this::toResponse)
        .toList();
}
```

### Atualizar sem Tocar no Consentimento

```java
@Transactional
public ClienteResponse atualizar(Long id, Long empresaId, AtualizarClienteRequest req) {
    Cliente cliente = clienteRepository
        .findByIdAndEmpresaId(id, empresaId)
        .orElseThrow(RecursoNaoEncontradoException::new);

    // Apenas estes 3 campos são mutáveis
    cliente.setNome(req.getNome());
    cliente.setTelefone(req.getTelefone());
    cliente.setDataNascimento(req.getDataNascimento());
    // AesAttributeConverter recriptografa automaticamente ao salvar

    // Campos de consentimento: não tocamos — imutáveis após criação
    return toResponse(clienteRepository.save(cliente));
}
```

### Campo de Consentimento Readonly no Frontend

```tsx
// ClienteEditarPage.tsx — exibir consentimento como informação, não como campo editável
{consentimentoTimestamp && (
  <div className="p-3 bg-[var(--bg-subtle)] rounded-radius-md border border-[var(--border)]">
    <p className="text-sm text-[var(--text-secondary)]">
      ✓ Consentimento LGPD coletado em{' '}
      <span className="font-medium text-[var(--text-primary)]">
        {formatarDataHora(consentimentoTimestamp, fuso)}
      </span>
      {' '}— Versão {consentimentoVersaoTermo}
    </p>
  </div>
)}
```

### `ClientesPage.tsx` — Estrutura de Query com Debounce

```tsx
export function ClientesPage() {
  const [busca, setBusca] = useState('')
  const [page, setPage] = useState(0)
  const buscaDebounced = useDebounce(busca, 300)

  // Resetar página ao buscar
  useEffect(() => { setPage(0) }, [buscaDebounced])

  const { data, isLoading } = useQuery({
    queryKey: ['clientes', buscaDebounced, page],
    queryFn: () => api.get(`/clientes?busca=${buscaDebounced}&page=${page}&size=20`).then(r => r.data),
    staleTime: 30_000,
  })

  return (
    <div className="p-4 space-y-4">
      <div className="flex items-center gap-3">
        <input
          type="search"
          placeholder="Buscar por nome ou telefone..."
          value={busca}
          onChange={e => setBusca(e.target.value)}
          className="flex-1 input-base"
        />
        <Button onClick={() => navigate('/clientes/novo')}>
          + Novo Cliente
        </Button>
      </div>

      {isLoading
        ? <SkeletonList rows={5} />
        : <ClientesList clientes={data?.content ?? []} />
      }

      {/* Paginação */}
      <div className="flex items-center justify-between text-sm text-[var(--text-secondary)]">
        <span>{data?.totalElements ?? 0} clientes</span>
        <div className="flex gap-2">
          <Button variant="outline" size="sm"
            disabled={page === 0} onClick={() => setPage(p => p - 1)}>Anterior</Button>
          <span className="px-2 py-1">{page + 1} / {data?.totalPages ?? 1}</span>
          <Button variant="outline" size="sm"
            disabled={page >= (data?.totalPages ?? 1) - 1}
            onClick={() => setPage(p => p + 1)}>Próximo</Button>
        </div>
      </div>
    </div>
  )
}
```

### `ClienteRepository` — Método Adicional para Busca Total

```java
// Adicionar ao ClienteRepository.java (Story 3.5):
// Retorna todos (sem paginação) para busca em memória:
List<Cliente> findAllByEmpresaIdAndAnonimizadoFalse(Long empresaId);
// (Este método já foi listado na Story 3.5 — verificar se já existe antes de criar)
```

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `domain/services/ClienteService.java` | **UPDATE** | Adicionar listar, autocomplete, atualizar |
| `domain/repositories/ClienteRepository.java` | **UPDATE** | Confirmar presença de `findAllByEmpresaIdAndAnonimizadoFalse` |
| `api/dtos/request/AtualizarClienteRequest.java` | NEW | Sem campo consentimento |
| `api/controllers/ClienteController.java` | **UPDATE** | Adicionar GET /, GET /autocomplete, PUT /{id} |
| `src/pages/ClientesPage.tsx` | NEW | Listagem + busca + paginação |
| `src/pages/ClienteEditarPage.tsx` | NEW | Formulário de edição + consentimento readonly |
| `src/shared/hooks/useDebounce.ts` | NEW | Debounce genérico |
| `test/.../ClienteServiceTest.java` | **UPDATE** | Adicionar casos de listagem, busca, atualização |
| `test/.../ClienteControllerIT.java` | **UPDATE** | Adicionar casos de GET, PUT, controle de acesso |

### Referências

- [Source: epics.md#Story 3.6] — Acceptance Criteria completos e FR-046, FR-050
- [Source: 3-5-cadastro-clientes-lgpd.md] — `AesAttributeConverter`, busca em memória, `ClienteRepository`
- [Source: architecture.md#Regras de Processo] — empresaId do JWT
- [Source: 3-1-componentes-base-ui.md] — `SkeletonList`, `useToast`
- [Source: 3-2-configuracoes-clinica.md] — `useFusoHorario` para formatar `consentimentoTimestamp`

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
