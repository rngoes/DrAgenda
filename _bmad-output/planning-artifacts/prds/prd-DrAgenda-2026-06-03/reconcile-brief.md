# Reviewer Gate — Input Reconciliation
data: 2026-06-04
revisor: Input Reconciliation (brief vs. PRD)

---

## Verdict

O PRD honra o problema central, os requisitos técnicos e a maior parte dos FRs do brief. Porém, três ativos estratégicos do brief desapareceram completamente (perfil segmentado de comprador, champion strategy, critérios de pivot), uma divergência de contagem de status passou despercebida (brief=4, sessão declarou 5, PRD implementa 6), e o tom de "simplicidade brutal" está na visão mas se dilui progressivamente na densidade dos FRs adicionados — criando uma promessa de produto que o próprio documento começa a contradizer nos detalhes.

---

## Gaps (brief → PRD)

| Severidade | Item Ausente | Fix Sugerido |
|-----------|-------------|-------------|
| **HIGH** | Perfil segmentado de comprador (solo 1-2, grupo 3-5, gerenciado 5+) com gatilhos de venda distintos | Adicionar seção "Buyer Personas e Estratégia de Aquisição"; afeta design de onboarding e métricas de ativação |
| **HIGH** | Champion Strategy — recepcionista como aliada de venda e influenciadora da compra | Registrar na seção de Modelo de Negócio; orienta UX: dar à recepcionista algo fácil de mostrar ao dono como prova de valor |
| **HIGH** | Critérios de pivot explícitos (churn > 15%, profissionais < 60% diários, CAC > 3x LTV) | Adicionar subsection "Critérios de Pivot" em Seção 2 — sem isso o time não sabe quando parar de iterar |
| **MEDIUM** | "Dashboard visual com todos os profissionais em uma tela" pode estar distorcido: FR-015 define filtro (um profissional por vez), não visão consolidada | Clarificar FR-014/FR-015: estado padrão exibe todos simultaneamente; filtro é redução opcional |
| **MEDIUM** | Paciente como usuário futuro/roadmap não tem sequer stub de jornada no PRD | Adicionar UJ-4 stub: "Paciente via link — cancela agendamento em 2-3 toques (Roadmap Ano 1)" |
| **LOW** | Narrativa "DrAgenda paga por si mesmo eliminando 1-2 no-shows/mês" sem tradução em feature de produto | Adicionar contador mensal de no-shows no dashboard do Admin como argumento de ROI embutido |

---

## Contradições (PRD vs. Brief)

| Severidade | Item | Natureza |
|-----------|------|----------|
| **HIGH** | Relatórios (FR-032 a FR-034) incluídos no MVP; brief os lista explicitamente fora do MVP | **Deliberada** — decisão de Rodrigo Navarro. Porém não registrada formalmente no decision-log; futuros revisores reabrirão a discussão |
| **MEDIUM** | Brief diz 4 status; sessão declarou expansão para 5; PRD implementa 6 (NoShow como status visual separado via FR-016+FR-030, sem ser declarado explicitamente como decisão deliberada) | **Parcialmente acidental** — "Presente" foi registrado como deliberado; "NoShow" entrou implicitamente sem declaração formal |
| **LOW** | Brief restringe target a clínicas < 5 profissionais; PRD menciona migração para schema-por-empresa com 100+ clínicas | **Acidental** — nota técnica de escalabilidade que extravasou para o PRD sem declaração de produto |

---

## Perdas Qualitativas

| Item Perdido | Fix |
|-------------|-----|
| Tom "simplicidade brutal" e "hiperssimples mobile-first" presente na visão mas progressivamente traído pelos FRs adicionados (double-booking com confirmação, reversão de NoShow com auditoria, alerta de reordenação) — sem que o PRD reconheça essa tensão | Adicionar NFR-008: "Qualquer feature deve ser descoberta e executada por usuário não-treinado em ≤ 3 toques/cliques, sem leitura de manual" — traduz o tom em restrição verificável |
| Paciente como co-herói da narrativa — brief posiciona paciente como protagonista do problema e da solução futura; PRD trata paciente exclusivamente como objeto de dados | Preservar na visão uma frase sobre experiência futura do paciente mesmo fora do MVP |
| Pitch de ROI ("paga por si mesmo eliminando 1-2 no-shows/mês") como argumento embutido de venda e retenção — não traduzido em nenhum touchpoint de produto | Criar momento de "aha de ROI" no produto: após 30 dias, Admin vê quantos NoShows foram detectados vs. período anterior |
