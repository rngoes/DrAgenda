---
title: "EXPERIENCE: DrAgenda"
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

# Experience Design: DrAgenda

## 1. Princípios de Design

1. **2 toques como regra, não meta** — qualquer ação frequente (atualizar status, ver agenda do dia, criar agendamento) deve ser completada em ≤2 toques. Ações de exceção (double-booking, reversão de NoShow, cancelamento de Presente) toleram toque adicional de confirmação explícita — conforme previsto em FR-028.
2. **Contexto nunca abandona o usuário** — Maria não perde a agenda enquanto preenche um formulário (bottom sheet). Wanessa não precisa navegar para encontrar o paciente atual.
3. **Erros explicam, não acusam** — mensagens de validação descrevem o conflito e sugerem a solução. Ex.: *"Dr. Carlos não atende às terças. Próximo disponível: quarta 09:00"*.
4. **Perfis enxergam apenas o que precisam** — Wanessa não vê botões de edição de cadastro. André não vê formulários de agendamento que não usará.
5. **Status é sempre visível, nunca ambíguo** — cor + ícone + rótulo em todo card de agendamento. Nenhum usuário depende apenas da cor.

---

## 2. Arquitetura de Informação

### 2.1 Mapa de Telas

```
DrAgenda PWA
│
├── /login                  → Tela de Login
│
├── /agenda                 → Dashboard de Agenda (tela principal)
│   ├── /agenda/dia         → Visualização diária (padrão)
│   └── /agenda/semana      → Visualização semanal
│
├── /agendamento/novo       → Bottom Sheet: Novo Agendamento (FAB)
├── /agendamento/:id        → Detalhe do Agendamento
│
├── /buscar                 → Busca de Próximo Disponível (somente Staff)
│
├── /clientes               → Lista de Clientes (somente Staff)
│   ├── /clientes/novo      → Cadastro de Cliente
│   └── /clientes/:id       → Detalhe do Cliente
│
├── /configuracoes          → Configurações (somente Admin Empresa)
│   ├── /profissionais      → Gerenciar Profissionais
│   ├── /disponibilidade    → Disponibilidade por Profissional
│   ├── /tipos-atend        → Tipos de Atendimento
│   └── /clinica            → Dados da Clínica (fuso horário, nome)
│
├── /cancelar/:token        → Autoatendimento Público (sem login — paciente confirma ou cancela)
│
└── /menu                   → Menu: Perfil, Alterar senha, Sair
```

### 2.2 Navegação por Perfil

**Staff (Maria)**
```
Bottom Tab:  [ 📅 Agenda ]  [ 🔍 Buscar ]  [ 👥 Clientes ]  [ ··· Menu ]
FAB:         [ + ]  →  Bottom Sheet: Novo Agendamento (todos os profissionais)
```

**Profissional (Wanessa)**
```
Bottom Tab:  [ 📅 Agenda ]  [ ··· Menu ]
FAB:         [ + ]  →  Bottom Sheet: Novo Agendamento
             (campo "Profissional" pré-preenchido com ela e bloqueado para edição)
```

**Admin Empresa (André)**
```
Bottom Tab:  [ 📅 Agenda ]  [ ⚙️ Configurações ]  [ ··· Menu ]
Sem FAB.
```

**Desktop (≥768px) — todos os perfis:**
```
Sidebar fixa, 240px à esquerda:
  [Logo DrAgenda]
  ─────────────────
  📅 Agenda
  🔍 Buscar          (somente Staff)
  👥 Clientes        (somente Staff)
  ⚙️ Configurações   (somente Admin Empresa)
  ─────────────────
  👤 Perfil
  Sair

FAB mantido no canto inferior direito da área de conteúdo principal (Staff e Profissional).
```

---

## 3. Padrões de Interação

### 3.1 Card de Agendamento

**Anatomia do Card (mobile, ≤767px):**

```
┌─────────────────────────────────────────────────┐
│▌ 14:00 → 14:30                  🕐 ⏰ Pendente  │  ← borda esquerda colorida + badge status
│  João da Silva                                  │  ← nome do paciente (text-lg, semibold)
│  Consulta de Retorno · Dra. Wanessa             │  ← tipo + profissional (text-sm, secondary)
│                                [Confirmar] ···  │  ← ação primária + menu
└─────────────────────────────────────────────────┘
```

**Borda esquerda colorida:** 4px sólida na cor principal do status. Identificador visual imediato mesmo para leitura periférica.

**Botão de ação primária contextual** (1 toque abre confirmação → 2 toques completa a ação):

| Status atual | Label do botão | Transição | Perfis autorizados |
|-------------|----------------|-----------|-------------------|
| Pendente    | "Confirmar"    | → Confirmado | Staff, Admin |
| Confirmado  | "Chegou"       | → Presente   | Staff, Admin |
| Presente    | "Concluir"     | → Concluído  | Staff, Admin, Profissional |
| NoShow      | "Reverter"     | → Dialog de reversão | Staff |
| Concluído   | —              | (estado terminal — sem botão) | — |
| Cancelado   | —              | (estado terminal — sem botão) | — |

**Menu ··· (mais ações):**
- Ver detalhes
- Editar agendamento *(Staff, somente em status não-terminais)*
- Gerar link de cancelamento *(Staff/Admin)*
- Cancelar agendamento *(Staff/Admin, somente em status não-terminais)*

**Toque no corpo do card** (fora dos botões): abre a tela de detalhe `/agendamento/:id`.

**Destaque do primeiro agendamento (FR-018):**
Card com badge `⭐ Primeiro do dia` em `brand-50`, visível somente na visualização do Profissional.

**Alerta de reordenação (FR-020):**
Quando um paciente "Presente" tem horário posterior ao próximo "Pendente/Confirmado", o card exibe badge `⚠️ Chegou antes do horário`. Nenhuma ação automática — Wanessa decide se atende fora de ordem.

**Profissional inativo:**
Cards de agendamentos futuros de profissional inativado exibem banner `⚠️ Profissional inativo — redistribuir manualmente` em cor `warning`. Não são cancelados automaticamente (FR-009).

---

### 3.2 FAB + Bottom Sheet (Novo Agendamento)

**FAB:**
- Posição: canto inferior direito, `24px` das bordas, `24px` acima da Bottom Tab Bar
- Tamanho: 56×56px, `radius-xl`, cor `brand-500`, sombra `shadow-md`
- Ícone: `plus` (Lucide, 28px, branco)
- Animação: rotação 45° ao abrir o bottom sheet (indica "fechar")
- Visível para: Staff e Profissional

**Bottom Sheet — formulário de novo agendamento:**

```
────────── handle ──────────
Novo Agendamento                                [✕]
────────────────────────────

🔍 Cliente *
   [Campo de busca por nome ou CPF          ]
   [+ Cadastrar novo cliente]

📋 Tipo de Atendimento *
   [Seletor                                 ▾]
   Duração calculada: "30 min"

👤 Profissional *
   [Seletor                                 ▾]
   (bloqueado para Wanessa — pré-preenchido)

📅 Data *
   [Date picker nativo                      ]

⏰ Hora *
   [Time picker nativo                      ]

   ✅ "Dra. Wanessa disponível às 14:00"
   ❌ "Dra. Wanessa não atende às terças.
       Próximo disponível: quarta 09:00"

────────────────────────────
[   Cancelar   ]            [   Agendar →   ]
```

**Comportamento de drag:** Bottom sheet expande ao arrastar para cima (altura máxima: 90vh). Fecha ao arrastar para baixo abaixo de 40% da altura ou ao tocar no overlay.

**Desktop (≥768px):** Mesmo formulário apresentado como drawer lateral deslizante da direita (380px de largura), mantendo a agenda visível ao fundo.

---

### 3.3 Transições de Status

**Fluxo permitido (FR-027):**

```
Pendente ──→ Confirmado ──→ Presente ──→ Concluído  (terminal)
                      ↘               ↘
                       Cancelado        Cancelado   (terminal, exige justificativa)
                       (terminal)

Pendente / Confirmado ──→ NoShow   (automático, job +30min — FR-030)
NoShow ──→ Presente                (reversão manual, Staff, confirmação explícita)
```

**Cancelamento de "Presente":**
Exige campo de justificativa (texto livre, obrigatório). Registrado no histórico com timestamp e usuário responsável.

**Reversão de NoShow:**
Dialog: *"Reverter NoShow para Presente?"*
Subtexto: *"O registro de NoShow será mantido no histórico do agendamento para fins de relatório."*
Botões: `[Cancelar]` `[Reverter]`

---

### 3.4 Feedback de Validação

**Erros inline (campos do formulário):**
- Campo problemático: borda `error` (`#DC2626`) + ícone `alert-circle` à direita
- Texto abaixo do campo: `text-sm`, cor `error`, linguagem direta com sugestão quando possível

Exemplos de mensagens de erro:
- *"Dra. Ana não atende às sextas-feiras."*
- *"Horário fora do expediente (atendimento até 18:00)."*
- *"Intervalo de almoço: 12:00–13:00. Escolha outro horário."*
- *"Terceiro agendamento com sobreposição não é permitido."*
- *"Profissional sem disponibilidade cadastrada."*

**Toasts:**

| Tipo | Cor | Ícone | Posição | Duração |
|------|-----|-------|---------|---------|
| Sucesso | `success #16A34A` | `check-circle` | Topo central | 3s |
| Erro    | `error #DC2626`   | `x-circle`     | Topo central | 5s |
| Aviso   | `warning #D97706` | `alert-triangle`| Topo central | 4s |
| Info    | `info #2563EB`    | `info`         | Topo central | 3s |

Exemplos de toasts de sucesso:
- *"Agendamento criado ✓"*
- *"Status atualizado para Confirmado ✓"*
- *"Link de cancelamento copiado ✓"*
- *"Cliente cadastrado ✓"*

---

### 3.5 Dialog de Double-Booking (FR-024)

Ativado quando o segundo agendamento com sobreposição de janela é submetido. Terceiro agendamento com sobreposição é bloqueado sem dialog (erro inline).

```
┌──────────────────────────────────────────────┐
│                                              │
│  ⚠️  Sobreposição de horário                 │
│                                              │
│  Dr. Carlos já tem um agendamento:           │
│  João da Silva — 14:00–14:30                 │
│  (Consulta de Retorno)                       │
│                                              │
│  Deseja confirmar a sobreposição             │
│  intencional?                                │
│                                              │
│  [     Cancelar     ]  [ Agendar mesmo assim ]│
└──────────────────────────────────────────────┘
```

- Botão "Cancelar": estilo ghost — retorna ao formulário sem perder os dados preenchidos
- Botão "Agendar mesmo assim": estilo `warning` (âmbar) — chamativo mas não destrutivo
- Dialog fecha com ESC ou toque fora → equivalente a "Cancelar"

---

### 3.6 Navegação da Agenda (Toggle + Swipe)

**Toggle Dia/Semana (topo da tela de agenda):**
```
[ Dia  |  Semana ]   ← segmented control, 2 opções
```
- Toque em "Dia": lista vertical do dia selecionado, cards médios (FR-021)
- Toque em "Semana": grade compacta de 7 dias com contagem de agendamentos por dia

**Swipe horizontal:**
- Visualização Dia: swipe esquerda → próximo dia; swipe direita → dia anterior
- Visualização Semana: swipe esquerda → próxima semana; swipe direita → semana anterior
- Animação: slide suave 200ms, easing ease-out

**Filtro de profissional (chips roláveis abaixo do toggle):**
```
[ Todos ] [ Dra. Ana ] [ Dr. Carlos ] [ Dr. Paulo ]  →  scroll horizontal
```
Chip ativo: fundo `brand-500`, texto branco. Inativo: borda `border`, fundo `bg-subtle`.

**Botão "Hoje":**
Aparece no header da agenda somente quando a data selecionada não é hoje. Toque retorna ao dia atual.

**Polling silencioso (FR-019):**
Atualização automática a cada 30–60s. Sem indicador de loading visível durante o polling. Se novos itens aparecerem: toast info discreta *"Agenda atualizada"* por 2s.

---

### 3.7 Busca de Próximo Disponível (FR-029)

**Tela `/buscar`:**

```
← Próximo Disponível

[ Todos os profissionais  ▾ ]  [ Qualquer tipo  ▾ ]

────── Hoje, 4 jun ──────────
  09:00  Dr. Carlos     Consulta de Retorno (30 min)    [ Agendar ]
  10:30  Dra. Ana       Avaliação (45 min)              [ Agendar ]

────── Amanhã, 5 jun ────────
  08:00  Dr. Carlos     Consulta de Retorno (30 min)    [ Agendar ]
  09:00  Dra. Ana       Retorno (20 min)                [ Agendar ]
  ...
```

- Máximo 20 resultados (FR-029)
- Chips de filtro: profissional e tipo de atendimento — 1 toque, resultado imediato
- Toque em "Agendar": abre Bottom Sheet pré-preenchido com data, hora, profissional e tipo. Somente campo "Cliente" fica em branco para Maria preencher
- Estado vazio: *"Nenhum horário disponível nos próximos 30 dias para os filtros selecionados. Verifique as configurações de disponibilidade dos profissionais."* + link para `/configuracoes/disponibilidade`

---

### 3.8 Tela de Autoatendimento Público (FR-042)

URL pública sem autenticação. Acessada pelo paciente via link enviado pelo Staff (WhatsApp, SMS ou outro canal). Permite confirmar presença ou cancelar o agendamento — 2 toques em qualquer ação.

**Estado Ativo (link válido, agendamento Pendente):**

```
┌──────────────────────────────────────────┐
│                                          │
│   [Logo: Dr Agenda]                      │
│                                          │
│   Olá, João da Silva                     │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  📅  Quarta, 5 de junho de 2026    │  │
│  │  ⏰  14:00                         │  │
│  │  👤  Dra. Wanessa                  │  │
│  │  🏥  Clínica São Lucas             │  │
│  └────────────────────────────────────┘  │
│                                          │
│  [ ✅  Confirmar presença ]              │  ← primário, brand-500, cheio
│  [ ✕   Cancelar consulta  ]              │  ← secundário, outline, cor error
│                                          │
│  ─────────────────────────────────────   │
│  Precisa remarcar? Entre em contato com  │
│  a clínica para novo agendamento.        │
│                                          │
└──────────────────────────────────────────┘
```

**Hierarquia visual intencional:** botão de confirmar é primário (maior, cheio, teal) — orienta o comportamento desejado. Botão de cancelar é secundário (outline, vermelho) — sempre acessível, nunca destacado.

**Estado Ativo (link válido, agendamento já Confirmado):**

```
│  [ ✓  Presença já confirmada ]          │  ← desabilitado, cinza, sem ação
│  [ ✕   Cancelar consulta     ]          │  ← ainda ativo
```

Botão de confirmar desabilitado com label *"Presença já confirmada ✓"* — previne operações redundantes ao backend. Botão de cancelar permanece funcional até o link expirar.

**Dialog de confirmação de presença** (1 toque no botão → dialog → 1 toque = 2 toques):
```
"Confirmar presença?"
"Você está confirmando sua presença na consulta acima."
[ Voltar ]   [ Sim, confirmar ]
```

**Dialog de cancelamento** (1 toque no botão → dialog → 1 toque = 2 toques):
```
"Cancelar consulta?"
"Esta ação não pode ser desfeita."
[ Voltar ]   [ Sim, cancelar ]
```

**Estado Pós-Confirmação:**
```
   ✅  Presença confirmada!

   Ótimo, João! Sua presença foi confirmada.
   Até quarta às 14:00!

   Imprevistos acontecem. Se precisar cancelar,
   entre em contato com a clínica.
```

**Estado Pós-Cancelamento:**
```
   ✅  Consulta cancelada.

   Obrigado por avisar, João!
   A clínica agradece o aviso com antecedência.

   Quer remarcar? Entre em contato com
   a clínica para novo agendamento.
```

**Estado: Link Expirado ou Inválido:**
```
   ⚠️  Este link não está mais disponível.

   Pode ter expirado ou a consulta
   já foi cancelada. Em caso de dúvida,
   entre em contato com a clínica.
```

**Regras do link (FR-042):** Expira automaticamente no horário do agendamento, ou quando o agendamento for cancelado, concluído ou marcado como NoShow — o que ocorrer primeiro. Confirmar não invalida o link (botão de cancelar permanece ativo); cancelar invalida o link para ambas as ações.

---

## 4. Jornadas de Uso

### 4.1 Maria — Gestão do Dia (Staff)

**Contexto:** Maria, recepcionista, 7h. Clínica com 2-3 médicos. Celular ou computador da recepção.

```
ABERTURA DO DIA
1. Abre DrAgenda → /agenda/dia, data de hoje
2. Chip "Todos os profissionais" ativo → vê agenda completa
3. Filtra por status "Pendente" → vê quem ainda não confirmou
4. Para cada pendente: vê telefone no card → liga → ao confirmar,
   toca "Confirmar" no card → Confirmado ✓ (2 toques)

NOVO AGENDAMENTO (paciente liga pedindo horário)
1. Toca FAB [ + ] → Bottom Sheet sobe
2. Preenche 5 campos: Cliente, Tipo, Profissional, Data, Hora
3. ✅ Sistema valida → toca "Agendar" → toast "Agendamento criado ✓"
   ❌ Erro inline → ajusta campo → toca "Agendar" novamente

PACIENTE SEM PREFERÊNCIA DE MÉDICO
1. Toca aba "Buscar" → /buscar
2. Filtra por tipo de atendimento (ex.: "Consulta de Retorno")
3. Vê lista agrupada → toca "Agendar" no slot desejado
4. Bottom Sheet abre pré-preenchido → preenche só "Cliente" → "Agendar"

CHECK-IN (paciente chega à recepção)
1. Localiza card do paciente na agenda
2. Toca "Chegou" → Presente ✓ (2 toques)
3. Wanessa vê "Presente" no próximo ciclo de polling (30–60s)

GERAÇÃO DE LINK DE CANCELAMENTO
1. Toca ··· no card → "Gerar link de cancelamento"
2. Toast "Link copiado ✓" → Maria cola no WhatsApp para o paciente

FIM DO DIA
Sem ação necessária. Agenda do dia seguinte já visível via swipe.
```

---

### 4.2 Wanessa — Acompanhamento da Agenda (Profissional)

**Contexto:** Wanessa, médica. Celular como dispositivo principal. Vê somente seus agendamentos.

```
MANHÃ (em casa, mobile)
1. Abre DrAgenda → /agenda/dia, filtrada para ela automaticamente
2. Vê card destacado "⭐ Primeiro do dia" → sabe a que horas chegar
3. Lê os próximos agendamentos → fecha app

NA CLÍNICA — INÍCIO DO TURNO
1. Abre app → verifica cards com status "Presente" (roxo)
2. Se badge "⚠️ Chegou antes do horário" → decide atender fora de ordem

DURANTE O DIA — ENTRE CONSULTAS
1. Ao finalizar consulta: toca "Concluir" no card do paciente → 2 toques
2. Repete para cada atendimento ao longo do dia

CRIAÇÃO EXCEPCIONAL DE AGENDAMENTO
1. Toca FAB [ + ] → Bottom Sheet
2. Campo "Profissional" pré-preenchido com "Dra. Wanessa" e bloqueado
3. Preenche demais campos → "Agendar"

FIM DO DIA
Nenhuma ação necessária — todas as atualizações foram feitas ao longo do dia.
```

---

### 4.3 André — Configuração e Controle (Admin Empresa)

**Contexto:** André, dono/admin. Usa principalmente no onboarding e manutenções pontuais.

```
ONBOARDING (primeira semana)
1. Login → /configuracoes
2. Dados da clínica → configura fuso horário (FR-041)
3. /profissionais → cadastra médicos: nome, especialidade, email, senha temporária
4. Para cada profissional → /disponibilidade: configura dias, horários, intervalos
5. /tipos-atend → cadastra tipos de atendimento com duração em minutos
6. Cadastra usuários Staff

USO RECORRENTE — MANUTENÇÃO
- Inativar profissional: toggle em /profissionais →
  sistema sinaliza agendamentos futuros: "⚠️ Profissional inativo — redistribuir manualmente"
  (agendamentos NÃO são cancelados automaticamente — FR-009)
- Ajustar disponibilidade: /disponibilidade/:profissional_id
- Adicionar tipo de atendimento: /tipos-atend

USO RECORRENTE — ACOMPANHAMENTO
- Acessa /agenda filtrando por profissional e período para visão gerencial
- Histórico permanente de agendamentos concluídos, cancelados e NoShows (FR-031)
```

---

## 5. Estados de Interface

### 5.1 Estados Vazios

| Tela | Quando | Mensagem | Ação sugerida |
|------|--------|---------|---------------|
| Agenda — dia | Nenhum agendamento no dia | *"Nenhum agendamento para hoje."* | FAB visível |
| Agenda — dia filtrada | Nenhum resultado para o profissional selecionado | *"Nenhum agendamento para [nome] hoje."* | — |
| Buscar | Sem horários nos próximos 30 dias | *"Nenhum horário disponível nos próximos 30 dias. Verifique as configurações de disponibilidade."* | Link → `/configuracoes/disponibilidade` |
| Clientes | Nenhum cliente cadastrado | *"Nenhum cliente cadastrado ainda."* | Botão "Cadastrar primeiro cliente" |

### 5.2 Estados de Carregamento

- **Skeleton screens** no lugar de cards enquanto a agenda carrega — 3 cards placeholder animados (shimmer)
- **Spinner inline no botão** durante a requisição de mudança de status: ex. "Confirmando..." — botão desabilitado durante a operação
- **Polling silencioso (FR-019):** Sem indicador de loading. Se novos itens aparecerem: toast info *"Agenda atualizada"* por 2s. Sem interrupção do fluxo do usuário.

### 5.3 Estados de Erro

| Erro | Comportamento |
|------|--------------|
| Falha na mudança de status | Toast error: *"Não foi possível atualizar o status. Tente novamente."* — status do card reverte visualmente ao estado anterior |
| Falha ao criar agendamento | Toast error: *"Erro ao criar agendamento. Verifique sua conexão."* — bottom sheet permanece aberto com dados preenchidos |
| Falha no polling | Banner sutil no topo: *"Agenda pode estar desatualizada"* + botão "Atualizar" para refresh manual |
| Link de cancelamento inválido | Tela de estado "Link inválido" (ver seção 3.8) |
| Sessão expirada | Redirecionamento automático para /login com mensagem: *"Sua sessão expirou. Faça login novamente."* |

### 5.4 Estado Offline (PWA)

- Banner amarelo fixo no topo: *"Sem conexão — algumas ações podem não funcionar"*
- Agenda do dia: exibe dados em cache (última sincronização — timestamp visível)
- Botões de ação: desabilitados com tooltip *"Sem conexão"*
- Ao reconectar: banner desaparece automaticamente, polling reinicia

---

## 6. Acessibilidade

### 6.1 Regras Gerais

- **Rótulos ARIA:** Todos os botões de ícone têm `aria-label` descritivo. Ex.: FAB → `aria-label="Novo agendamento"`; botão "···" → `aria-label="Mais ações para João da Silva 14:00"`.
- **Foco gerenciado:** Ao abrir Bottom Sheet ou Dialog, o foco move para o primeiro elemento interativo. Ao fechar, o foco retorna ao elemento que o abriu.
- **Focus trap:** Bottom sheets e dialogs implementam armadilha de foco — Tab não escapa do componente enquanto aberto.
- **Anúncios de status:** Mudanças de status de agendamento anunciadas via `aria-live="polite"`. Ex.: *"Status atualizado para Confirmado"*.
- **Formulários:** Cada campo tem `<label>` associado explicitamente via `htmlFor`. Campos obrigatórios têm `aria-required="true"`.

### 6.2 Daltonismo

Regra inviolável: **nenhum componente usa cor como único diferenciador**.

| Componente | Indicadores combinados |
|-----------|----------------------|
| Card de status | Cor de fundo + borda lateral colorida + ícone + rótulo de texto |
| Botão de ação contextual | Label de texto descritivo (nunca somente ícone) |
| Erros de validação | Borda vermelha + ícone `alert-circle` + texto da mensagem |
| Toasts | Ícone + texto descritivo |
| Chips de filtro | Cor de fundo + texto do label |

### 6.3 Tamanho de Toque

- Área mínima tocável: 44×44px em todos os elementos interativos
- Botões em cards: `min-height: 44px`, padding generoso
- Chips de filtro: `min-height: 36px`, `padding: 8px 16px`
- FAB: 56×56px (acima do mínimo)
- Itens da Bottom Tab Bar: `min-height: 48px`

### 6.4 Tipografia e Leitura

- Nenhum texto de interface abaixo de 12px
- Campos de formulário: 16px mínimo (evita zoom automático no iOS/Android)
- Line-height mínimo 1.4 para textos de parágrafo
- Contraste mínimo WCAG 2.1 AA em toda a interface (ver DESIGN.md seção 1.7)
- Padrão WCAG 2.1 AA + daltonismo aplicado em toda a interface
