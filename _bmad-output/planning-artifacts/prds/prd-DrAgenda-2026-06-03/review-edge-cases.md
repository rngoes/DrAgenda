# Reviewer Gate — Edge Case Hunter
data: 2026-06-04
revisor: Edge Case Hunter (casos limítrofes em FRs)

---

## Verdict

Os requisitos definem um fluxo de status razoável, mas deixam lacunas críticas em conflitos de duração no double-booking, ausência de tratamento de fuso horário e contradição direta entre anonimização LGPD e histórico permanente. Sem esclarecimentos explícitos nessas áreas, o sistema pode gerar dados inconsistentes em produção e expor a clínica a riscos legais.

---

## Edge Cases por Severidade

### CRITICAL

**[EC-01] FR-031 + FR-039 — Exclusão LGPD vs. histórico permanente**
- **Cenário:** Paciente solicita exclusão de dados (FR-039). FR-031 exige histórico permanente de todos os agendamentos, incluindo NoShows e cancelamentos.
- **Risco:** Agendamentos ficam órfãos (patient_id nulo) ou são removidos. Contadores de FR-033 ficam inconsistentes retroativamente. Risco de multa ANPD por não anonimizar corretamente.
- **Fix:** Definir explicitamente que a exclusão LGPD anonimiza o paciente (substitui dados pessoais por token irreversível) mas preserva o agendamento como registro agregado. Adicionar: "Agendamentos de pacientes excluídos mantêm status, timestamps e profissional; dados identificadores são substituídos por [PACIENTE REMOVIDO]."

**[EC-02] FR-013 + FR-023 — Profissional sem disponibilidade cadastrada**
- **Cenário:** Profissional nunca configurou disponibilidade; sistema tenta agendar para ele.
- **Risco:** Comportamento indefinido — o sistema pode bloquear todos os horários (correto) ou liberar todos por ausência de regra (incorreto). FR-029 pode retornar horários inválidos para esse profissional.
- **Fix:** Adicionar regra explícita: "Profissional sem disponibilidade cadastrada é tratado como indisponível em todos os horários. FR-029 ignora esse profissional na busca."

**[EC-03] FR-013 — Fuso horário ausente**
- **Cenário:** Sistema multi-tenant com clínicas em fusos diferentes (SP UTC-3, Manaus UTC-4).
- **Risco:** Horários salvos em horário local de uma timezone aparecem incorretos em outra. NoShow automático (FR-030) dispara no horário errado. Relatórios cruzados entre clínicas geram comparações inválidas.
- **Fix:** Adicionar FR: "Cada clínica possui fuso horário configurado. Todos os horários são armazenados em UTC e exibidos convertidos para o fuso da clínica. O NoShow automático usa UTC como referência."

---

### HIGH

**[EC-04] FR-024 + FR-025 — Double-booking com durações diferentes**
- **Cenário:** Paciente A às 10h00 (30min → término 10h30). Paciente B às 10h00 (60min → término 11h00). Paciente C tenta agendar às 10h30 com o mesmo profissional.
- **Risco:** FR-024 verifica conflito por "mesmo horário de início", não por sobreposição de janelas. O sistema pode permitir C às 10h30, ignorando que B ainda está em atendimento até 11h00. Resultado: 3 pacientes simultâneos, violando FR-024.
- **Fix:** Reformular FR-024: "Conflito é definido como sobreposição de janelas [início, início+duração). Um agendamento é bloqueado se o intervalo temporal sobrepõe qualquer agendamento ativo do mesmo profissional, respeitando o limite máximo de 2 atendimentos simultâneos."

**[EC-05] FR-027 — Matriz de transições de status não documentada**
- **Cenário:** Staff tenta mover agendamento de Cancelado → Confirmado, de Concluído → Presente, ou de NoShow → Concluído.
- **Risco:** Sem matriz explícita, implementação pode permitir qualquer transição (estados inconsistentes) ou bloquear todas retroativas (impedindo correções legítimas).
- **Fix:** Documentar matriz completa. Sugestão mínima: Cancelado e Concluído são estados terminais. NoShow pode ser revertido para Presente (já coberto pelo FR-030, mas deve ser explicitado no FR-027). Presente → Cancelado exige justificativa.

**[EC-06] FR-024 + FR-030 — Double-booking com NoShow automático**
- **Cenário:** Dois pacientes no mesmo horário/profissional. Paciente A chega (Presente). Timer de +30min expira — o NoShow deve disparar para Paciente B. Como o sistema diferencia?
- **Risco:** Se o timer dispara baseado em "nenhum do slot recebeu Presente", ambos podem ficar em Pendente e ambos recebem NoShow. Se "qualquer um recebeu Presente cancela o timer para todos", Paciente B nunca gera NoShow.
- **Fix:** Especificar: "Em cenário de double-booking, o NoShow automático é avaliado individualmente por agendamento. Cada agendamento tem seu próprio timer independente de +30min. A marcação de Presente em um agendamento não afeta o timer dos demais agendamentos do mesmo slot."

**[EC-07] FR-023 + FR-025 — Agendamento que invade período de intervalo**
- **Cenário:** Agendamento às 11h30 com duração de 60min (término 12h30). Profissional com intervalo configurado de 12h00 às 13h00.
- **Risco:** FR-023 valida apenas se o horário de início está "fora do intervalo", mas não verifica se a duração invade o intervalo. O agendamento passa na validação mas o profissional ficará em atendimento durante o intervalo.
- **Fix:** Ampliar FR-023: "A validação deve verificar que a janela [horário_início, horário_início + duração] não intersecta nenhum período de intervalo configurado."

---

### MEDIUM

**[EC-08] FR-034 — NoShow na "ocupação confirmada" distorce métricas**
- **Cenário:** Profissional com alta taxa de NoShow aparece com alta "ocupação confirmada".
- **Risco:** Métricas de desempenho distorcidas — gestor interpreta ocupação de 90% quando 30% foi NoShow. Decisões de contratação baseadas em dados enganosos.
- **Fix:** Verificar intenção: se o objetivo é medir "slots comprometidos", renomear para "Ocupação Comprometida". Recomendar linha adicional: (3) efetiva = Presente+Concluído.

**[EC-09] FR-030 — Condição de corrida cancelamento vs. timer de NoShow**
- **Cenário:** Agendamento das 10h00. Paciente cancela às 10h25. Timer de NoShow dispara às 10h30.
- **Risco:** Cancelamento e timer processados quase simultaneamente podem gerar estado inconsistente com dupla entrada no histórico (Cancelado + NoShow).
- **Fix:** Definir prioridade: "O job de NoShow só dispara se o status ainda for Pendente ou Confirmado no momento da execução. Se o status for Cancelado, o job é ignorado."

**[EC-10] FR-028 + FR-024 — Limite de 2 toques vs. fluxo de confirmação**
- **Cenário:** FR-028 exige atualização de status em máx 2 toques. FR-024 exige confirmação explícita para double-booking (adiciona pelo menos 1 toque extra).
- **Risco:** Implementação pode sacrificar a confirmação para cumprir o limite de toques, eliminando proteção contra double-booking acidental.
- **Fix:** Adicionar exceção explícita: "O limite de 2 toques do FR-028 aplica-se a transições de status padrão. Ações que exigem confirmação explícita por regra de negócio (FR-024, reversão de NoShow do FR-030) podem requerer toque adicional e estão isentas do limite de 2 toques."
