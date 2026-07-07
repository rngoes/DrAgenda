---
title: "Product Brief: DrAgenda"
status: "approved"
created: "2026-04-06"
updated: "2026-06-03"
inputs: 
  - "docs/Prompt do MVP de Agenda.txt"
  - "Análise de mercado healthtech 2026"
  - "Pesquisa competitiva: Doctoralia, Ninsaúde, iClinic"
---

# Product Brief: DrAgenda

## Executive Summary

**DrAgenda** é um sistema de agendamento profissional projetado para consultórios médicos e odontológicos de pequeno e médio porte que ainda operam com planilhas, cadernos e WhatsApp. Oferecemos uma solução SaaS multi-tenant que possibilita visibilidade em tempo real da agenda, agendamentos e cancelamentos simplificados, e controle granular de permissões — tudo acessível via mobile.

O problema central que resolvemos é caro e doloroso: **pacientes que não comparecem porque desmarcar é complicado, e profissionais que perdem tempo aguardando quem não virá**. Em um setor onde 20-30% dos agendamentos resultam em no-shows, cada consulta perdida representa receita não-recuperável e tempo desperdiçado. **DrAgenda paga por si mesmo eliminando apenas 1-2 no-shows por mês.**

DrAgenda ataca um mercado de **~80% das clínicas pequenas** (<5 profissionais) que ainda não adotaram sistemas digitais por considerarem soluções existentes complexas demais (R$ 200-500/mês) ou caras demais. Nosso modelo híbrido de **R$ 99/mês para até 3 profissionais (+ R$ 29/profissional adicional)** torna adoção acessível enquanto escala com crescimento da clínica. Nossa estratégia de entrada é validação via network pessoal seguida de aquisição digital. O diferencial defensável é construído em camadas: simplicidade hiperssimples mobile-first no curto prazo, network effect via auto-agendamento no médio prazo, e inteligência de dados no longo prazo.

## O Problema

### A Dor Real dos Consultórios

Dra. Silva chega às 14h para atender seu paciente das 14h. Ele não vem. Ela espera 15 minutos. Liga, não atende. A recepcionista não sabe se ele confirmou. Às 14:20, desiste e chama o próximo — que só estava agendado para 15h e não pode vir antes. **Uma hora de trabalho perdida. R$ 200-400 de receita evaporada.**

Esse cenário se repete 2-3 vezes por semana em clínicas pequenas. **O no-show rate no Brasil varia de 20% a 30%** — e a principal causa não é má-fé, é **fricção para desmarcar**. Para cancelar, o paciente precisa:
1. Lembrar do telefone da clínica
2. Ligar em horário comercial
3. Esperar a recepcionista atender (que pode estar atendendo outro paciente)
4. Explicar quem é, qual profissional, qual horário
5. Torcer para que a informação chegue ao profissional

**É mais fácil simplesmente não aparecer.**

### A Operação Caótica

Do lado da clínica, o problema é sistêmico:

- **Recepcionistas** gastam horas por dia checando agendas de múltiplos profissionais em planilhas diferentes, ligando para confirmar consultas, e gerenciando conflitos de horário escritos à mão
- **Profissionais** não têm visibilidade em tempo real — não sabem quem vem hoje, se confirmou, ou se cancelou até chegarem na clínica
- **Conflitos de horário** acontecem porque não há validação automática — dois pacientes marcados no mesmo slot, ou agendamentos durante o horário de almoço do profissional
- **Sem histórico confiável** — impossível saber taxa real de no-show, horários mais problemáticos, ou quais pacientes têm padrão de faltas

As alternativas atuais não resolvem:
- **Planilhas/Cadernos:** Sem validação, sem visibilidade remota, sem histórico estruturado
- **WhatsApp:** Informação dispersa em milhares de mensagens, impossível buscar ou consolidar
- **Sistemas completos (Doctoralia, Ninsaúde, iClinic):** Complexos demais, caros demais (R$ 200-500/mês), exigem treinamento, pesados para usar no celular

## A Solução

**DrAgenda é a agenda digital que profissionais de saúde conseguem usar no celular sem treinamento, e que pacientes conseguem desmarcar em 2 toques.**

### Para a Recepcionista (Staff)
Um dashboard visual que mostra todos os profissionais da clínica em uma única tela. Criar agendamento é preencher 5 campos: Cliente, Tipo de Atendimento, Profissional, Data, Hora. O sistema valida automaticamente:
- ✅ Profissional disponível naquele horário da semana?
- ✅ Sem conflito com intervalo de almoço?
- ✅ Sem sobreposição com outro agendamento?
- ✅ Duração do atendimento cabe na disponibilidade?

**Se passar, está agendado. Se não passar, a interface explica o problema em linguagem clara.**

### Para o Profissional (Médico/Dentista)
Abre o celular pela manhã, vê sua agenda do dia. Cada paciente aparece com horário, nome, tipo de atendimento, e status em cores:
- 🟡 **Pendente** (sem confirmação)
- 🔵 **Confirmado** (paciente confirmou presença)
- 🟢 **Concluído** (atendimento realizado)
- ⚫ **Cancelado** (desmarcado)

Com dois toques, muda status. Com um scroll, vê semana inteira. **Sem complexidade. Sem curva de aprendizado.**

### Para o Paciente (Roadmap Futuro)
Recebe link personalizado via WhatsApp/SMS. Abre no navegador do celular, vê horários disponíveis do profissional preferido naquela semana, agenda em 3 toques. **Mais importante: consegue desmarcar com o mesmo link, sem ligar, sem explicar.**

## O Que Faz Isso Ser Diferente

### 1. Simplicidade Brutal (Diferencial Imediato — MVP)
Não tentamos ser prontuário eletrônico, telemedicina, sistema financeiro ou CRM. **Somos a melhor agenda do mercado. Ponto.**

Essa disciplina de dizer "não" a features nos permite:
- Interface tão simples que recepcionista aprende em 5 minutos
- App mobile que profissionais usam sem treinamento
- Zero bloat, zero confusão, zero cursos de onboarding

Competidores sempre adicionam complexidade. Nós resistimos.

### 2. Network Effect via Auto-Agendamento (Diferencial Médio Prazo)
Quando implementarmos perfil público para pacientes agendarem diretamente (pós-MVP):
- Quanto mais clínicas usarem DrAgenda, mais pacientes reconhecem o sistema
- Pacientes que já usaram DrAgenda em uma clínica preferem clínicas que usam DrAgenda
- Clínicas que adotam DrAgenda atraem pacientes que valorizam facilidade de agendamento
- **Efeito de rede positivo: valor cresce não-linearmente com adoção**

### 3. Inteligência de Dados (Diferencial Longo Prazo)
Com escala (milhares de agendamentos):
- Identificamos horários com maior taxa de no-show por especialidade
- Sugerimos otimizações de agenda baseadas em padrões históricos
- Alertamos sobre pacientes com padrão de faltas recorrentes
- Oferecemos benchmarks: "Sua taxa de no-show é 15%, média do setor é 25%"

**Barreira de entrada cresce com cada agendamento processado.**

## Quem Isso Serve

### Usuário Primário: Recepcionista / Staff
Mulher 25-45 anos, recepcionista de consultório pequeno (2-4 profissionais). Gerencia agendas de múltiplos médicos/dentistas simultaneamente. Atende telefone, recebe pacientes, cobra, e ainda precisa organizar a agenda. **Não tem tempo para sistemas complicados.** Sucesso para ela = marcar consulta em menos de 1 minuto, sem conflitos, sem reclamações.

### Usuário Secundário: Profissional de Saúde
Médico ou dentista 30-60 anos, opera em consultório próprio ou clínica compartilhada. Usa smartphone mas não é "tech-savvy". **Quer ver sua agenda do celular no caminho para o trabalho.** Sucesso para ele = saber exatamente quem vem hoje, atualizar status com 2 toques, sem surpresas.

### Usuário Terciário (Futuro): Paciente
Qualquer pessoa que precisa de atendimento médico/odontológico. **Quer agendar e desmarcar sem burocracia, de preferência sem ligar.** Sucesso = marcar consulta em 3 toques, desmarcar quando imprevisto surgir sem culpa ou atrito.

### Comprador: Admin da Clínica
O perfil do comprador varia com o tamanho da clínica — e essa distinção define a abordagem de venda:

| Perfil | Quem Decide | Abordagem de Venda |
|--------|-------------|-------------------|
| 1-2 profissionais (solo) | O próprio médico/dentista | Pitch de ROI direto: "quanto você perde por mês em no-shows?" Demo de 10 min + trial grátis |
| 3-5 profissionais | Recepcionista sênior + dono | A recepcionista é a **champion** — ela sente a dor diariamente e influencia o dono |
| 5+ profissionais | Gerente administrativo | Processo mais longo, requer proposta formal com ROI documentado |

**Champion Strategy:** Em clínicas médias, a recepcionista é a aliada estratégica. Uma demonstração de 15 minutos com ela tende a resultar na venda. **Em consultórios solo, o médico é simultaneamente comprador, admin e usuário** — o pitch deve focar em tempo desperdiçado esperando paciente que não vem, não em eficiência operacional abstrata.

**Sucesso** = ROI claro e imediato (1-2 no-shows evitados/mês > custo da ferramenta).

## Critérios de Sucesso

### Sinais de Adoção
1. **Profissionais usando diariamente:** Meta = 80%+ dos profissionais cadastrados acessam o app 5+ dias por semana
2. **Baixa fricção de onboarding:** Clínica consegue operar 100% no sistema em menos de 1 semana após cadastro
3. **Retenção mensal:** Churn < 5% ao mês (clínicas que começam não abandoham)

### Impacto no Negócio do Cliente
4. **Redução de no-shows:** Comparar no-show rate antes vs depois de 60 dias (meta: redução de 30% no rate)
   - **Metodologia de Baseline:** No onboarding, o Admin informa estimativas atuais ("quantos pacientes por semana não comparecem sem avisar?"). Após 60 dias, o sistema exibe comparação automática entre o baseline declarado e os cancelamentos registrados. Para as primeiras 10 clínicas piloto, oferecer 2 semanas de registro paralelo (sistema + método antigo) para gerar baseline real antes da adoção plena.
5. **Tempo economizado:** Recepcionista gasta 40%+ menos tempo gerenciando agenda vs método anterior
6. **NPS alto:** Net Promoter Score > 50 (clientes indicam espontaneamente)

### Objetivos de Negócio DrAgenda
7. **Primeiras 10 clínicas:** Network pessoal + boca-a-boca (0-3 meses)
8. **Primeiras 50 clínicas:** Aquisição digital + indicações (3-12 meses)
9. **CAC < LTV:** Custo de aquisição por clínica menor que valor vitalício em 12 meses

## Escopo

### MVP — O Que Está Dentro
**Objetivo: Operação básica funcional e indispensável**

✅ **Cadastros Base:**
- Empresas (clínicas) com isolamento de dados
- Usuários com 4 perfis: Admin Sistema, Admin Empresa, Staff, Profissional
- Clientes (pacientes)
- Tipos de Atendimento (com duração configurável)
- Disponibilidade do Profissional (por dia da semana, com horário e intervalos)

✅ **Dashboard de Agenda:**
- Visualização por dia/semana, filtro por profissional
- Cards coloridos por status (Pendente/Confirmado/Concluído/Cancelado)
- Criação e edição de agendamentos com validações automáticas
- Atualização de status em tempo real
- Visão mobile-first (lista vertical) e desktop (grade horária)

✅ **Regras de Negócio Críticas:**
- Bloqueio de agendamentos duplicados no mesmo horário
- Validação de disponibilidade do profissional (dia da semana + horário + intervalo)
- Cálculo automático de duração baseado no Tipo de Atendimento
- Histórico permanente de agendamentos passados

✅ **Controle de Acesso:**
- Admin Sistema: cadastra empresas
- Admin Empresa: acesso total aos dados da sua empresa
- Staff: cadastra clientes e agendamentos de todos os profissionais
- Profissional: vê apenas seus próprios agendamentos, atualiza status

✅ **Conformidade LGPD (Compliance Mínima Viável):**
- Criptografia de dados sensíveis em repouso e em trânsito (AES-256)
- Termo de consentimento explícito no cadastro de clientes
- Política de privacidade acessível e clara
- Log de auditoria: rastreamento de quem acessou dados de quais pacientes
- Funcionalidade de exclusão de dados (direito ao esquecimento)
- **Limitação crítica:** Sistema NÃO armazena diagnósticos, procedimentos ou informações clínicas (evita certificação ICP-Brasil)

### MVP — O Que Está Explicitamente Fora
(para evitar scope creep — roadmap futuro)

❌ Integração WhatsApp para lembretes/confirmações
❌ Perfil público para paciente auto-agendar
❌ Lista de espera automática para cancelamentos de última hora
❌ Repetição automática de agendamentos (semanal/mensal)
❌ Dashboard com métricas/analytics/relatórios
❌ Integração com sistemas de pagamento
❌ Integração com planos de saúde (validação de elegibilidade)
❌ Prontuário eletrônico
❌ Telemedicina/videochamadas
❌ Envio automático de SMS/Email
❌ Programa de indicações (referral)

### Tech Stack Definido
- **Frontend:** React (interface responsiva e componentizada)
- **Backend:** Java 21 com Spring Boot 3.x (APIs REST)
- **Banco de Dados:** MySQL 8.0+
- **Hospedagem:** Heroku ou Railway (facilita deploy e escalabilidade inicial)
- **Mobile:** PWA (Progressive Web App - funciona como app nativo sem necessidade de lojas)
- **Autenticação:** JWT (JSON Web Tokens) — o token carrega `empresa_id` para garantir isolamento multi-tenant
- **Infraestrutura:** Backup automático diário, SSL/TLS obrigatório
- **Seed User:** admin@agenda.com / admin (Admin do Sistema - **trocar senha em produção**)

**Estratégia de Isolamento Multi-Tenant (LGPD):**
- **MVP — Row-level Security:** Toda tabela sensível (clientes, agendamentos) possui coluna `empresa_id`. Toda query é filtrada automaticamente por `empresa_id` via middleware de autenticação. Sem acesso cross-tenant possível por design.
- **Roadmap — Schema por empresa:** Ao atingir 100 clínicas ativas, migrar para isolamento por schema no MySQL para máxima separação lógica de dados. Atende auditorias LGPD avançadas.
- **Justificativa:** Row-level cobre todos os requisitos legais de separação lógica para o MVP. Isolamento físico (schema/banco separado) só é exigido em contextos enterprise, fora do escopo atual.

## Modelo de Negócio

### Tamanho de Mercado (TAM/SAM/SOM)

| Camada | Base | Cálculo | Valor |
|--------|------|---------|-------|
| **TAM** | ~264.000 clínicas pequenas (<5 profissionais) no Brasil¹ | 264.000 × R$ 99 × 12 meses | **~R$ 314M/ano** |
| **SAM** | ~30% digitalmente alcançáveis (centros urbanos) | 79.200 clínicas × R$ 99 × 12 | **~R$ 94M/ano** |
| **SOM Ano 1** | Meta conservadora: 0,1% do SAM | ~80 clínicas | **~R$ 95K/ano** |
| **SOM Ano 2** | 0,5% do SAM | ~400 clínicas | **~R$ 475K/ano** |

> ¹ *Estimativa baseada em dados do CFM (~550K médicos registrados) e CFO (~340K dentistas registrados), com ~80% dos estabelecimentos classificados como pequenos (<5 profissionais).*

**Meta de 50 clínicas em 12 meses representa 0,06% do SAM — extremamente conservadora e alcançável.**

### Viabilidade Financeira (CAC vs. LTV)

| Métrica | Cálculo | Resultado |
|---------|---------|-----------|
| **LTV** | R$ 99/mês × vida média 20 meses (churn 5%) | **R$ 1.980** |
| **CAC máximo saudável** | LTV ÷ 3 | **R$ 660** |
| **CAC estimado — Indicação** | Custo de incentivo + tempo | **R$ 0–150** ✅ |
| **CAC estimado — Google Ads** | Keywords de nicho healthtech | **R$ 400–700** ✅ |
| **CAC estimado — Meta/Instagram** | Segmentação gestores de clínica | **R$ 300–500** ✅ |
| **CAC estimado — Parceria fornecedores** | Co-marketing com distribuidores de equipamentos | **R$ 100–200** ✅ |

> **Canal prioritário Fase 2:** Parcerias com distribuidores de equipamentos médicos/odontológicos entregam CAC próximo a zero com alta qualificação do lead.

### Precificação
**Modelo Híbrido - Acessível e Escalável:**
- **Plano Base:** R$ 99/mês (até 3 profissionais ativos)
- **Adicional:** + R$ 29/mês por profissional adicional
- **Sem limite** de agendamentos, clientes, ou usuários staff
- **Pagamento:** Recorrente mensal (cartão de crédito ou boleto)

**Posicionamento de Preço:**
- 50-60% mais barato que competidores (Doctoralia R$ 200+, iClinic R$ 300+, Ninsaúde R$ 400+)
- ROI imediato: **1-2 no-shows evitados/mês = sistema pago**
- Custo médio de 1 no-show: R$ 200-400 (consulta + tempo perdido)

**Roadmap de Monetização:**
- **Ano 1:** Plano único (foco em adoção)
- **Ano 2:** Plano Premium (R$ 199/mês) com lista de espera automática, analytics, WhatsApp integrado
- **Ano 3:** Add-ons premium (integração planos de saúde, auditoria LGPD avançada)

### Estratégia de Crescimento
**Fase 1 (0-3 meses):** Network pessoal - 10 clínicas piloto | CAC: ~R$ 0–150 (indicação direta)  
**Fase 2 (3-12 meses):** Google Ads + Meta + indicações - 50 clínicas | CAC alvo: < R$ 500  
**Fase 3 (12-24 meses):** Parcerias com fornecedores de equipamentos + referral program - 500+ clínicas | CAC alvo: < R$ 200

## Visão: Onde Isso Vai Se Funcionar

### Ano 1: Dominar a Agenda
Ser **a melhor e mais simples agenda** para clínicas pequenas no Brasil. 100+ clínicas operando, profissionais e recepcionistas preferem DrAgenda sobre qualquer alternativa. 

**Marcos principais:**
- Implementar **perfil público para auto-agendamento** de pacientes
- Lançar **lista de espera automática** para cancelamentos de última hora (feature killer: clínica preenche vagas abertas em minutos)
- Atingir churn < 5% ao mês (retenção forte)

### Ano 2: Network Effect + Parcerias Estratégicas
Pacientes reconhecem DrAgenda como "o sistema fácil de agendar e desmarcar". Clínicas reportam **40%+ redução em no-shows**. 1.000+ clínicas ativas.

**Marcos principais:**
- Lançar integração **WhatsApp** para lembretes automáticos e confirmações
- Firmar parcerias com **fornecedores de equipamentos** (Dabi Atlante, Gnatus): "Compre equipamento + ganhe 6 meses DrAgenda" = CAC zero
- Lançar **Plano Premium** com analytics e automações avançadas
- Programa de indicações (referral): clínica indica outra, ambas ganham desconto

### Ano 3: Inteligência de Dados + Expansão
Dados acumulados permitem oferecer insights únicos: "Pacientes que agendam terça à tarde têm 2x mais chance de faltar" ou "Tipo de consulta X historicamente dura 15min a mais que o previsto". 

**Marcos principais:**
- Analytics preditivos para otimização de agenda
- Expansão vertical: clínicas de estética, fisioterapia, psicologia (mesma dor, mesma solução)
- Considerar expansão regional (LATAM)
- White-label para associações de classe (CRO, CRM regionais)

### O Que NÃO Seremos
Não vamos competir com prontuários eletrônicos completos (Ninsaúde, MV). Não vamos virar marketplace de descoberta de profissionais (Doctoralia). **Vamos ser insubstituíveis em uma coisa: a melhor experiência de agendamento do setor de saúde.**

## Riscos e Mitigações

### Riscos Críticos

**1. Adoção Multi-Camada**  
**Risco:** Sistema só funciona se Admin, Staff E Profissionais adotarem. Se profissionais não atualizarem status no celular, sistema perde valor.  
**Mitigação:** Onboarding hands-on nas primeiras 10 clínicas. Check-in semanal no primeiro mês. Interface tão simples que "não dá pra errar".

**2. Conformidade LGPD**  
**Risco:** Violação de dados de saúde = multa até R$ 50 milhões + perda de reputação.  
**Mitigação:** Compliance mínima viável no MVP (criptografia, auditoria, consentimento). Consultoria jurídica antes do lançamento. Seguro cyber para primeiros 2 anos.

**3. Competição de Players Estabelecidos**  
**Risco:** Doctoralia, iClinic podem lançar "modo simples" e copiar diferencial.  
**Mitigação:** Vantagem de velocidade (MVP em 3-4 meses). Construir network effect antes que respondam. Foco em NPS altíssimo = boca-a-boca incopiável.

**4. Resistência a Pagamento Recorrente**  
**Risco:** Clínicas brasileiras resistem a SaaS, preferem pagamento único.  
**Mitigação:** ROI claro no pitch: "R$ 99/mês vs R$ 5.000/mês perdidos em no-shows". Oferecer desconto anual (12 meses pelo preço de 10). Freemium trial de 30 dias.

### Critérios de Pivot
Se após 6 meses de operação:
- Churn > 15% ao mês (clientes abandonando)
- < 60% dos profissionais usando diariamente
- CAC > 3x LTV (custo de aquisição maior que valor vitalício)

→ Reavaliar modelo de negócio, pricing, ou foco de mercado.

---

## Próximos Passos Imediatos

1. **Validação MVP:** Implementar funcionalidades core conforme especificação técnica
2. **Piloto:** Onboarding de 3-5 clínicas do network pessoal
3. **Coleta de Feedback:** Observar uso real, identificar fricções não-previstas
4. **Iteração:** Ajustar interface e validações baseado em uso real
5. **Escala:** Refinar GTM digital para primeiras 50 clínicas

---

**Contato do Projeto:**  
Produto: DrAgenda  
Responsável: Navarro  
Data: Abril 2026
