# Story 5.3: Paginação Server-Side em Todas as Listagens

Status: ready-for-dev

## Story

Como usuário do DrAgenda,
Quero que todas as listas com crescimento ilimitado tenham paginação eficiente,
Para que o sistema continue performático independente do volume de dados.

## Acceptance Criteria

**AC-1 — Envelope de paginação padrão (FR-046)**
- Given qualquer endpoint de listagem retorna dados paginados
- When a API responde
- Then o envelope contém exatamente: `content` (array), `page` (número atual, 0-based), `size` (tamanho da página), `totalElements` (total de registros), `totalPages` (total de páginas)
- And o page size padrão é 20 se não especificado
- And o cliente pode passar `?page=0&size=20` para controlar a paginação

**AC-2 — Endpoints existentes em conformidade**
- Given os endpoints abaixo já implementados em stories anteriores
- When são auditados nesta story
- Then todos devem retornar o envelope padrão do AC-1 sem alteração de contrato:
  - `GET /api/v1/clientes` (Story 3.6 — `PageImpl` manual; verificar envelope)
  - `GET /api/v1/profissionais` (Story 2.4 — verificar envelope)
  - `GET /api/v1/staff` (Story 2.5 — verificar envelope)
  - `GET /api/v1/relatorios/noshow-pacientes` (Story 5.2 — Spring `Page<T>`)
- And se algum endpoint não usar o envelope padrão, deve ser corrigido nesta story

**AC-3 — Novo endpoint `GET /api/v1/agendamentos/{id}/historico` (FR-046)**
- Given qualquer usuário autenticado acessa `GET /api/v1/agendamentos/{id}/historico?page=0&size=20`
- When o backend processa
- Then retorna `historico_status` paginado pelo envelope padrão (AC-1)
- And ordenado por `timestamp` DESC (mais recente primeiro)
- And `@PreAuthorize` para `STAFF`, `PROFISSIONAL` e `ADMIN_EMPRESA` — `empresaId` validado para isolamento

**AC-4 — Componente `<Paginacao>` reutilizável**
- Given qualquer listagem do frontend precisa de controles de navegação
- When `totalPages > 1`
- Then o componente `<Paginacao>` exibe botões `← Anterior` e `Próxima →`
- And exibe indicador: `"Página N de T (X registros)"` onde N = `page + 1` (1-based para exibição)
- And botão `← Anterior` desabilitado quando `page === 0`
- And botão `Próxima →` desabilitado quando `page === totalPages - 1`
- And ao trocar de página, chama callback `onPageChange(novaPagina)`
- And se `totalPages <= 1`, o componente renderiza null (sem controles desnecessários)

**AC-5 — Integração do componente `<Paginacao>` nas telas existentes**
- Given as telas abaixo já renderizam listas
- When têm `totalPages > 1`
- Then exibem o componente `<Paginacao>` abaixo da lista:
  - `ClientesPage.tsx` (Story 3.6)
  - `ProfissionaisPage.tsx` (Story 2.4)
  - `StaffPage.tsx` (Story 2.5)
  - `RelatoriosPage.tsx` — seção de no-shows (Story 5.2)
- And ao trocar de página, o `useQuery` é reexecutado com o novo `page`

**AC-6 — Hook `usePaginacao` para gerenciar estado de página**
- Given qualquer tela usa paginação
- When usa o hook `usePaginacao(pageSize = 20)`
- Then retorna `{ page, setPage, resetPage }` onde `resetPage` volta ao page 0
- And `resetPage` é chamado automaticamente ao mudar filtros (ex: busca, select de profissional)

## Tasks / Subtasks

- [ ] **Task 1 — Endpoint `GET /api/v1/agendamentos/{id}/historico`** (AC-3)
  - [ ] Adicionar ao `AgendamentoController.java`:
    ```java
    @GetMapping("/{id}/historico")
    @PreAuthorize("hasAnyRole('STAFF','PROFISSIONAL','ADMIN_EMPRESA')")
    public Page<HistoricoStatusResponse> listarHistorico(
        @PathVariable Long id,
        @PageableDefault(size = 20, sort = "timestamp", direction = DESC) Pageable pageable
    ) { ... }
    ```
  - [ ] Adicionar ao `AgendamentoService.java`:
    - Verificar que `agendamento.empresaId == SecurityUtils.getEmpresaId()` → 404 se não
    - Retornar `historicoRepository.findAllByAgendamentoId(id, pageable)` (Page)
  - [ ] Adicionar ao `HistoricoStatusRepository.java`:
    ```java
    Page<HistoricoStatus> findAllByAgendamentoIdOrderByTimestampDesc(Long agendamentoId, Pageable pageable);
    ```

- [ ] **Task 2 — Auditoria dos endpoints existentes** (AC-2)
  - [ ] Verificar `GET /api/v1/clientes` (Story 3.6 usou `PageImpl` manual): confirmar que retorna `{ content, page, size, totalElements, totalPages }` — corrigir se divergir
  - [ ] Verificar `GET /api/v1/profissionais` (Story 2.4): confirmar envelope Spring `Page<T>`
  - [ ] Verificar `GET /api/v1/staff` (Story 2.5): confirmar envelope Spring `Page<T>`
  - [ ] Registrar no `Completion Notes List` qualquer divergência encontrada e corrida

- [ ] **Task 3 — `PageResponseWrapper` utilitário (se necessário)** (AC-1)
  - [ ] Se algum endpoint retornar `Page<T>` diretamente do Spring (que serializa com campos extras), criar `api/dtos/response/PageResponse.java` para serialização explícita com exatamente 5 campos:
    ```java
    record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
        public static <T> PageResponse<T> of(Page<T> page) {
            return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
            );
        }
    }
    ```
  - [ ] Se `Page<T>` do Spring já serializa com os campos necessários (depende da versão), wrapper não é necessário — verificar e decidir

- [ ] **Task 4 — `Paginacao.tsx` — componente reutilizável** (AC-4)
  - [ ] Criar `src/shared/components/Paginacao.tsx`:
    ```tsx
    interface PaginacaoProps {
      page: number          // 0-based
      totalPages: number
      totalElements: number
      onPageChange: (page: number) => void
    }

    export function Paginacao({ page, totalPages, totalElements, onPageChange }: PaginacaoProps) {
      if (totalPages <= 1) return null

      return (
        <div className="flex items-center justify-between py-3">
          <span className="text-sm text-muted-foreground">
            Página {page + 1} de {totalPages} ({totalElements} registros)
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm"
              disabled={page === 0}
              onClick={() => onPageChange(page - 1)}>
              ← Anterior
            </Button>
            <Button variant="outline" size="sm"
              disabled={page === totalPages - 1}
              onClick={() => onPageChange(page + 1)}>
              Próxima →
            </Button>
          </div>
        </div>
      )
    }
    ```

- [ ] **Task 5 — `usePaginacao` hook** (AC-6)
  - [ ] Criar `src/shared/hooks/usePaginacao.ts`:
    ```ts
    export function usePaginacao(pageSize = 20) {
      const [page, setPage] = useState(0)
      const resetPage = () => setPage(0)
      return { page, setPage, pageSize, resetPage }
    }
    ```

- [ ] **Task 6 — Integrar `<Paginacao>` e `usePaginacao` nas telas** (AC-5)
  - [ ] `ClientesPage.tsx`: substituir controles de paginação manuais (se existirem) por `<Paginacao>`; usar `usePaginacao`; chamar `resetPage` ao mudar busca
  - [ ] `ProfissionaisPage.tsx`: adicionar `<Paginacao>` + `usePaginacao`; `resetPage` ao mudar filtros
  - [ ] `StaffPage.tsx`: adicionar `<Paginacao>` + `usePaginacao`
  - [ ] `RelatoriosPage.tsx` — seção no-shows: já integrada na Story 5.2; verificar uso de `<Paginacao>`

- [ ] **Task 7 — Atualizar `AgendamentoDetalhePage.tsx` para usar endpoint de histórico** (AC-3)
  - [ ] Separar carregamento do histórico em `useQuery` separado usando `GET /api/v1/agendamentos/{id}/historico`
  - [ ] Adicionar `<Paginacao>` na lista de histórico da `AgendamentoDetalhePage.tsx` (Story 4.3)

- [ ] **Task 8 — Testes** (AC-1, AC-3, AC-4)
  - [ ] Teste de integração `HistoricoControllerIT`:
    - GET histórico sem auth → 401
    - GET histórico de outro empresa → 404
    - GET histórico com 25 registros: page=0,size=20 → `totalElements=25`, `totalPages=2`, `content.length=20`
    - GET histórico page=1,size=20 → `content.length=5`
  - [ ] Teste unitário `PaginacaoComponentTest` (React Testing Library):
    - `totalPages <= 1`: renderiza null
    - Botão Anterior desabilitado em page=0
    - Botão Próxima desabilitado em última página
    - Clique em Próxima: chama `onPageChange(1)`

## Dev Notes

### Spring `Page<T>` — Campos Serializados por Padrão

O `org.springframework.data.domain.Page<T>` serializado por Jackson inclui, além dos 5 campos, campos extras como `pageable`, `sort`, `first`, `last`, `empty`, `numberOfElements`. Se a API deve retornar exatamente 5 campos, usar `PageResponse.of(page)` em todos os controllers.

```java
// Controller — retornar PageResponse explícito:
@GetMapping
public PageResponse<ClienteResponse> listar(
    @PageableDefault(size = 20) Pageable pageable
) {
    return PageResponse.of(clienteService.listar(pageable));
}
```

> Se o frontend já consome os campos extras sem problema, `PageResponse` não é necessário. Decidir na auditoria da Task 2.

### `@PageableDefault` vs `?page=&size=`

```java
// Usar @PageableDefault para valores padrão legíveis:
@PageableDefault(size = 20, sort = "nome", direction = ASC) Pageable pageable
// Equivale a: ?page=0&size=20&sort=nome,asc
```

### `usePaginacao` — Uso com `useQuery`

```tsx
// Padrão de uso nas telas:
const { page, setPage, pageSize, resetPage } = usePaginacao()

// Resetar ao mudar filtro de busca:
const handleBuscaChange = (valor: string) => {
  setBusca(valor)
  resetPage()
}

// Passar page ao useQuery:
const { data } = useQuery({
  queryKey: ['clientes', page, busca],
  queryFn: () => api.get(`/api/v1/clientes?page=${page}&size=${pageSize}&nome=${busca}`)
    .then(r => r.data)
})

// Renderizar paginação:
<Paginacao
  page={page}
  totalPages={data?.totalPages ?? 0}
  totalElements={data?.totalElements ?? 0}
  onPageChange={setPage}
/>
```

### Story 3.6 — `PageImpl` Manual

A Story 3.6 implementou busca de clientes com `PageImpl` manual (busca em memória por causa da criptografia AES). Verificar se o objeto retornado já serializa com `{ content, page, size, totalElements, totalPages }` compatíveis com o envelope padrão. Se sim, nenhuma mudança de backend — apenas integrar `<Paginacao>` no frontend.

### Mapa de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `domain/repositories/HistoricoStatusRepository.java` | **UPDATE** | Adicionar `findAllByAgendamentoId(id, pageable)` |
| `domain/services/AgendamentoService.java` | **UPDATE** | Método `listarHistorico()` |
| `api/controllers/AgendamentoController.java` | **UPDATE** | `GET /{id}/historico` |
| `api/dtos/response/PageResponse.java` | NEW (se necessário) | Wrapper com exatamente 5 campos |
| `src/shared/components/Paginacao.tsx` | NEW | Componente reutilizável |
| `src/shared/hooks/usePaginacao.ts` | NEW | Hook de estado de página |
| `src/pages/ClientesPage.tsx` | **UPDATE** | Integrar `<Paginacao>` |
| `src/pages/ProfissionaisPage.tsx` | **UPDATE** | Integrar `<Paginacao>` |
| `src/pages/StaffPage.tsx` | **UPDATE** | Integrar `<Paginacao>` |
| `src/pages/RelatoriosPage.tsx` | **UPDATE** (verificar) | Confirmar `<Paginacao>` na seção no-shows |
| `src/pages/AgendamentoDetalhePage.tsx` | **UPDATE** | Separar histórico em query paginada |
| `test/.../HistoricoControllerIT.java` | NEW | |

### Referências

- [Source: epics.md#Story 5.3] — Acceptance Criteria completos e FR-046
- [Source: 3-6-listagem-busca-clientes.md] — `PageImpl` manual; busca em memória AES
- [Source: 4-3-transicoes-status-historico.md] — `HistoricoStatusRepository`, `HistoricoStatusResponse`
- [Source: 3-1-componentes-base-ui.md] — `Skeleton`, padrões de componentes Shadcn/ui

## Dev Agent Record

### Agent Model Used

_a preencher pelo agente dev_

### Debug Log References

### Completion Notes List

### File List

_a preencher após implementação_
