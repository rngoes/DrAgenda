# Story 1.4: Página Pública de Política de Privacidade

Status: done

## Story

Como paciente ou visitante,
Quero acessar a política de privacidade e o termo de consentimento LGPD sem precisar fazer login,
Para que eu possa conhecer como meus dados serão usados antes de consentir.

## Acceptance Criteria

**AC-1 — Rota pública sem autenticação**
- Given qualquer visitante (sem token JWT) acessa `/politica`
- When a página carrega
- Then o conteúdo é exibido sem exigir autenticação
- And a rota HTTP retorna 200 mesmo sem cookies ou headers de Authorization
- And a rota já está configurada como pública no `App.tsx` (feito na Story 1.3 — sem ProtectedRoute)

**AC-2 — Conteúdo da página**
- Given o visitante acessa `/politica`
- When a página renderiza
- Then exibe o wordmark "DrAgenda" (DM Sans) no topo
- And exibe o texto da política de privacidade com ao menos as seções: "Dados que coletamos", "Como usamos seus dados", "Seus direitos (LGPD)", "Contato"
- And exibe o texto do termo de consentimento LGPD com a versão vigente (`v1.0`) e a data de vigência
- And a página é responsiva e funcional a partir de 320px de largura

**AC-3 — Link visível na tela de Login**
- Given o usuário está na tela de login (`/login`)
- When a tela renderiza
- Then um link visível "Política de Privacidade" está presente no rodapé da tela
- And o link navega para `/politica` (FR-054)

**AC-4 — Link visível na página de Autoatendimento**
- Given o paciente acessa `/cancelar/:token`
- When a página de autoatendimento renderiza
- Then um link visível "Política de Privacidade" está presente na página
- And o link navega para `/politica` (FR-054)

**AC-5 — Versão do termo acessível como constante**
- Given o backend precisa registrar a versão do termo no momento do consentimento (Story 3.5)
- When o texto do termo é exibido
- Then a versão `v1.0` é visível na página E armazenada como constante exportável `TERMO_VERSAO_VIGENTE = "v1.0"` em `src/shared/lib/constants.ts`
- And qualquer mudança futura de versão requer atualização apenas nessa constante

## Tasks / Subtasks

- [ ] **Task 1 — Criar `PoliticaPage.tsx` com conteúdo real** (AC-1, AC-2)
  - [ ] Substituir o stub criado na Story 1.3 com conteúdo estruturado
  - [ ] Usar layout centralizado, `max-w-2xl mx-auto px-4 py-8`
  - [ ] Exibir wordmark "DrAgenda" no topo com `font-brand` (DM Sans) e cor `brand-500`
  - [ ] Estruturar o documento com `<h1>`, `<h2>` e `<p>` com tipografia da escala Inter
  - [ ] Incluir versão e data de vigência do termo visíveis no final: `"Versão v1.0 — vigência a partir de 04/06/2026"`

- [ ] **Task 2 — Criar constante da versão do termo** (AC-5)
  - [ ] Criar `src/shared/lib/constants.ts` (ou adicionar a um arquivo de constantes existente)
  - [ ] Exportar: `export const TERMO_VERSAO_VIGENTE = 'v1.0'`
  - [ ] Importar e usar na `PoliticaPage.tsx` para exibir a versão dinamicamente

- [ ] **Task 3 — Adicionar link na `LoginPage.tsx`** (AC-3)
  - [ ] Atualizar o stub de `LoginPage.tsx` (criado na Story 1.3) para incluir um rodapé com link
  - [ ] Link: `<a href="/politica">Política de Privacidade</a>` com estilo `text-sm text-secondary underline`
  - [ ] Posicionado no `<footer>` ou `<div>` inferior do formulário de login

- [ ] **Task 4 — Adicionar link na `AutoatendimentoPage.tsx`** (AC-4)
  - [ ] Atualizar o stub de `AutoatendimentoPage.tsx` (criado na Story 1.3) para incluir link
  - [ ] Link: `<Link to="/politica">Política de Privacidade</Link>` com React Router
  - [ ] Posicionado no rodapé da página pública

- [ ] **Task 5 — Garantir responsividade a partir de 320px** (AC-2)
  - [ ] Testar layout em viewport de 320px (iPhone SE mais antigo)
  - [ ] Garantir que o texto não causa scroll horizontal (`overflow-x: hidden` no container se necessário)
  - [ ] Padding mínimo `px-4` (16px) em ambos os lados

## Dev Notes

### ⚠️ Esta story modifica stubs da Story 1.3

`LoginPage.tsx` e `AutoatendimentoPage.tsx` foram criados como stubs na Story 1.3. Esta story adiciona conteúdo mínimo a ambos para satisfazer FR-054. As implementações completas dessas páginas ocorrem nas Stories 2.1 (Login completo) e 4.5 (Autoatendimento completo) respectivamente. **Não implementar** a lógica de login ou autoatendimento aqui — apenas o link de rodapé.

### Estrutura de Arquivos

| Arquivo | Operação | Notas |
|---|---|---|
| `src/pages/PoliticaPage.tsx` | **UPDATE** | Substituir stub por conteúdo real |
| `src/pages/LoginPage.tsx` | **UPDATE** | Adicionar link de rodapé para `/politica` |
| `src/pages/AutoatendimentoPage.tsx` | **UPDATE** | Adicionar link de rodapé para `/politica` |
| `src/shared/lib/constants.ts` | NEW | `TERMO_VERSAO_VIGENTE = 'v1.0'` |

### `PoliticaPage.tsx` — Estrutura de Conteúdo

```tsx
import { TERMO_VERSAO_VIGENTE } from '../shared/lib/constants'

export default function PoliticaPage() {
  return (
    <div className="min-h-screen bg-[var(--bg-base)]">
      <div className="max-w-2xl mx-auto px-4 py-8">
        {/* Wordmark */}
        <h1 className="font-brand font-medium text-2xl text-brand-500 mb-8">
          <span className="font-light text-[var(--text-primary)]">Dr</span>Agenda
        </h1>

        <h2 className="text-xl font-semibold text-[var(--text-primary)] mb-6">
          Política de Privacidade e Termo de Consentimento LGPD
        </h2>

        {/* Seção 1 */}
        <section className="mb-6">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            1. Dados que coletamos
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Coletamos nome completo, telefone e data de nascimento para fins de
            agendamento de consultas. Não armazenamos diagnósticos, procedimentos
            clínicos ou qualquer informação de saúde além dos dados de agendamento.
          </p>
        </section>

        {/* Seção 2 */}
        <section className="mb-6">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            2. Como usamos seus dados
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Seus dados são utilizados exclusivamente para gerenciar seus agendamentos
            de consulta na clínica. Não compartilhamos seus dados com terceiros.
            Todos os dados sensíveis são criptografados em repouso (AES-256).
          </p>
        </section>

        {/* Seção 3 */}
        <section className="mb-6">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            3. Seus direitos (LGPD)
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Conforme a Lei Geral de Proteção de Dados (Lei nº 13.709/2018), você
            tem direito a: acessar seus dados, corrigir informações incorretas,
            solicitar a exclusão (anonimização) de seus dados pessoais e revogar
            o consentimento a qualquer momento. Entre em contato com a clínica
            para exercer seus direitos.
          </p>
        </section>

        {/* Seção 4 */}
        <section className="mb-8">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            4. Contato
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Para dúvidas ou solicitações sobre seus dados pessoais, entre em
            contato diretamente com a clínica responsável pelo seu agendamento.
          </p>
        </section>

        {/* Rodapé com versão */}
        <footer className="border-t border-[var(--border)] pt-4">
          <p className="text-xs text-[var(--text-disabled)]">
            Versão {TERMO_VERSAO_VIGENTE} — vigência a partir de 04/06/2026
          </p>
        </footer>
      </div>
    </div>
  )
}
```

### Link na `LoginPage.tsx` — Adição Mínima

```tsx
// Adicionar ao stub de LoginPage.tsx — apenas o rodapé com link
// A implementação completa do formulário de login é responsabilidade da Story 2.1

export default function LoginPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[var(--bg-base)]">
      <div className="w-full max-w-sm px-4">
        {/* TODO Story 2.1: formulário de login completo */}
        <p className="text-center text-[var(--text-secondary)]">Login — em breve</p>
      </div>
      <footer className="mt-8 text-center">
        <a
          href="/politica"
          className="text-sm text-[var(--text-secondary)] underline hover:text-[var(--text-primary)]"
        >
          Política de Privacidade
        </a>
      </footer>
    </div>
  )
}
```

### Link na `AutoatendimentoPage.tsx` — Adição Mínima

```tsx
import { Link } from 'react-router-dom'

export default function AutoatendimentoPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[var(--bg-base)]">
      <div className="w-full max-w-sm px-4">
        {/* TODO Story 4.5: implementação completa do autoatendimento */}
        <p className="text-center text-[var(--text-secondary)]">Autoatendimento — em breve</p>
      </div>
      <footer className="mt-8 text-center">
        <Link
          to="/politica"
          className="text-sm text-[var(--text-secondary)] underline hover:text-[var(--text-primary)]"
        >
          Política de Privacidade
        </Link>
      </footer>
    </div>
  )
}
```

> **Nota:** `LoginPage` usa `<a href>` (não React Router `<Link>`) por simplicidade — a rota `/politica` é no mesmo app, mas dado que o `LoginPage` ainda não tem `BrowserRouter` wrapper direto neste contexto de stub, `<a href>` funciona de forma segura. Em produção, após a Story 2.1, pode ser migrado para `<Link>`.

### `src/shared/lib/constants.ts`

```typescript
/** Versão do termo de consentimento LGPD vigente.
 *  Ao atualizar o texto do termo, incrementar esta versão.
 *  Usada em: PoliticaPage (exibição) e Story 3.5 (registro no banco).
 */
export const TERMO_VERSAO_VIGENTE = 'v1.0'
```

### Conclusão do Epic 1

Esta é a última story do Epic 1. Ao concluir a Story 1.4, o Epic 1 estará completo:

| Story | Entrega |
|---|---|
| 1.1 | Monorepo + CI/CD + deploy Railway/Vercel |
| 1.2 | Flyway migrations V1+V2 + seed Admin Sistema |
| 1.3 | Design tokens + roteamento + QueryClient + Axios |
| 1.4 | Página de política de privacidade + links FR-054 |

Após completar e revisar todas as stories do Epic 1, o Epic 2 pode ser iniciado. O Epic 2 assume que:
- Backend está rodando no Railway com MySQL configurado
- Migrations V1 e V2 existem
- Frontend está deployado no Vercel com todas as rotas configuradas
- `useAuth` está pronto para receber dados de login (Story 2.1 popula o localStorage)

### Referências

- [Source: epics.md#Story 1.4] — Acceptance Criteria completos
- [Source: EXPERIENCE.md#2.1 Mapa de Telas] — `/politica` e `/cancelar/:token` como rotas públicas
- [Source: architecture.md#Regras de Processo] — `GET /api/v1/cancelar/{token}` bypassa filtro de tenant (aplicável analogamente à `/politica` no frontend)
- [Source: epics.md#FR-054] — Link visível em login e autoatendimento

## Dev Agent Record

### Agent Model Used
Claude Sonnet 4.6 (GitHub Copilot)

### Completion Notes List
- `constants.ts` já existia (criado pelo Shadcn init na Story 1.3) — `TERMO_VERSAO_VIGENTE` adicionado ao arquivo existente
- Build, lint e type-check validados localmente antes do push

### File List
- `frontend/src/pages/PoliticaPage.tsx` — UPDATED (stub → conteúdo real com 4 seções LGPD)
- `frontend/src/pages/LoginPage.tsx` — UPDATED (adicionado rodapé com link)
- `frontend/src/pages/AutoatendimentoPage.tsx` — UPDATED (adicionado rodapé com link)
- `frontend/src/shared/lib/constants.ts` — NEW (`TERMO_VERSAO_VIGENTE = 'v1.0'`)
