# Reviewer Gate — Rubric Walker
data: 2026-06-04
revisor: Rubric Walker (7 dimensões de qualidade de PRD)

---

## Verdict

PRD com tese estratégica clara, FRs bem especificados e UJs comportamentais nomeados — bem acima da média para estágio de MVP. O problema central é uma incoerência estratégica não nomeada: a visão promete "pacientes desmarcar em 2 toques" mas nenhum FR do MVP entrega isso, reproduzindo exatamente a fricção diagnosticada como causa raiz dos no-shows. Somado a uma seção de questões em aberto que é teatro puro e uma pendência jurídica escondida dentro de um Must, o PRD suaviza tensões reais em vez de expô-las.

---

## Dimensões

| Dimensão | Julgamento | Top Finding |
|----------|-----------|-------------|
| Decision-readiness | **adequate** | Seção 9 elimina o espaço para expor tensões. FR-038 esconde bloqueio de launch ("Validar com jurídico") dentro de um Must. |
| Substance over theater | **adequate** | UJs são concretos. NFR-007 é checklist de deployment disfarçado de NFR. Seção 6 menciona "10 itens" sem listá-los. |
| Strategic coherence | **thin** | A tese é reduzir no-shows eliminando fricção de cancelamento do paciente. Nenhum FR do MVP entrega isso. MS-3 (30% redução) não é atingível via confirmações da recepcionista. |
| Done-ness clarity | **strong** | FRs majoritariamente testáveis. Exceções: FR-021 sem breakpoint de viewport; FR-029 sem horizonte de busca; FR-031 vs FR-039 (anonimização vs. exclusão não resolvida). |
| Scope honesty | **thin** | Cancelamento autônomo pelo paciente ausente da lista fora do MVP. FR-038 contém decisão jurídica não tomada como requisito Must. |
| Downstream usability | **strong** | IDs contíguos e cross-referenciados. UJs com protagonistas e comportamentos específicos. Gap: inativação de profissional (FR-009) sem definição de impacto em agendamentos futuros. |
| Shape fit | **adequate** | Estrutura adequada para B2B SaaS mobile-first. Ponto cego: requisitos visuais complexos sem referência a wireframe — risco de implementação divergente. |

---

## Findings por Severidade

### CRITICAL
- **[C-01]** Visão promete "pacientes desmarcar em 2 toques" mas nenhum FR do MVP entrega self-service de cancelamento ao paciente. O produto construído exige que o paciente ligue para a recepcionista, reproduzindo a fricção diagnosticada como causa raiz dos no-shows. Meta MS-3 (30% redução de no-shows) provavelmente não será atingida sem essa feature.
- **[C-02]** FR-038 contém decisão jurídica não tomada ("Validar com jurídico antes do launch") dentro de um requisito Must com prioridade obrigatória. Isso é um bloqueio de launch não reconhecido. A pendência precisa sair do corpo do FR.

### HIGH
- **[H-01]** Seção 6 (Fora do MVP) menciona "10 itens documentados" mas não os lista neste documento. O leitor não pode verificar o que está fora do escopo.
- **[H-02]** Seção 9 é teatro que elimina a função da seção. Questões resolvidas sem registro de decisão são conhecimento perdido.

### MEDIUM
- **[M-01]** FR-021 não define o breakpoint de viewport que separa layout mobile de desktop. Engenheiros frontend precisam desse threshold.
- **[M-02]** FR-029 não define horizonte de busca (quantos dias à frente?), limite de resultados, nem comportamento quando não há horário disponível.
- **[M-03]** FR-009 (inativar profissional) não define o que acontece com agendamentos futuros: cancelamento automático, alerta para redistribuição, ou manutenção sem alteração.

### LOW
- **[L-01]** FR-031 ("histórico permanente") e FR-039 ("exclusão permanente de dados") criam tensão não resolvida: anonimização vs. exclusão afeta compliance LGPD e integridade dos relatórios.
- **[L-02]** Intervalo de polling (30-60s) aparece tanto em FR-019 quanto na Seção 10 sem diferenciar decisão de hipótese.

### MECHANICAL
- **[MC-01]** UJ-3 referencia FR-031 a FR-034 como bloco, mas FR-031 está na seção 4.4 e FR-032-034 na seção 4.5 — referência ambígua.
- **[MC-02]** Ausência de glossário formal para termos críticos: slot, ocupação, Tipo de Atendimento, empresa_id.
