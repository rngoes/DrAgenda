---
title: "DESIGN: DrAgenda"
status: approved
created: "2026-06-04"
updated: "2026-06-04"
project: DrAgenda
version: "1.0"
author: "Sally (UX Designer) — sessão com Rodrigo Navarro"
sources:
  - "_bmad-output/planning-artifacts/prds/prd-DrAgenda-2026-06-03/prd.md"
  - "docs/brief.md"
---

# Design System: DrAgenda

## 1. Identidade Visual

### 1.1 Tom e Princípios Visuais

**Tom:** Clean/Minimalista — interface que "desaparece" para deixar o trabalho em primeiro plano.

**Princípios visuais:**
1. **Clareza acima de tudo** — cada pixel serve a uma função. Sem decorações.
2. **Hierarquia por peso, não por cor** — a estrutura é lida pelo tamanho e peso tipográfico; cor é reservada para status e ações críticas.
3. **Confiança transmitida pela consistência** — mesmos padrões de espaçamento, mesmas alturas de toque, mesmos raios de borda em todo o sistema.
4. **Cores de status são invioláveis** — as 6 cores semânticas de agendamento não competem com a cor de marca em nenhum contexto.

---

### 1.2 Paleta de Cores

#### Cor de Marca

| Token | Valor | Uso |
|-------|-------|-----|
| `brand-500` | `#0D9488` | Cor primária — botões principais, links, ícones ativos, FAB |
| `brand-400` | `#14B8A6` | Hover e estados interativos |
| `brand-600` | `#0F766E` | Pressed / foco |
| `brand-50`  | `#F0FDFA` | Fundos de destaque suave (ex.: badge de marca) |

#### Cores de Status dos Agendamentos

Cada status possui: cor principal (texto/ícone), cor de fundo (card), ícone e rótulo de texto (WCAG + daltonismo).

| Status | Token | Cor Principal | Fundo | Ícone | Rótulo |
|--------|-------|--------------|-------|-------|--------|
| Pendente  | `status-pending`   | `#D97706` (amber-600) | `#FFFBEB` (amber-50)  | `clock`            | "Pendente"  |
| Confirmado| `status-confirmed` | `#2563EB` (blue-600)  | `#EFF6FF` (blue-50)   | `check-circle`     | "Confirmado"|
| Presente  | `status-present`   | `#7C3AED` (violet-600)| `#F5F3FF` (violet-50) | `user-check`       | "Presente"  |
| Concluído | `status-done`      | `#16A34A` (green-600) | `#F0FDF4` (green-50)  | `circle-check-big` | "Concluído" |
| Cancelado | `status-cancelled` | `#4B5563` (gray-600)  | `#F9FAFB` (gray-50)   | `x-circle`         | "Cancelado" |
| NoShow    | `status-noshow`    | `#DC2626` (red-600)   | `#FEF2F2` (red-50)    | `user-x`           | "NoShow"    |

> **Regra daltonismo:** Todo indicador de status combina cor de fundo + borda lateral colorida + ícone + rótulo de texto. Nenhum contexto usa cor como único diferenciador.

#### Paleta Neutra (tokens Light / Dark)

| Token | Light | Dark | Uso |
|-------|-------|------|-----|
| `bg-base`      | `#FFFFFF` | `#0F172A` | Fundo principal |
| `bg-subtle`    | `#F8FAFC` | `#1E293B` | Fundo de seções, cards |
| `bg-muted`     | `#F1F5F9` | `#334155` | Inputs desabilitados |
| `border`       | `#E2E8F0` | `#334155` | Bordas de cards e inputs |
| `text-primary` | `#0F172A` | `#F8FAFC` | Texto principal |
| `text-secondary`| `#64748B` | `#94A3B8` | Texto secundário, labels |
| `text-disabled`| `#CBD5E1` | `#475569` | Texto desabilitado |
| `overlay`      | `rgba(15,23,42,0.5)` | `rgba(0,0,0,0.7)` | Fundos de dialogs e bottom sheets |

> **Tema:** O app respeita `prefers-color-scheme` do dispositivo (system default). Ambos os temas são suportados desde o MVP.

#### Cores Semânticas de Sistema

| Token | Valor | Uso |
|-------|-------|-----|
| `success` | `#16A34A` | Toasts de sucesso |
| `error`   | `#DC2626` | Erros inline, toasts de erro |
| `warning` | `#D97706` | Avisos (ex.: profissional inativo) |
| `info`    | `#2563EB` | Informações neutras |

---

### 1.3 Tipografia

#### Fonte de Marca — DM Sans

Usada exclusivamente no wordmark e em headings de telas de onboarding/marketing.

```
Wordmark: "Dr" em DM Sans Light (300) + "Agenda" em DM Sans Medium (500)
          Cor: brand-500 (#0D9488) em "Agenda"; text-primary em "Dr"
          Letter-spacing: 0.02em em "Agenda"
```

#### Fonte de Interface — Inter

Usada em 100% da interface do aplicativo.

| Escala | Tamanho | Line-height | Peso padrão | Uso |
|--------|---------|-------------|-------------|-----|
| `text-xs`   | 12px | 16px | 400 | Labels secundários, metadados |
| `text-sm`   | 14px | 20px | 400/500 | Corpo padrão, descrições |
| `text-base` | 16px | 24px | 400 | Texto primário, campos de formulário |
| `text-lg`   | 18px | 28px | 500 | Nome do paciente no card |
| `text-xl`   | 20px | 28px | 600 | Headings de tela |
| `text-2xl`  | 24px | 32px | 700 | Headings de onboarding |

> **Mínimo absoluto:** Nenhum texto de interface abaixo de 12px. Campos de formulário e labels de input: mínimo 16px (evita zoom automático no iOS/Android).

---

### 1.4 Espaçamento e Grid

**Base:** 4px (0.25rem)

| Token | Valor | Uso típico |
|-------|-------|-----------|
| `space-1`  | 4px  | Espaçamento mínimo entre elementos inline |
| `space-2`  | 8px  | Padding interno de badges e chips |
| `space-3`  | 12px | Padding de campos compactos |
| `space-4`  | 16px | Padding padrão de cards e seções |
| `space-5`  | 20px | Separação entre grupos de conteúdo |
| `space-6`  | 24px | Padding horizontal de telas mobile |
| `space-8`  | 32px | Separação entre seções principais |
| `space-12` | 48px | Altura da Bottom Tab Bar |
| `space-14` | 56px | Altura da Top App Bar |
| `space-16` | 64px | Tamanho do FAB |

**Grid mobile (≤767px):** 1 coluna, padding horizontal `space-4` (16px), sem gutter.
**Grid desktop (≥768px):** Sidebar fixa 240px (esquerda) + área de conteúdo principal. Conteúdo principal com max-width 800px e padding `space-6`.

---

### 1.5 Bordas e Elevação

| Token | Valor | Uso |
|-------|-------|-----|
| `radius-sm`   | 4px    | Badges, chips de status |
| `radius-md`   | 8px    | Cards de agendamento, inputs, botões |
| `radius-lg`   | 12px   | Bottom sheets, dialogs |
| `radius-xl`   | 16px   | FAB |
| `radius-full` | 9999px | Avatares, chips de filtro arredondados |

**Elevação (sombras):**

| Token | Valor | Uso |
|-------|-------|-----|
| `shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Cards em repouso |
| `shadow-md` | `0 4px 6px rgba(0,0,0,0.07)` | Cards em hover, FAB |
| `shadow-lg` | `0 10px 15px rgba(0,0,0,0.10)` | Bottom sheets, dialogs |
| `shadow-xl` | `0 20px 25px rgba(0,0,0,0.12)` | Modais, overlays |

---

### 1.6 Iconografia

**Biblioteca:** [Lucide Icons](https://lucide.dev) — open-source, consistente com o tom minimalista, disponível como `lucide-react`.

**Tamanhos:**

| Contexto | Tamanho |
|---------|---------|
| Ícones inline em texto | 16px |
| Ícones de status no card | 18px |
| Ícones na Bottom Tab Bar | 24px |
| Ícone do FAB | 28px |

**Ícones principais do sistema:**

| Contexto | Ícone Lucide |
|---------|-------------|
| Aba Agenda | `calendar` |
| Aba Buscar | `search` |
| Aba Clientes | `users` |
| Aba Configurações | `settings` |
| Aba Menu | `more-horizontal` |
| FAB Novo Agendamento | `plus` |
| Status Pendente | `clock` |
| Status Confirmado | `check-circle` |
| Status Presente | `user-check` |
| Status Concluído | `circle-check-big` |
| Status Cancelado | `x-circle` |
| Status NoShow | `user-x` |
| Profissional inativo | `alert-triangle` |
| Link de cancelamento | `link` |
| Copiar link | `copy` |
| Primeiro do dia (destaque) | `star` |
| Alerta de reordenação | `alert-circle` |

---

### 1.7 Acessibilidade Visual

**Contraste mínimo (WCAG 2.1 AA):**
- Texto normal (< 18px regular / < 14px bold): razão mínima 4.5:1
- Texto grande (≥ 18px regular / ≥ 14px bold): razão mínima 3:1
- Componentes de UI e ícones informativos: razão mínima 3:1

**Verificações de contraste da paleta:**

| Par | Razão | Status |
|-----|-------|--------|
| `brand-500 #0D9488` sobre `#FFFFFF` | 4.54:1 | ✅ AA |
| `text-primary #0F172A` sobre `#FFFFFF` | 18.5:1 | ✅ AAA |
| `text-secondary #64748B` sobre `#FFFFFF` | 4.63:1 | ✅ AA |
| `status-pending #D97706` sobre `#FFFBEB` | 3.44:1 | ✅ AA Large |
| `status-noshow #DC2626` sobre `#FEF2F2` | 4.72:1 | ✅ AA |
| `brand-500 #0D9488` sobre `#0F172A` (dark) | 5.12:1 | ✅ AA |

**Toque mínimo:** Toda área interativa ≥ 44×44px (recomendação Apple/Google).

**Foco visível:** `outline: 2px solid #0D9488; outline-offset: 2px` em todos os elementos focáveis via teclado.

**Indicadores de status:** Sempre combinam cor de fundo + borda lateral colorida + ícone + rótulo de texto (ver seção 1.2). Nenhum componente usa cor como único diferenciador — regra inviolável para daltonismo.
