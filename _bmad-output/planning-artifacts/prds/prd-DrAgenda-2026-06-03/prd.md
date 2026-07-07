---
title: "PRD: DrAgenda — Sistema de Agendamento para Consultórios"
status: final
created: "2026-06-03"
updated: "2026-06-04"
version: "2.3"
author: "Mary (Business Analyst) — sessão com Rodrigo Navarro"
source: "docs/brief.md"
---

# PRD: DrAgenda

## 1. Visão e Problema

### Visão
DrAgenda é a agenda digital que profissionais de saúde conseguem usar no celular sem treinamento, e que pacientes conseguem confirmar ou cancelar a consulta em 2 toques via link de autoatendimento. **Ser a melhor e mais simples agenda para clínicas médicas e odontológicas de pequeno porte no Brasil.**

### Problema
Consultórios de pequeno porte (<5 profissionais) operam com planilhas, cadernos e WhatsApp. Isso gera dois problemas custosos e interligados:

**Para o profissional:** 20-30% dos agendamentos resultam em no-shows. A principal causa não é má-fé — é fricção para desmarcar. Cancelar exige ligar em horário comercial, esperar atendimento e explicar quem é e qual o horário. É mais fácil simplesmente não aparecer.

**Para a recepcionista:** horas por dia gerenciando agendas em planilhas diferentes, ligando para confirmar consultas e resolvendo conflitos de horário escritos à mão — sem validação automática, sem visibilidade remota, sem histórico confiável.

**Impacto:** uma hora de trabalho perdida e R$ 200-400 de receita evaporada a cada no-show. Em clínicas pequenas, isso ocorre 2-3 vezes por semana.

Sistemas existentes (Doctoralia, iClinic, Ninsaúde) custam R$ 200-500/mês, são complexos demais e pesados para uso mobile — inacessíveis para o público-alvo.

---

## 2. Objetivos e Métricas de Sucesso

### Objetivos de Negócio
| # | Objetivo | Meta | Prazo |
|---|---------|------|-------|
| OB-1 | Primeiras clínicas via network pessoal | 10 clínicas ativas | 3 meses |
| OB-2 | Crescimento via aquisição digital | 50 clínicas ativas | 12 meses |
| OB-3 | Unidade econômica saudável | CAC < R$ 660 (LTV/3) | 12 meses |
| OB-4 | Retenção forte | Churn < 5%/mês | 6 meses |

### Métricas de Sucesso do Produto
| # | Métrica | Meta | Como Medir |
|---|---------|------|-----------|
| MS-1 | Profissionais usando diariamente | 80%+ acessam 5+ dias/semana | Logs de acesso |
| MS-2 | Fricção de onboarding | Clínica operando 100% em < 1 semana | Data de primeiro agendamento real vs. cadastro |
| MS-3 | Redução de no-shows | 30% de redução após 60 dias | Baseline declarado no onboarding vs. contadores do sistema |
| MS-4 | Tempo de agendamento | < 1 minuto por agendamento | Timestamp de abertura vs. confirmação do form |
| MS-5 | NPS | > 50 | Survey trimestral |

### Counter-Metrics (o que não devemos otimizar às custas do objetivo)
- Quantidade de funcionalidades adicionadas — não sacrificar simplicidade por cobertura
- Tempo de sessão elevado — mais tempo no sistema pode significar confusão, não engajamento

---

## 3. Jornadas de Usuário

### UJ-1 — Maria, Recepcionista: Gestão do Dia

**Protagonista:** Maria, recepcionista de clínica com 2-3 médicos. Chega cedo, gerencia a agenda de todos os profissionais sozinha durante o dia. Não tem tempo para sistemas complicados.

```
7h — ABERTURA
Maria acessa o DrAgenda e filtra os agendamentos do dia por status "Pendente"
(sem confirmação). Para cada paciente pendente, vê o telefone no card e liga
para confirmar manualmente.

DURANTE O DIA — NOVOS AGENDAMENTOS
Paciente liga querendo marcar. Maria abre o formulário de agendamento, informa
Cliente, Tipo de Atendimento, Profissional desejado, Data e Hora. O sistema
valida disponibilidade e confirma o agendamento em menos de 1 minuto.

Se o paciente não tem preferência de médico ("quero o primeiro disponível"),
Maria usa a busca de próximo horário disponível e vê vagas de todos os
profissionais ordenadas por horário.

DURANTE O DIA — CHECK-IN
Paciente chega à recepção. Maria localiza o card do agendamento e muda o
status para "Presente" em 2 toques. Wanessa (médica) vê a atualização no
celular no próximo ciclo de refresh.

FIM DO DIA — FOLLOW-UP
Maria filtra os agendamentos do dia seguinte com status "Pendente" e liga
para os que ainda não confirmaram.
```

**Requisitos-chave desta jornada:** FR-015, FR-017, FR-022 a FR-029

---

### UJ-2 — Wanessa, Médica: Acompanhamento da Agenda

**Protagonista:** Wanessa, médica que não tem laptop fixo no consultório. Usa celular ou tablet como dispositivo principal. Quer saber quem vem e atualizar o status de cada consulta sem fricção.

```
MANHÃ — EM CASA (mobile)
Wanessa abre o DrAgenda no celular antes de sair. Vê a agenda do dia com o
primeiro horário em destaque — sabe exatamente a que horas precisa chegar.

NA CLÍNICA — INÍCIO DO TURNO
Wanessa verifica quais pacientes já estão com status "Presente" (chegaram
à recepção). Se um paciente de horário posterior chegou antes do próximo
agendado, o sistema exibe alerta visual de reordenação sugerida — Wanessa
decide se atende fora de ordem.

DURANTE O DIA — ENTRE CONSULTAS
Wanessa atende na ordem de marcação. Ao finalizar cada consulta, abre o
card do paciente e muda o status para "Concluído" em 2 toques.

FIM DO DIA
Nenhuma ação necessária — todas as atualizações foram feitas ao longo do dia.
```

**Nota de dispositivo:** todas as ações de Wanessa devem ser realizáveis em celular (tela ≥ 320px) sem necessidade de laptop.

**Requisitos-chave desta jornada:** FR-014, FR-016, FR-018, FR-019, FR-020, FR-021, FR-022, FR-027, FR-028

---

### UJ-3 — André, Admin da Clínica: Configuração e Controle

**Protagonista:** André, dono ou responsável administrativo da clínica. Usa o sistema principalmente para configuração inicial e acompanhamento gerencial periódico.

```
ONBOARDING (primeira semana)
André acessa o sistema e cadastra os médicos da clínica (nome, especialidade,
login, senha temporária). Em seguida, cadastra os profissionais da recepção
com perfil Staff. Por fim, configura os horários disponíveis de cada médico
por dia da semana (horário início, horário fim, intervalo de almoço).

USO RECORRENTE — RELATÓRIOS
André acessa os relatórios filtrados por período e por profissional para
verificar: taxa de ocupação da agenda, consultas agendadas, canceladas,
NoShows e finalizadas. Usa os dados para identificar padrões e conversar
com a equipe.

USO RECORRENTE — MANUTENÇÃO
André ajusta cadastros (novo médico, mudança de horário de atendimento) ou
corrige configurações de disponibilidade quando necessário.
```

**Requisitos-chave desta jornada:** FR-008 a FR-013, FR-031 a FR-034

---

## 4. Requisitos Funcionais

### 4.1 Autenticação e Controle de Acesso

| ID | Requisito | Prioridade |
|----|-----------|-----------|
| FR-001 | O sistema deve autenticar usuários via email e senha com token JWT contendo `empresa_id` | Must |
| FR-002 | O sistema deve suportar 4 perfis de acesso: Admin Sistema, Admin Empresa, Staff, Profissional | Must |
| FR-003 | Admin Empresa deve ter acesso total aos dados de sua empresa e somente dela | Must |
| FR-004 | Staff deve poder criar e editar agendamentos e clientes de todos os profissionais da clínica | Must |
| FR-005 | Profissional deve visualizar apenas seus próprios agendamentos, criar agendamentos para si mesmo (com campo Profissional pré-preenchido e bloqueado) e atualizar o status dos seus agendamentos | Must |
| FR-006 | Admin Sistema deve poder cadastrar empresas (clínicas) sem acesso aos dados de pacientes | Must |
| FR-056 | Ao cadastrar uma clínica, Admin Sistema deve criar simultaneamente o primeiro usuário Admin Empresa dessa clínica (nome, email, senha temporária exibida na tela); o acesso à clínica é impossível sem esse usuário; mesmas regras de complexidade de senha de FR-007 | Must |
| FR-007 | Admin Empresa deve poder criar usuários com perfil Staff ou Profissional (não pode criar outro Admin Empresa) e definir senha temporária a ser informada pessoalmente; a senha temporária deve ter no mínimo 8 caracteres contendo letras maiúsculas, minúsculas e caractere especial | Must |
| FR-043 | Admin Empresa deve poder redefinir a senha de qualquer usuário de sua empresa, gerando nova senha temporária exibida na tela (sem envio de email no MVP); a redefinição força o usuário a trocar a senha no próximo login; mesmas regras de complexidade de FR-007 | Must |
| FR-044 | Na primeira autenticação com senha temporária, o sistema deve bloquear o acesso às demais telas e exibir formulário obrigatório de troca de senha; a nova senha deve ter no mínimo 8 caracteres contendo letras maiúsculas, minúsculas e caractere especial; o acesso só é liberado após troca bem-sucedida | Must |
| FR-047 | Todos os perfis devem ter opção de logout explícito no menu de perfil; o logout remove o JWT do armazenamento local e redireciona para a tela de login; sem invalidação server-side no MVP (JWT stateless) | Must |
| FR-048 | Qualquer usuário autenticado pode alterar sua própria senha nas configurações de perfil, informando a senha atual e a nova senha; a nova senha deve ter no mínimo 8 caracteres contendo letras maiúsculas, minúsculas e caractere especial; sem envio de email no MVP | Must |

### 4.2 Cadastros Base

| ID | Requisito | Prioridade |
|----|-----------|-----------|
| FR-008 | Admin Sistema deve poder cadastrar clínicas com isolamento total de dados entre empresas | Must |
| FR-051 | Admin Sistema deve poder listar todas as clínicas cadastradas, editar seus dados e inativar/reativar uma clínica; ao inativar, todos os usuários da clínica perdem acesso ao sistema imediatamente | Must |
| FR-009 | Admin Empresa deve poder cadastrar, editar, inativar e reativar profissionais (médicos/dentistas). Ao inativar um profissional, o sistema sinaliza visualmente ao Staff todos os seus agendamentos futuros como “Profissional Inativo — redistribuir manualmente”, sem cancelá-los automaticamente. Ao reativar, os sinais “Profissional Inativo” são removidos automaticamente dos agendamentos futuros que ainda não foram redistribuídos. | Must |
| FR-010 | Admin Empresa deve poder cadastrar, editar, inativar e reativar usuários Staff | Must |
| FR-011 | Staff deve poder cadastrar clientes (pacientes) com coleta de termo de consentimento LGPD explícito no momento do cadastro | Must |
| FR-050 | Staff deve poder listar, buscar (por nome ou telefone) e editar dados de pacientes já cadastrados (nome, telefone, data de nascimento); a busca alimenta o campo "Cliente" na criação de agendamentos (FR-022); exclusão/anonimização segue FR-039 | Must |
| FR-012 | Admin Empresa deve poder cadastrar e editar Tipos de Atendimento com duração configurável em minutos | Must |
| FR-053 | Admin Empresa deve poder inativar um Tipo de Atendimento; tipos inativos não aparecem no formulário de criação e edição de agendamentos; agendamentos existentes com o tipo inativo não são afetados | Must |
| FR-013 | Admin Empresa deve poder configurar disponibilidade de cada profissional por dia da semana, definindo: horário de início, horário de fim e intervalo (ex.: almoço) | Must |
| FR-041 | Admin Empresa deve poder configurar o fuso horário da clínica nas configurações da clínica. Todos os horários são armazenados internamente em UTC e exibidos convertidos para o fuso configurado. O job de NoShow automático (FR-030) usa UTC como referência para todos os cálculos de tempo | Must |
| FR-058 | Admin Empresa deve poder editar os dados básicos da própria clínica (nome, telefone de contato, endereço); fuso horário (FR-041) e baseline de no-show (FR-045) fazem parte das mesmas configurações da clínica | Must |

### 4.3 Dashboard de Agenda

| ID | Requisito | Prioridade |
|----|-----------|-----------|
| FR-014 | O sistema deve exibir dashboard de agenda com visualização diária e semanal | Must |
| FR-015 | O sistema deve permitir filtro da agenda por profissional | Must |
| FR-016 | Cada agendamento deve ser exibido como card com cor por status: Pendente (amarelo), Confirmado (azul), Presente (roxo), Concluído (verde), Cancelado (cinza escuro), NoShow (vermelho) | Must |
| FR-017 | O sistema deve permitir filtro de agendamentos por status | Must |
| FR-018 | Na visualização do profissional, o primeiro agendamento do dia deve ser destacado visualmente para consulta rápida no mobile | Must |
| FR-019 | O dashboard deve atualizar automaticamente via polling a cada 30 a 60 segundos, sem ação manual do usuário | Must |
| FR-020 | O sistema deve exibir alerta visual quando houver paciente com status "Presente" cujo horário de agendamento é posterior ao próximo agendamento com status "Pendente" ou "Confirmado" (reordenação sugerida) | Must |
| FR-021 | No mobile (viewport ≤ 767px), a agenda deve ser exibida em lista vertical ordenada por horário. No desktop (viewport ≥ 768px), deve ser exibida em grade horária | Must |

### 4.4 Gestão de Agendamentos

| ID | Requisito | Prioridade |
|----|-----------|-----------|
| FR-022 | Staff e Profissional devem poder criar agendamento preenchendo 5 campos: Cliente, Tipo de Atendimento, Profissional, Data e Hora; para Profissional, o campo Profissional é pré-preenchido com o próprio usuário e bloqueado para edição | Must |
| FR-023 | O sistema deve validar automaticamente a disponibilidade do profissional no horário selecionado verificando: (1) o dia da semana está configurado como disponível; (2) o horário de início está dentro da faixa configurada; (3) a janela completa `[horário_início, horário_início + duração]` não intersecta nenhum período de intervalo configurado. Profissional sem disponibilidade cadastrada é tratado como indisponível em todos os horários | Must |
| FR-024 | O sistema deve permitir no máximo 2 agendamentos com janelas de tempo sobrepostas para o mesmo profissional (double-booking intencional para gestão de no-shows). Conflito é definido por sobreposição das janelas `[horário_início, horário_início + duração]` — não apenas mesmo horário de início. O segundo agendamento com sobreposição exige confirmação explícita de Staff ou Admin Empresa com aviso de sobreposição. Terceiro ou mais agendamentos com sobreposição de janelas para o mesmo profissional são bloqueados. | Must |
| FR-025 | O sistema deve calcular automaticamente o horário de término do agendamento com base na duração do Tipo de Atendimento | Must |
| FR-026 | Quando uma validação falhar, o sistema deve exibir mensagem em linguagem clara explicando o motivo e o conflito | Must |
| FR-027 | As transições de status seguem o fluxo: Pendente → Confirmado → Presente → Concluído / Cancelado. **Estados terminais:** Concluído e Cancelado não admitem novas transições. NoShow é revertível para Presente exclusivamente por Staff. Transição Presente → Cancelado exige justificativa. **Matriz de permissões por perfil:** Staff pode fazer todas as transições; Profissional pode fazer Presente → Concluído; Admin Empresa tem as mesmas permissões de Staff; Pendente → Cancelado e Confirmado → Cancelado são permitidos para Staff e Admin Empresa. | Must |
| FR-028 | A atualização de status deve ser realizável em no máximo 2 toques/cliques no mobile. **Exceção:** ações que exigem confirmação explícita por regra de negócio (segundo agendamento com sobreposição conforme FR-024; reversão de NoShow conforme FR-030; cancelamento de Presente com justificativa) podem requerer toque adicional de confirmação e estão isentas do limite de 2 toques. | Must |
| FR-029 | Exclusivamente Staff deve poder usar a busca de “próximo horário disponível”, que exibe vagas de todos os profissionais da clínica ordenadas por data e horário, com filtro opcional por profissional e por Tipo de Atendimento. A busca cobre um horizonte de até 30 dias a partir da data atual e retorna no máximo 20 resultados. Se não houver horários disponíveis no horizonte, o sistema exibe mensagem informativa ao usuário. | Must |
| FR-030 | O sistema deve marcar automaticamente como "NoShow" agendamentos que atingirem o horário agendado acrescido de 30 minutos sem transição para o status "Presente" ou "Concluído". **Avaliação individual:** em double-booking, o timer é independente por agendamento — Presente em um não afeta o timer dos demais no mesmo slot. **Condição de execução:** o job só aplica NoShow se o status no momento da execução ainda for "Pendente" ou "Confirmado"; se for "Cancelado" ou "Presente", o job ignora o agendamento. O status NoShow é revertível — Staff pode revertê-lo para "Presente" mediante confirmação explícita. A reversão deve ser registrada no histórico com timestamp e usuário responsável, preservando o registro de NoShow revertido para fins de relatório. | Must |
| FR-031 | O sistema deve manter histórico permanente de todos os agendamentos, incluindo os cancelados e NoShows | Must |
| FR-052 | Staff deve poder editar um agendamento com status Pendente ou Confirmado, alterando data, hora, tipo de atendimento ou profissional; as mesmas validações de disponibilidade (FR-023), double-booking (FR-024) e cálculo de duração (FR-025) se aplicam; o link de autoatendimento gerado anteriormente é invalidado e deve ser regerado manualmente (FR-042) | Must |
| FR-042 | Staff e Admin Empresa devem poder gerar e copiar um link único de confirmação/cancelamento para um agendamento específico. O paciente acessa o link e pode: (1) confirmar presença — transição para status “Confirmado”; ou (2) cancelar o agendamento — transição para status “Cancelado”. Ambas as ações em até 2 toques, sem necessidade de login ou cadastro. **Estados da página pública:** (a) Pendente: dois botões ativos; (b) Já Confirmado: botão de confirmação desabilitado com texto “Presença já confirmada ✓”, botão de cancelamento ativo; (c) Presente / Concluído / NoShow: ambos os botões desabilitados com mensagem informativa “Esta consulta já foi atualizada pelo sistema”; (d) Cancelado / expirado: página exibe mensagem “Este link não é mais válido”. O link expira automaticamente no horário do agendamento ou quando o agendamento for cancelado, concluído ou marcado como NoShow, o que ocorrer primeiro. O envio do link ao paciente é responsabilidade do Staff — o sistema não envia mensagens automaticamente. | Must |
| FR-055 | Staff, Profissional e Admin Empresa devem poder visualizar o detalhe completo de um agendamento ao tocar em um card: dados do paciente (nome, telefone), tipo de atendimento, profissional, data/hora de início e término, status atual e histórico de transições de status com timestamps e usuário responsável | Must |
| FR-057 | O sistema deve registrar cada transição de status de agendamento em tabela de histórico contendo: agendamento_id, status_anterior, status_novo, timestamp (UTC), usuario_id e justificativa (obrigatória apenas em Presente→Cancelado e reversão de NoShow) | Must |

### 4.5 Relatórios

| ID | Requisito | Prioridade |
|----|-----------|-----------|
| FR-032 | O sistema deve exibir relatório de agendamentos filtrável por período (data início e data fim) e por profissional | Must |
| FR-033 | O relatório deve exibir contadores de: consultas agendadas, consultas confirmadas, consultas com NoShow, consultas canceladas e consultas finalizadas (Concluídas) | Must |
| FR-034 | O relatório deve exibir percentual de ocupação dos horários disponíveis da clínica no período em duas linhas: (1) **Ocupação confirmada** — agendamentos com status Confirmado, Presente, Concluído ou NoShow; (2) **Ocupação total** — todos os agendamentos exceto Cancelados (inclui Pendentes). Exibido de forma consolidada e por profissional. | Must |
| FR-045 | Admin Empresa deve poder informar a taxa de no-show estimada (campo numérico 0–100%) nas configurações da clínica; este valor é exibido como referência no relatório comparativo de ocupação (FR-034), disponível para comparação após 60 dias de operação | Must |
| FR-046 | Listagens com crescimento ilimitado devem suportar paginação server-side: relatório de agendamentos, histórico de agendamentos, lista de clientes, lista de profissionais e lista de usuários Staff. A API retorna `page`, `size`, `totalElements` e `totalPages`; page size padrão = 20 | Must |
| FR-049 | O relatório deve exibir lista paginada de pacientes que tiveram pelo menos 1 NoShow no período filtrado, contendo: nome do paciente (descriptografado para exibição), quantidade de NoShows e data do último NoShow | Must |

### 4.6 Conformidade LGPD

| ID | Requisito | Prioridade |
|----|-----------|-----------|
| FR-035 | Dados sensíveis de pacientes (nome, telefone, data de nascimento) devem ser criptografados em repouso com AES-256 | Must |
| FR-036 | Toda comunicação entre cliente e servidor deve ser realizada via TLS (HTTPS obrigatório) | Must |
| FR-037 | O cadastro de cliente deve exibir e registrar aceite explícito do termo de consentimento LGPD antes de salvar os dados; o sistema deve armazenar: timestamp do aceite (UTC), versão do termo vigente e usuario_id de quem realizou o cadastro | Must |
| FR-038 | O sistema deve manter log de auditoria registrando: usuário, ação, registro acessado e timestamp — para dados de pacientes. Os logs devem ser retidos por **5 anos**. O prazo de retenção deve ser implementado como constante configurável no backend para permitir ajuste futuro sem redeploy. ⚠️ *Validar prazo com consultoria jurídica antes do launch.* | Must |
| FR-039 | Admin Empresa deve poder exercer o direito ao esquecimento de um paciente mediante confirmação explícita. A operação anonimiza os dados pessoais identificáveis (nome, telefone, data de nascimento) substituindo-os por `[PACIENTE REMOVIDO]`, preservando o registro histórico dos agendamentos com status, timestamps e profissional para fins de auditoria e relatório. A anonimização é irreversível. | Must |
| FR-040 | O sistema não deve armazenar diagnósticos, procedimentos clínicos ou qualquer informação de saúde além dos dados de agendamento | Must |
| FR-054 | O sistema deve exibir a política de privacidade e o termo de consentimento em página pública acessível sem login; o link deve estar visível na tela de login e na tela de autoatendimento público (FR-042) | Must |

---

## 5. Requisitos Não-Funcionais

| ID | Requisito | Meta |
|----|-----------|------|
| NFR-001 | **Performance** — A tela de agenda deve carregar em menos de 2 segundos em conexão 4G | < 2s |
| NFR-002 | **Disponibilidade** — O sistema deve ter uptime mínimo de 99,5% ao mês | 99,5% |
| NFR-003 | **Responsividade** — O sistema deve ser funcional em telas a partir de 320px de largura (mobile-first) via PWA | 320px+ |
| NFR-004 | **Segurança Multi-Tenant** — Toda query em tabelas sensíveis deve ser filtrada por `empresa_id` via middleware de autenticação, sem possibilidade de acesso cross-tenant | Zero cross-tenant |
| NFR-005 | **Backup** — Backup automático diário do banco de dados com retenção mínima de 30 dias | Diário |
| NFR-006 | **Usabilidade** — Recepcionista deve conseguir realizar o primeiro agendamento sem treinamento formal em menos de 5 minutos | < 5 min |
| NFR-007 | **Seed seguro** — O usuário seed de Admin Sistema (`admin@agenda.com`) deve ter senha obrigatoriamente alterada antes do deploy em produção | Obrigatório |
| NFR-008 | **Simplicidade** — Qualquer ação primária do sistema deve ser completável por usuário não-treinado em no máximo 3 toques/cliques a partir da tela principal, sem necessidade de leitura de manual | ≤ 3 toques |

---

## 6. Escopo — Fora do MVP

As seguintes funcionalidades estão **explicitamente fora do escopo do MVP** para evitar scope creep. Estão documentadas como roadmap futuro:

| Funcionalidade | Roadmap |
|----------------|---------|
| Envio automático de mensagens de confirmação (WhatsApp, SMS, Email) | Ano 1 — pós-MVP |
| Perfil público para paciente auto-agendar | Ano 1 |
| Lista de espera automática para cancelamentos | Ano 1 |
| Repetição automática de agendamentos (recorrência) | Ano 1 |
| Integração com sistemas de pagamento | Ano 2 |
| Integração com planos de saúde | Ano 2 |
| Prontuário eletrônico | Fora do escopo |
| Telemedicina / videochamadas | Fora do escopo |
| Programa de indicações (referral) | Ano 2 |
| Profissional atuando em mais de uma clínica com o mesmo login | Pós-MVP |

---

## 7. Stack Técnico

> **Nota:** Decisões de implementação pertencem ao documento de arquitetura. O stack é registrado aqui apenas como restrição de produto acordada com o responsável.

| Camada | Tecnologia |
|--------|-----------|
| Frontend | React |
| Backend | Java 21 + Spring Boot 3.x |
| Banco de Dados | MySQL 8.0+ |
| Mobile | PWA (Progressive Web App) |
| Autenticação | JWT com `empresa_id` no payload |
| Hospedagem | Heroku ou Railway |
| Criptografia em repouso | AES-256 |
| Comunicação | HTTPS/TLS obrigatório |
| Isolamento multi-tenant | Row-level security por `empresa_id` (MVP) → Schema por empresa (Ano 2, 100+ clínicas) |

---

## 8. Modelo de Negócio e Contexto de Mercado

### Precificação
- **Plano Base:** R$ 99/mês — até 3 profissionais ativos
- **Adicional:** + R$ 29/mês por profissional adicional
- **Sem limite** de agendamentos, clientes ou usuários Staff
- **Trial:** 30 dias gratuitos

### Tamanho de Mercado
| Camada | Valor |
|--------|-------|
| TAM | ~R$ 314M/ano (264.000 clínicas pequenas) |
| SAM | ~R$ 94M/ano (centros urbanos, digitalmente alcançáveis) |
| SOM Ano 1 | ~R$ 95K/ano (~80 clínicas = 0,1% do SAM) |

### LTV vs. CAC
- **LTV estimado:** R$ 1.980 (vida média 20 meses com churn 5%)
- **CAC máximo saudável:** R$ 660 (LTV ÷ 3)
- **Canais prioritários:** Indicação (R$ 0-150), parceria com fornecedores de equipamentos (R$ 100-200)

### Perfil do Comprador e Estratégia de Aquisição

| Segmento | Quem decide | Gatilho de compra | Champion |
|---------|------------|------------------|----------|
| Solo (1-2 profissionais) | O próprio médico | Tempo perdido com no-show. ROI direto: "1-2 no-shows/mês = paga o plano" | Médico/dono |
| Grupo (3-5 profissionais) | Dono da clínica, influenciado pela recepcionista | Recepcionista apresenta como solução para sua carga operacional | **Recepcionista** |
| Médio (5+) | Gerente administrativo | Processo formal, comparativo de soluções | Gerente admin |

**Champion Strategy:** A recepcionista é o usuário mais frequente e a principal aliada de venda no segmento de maior volume (3-5 profissionais). O produto deve dar à recepcionista algo visível para mostrar ao dono como prova de ROI — o contador de no-shows evitados no dashboard do Admin é esse argumento.

### Critérios de Pivot

| Indicador | Threshold de pivot |
|----------|-------------------|
| Churn mensal | > 15% por 2 meses consecutivos |
| Profissionais com acesso diário | < 60% dos usuários ativos |
| CAC realizado | > 3× LTV (> R$ 5.940) |

---

## 9. Questões em Aberto

Todas as questões foram resolvidas em sessão com Rodrigo Navarro em 2026-06-04 (ver `.decision-log.md` para rationale completo).

| # | Questão | Decisão | FR Impactado |
|---|---------|---------|-------------|
| OQ-1 | NoShow automático deve ser irreversível ou revertível? | Revertível com confirmação explícita de Staff. Reversão registrada com timestamp e usuário no histórico. | FR-030 |
| OQ-2 | Como calcular ocupação no relatório? | Duplo: (1) ocupação confirmada + (2) ocupação total (exceto cancelados) — consolidado e por profissional. | FR-034 |
| OQ-3 | Permitir agendamentos simultâneos? | Double-booking até 2 agendamentos por slot/profissional com sobreposição de janelas. Terceiro bloqueado. Confirmação explícita de Staff obrigatória para o segundo. | FR-024 |
| OQ-4 | Retenção de logs LGPD: qual prazo? | 5 anos, implementado como constante configurável no backend. Validação jurídica aceita como risco de launch consciente (D-16). | FR-038 |

---

## 10. Premissas

- `[ASSUMPTION]` O alerta de reordenação sugerida (FR-020) exige que o profissional e a recepcionista acessem o sistema simultaneamente — assume-se que o polling de 30-60s é suficiente para essa sincronização no contexto de uma clínica pequena.
- `[ASSUMPTION]` A geração automática de NoShow (FR-030) ocorre server-side via job agendado — assume-se que a infraestrutura de hospedagem (Heroku/Railway) suporta scheduled jobs.
- `[ASSUMPTION]` O relatório de ocupação (FR-034) calcula capacidade total como soma de todos os slots disponíveis configurados (FR-013) no período — assume-se que a disponibilidade está sempre cadastrada corretamente.
- `[ASSUMPTION]` Um profissional de saúde opera em apenas uma clínica cadastrada no sistema. Se operar em duas clínicas que usem DrAgenda, precisará de dois logins distintos.
- `[ASSUMPTION]` O double-booking (FR-024) é sempre intencional — assume-se que Staff que confirma a sobreposição está ciente de que dois pacientes estão marcados para o mesmo horário do mesmo profissional.
- `[ASSUMPTION]` MVP tem escopo geográfico inicial de clínicas no Brasil (UTC-3 a UTC-5). A configuração de fuso horário por clínica (FR-041) resolve diferenças internas; fusos fora desse range não são testados no MVP.
