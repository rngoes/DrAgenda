---
stepsCompleted: [1, 2, 3, 4]
status: final
inputDocuments:
  - "_bmad-output/planning-artifacts/prds/prd-DrAgenda-2026-06-03/prd.md"
  - "_bmad-output/planning-artifacts/architecture.md"
  - "_bmad-output/planning-artifacts/ux-designs/ux-DrAgenda-2026-06-04/DESIGN.md"
  - "_bmad-output/planning-artifacts/ux-designs/ux-DrAgenda-2026-06-04/EXPERIENCE.md"
---

# DrAgenda - Epic Breakdown

## Overview

Este documento fornece o detalhamento completo de epics e stories para o DrAgenda, decompondo os requisitos do PRD, UX Design e Arquitetura em stories implementáveis.

## Requirements Inventory

### Functional Requirements

FR-001: O sistema deve autenticar usuários via email e senha com token JWT contendo `empresa_id`
FR-002: O sistema deve suportar 4 perfis de acesso: Admin Sistema, Admin Empresa, Staff, Profissional
FR-003: Admin Empresa deve ter acesso total aos dados de sua empresa e somente dela
FR-004: Staff deve poder criar e editar agendamentos e clientes de todos os profissionais da clínica
FR-005: Profissional deve visualizar apenas seus próprios agendamentos, criar agendamentos para si mesmo (campo Profissional pré-preenchido e bloqueado) e atualizar o status dos seus agendamentos
FR-006: Admin Sistema deve poder cadastrar empresas sem acesso aos dados de pacientes
FR-056: Ao cadastrar uma clínica, Admin Sistema cria simultaneamente o primeiro usuário Admin Empresa (nome, email, senha temporária exibida na tela); mesmas regras de complexidade de FR-007
FR-007: Admin Empresa pode criar usuários com perfil Staff ou Profissional (não pode criar outro Admin Empresa); senha temporária mín. 8 caracteres com maiúsculas + minúsculas + caractere especial
FR-043: Admin Empresa pode redefinir senha de usuário da empresa, gerando nova senha temporária exibida na tela (mesmas regras de complexidade); força troca no próximo login
FR-044: Na primeira autenticação com senha temporária, bloquear acesso e exibir formulário obrigatório de troca; nova senha mín. 8 caracteres com maiúsculas, minúsculas e caractere especial
FR-047: Todos os perfis devem ter botão de logout explícito no menu de perfil; remove o JWT do armazenamento local e redireciona para login; sem invalidação server-side no MVP (JWT stateless)
FR-048: Usuário autenticado pode alterar sua própria senha no perfil; nova senha mín. 8 caracteres com maiúsculas, minúsculas e caractere especial; sem email no MVP
FR-008: Admin Sistema deve poder cadastrar clínicas com isolamento total de dados entre empresas
FR-051: Admin Sistema deve poder listar todas as clínicas cadastradas, editar seus dados e inativar/reativar; ao inativar, todos os usuários da clínica perdem acesso imediatamente
FR-009: Admin Empresa pode cadastrar, editar, inativar e reativar profissionais; ao inativar, sinalizar agendamentos futuros como "Profissional Inativo — redistribuir manualmente"; ao reativar, sinais removidos automaticamente dos agendamentos futuros não redistribuídos
FR-010: Admin Empresa deve poder cadastrar, editar, inativar e reativar usuários Staff
FR-011: Staff deve poder cadastrar clientes com coleta de termo de consentimento LGPD explícito
FR-050: Staff deve poder listar, buscar (por nome ou telefone) e editar dados de pacientes já cadastrados; a busca alimenta o campo "Cliente" na criação de agendamentos (FR-022); exclusão/anonimização segue FR-039
FR-012: Admin Empresa deve poder cadastrar e editar Tipos de Atendimento com duração configurável em minutos
FR-053: Admin Empresa pode inativar um Tipo de Atendimento; tipos inativos não aparecem no formulário de criação/edição de agendamentos; agendamentos existentes não são afetados
FR-013: Admin Empresa deve poder configurar disponibilidade de cada profissional por dia da semana (horário início, fim e intervalo)
FR-014: O sistema deve exibir dashboard de agenda com visualização diária e semanal
FR-015: O sistema deve permitir filtro da agenda por profissional
FR-016: Cada agendamento deve ser exibido como card com cor por status: Pendente (amarelo), Confirmado (azul), Presente (roxo), Concluído (verde), Cancelado (cinza escuro), NoShow (vermelho)
FR-017: O sistema deve permitir filtro de agendamentos por status
FR-018: Na visualização do profissional, o primeiro agendamento do dia deve ser destacado visualmente para consulta rápida no mobile
FR-019: O dashboard deve atualizar automaticamente via polling a cada 30–60 segundos, sem ação manual
FR-020: O sistema deve exibir alerta visual quando houver paciente com status "Presente" cujo horário é posterior ao próximo agendamento Pendente/Confirmado
FR-021: No mobile (≤767px) agenda em lista vertical; no desktop (≥768px) em grade horária
FR-022: Staff e Profissional devem poder criar agendamento preenchendo 5 campos: Cliente, Tipo de Atendimento, Profissional, Data e Hora; para Profissional, o campo Profissional é pré-preenchido com o próprio usuário e bloqueado para edição
FR-023: O sistema deve validar disponibilidade do profissional verificando: dia da semana disponível, horário dentro da faixa, janela completa não intersecta período de intervalo
FR-024: Máximo 2 agendamentos com janelas sobrepostas para o mesmo profissional; segundo exige confirmação explícita de Staff ou Admin Empresa; terceiro é bloqueado
FR-025: O sistema deve calcular automaticamente o horário de término com base na duração do Tipo de Atendimento
FR-026: Quando validação falhar, exibir mensagem em linguagem clara explicando o motivo e conflito
FR-027: Transições de status: Pendente→Confirmado→Presente→Concluído/Cancelado. Staff: todas as transições. Profissional: Presente→Concluído. Admin Empresa: mesmas de Staff. Pendente→Cancelado e Confirmado→Cancelado permitidos para Staff e Admin Empresa. Estados terminais: Concluído e Cancelado sem novas transições. NoShow revertível para Presente apenas por Staff com confirmação
FR-028: Atualização de status em no máximo 2 toques no mobile (exceções documentadas por regra de negócio)
FR-029: Exclusivamente Staff pode usar busca de “próximo horário disponível” de todos os profissionais da clínica; filtro opcional por profissional e tipo; horizonte 30 dias; máximo 20 resultados
FR-030: Marcar automaticamente como NoShow agendamentos que atingirem horário + 30min sem transição para Presente/Concluído; timer independente por agendamento; job executa somente se status ainda for Pendente/Confirmado; reversão disponível para Staff com registro no histórico
FR-031: Manter histórico permanente de todos os agendamentos incluindo cancelados e NoShows

FR-032: Admin Empresa deve poder visualizar relatório de agendamentos filtrado por período e profissional
FR-033: Relatório deve exibir contagem de agendamentos por status (Agendados, Confirmados, Concluídos, Cancelados, NoShows)
FR-034: Relatório deve exibir taxa de ocupação: confirmada e total, consolidado e por profissional
FR-045: Admin Empresa pode informar taxa de no-show estimada (0–100%) nas configurações da clínica; exibida no relatório comparativo de ocupação (FR-034) após 60 dias
FR-046: Listagens com crescimento ilimitado devem suportar paginação server-side (relatório, histórico, clientes, profissionais, usuários); API retorna `page`, `size`, `totalElements`, `totalPages`; page size padrão = 20
FR-049: O relatório deve exibir lista paginada de pacientes com pelo menos 1 NoShow no período filtrado, contendo nome do paciente (descriptografado), quantidade de NoShows e data do último NoShow
FR-035: Dados sensíveis de pacientes (nome, telefone, data de nascimento) devem ser criptografados em repouso com AES-256
FR-036: Toda comunicação entre cliente e servidor deve ser realizada via TLS (HTTPS obrigatório)
FR-037: O cadastro de cliente deve exibir e registrar aceite explícito do termo de consentimento LGPD; armazenar timestamp do aceite (UTC), versão do termo vigente e usuario_id de quem realizou o cadastro
FR-038: O sistema deve manter log de auditoria (usuário, ação, registro, timestamp) para dados de pacientes, retidos por 5 anos (constante configurável no backend)
FR-039: Admin Empresa pode exercer direito ao esquecimento de um paciente: anonimiza dados pessoais substituíndos por `[PACIENTE REMOVIDO]`, preservando histórico de agendamentos; irreversível
FR-040: O sistema não deve armazenar diagnósticos, procedimentos clínicos ou qualquer informação de saúde além dos dados de agendamento
FR-054: O sistema deve exibir política de privacidade e termo de consentimento em página pública sem login; link visível na tela de login e na tela de autoatendimento (FR-042)
FR-041: Admin Empresa configura fuso horário da clínica nas configurações; todos os horários em UTC; job de NoShow usa UTC
FR-058: Admin Empresa pode editar dados básicos da própria clínica (nome, telefone, endereço); fuso horário e baseline de no-show fazem parte das mesmas configurações
FR-042: Staff e Admin Empresa podem gerar e copiar link único de confirmação/cancelamento. Estados da página pública: (a) Pendente: 2 botões ativos; (b) Já Confirmado: botão confirmação desabilitado “Presença já confirmada ✓”; (c) Presente/Concluído/NoShow: ambos desabilitados “Consulta já atualizada pelo sistema”; (d) Cancelado/expirado: “Link não é mais válido”. Link expira no horário ou ao cancelar/concluir/noshow. Envio via Staff
FR-043: Admin Empresa pode redefinir senha de usuário da empresa, gerando nova senha temporária exibida na tela (mesmas regras de complexidade de FR-007); força troca no próximo login
FR-044: Na primeira autenticação com senha temporária, bloquear acesso e exibir formulário obrigatório de troca; nova senha mín. 8 caracteres com maiúsculas, minúsculas e caractere especial
FR-047: Todos os perfis devem ter botão de logout explícito no menu de perfil; remove o JWT do armazenamento local e redireciona para login; sem invalidação server-side no MVP (JWT stateless)
FR-048: Usuário autenticado pode alterar sua própria senha no perfil; nova senha mín. 8 caracteres com maiúsculas, minúsculas e caractere especial; sem email no MVP
FR-051: Admin Sistema deve poder listar todas as clínicas cadastradas, editar seus dados e inativar/reativar; ao inativar, todos os usuários da clínica perdem acesso imediatamente
FR-052: Staff deve poder editar agendamento com status Pendente ou Confirmado (data, hora, tipo, profissional); mesmas validações de FR-023/024/025; link de autoatendimento anterior é invalidado
FR-053: Admin Empresa pode inativar um Tipo de Atendimento; tipos inativos não aparecem no formulário de criação/edição de agendamentos; agendamentos existentes não são afetados
FR-055: Staff, Profissional e Admin Empresa podem visualizar detalhe completo de um agendamento ao tocar em um card: dados do paciente (nome, telefone), tipo, profissional, data/hora início e término, status atual e histórico de transições com timestamps e usuário responsável
FR-057: O sistema deve registrar cada transição de status em tabela de histórico: agendamento_id, status_anterior, status_novo, timestamp (UTC), usuario_id e justificativa (obrigatória em Presente→Cancelado e reversão de NoShow)

### NonFunctional Requirements

NFR-001: Tempo de resposta da API < 2 segundos para 95% das requisições sob carga normal
NFR-002: Disponibilidade mínima de 99,5% no horário comercial (7h–20h, segunda a sábado)
NFR-003: Suporte a múltiplos usuários simultâneos por clínica sem degradação de performance
NFR-004: Criptografia AES-256 para dados sensíveis em repouso; HTTPS/TLS em trânsito
NFR-005: Conformidade LGPD: consentimento explícito, direito ao esquecimento (anonimização), log de auditoria 5 anos
NFR-006: Interface mobile-first responsiva, utilizável em telas ≥ 320px sem scroll horizontal
NFR-007: WCAG 2.1 AA + daltonismo: indicadores de status sempre combinam cor + ícone + rótulo
NFR-008: Backup automático diário do banco de dados com retenção mínima de 30 dias

### Additional Requirements

- Monorepo: `/frontend` (Vite+React+TS) e `/backend` (Spring Boot 3.x+Java 21+Maven) em repositório único
- Migrations de banco via Flyway (`V{N}__{descricao}.sql`) — nunca `ddl-auto: update` em produção
- Multi-tenancy: `empresaId` extraído do JWT em todo endpoint autenticado; nunca de parâmetro da request
- `GET /api/v1/cancelar/{token}` é a única rota que bypassa filtro de tenant
- CORS configurável via `APP_FRONTEND_URL` (variável de ambiente, permite múltiplas origens)
- Variáveis de ambiente canônicas definidas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `ENCRYPTION_KEY`, `APP_FRONTEND_URL`, `VITE_API_URL`
- Frontend deploy: Vercel/Netlify (CDN estático); Backend deploy: Railway (com MySQL add-on)
- CI/CD via GitHub Actions: `ci-frontend.yml` (build+lint+test) e `ci-backend.yml` (build+test Maven)
- Springdoc OpenAPI para documentação automática da API
- Logback com output JSON estruturado para Railway

### UX Design Requirements

UX-DR1: Implementar Design System completo — tokens de cor (brand teal `#0D9488`, 6 cores de status, neutros light/dark) como variáveis CSS via Tailwind config; suporte a `prefers-color-scheme` (system default)
UX-DR2: Criar componente `StatusBadge` — combina obrigatoriamente cor de fundo + borda lateral colorida + ícone Lucide + rótulo de texto (regra inviolável WCAG + daltonismo)
UX-DR3: Implementar `AgendamentoCard` (card médio) — exibe horário, nome do paciente, tipo+profissional, botão de ação primária contextual (label muda por status), menu ···
UX-DR4: Implementar `BottomTabBar` com abas por perfil: Staff (Agenda/Buscar/Clientes/Menu), Profissional (Agenda/Menu), Admin (Agenda/Configurações/Menu)
UX-DR5: Implementar FAB flutuante para Staff e Profissional; para Profissional campo "Profissional" pré-preenchido e bloqueado no formulário
UX-DR6: Implementar `NovoAgendamentoSheet` — Bottom Sheet no mobile, drawer lateral no desktop (380px); agenda visível ao fundo; validação inline por campo + toast de sucesso
UX-DR7: Implementar `DialogDoubleBooking` — dialog bloqueante centralizado para confirmação de segundo agendamento com sobreposição (FR-024)
UX-DR8: Implementar navegação da agenda: toggle segmentado Dia/Semana + swipe horizontal para avançar/retroceder dias/semanas; chips de filtro de profissional roláveis
UX-DR9: Implementar `BuscarDisponivelView` — lista agrupada por data + chips de filtro (profissional + tipo de atendimento); toque em vaga abre Bottom Sheet pré-preenchido
UX-DR10: Implementar `AutoatendimentoPage` (rota pública `/cancelar/:token`) — 4 estados: Pendente (2 botões: confirmar primário/cancelar secundário), Já Confirmado (botão confirmar desabilitado "Presença já confirmada ✓"), Pós-Confirmação, Pós-Cancelamento; 2 dialogs de confirmação separados
UX-DR11: Implementar skeleton screens (shimmer) para carregamento, estados vazios por tela, banner offline PWA, toast de atualização silenciosa de polling
UX-DR12: Implementar badge "⭐ Primeiro do dia" no card do profissional (FR-018) e badge "⚠️ Chegou antes do horário" para alerta de reordenação (FR-020)
UX-DR13: Wordmark "DrAgenda" em DM Sans (light + medium); fonte de interface Inter; tipografia com escala completa documentada no DESIGN.md

---

## Epic 1: Fundação Técnica

Infraestrutura do monorepo, CI/CD, banco de dados versionado com Flyway, deploy inicial Vercel + Railway, seed seguro do Admin Sistema e página pública de política de privacidade.

### Story 1.1: Monorepo com CI/CD e Deploy Base

Como desenvolvedor,
Quero um monorepo configurado com pipeline de CI/CD e deploy automático,
Para que cada push entregue código validado em produção sem intervenção manual.

**Acceptance Criteria:**

**Given** o repositório Git existe com estrutura `/frontend` (Vite+React+TS) e `/backend` (Spring Boot 3.x+Java 21+Maven)
**When** um push é feito na branch principal
**Then** o GitHub Actions executa `ci-frontend.yml` (build + lint + vitest) e `ci-backend.yml` (build + testes Maven) em paralelo
**And** o frontend é deployed automaticamente no Vercel via integração nativa
**And** o backend é deployed automaticamente no Railway com MySQL add-on

**Given** o ambiente de banco de dados está configurado no Railway
**When** o MySQL add-on está ativo
**Then** o backup automático diário está habilitado no Railway com retenção mínima de 30 dias (NFR-008)
**And** o procedimento de verificação de backup é documentado no `README.md` de operações (como fazer restore manual a partir de um snapshot do Railway)

**Given** o backend está em execução
**When** qualquer endpoint é acessado
**Then** a comunicação ocorre exclusivamente via HTTPS (FR-036)
**And** CORS aceita apenas origens definidas em `APP_FRONTEND_URL`

**Given** as variáveis de ambiente canônicas são necessárias
**When** o sistema inicializa
**Then** as seguintes variáveis estão disponíveis e documentadas em `.env.example`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `ENCRYPTION_KEY`, `APP_FRONTEND_URL`, `VITE_API_URL`
**And** a aplicação falha com mensagem clara se qualquer variável obrigatória estiver ausente

**Given** o backend está rodando
**When** `GET /v3/api-docs` é acessado
**Then** o Springdoc OpenAPI retorna a especificação da API em JSON
**And** todos os logs do backend são emitidos em formato JSON estruturado via Logback (compatível com Railway log drain)

---

### Story 1.2: Banco de Dados Versionado com Flyway e Seed do Admin Sistema

Como sistema,
Quero um banco de dados versionado com Flyway e um Admin Sistema seed seguro,
Para que o schema evolua de forma auditável e o sistema seja inicializável com segurança em produção.

**Acceptance Criteria:**

**Given** o backend inicia pela primeira vez contra um banco MySQL vazio
**When** o Flyway executa
**Then** a migration `V1__create_empresa.sql` é aplicada criando a tabela `empresa` com colunas: `id` (BIGINT PK), `nome` (VARCHAR 255 NOT NULL), `ativo` (BOOLEAN DEFAULT TRUE), `created_at` (DATETIME NOT NULL)
**And** `ddl-auto` está configurado como `validate` (nunca `update` ou `create`)
**And** a tabela `flyway_schema_history` registra a migration aplicada

**Given** a migration V1 foi aplicada e a variável `ADMIN_SEED_PASSWORD` está definida no ambiente
**When** o contexto Spring inicializa
**Then** o usuário seed `admin@agenda.com` com perfil `ADMIN_SISTEMA` é criado se ainda não existir (idempotente)
**And** a senha é armazenada com hash BCrypt (nunca em texto plano)
**And** o campo `senha_temporaria = true` está marcado, forçando troca no primeiro login (FR-044)

**Given** `ADMIN_SEED_PASSWORD` não está definida em produção
**When** o sistema tenta inicializar
**Then** a aplicação falha com erro explícito: "ADMIN_SEED_PASSWORD não configurada — deploy bloqueado por segurança" (NFR-007)

**Given** uma migration nova é adicionada
**When** o Flyway detecta checksum diferente de uma migration já aplicada
**Then** o sistema falha na inicialização com erro de validação do Flyway (proteção contra edição retroativa)

**Given** dois desenvolvedores trabalham em branches paralelas e ambos precisam criar uma nova migration
**When** uma nova migration é planejada
**Then** a convenção de nomenclatura usa timestamp como prefixo: `V{yyyyMMddHHmm}__{descricao}.sql` (ex: `V202606041430__create_x.sql`) para eliminar conflito de numeração sequencial entre branches
**And** este padrão é documentado no `README.md` do backend como regra obrigatória antes de criar qualquer migration (R6 — prevenção de conflito Flyway em desenvolvimento paralelo)

---

### Story 1.3: Fundação do Frontend — Design Tokens, Roteamento e Layout Base

Como usuário do DrAgenda,
Quero uma interface com identidade visual consistente e navegação estruturada,
Para que o produto seja reconhecível e navegável desde o primeiro acesso.

**Acceptance Criteria:**

**Given** o frontend é carregado em qualquer dispositivo
**When** os estilos são aplicados
**Then** o Tailwind CSS usa os tokens customizados: `brand-500: #0D9488`, `brand-50` a `brand-900` (escala completa), 6 cores de status (pendente amarelo, confirmado azul, presente roxo, concluído verde, cancelado cinza escuro, noshow vermelho) e neutros light/dark
**And** a fonte Inter é carregada para interface e DM Sans para o wordmark "DrAgenda" (UX-DR13)
**And** `prefers-color-scheme` é respeitado como padrão do sistema

**Given** o usuário acessa qualquer rota autenticada sem estar logado
**When** o React Router avalia a rota
**Then** o usuário é redirecionado para `/login`

**Given** o usuário está autenticado
**When** navega entre as rotas `/agenda`, `/buscar`, `/clientes`, `/configuracoes`, `/menu`
**Then** o React Router renderiza o componente correto sem recarregamento de página
**And** o TanStack Query está configurado com `staleTime: 30000` e `retry: 2` como padrão global

**Given** o frontend está em build de produção
**When** `npm run build` é executado
**Then** o build completa sem erros de TypeScript e sem warnings de lint (ESLint configurado)
**And** o `manifest.json` PWA está presente com nome "DrAgenda", ícone e `display: standalone`

---

### Story 1.4: Página Pública de Política de Privacidade

Como paciente ou visitante,
Quero acessar a política de privacidade e o termo de consentimento LGPD sem precisar fazer login,
Para que eu possa conhecer como meus dados serão usados antes de consentir.

**Acceptance Criteria:**

**Given** qualquer visitante acessa `/politica`
**When** a página carrega
**Then** o conteúdo da política de privacidade e do termo de consentimento LGPD é exibido sem exigir autenticação
**And** a página exibe o wordmark "DrAgenda" no topo
**And** a página é responsiva (funcional a partir de 320px de largura)

**Given** o usuário está na tela de login (`/login`)
**When** a tela é renderizada
**Then** um link visível para `/politica` está presente no rodapé da tela de login (FR-054)

**Given** o usuário está na página de autoatendimento público (`/cancelar/:token`)
**When** a tela é renderizada
**Then** um link visível para `/politica` está presente na página (FR-054)

**Given** a rota `/politica` é acessada
**When** o servidor processa a requisição
**Then** a rota não requer token JWT (bypass de autenticação)
**And** a página retorna HTTP 200 mesmo sem cookies ou headers de autorização

---

### Epic 2: Autenticação e Gestão de Usuários

Usuários conseguem fazer login, trocar senha e fazer logout. Admin Sistema cadastra clínicas e cria Admin Empresa. Admin Empresa cadastra Staff e Profissionais. Troca obrigatória de senha temporária no primeiro login.

### Story 2.1: Login, JWT Multi-Tenant e Logout

Como usuário do DrAgenda,
Quero fazer login com email e senha e receber um token que identifica minha empresa e meu perfil,
Para que eu possa acessar somente os dados da minha clínica com as permissões do meu perfil.

**Acceptance Criteria:**

**Given** a migration `V2__create_usuarios.sql` foi aplicada criando a tabela `usuarios` com colunas: `id` (BIGINT PK), `nome` (VARCHAR 255 NOT NULL), `email` (VARCHAR 255 UNIQUE NOT NULL), `senha_hash` (VARCHAR 255 NOT NULL), `perfil` (ENUM: ADMIN_SISTEMA, ADMIN_EMPRESA, STAFF, PROFISSIONAL NOT NULL), `empresa_id` (BIGINT FK nullable — NULL para ADMIN_SISTEMA), `ativo` (BOOLEAN DEFAULT TRUE), `senha_temporaria` (BOOLEAN DEFAULT FALSE), `created_at` (DATETIME NOT NULL)
**When** o sistema inicializa
**Then** a tabela está criada com índice `idx_usuarios_email` e `idx_usuarios_empresa_id`

**Given** um usuário ativo envia `POST /api/v1/auth/login` com email e senha válidos
**When** o backend processa a requisição
**Then** retorna HTTP 200 com `{ "token": "...", "perfil": "STAFF", "nome": "Maria" }`
**And** o JWT contém claims: `sub` (userId), `empresaId` (Long, null para ADMIN_SISTEMA), `perfil` (String), `exp` configurável via `JWT_EXPIRATION_MS`
**And** o token é assinado com a chave `JWT_SECRET` via JJWT

**Given** um usuário envia credenciais inválidas (email não existe ou senha errada)
**When** o backend processa a requisição
**Then** retorna HTTP 401 com ProblemDetail `"Credenciais inválidas"`
**And** não é possível distinguir se o email existe (mensagem genérica — proteção contra user enumeration)

**Given** um usuário autenticado com `senha_temporaria = true` acessa qualquer rota protegida exceto `POST /api/v1/auth/trocar-senha`
**When** o middleware de autenticação avalia o token
**Then** retorna HTTP 403 com ProblemDetail `"Troca de senha obrigatória antes de prosseguir"` (FR-044)

**Given** o usuário está logado e toca em "Sair" no menu de perfil
**When** o frontend processa o logout (FR-047)
**Then** o JWT é removido do `localStorage` e o usuário é redirecionado para `/login`
**And** nenhuma chamada de invalidação é feita ao backend (JWT stateless no MVP)

---

### Story 2.2: Middleware Multi-Tenant, Autorização por Perfil e Navegação

Como sistema,
Quero garantir que cada usuário acesse apenas os dados da sua empresa e apenas as funcionalidades do seu perfil,
Para que não haja vazamento de dados entre clínicas nem acesso não autorizado a funcionalidades.

**Acceptance Criteria:**

**Given** qualquer endpoint autenticado recebe uma requisição
**When** o `JwtAuthenticationFilter` processa o token
**Then** o `empresaId` é extraído do JWT e injetado no contexto de segurança do Spring
**And** nenhum endpoint autenticado aceita `empresaId` como parâmetro da request (NFR-004)
**And** todo `Repository` de entidade multi-tenant filtra obrigatoriamente por `empresaId` extraído do contexto

**Given** um usuário STAFF tenta acessar `GET /api/v1/configuracoes`
**When** o `@PreAuthorize` avalia a permissão
**Then** retorna HTTP 403 (apenas ADMIN_EMPRESA acessa configurações)

**Given** um usuário PROFISSIONAL tenta acessar `GET /api/v1/clientes`
**When** o `@PreAuthorize` avalia a permissão
**Then** retorna HTTP 403 (apenas STAFF acessa clientes)

**Given** um usuário ADMIN_EMPRESA da empresa A tenta buscar dados da empresa B
**When** qualquer query ao banco é executada
**Then** o filtro por `empresaId` impede o retorno de dados da empresa B — zero cross-tenant

**Given** o isolamento multi-tenant está implementado
**When** testes de integração desta story são executados
**Then** deve existir um teste de integração explícito que: (1) cria registros em duas empresas distintas (empresa A e empresa B), (2) autentica como usuário da empresa A, (3) chama `GET /api/v1/profissionais` e `GET /api/v1/staff` e verifica que a resposta contém apenas registros da empresa A, (4) afirma que nenhum ID pertencente à empresa B aparece em nenhum item do response — este teste é critério de aceite obrigatório desta story (R1 — isolamento multi-tenant testado end-to-end)

**Given** o usuário faz login com perfil STAFF
**When** o frontend renderiza o layout principal
**Then** a `BottomTabBar` exibe: `📅 Agenda`, `🔍 Buscar`, `👥 Clientes`, `··· Menu` (UX-DR4)

**Given** o usuário faz login com perfil PROFISSIONAL
**When** o frontend renderiza o layout principal
**Then** a `BottomTabBar` exibe: `📅 Agenda`, `··· Menu`

**Given** o usuário faz login com perfil ADMIN_EMPRESA
**When** o frontend renderiza o layout principal
**Then** a `BottomTabBar` exibe: `📅 Agenda`, `⚙️ Configurações`, `··· Menu`

**Given** o viewport é ≥ 768px (desktop)
**When** o layout é renderizado
**Then** exibe sidebar fixa 240px com itens condicionais por perfil (Buscar e Clientes para Staff, Configurações para Admin Empresa) e FAB no canto inferior direito da área de conteúdo (Staff e Profissional)

---

### Story 2.3: Admin Sistema — Gestão de Clínicas e Criação de Admin Empresa

Como Admin Sistema,
Quero cadastrar clínicas e criar o primeiro Admin Empresa automaticamente,
Para que cada nova clínica tenha acesso imediato ao sistema com isolamento total de dados.

**Acceptance Criteria:**

**Given** o Admin Sistema está autenticado e envia `POST /api/v1/admin/empresas` com nome da clínica + dados do Admin Empresa (nome, email, senha temporária)
**When** o backend processa a criação
**Then** a empresa é criada com `ativo = true`
**And** o usuário Admin Empresa é criado simultaneamente com `perfil = ADMIN_EMPRESA`, `empresa_id` vinculado, `senha_temporaria = true` (FR-056)
**And** a senha temporária é exibida uma única vez no response e não é armazenada em texto plano
**And** retorna HTTP 201 com `Location: /api/v1/admin/empresas/{id}`

**Given** o Admin Sistema acessa `GET /api/v1/admin/empresas`
**When** a listagem é retornada
**Then** exibe todas as clínicas com paginação server-side (`page`, `size`, `totalElements`, `totalPages`, page size padrão 20)
**And** cada item exibe: id, nome, ativo, data de criação

**Given** o Admin Sistema acessa uma clínica existente via `PUT /api/v1/admin/empresas/{id}`
**When** edita o nome da clínica
**Then** os dados são atualizados e retorna HTTP 200

**Given** o Admin Sistema inativa uma clínica via `PATCH /api/v1/admin/empresas/{id}/inativar`
**When** a operação é confirmada
**Then** `ativo = false` na empresa (FR-051)
**And** todos os usuários da clínica recebem HTTP 401 na próxima requisição autenticada (token válido mas empresa inativa verificada no filtro)

**Given** o Admin Sistema reativa uma clínica via `PATCH /api/v1/admin/empresas/{id}/reativar`
**When** a operação é realizada
**Then** `ativo = true` e os usuários da clínica voltam a ter acesso normalmente

**Given** qualquer usuário não-ADMIN_SISTEMA tenta acessar `/api/v1/admin/**`
**When** o `@PreAuthorize` avalia
**Then** retorna HTTP 403

---

### Story 2.4: Admin Empresa — Cadastro e Gestão de Profissionais

Como Admin Empresa,
Quero cadastrar e gerenciar os profissionais de saúde da minha clínica,
Para que eles possam fazer login no sistema e ter seus agendamentos gerenciados.

**Acceptance Criteria:**

**Given** a migration `V3__create_profissionais.sql` foi aplicada criando a tabela `profissionais` com colunas: `id` (BIGINT PK), `usuario_id` (BIGINT FK UNIQUE → `usuarios.id`), `empresa_id` (BIGINT FK NOT NULL), `especialidade` (VARCHAR 255), `ativo` (BOOLEAN DEFAULT TRUE), `created_at` (DATETIME NOT NULL) e índice `idx_profissionais_empresa_id`
**When** o sistema inicializa
**Then** a tabela está criada e vinculada à tabela `usuarios`

**Given** o Admin Empresa envia `POST /api/v1/profissionais` com nome, email, especialidade e senha temporária
**When** o backend processa
**Then** cria o usuário com `perfil = PROFISSIONAL` e `senha_temporaria = true` na tabela `usuarios` (FR-007)
**And** cria o registro em `profissionais` vinculado ao usuário
**And** a senha temporária deve ter ≥ 8 caracteres com maiúsculas, minúsculas e caractere especial — retorna HTTP 400 se inválida
**And** retorna HTTP 201

**Given** o Admin Empresa acessa `GET /api/v1/profissionais`
**When** a listagem é retornada
**Then** exibe apenas profissionais da sua empresa (filtro por `empresaId` do JWT)
**And** suporta paginação server-side (FR-046)

**Given** o Admin Empresa inativa um profissional via `PATCH /api/v1/profissionais/{id}/inativar`
**When** a operação é realizada
**Then** `ativo = false` no profissional (FR-009)
**And** o profissional não consegue mais fazer login (verificado no filtro de autenticação)
**And** o sinal visual nos agendamentos futuros ("Profissional Inativo") será implementado no Epic 4 quando a tabela de agendamentos existir

**Given** o Admin Empresa reativa um profissional via `PATCH /api/v1/profissionais/{id}/reativar`
**When** a operação é realizada
**Then** `ativo = true` e o profissional volta a poder fazer login

**Given** o Admin Empresa edita dados de um profissional via `PUT /api/v1/profissionais/{id}`
**When** a operação é realizada
**Then** nome e especialidade são atualizados; email não pode ser alterado pelo Admin Empresa (imutável após criação)

---

### Story 2.5: Admin Empresa — Cadastro e Gestão de Usuários Staff

Como Admin Empresa,
Quero cadastrar e gerenciar os usuários de recepção (Staff) da minha clínica,
Para que eles possam operar o sistema de agendamento.

**Acceptance Criteria:**

**Given** o Admin Empresa envia `POST /api/v1/staff` com nome, email e senha temporária
**When** o backend processa
**Then** cria o usuário com `perfil = STAFF`, `empresa_id` do JWT, `senha_temporaria = true` (FR-010)
**And** a senha temporária deve ter ≥ 8 caracteres com maiúsculas, minúsculas e caractere especial — retorna HTTP 400 se inválida (FR-007)
**And** retorna HTTP 201

**Given** o Admin Empresa acessa `GET /api/v1/staff`
**When** a listagem é retornada
**Then** exibe apenas usuários Staff da sua empresa com paginação server-side

**Given** o Admin Empresa inativa um usuário Staff via `PATCH /api/v1/staff/{id}/inativar`
**When** a operação é realizada
**Then** `ativo = false` e o Staff perde acesso imediatamente na próxima requisição

**Given** o Admin Empresa reativa um Staff via `PATCH /api/v1/staff/{id}/reativar`
**When** a operação é realizada
**Then** `ativo = true` e o Staff volta a ter acesso

---

### Story 2.6: Troca de Senha Obrigatória, Redefinição e Alteração de Senha

Como usuário do DrAgenda,
Quero poder trocar minha senha temporária, alterar minha senha atual e ter minha senha redefinida pelo Admin Empresa,
Para que minha conta seja segura e eu tenha controle sobre meu acesso.

**Acceptance Criteria:**

**Given** um usuário com `senha_temporaria = true` acessa o sistema pela primeira vez
**When** o frontend detecta o campo no payload JWT ou response de login
**Then** redireciona obrigatoriamente para `/trocar-senha` bloqueando todas as outras rotas (FR-044)
**And** o formulário exige: senha atual, nova senha (≥ 8 chars, maiúsculas + minúsculas + especial) e confirmação
**And** após troca bem-sucedida via `POST /api/v1/auth/trocar-senha`, `senha_temporaria = false` e o usuário é redirecionado para `/agenda`

**Given** um usuário autenticado acessa `/menu` → "Alterar senha"
**When** preenche senha atual correta + nova senha válida e confirma via `PUT /api/v1/auth/minha-senha` (FR-048)
**Then** a senha é atualizada com novo hash BCrypt
**And** retorna HTTP 204

**Given** a nova senha não atende os critérios de complexidade (< 8 chars, sem maiúscula, sem minúscula ou sem especial)
**When** o formulário é submetido
**Then** o backend retorna HTTP 400 com mensagem clara de qual critério falhou
**And** o frontend exibe erro inline no campo de nova senha

**Given** o Admin Empresa acessa o perfil de um usuário da sua empresa e solicita redefinição via `POST /api/v1/usuarios/{id}/redefinir-senha` com nova senha temporária (FR-043)
**When** o backend processa
**Then** a senha é atualizada com hash BCrypt e `senha_temporaria = true`
**And** a nova senha temporária é exibida uma única vez no response
**And** o usuário será forçado a trocar no próximo login

**Given** o Admin Empresa tenta redefinir senha de usuário de outra empresa
**When** o backend verifica o `empresaId` do JWT contra o `empresa_id` do usuário alvo
**Then** retorna HTTP 403

### Epic 3: Cadastros Base da Clínica

Admin Empresa configura a clínica: tipos de atendimento, disponibilidade dos profissionais, fuso horário e dados básicos. Staff cadastra e gerencia clientes com consentimento LGPD explícito.

---

### Story 3.1: Componentes Base de UI e Design System

Como usuário do DrAgenda,
Quero que toda a interface use cores, ícones e rótulos consistentes para indicar o estado dos agendamentos,
Para que eu reconheça o status de qualquer agendamento em qualquer tela sem depender apenas de cor.

**Acceptance Criteria:**

**Given** o componente `StatusBadge` é implementado em `src/components/StatusBadge.tsx` (UX-DR2)
**When** renderizado com qualquer status de agendamento
**Then** exibe obrigatoriamente: cor de fundo do token correspondente + borda lateral colorida 4px + ícone Lucide + rótulo de texto
**And** os 6 status mapeiam: `PENDENTE` (amarelo `#D97706` + `clock`) · `CONFIRMADO` (azul `#2563EB` + `check`) · `PRESENTE` (roxo `#7C3AED` + `user-check`) · `CONCLUIDO` (verde `#16A34A` + `check-circle`) · `CANCELADO` (cinza `#6B7280` + `x-circle`) · `NOSHOW` (vermelho `#DC2626` + `alert-circle`)
**And** nenhum status é diferenciado apenas por cor (WCAG 2.1 AA + daltonismo — regra inviolável)

**Given** qualquer tela carrega dados da API
**When** a requisição está em andamento
**Then** skeleton screens (shimmer) substituem os elementos de conteúdo: 3 cards placeholder na agenda, linhas placeholder em listas

**Given** o componente `Toast` é implementado
**When** uma ação bem-sucedida ou com erro ocorre
**Then** o toast aparece no topo central com: ícone + texto descritivo + cor correspondente ao tipo (sucesso verde, erro vermelho, aviso âmbar, info azul)
**And** durações: sucesso 3s, info 3s, aviso 4s, erro 5s

---

### Story 3.2: Configurações da Clínica — Dados Básicos e Fuso Horário

Como Admin Empresa,
Quero configurar os dados básicos e o fuso horário da minha clínica,
Para que o sistema exiba horários corretos para todos os usuários e o sistema operacional da clínica esteja identificado corretamente.

**Acceptance Criteria:**

**Given** a migration `V4__add_configuracoes_empresa.sql` foi aplicada adicionando à tabela `empresas`: `telefone` (VARCHAR 20), `endereco` (VARCHAR 500), `fuso_horario` (VARCHAR 50 DEFAULT 'America/Sao_Paulo'), `updated_at` (DATETIME)
**When** o sistema inicializa
**Then** as colunas estão presentes na tabela `empresas`

**Given** o Admin Empresa acessa `/configuracoes/clinica`
**When** a tela carrega
**Then** exibe formulário com campos: Nome da Clínica, Telefone de Contato, Endereço e Fuso Horário (seletor com fusos brasileiros: America/Sao_Paulo, America/Manaus, America/Belem, America/Fortaleza, America/Noronha) (FR-058)

**Given** o Admin Empresa submete `PUT /api/v1/configuracoes/clinica` com dados válidos
**When** o backend persiste
**Then** retorna HTTP 200 com os dados atualizados
**And** o `fuso_horario` é armazenado como string IANA (ex: `"America/Sao_Paulo"`) (FR-041)
**And** o toast "Configurações salvas ✓" é exibido no frontend

**Given** o fuso horário está configurado como `America/Manaus` (UTC-4)
**When** qualquer horário é exibido na interface
**Then** o frontend converte o UTC recebido da API para o fuso configurado antes de exibir
**And** todos os horários são armazenados internamente em UTC no banco de dados

**Given** o Admin Empresa tenta acessar as configurações de outra empresa
**When** o backend verifica o `empresaId` do JWT
**Then** retorna HTTP 403

---

### Story 3.3: Tipos de Atendimento — Cadastro, Edição e Inativação

Como Admin Empresa,
Quero cadastrar e gerenciar os tipos de atendimento com suas durações,
Para que o sistema calcule automaticamente o tempo de cada consulta ao criar agendamentos.

**Acceptance Criteria:**

**Given** a migration `V5__create_tipos_atendimento.sql` foi aplicada criando a tabela `tipos_atendimento` com colunas: `id` (BIGINT PK), `empresa_id` (BIGINT FK NOT NULL), `nome` (VARCHAR 255 NOT NULL), `duracao_minutos` (INT NOT NULL), `ativo` (BOOLEAN DEFAULT TRUE), `created_at` (DATETIME NOT NULL) e índice `idx_tipos_atendimento_empresa_id`
**When** o sistema inicializa
**Then** a tabela está criada

**Given** o Admin Empresa envia `POST /api/v1/tipos-atendimento` com nome e duração em minutos
**When** o backend persiste
**Then** o tipo é criado com `ativo = true` e `empresa_id` do JWT (FR-012)
**And** `duracao_minutos` deve ser inteiro positivo entre 5 e 480 — retorna HTTP 400 se inválido
**And** retorna HTTP 201

**Given** o Admin Empresa acessa `GET /api/v1/tipos-atendimento`
**When** a listagem é retornada
**Then** exibe apenas tipos da sua empresa, incluindo ativos e inativos
**And** suporta paginação server-side

**Given** o Admin Empresa inativa um tipo via `PATCH /api/v1/tipos-atendimento/{id}/inativar`
**When** a operação é realizada
**Then** `ativo = false` (FR-053)
**And** `GET /api/v1/tipos-atendimento/ativos` não retorna esse tipo (usado no formulário de agendamento)
**And** agendamentos existentes com esse tipo não são afetados

**Given** o Admin Empresa edita um tipo via `PUT /api/v1/tipos-atendimento/{id}`
**When** nome ou duração são alterados
**Then** os dados são atualizados e retorna HTTP 200
**And** agendamentos já criados com esse tipo NÃO têm sua duração recalculada retroativamente

---

### Story 3.4: Disponibilidade dos Profissionais por Dia da Semana

Como Admin Empresa,
Quero configurar os dias e horários de atendimento de cada profissional,
Para que o sistema valide automaticamente a disponibilidade ao criar agendamentos.

**Acceptance Criteria:**

**Given** a migration `V6__create_disponibilidades.sql` foi aplicada criando a tabela `disponibilidades` com colunas: `id` (BIGINT PK), `profissional_id` (BIGINT FK NOT NULL → `profissionais.id`), `empresa_id` (BIGINT FK NOT NULL), `dia_semana` (ENUM: SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA, SABADO, DOMINGO NOT NULL), `horario_inicio` (TIME NOT NULL), `horario_fim` (TIME NOT NULL), `inicio_intervalo` (TIME nullable), `fim_intervalo` (TIME nullable) e índice `idx_disponibilidades_profissional_id`
**When** o sistema inicializa
**Then** a tabela está criada com constraint UNIQUE em `(profissional_id, dia_semana)`

**Given** o Admin Empresa acessa `GET /api/v1/profissionais/{id}/disponibilidade`
**When** a tela `/configuracoes/disponibilidade` carrega
**Then** exibe grade semanal com os dias configurados para o profissional selecionado

**Given** o Admin Empresa envia `PUT /api/v1/profissionais/{id}/disponibilidade` com array de slots por dia da semana
**When** o backend persiste
**Then** substitui toda a disponibilidade do profissional pelo novo array (upsert completo) (FR-013)
**And** `horario_fim` deve ser posterior a `horario_inicio` — retorna HTTP 400 se inválido
**And** se `inicio_intervalo` e `fim_intervalo` são fornecidos, ambos devem estar dentro da faixa `horario_inicio`–`horario_fim`

**Given** um profissional não tem disponibilidade cadastrada para um dia
**When** o sistema tenta criar um agendamento nesse dia (Epic 4)
**Then** o profissional é tratado como indisponível e retorna HTTP 409 `"Profissional sem disponibilidade cadastrada"` (FR-023)

**Given** o Admin Empresa remove a disponibilidade de um dia enviando o slot sem esse dia no array
**When** o backend persiste
**Then** a disponibilidade desse dia é excluída

---

### Story 3.5: Cadastro de Clientes com Consentimento LGPD e Criptografia

Como Staff,
Quero cadastrar clientes com coleta obrigatória do termo de consentimento LGPD e ter seus dados sensíveis protegidos,
Para que a clínica esteja em conformidade com a LGPD desde o primeiro cadastro.

**Acceptance Criteria:**

**Given** a migration `V7__create_clientes.sql` foi aplicada criando a tabela `clientes` com colunas: `id` (BIGINT PK), `empresa_id` (BIGINT FK NOT NULL), `nome_enc` (TEXT NOT NULL — AES-256), `telefone_enc` (TEXT nullable — AES-256), `data_nascimento_enc` (TEXT nullable — AES-256), `consentimento_lgpd` (BOOLEAN DEFAULT FALSE NOT NULL), `consentimento_timestamp` (DATETIME nullable), `consentimento_versao_termo` (VARCHAR 50 nullable), `consentimento_usuario_id` (BIGINT FK nullable → `usuarios.id`), `anonimizado` (BOOLEAN DEFAULT FALSE), `created_at` (DATETIME NOT NULL) e índices `idx_clientes_empresa_id`
**When** o sistema inicializa
**Then** a tabela está criada — sem colunas de diagnóstico, procedimento clínico ou saúde (FR-040)

**Given** o `AesAttributeConverter` está implementado como `JPA AttributeConverter<String, String>`
**When** qualquer entidade `Cliente` é persistida
**Then** `nome_enc`, `telefone_enc` e `data_nascimento_enc` são criptografados com AES-256 usando `ENCRYPTION_KEY` antes de salvar (FR-035)
**And** na leitura, os campos são descriptografados automaticamente pelo converter
**And** buscas por nome/telefone no banco usam descriptografia em memória (não em SQL)
**And** `AesAttributeConverter` é o componente compartilhado único para criptografia AES-256 em todo o projeto — nenhuma outra classe ou story reimplementa AES diretamente; todas as entidades com campos sensíveis (presentes ou futuros) devem reutilizar este converter via JPA `@Convert`, garantindo comportamento de criptografia uniforme em Epic 4 (exibição em cards/detalhes), Epic 5 (relatório de no-show por paciente) e Epic 6 (anonimização) (R3 — consistência AES-256 cross-story)

**Given** o Staff acessa o formulário de cadastro de cliente (FR-011)
**When** a tela renderiza
**Then** exibe campos: Nome (obrigatório), Telefone, Data de Nascimento e checkbox obrigatório "Li e aceito o Termo de Consentimento LGPD" com link para `/politica`

**Given** o Staff submete `POST /api/v1/clientes` com `consentimento_lgpd: true`
**When** o backend persiste
**Then** `consentimento_lgpd = true`, `consentimento_timestamp` = instante atual em UTC, `consentimento_versao_termo` = versão corrente do termo (constante configurável, ex: `"v1.0"`), `consentimento_usuario_id` = id do usuário autenticado (FR-037)
**And** retorna HTTP 201

**Given** o Staff tenta submeter o formulário com `consentimento_lgpd: false` (checkbox desmarcado)
**When** o backend valida
**Then** retorna HTTP 400 com mensagem `"Consentimento LGPD é obrigatório para cadastrar cliente"`
**And** o frontend bloqueia a submissão se o checkbox não estiver marcado (validação dupla: frontend + backend)

---

### Story 3.6: Listagem, Busca e Edição de Clientes

Como Staff,
Quero listar, buscar e editar clientes cadastrados,
Para que eu possa encontrar rapidamente um cliente ao criar agendamentos e manter os dados atualizados.

**Acceptance Criteria:**

**Given** o Staff acessa `GET /api/v1/clientes` com parâmetro opcional `?busca=João`
**When** o backend processa
**Then** retorna clientes da empresa com paginação server-side (page size padrão 20) (FR-050, FR-046)
**And** a busca por nome descriptografa todos os registros da empresa em memória e filtra pelo termo
**And** a busca por telefone funciona da mesma forma

**Given** o Staff acessa a tela `/clientes`
**When** a lista renderiza
**Then** cada item exibe: nome (descriptografado), telefone e data de nascimento formatada
**And** campo de busca no topo com debounce de 300ms que dispara `GET /api/v1/clientes?busca=...`

**Given** o Staff toca em um cliente e acessa a tela de edição via `PUT /api/v1/clientes/{id}`
**When** submete com dados válidos
**Then** nome, telefone e data de nascimento são atualizados com re-criptografia dos campos (FR-050)
**And** retorna HTTP 200
**And** `consentimento_lgpd` e dados de consentimento NÃO podem ser alterados após o cadastro (imutáveis)

**Given** um Profissional ou Admin Empresa tenta acessar `GET /api/v1/clientes`
**When** o `@PreAuthorize` avalia
**Then** retorna HTTP 403 (apenas Staff acessa a lista de clientes)

**Given** o campo "Cliente" no formulário de agendamento (Epic 4) é preenchido
**When** o Staff digita 3+ caracteres
**Then** a busca `GET /api/v1/clientes?busca=...` é disparada e os resultados populam o autocomplete do campo

### Epic 4: Agenda e Agendamentos

Usuários visualizam a agenda, criam agendamentos, fazem check-in, atualizam status e veem detalhes completos. Inclui double-booking intencional, NoShow automático, busca de próximo disponível e link de autoatendimento público.

> **Pré-requisito obrigatório (R7):** Epics 1, 2 e 3 devem estar concluídos antes de iniciar este Epic. As stories 4.x dependem de: tabela `agendamentos` referenciando `clientes` (Epic 3), `profissionais` (Epic 2) e `tipos_atendimento` (Epic 3); isolamento multi-tenant testado end-to-end (Epic 2 — Story 2.2); `AesAttributeConverter` compartilhado disponível (Epic 3 — Story 3.5). Iniciar Epic 4 antes desses gates resultará em integração impossível.

---

### Story 4.1: Dashboard de Agenda — Estrutura de Dados, Cards e Navegação

Como usuário do DrAgenda,
Quero visualizar a agenda do dia e da semana com cards coloridos por status e navegar entre datas,
Para que eu saiba imediatamente o estado de cada agendamento sem ambiguidade.

**Acceptance Criteria:**

**Given** a migration `V8__create_agendamentos.sql` foi aplicada criando a tabela `agendamentos` com colunas: `id` (BIGINT PK), `empresa_id` (BIGINT FK NOT NULL), `cliente_id` (BIGINT FK NOT NULL → `clientes.id`), `profissional_id` (BIGINT FK NOT NULL → `profissionais.id`), `tipo_atendimento_id` (BIGINT FK NOT NULL → `tipos_atendimento.id`), `horario_inicio` (DATETIME NOT NULL — UTC), `horario_fim` (DATETIME NOT NULL — UTC), `status` (ENUM: PENDENTE, CONFIRMADO, PRESENTE, CONCLUIDO, CANCELADO, NOSHOW NOT NULL DEFAULT PENDENTE), `public_token` (VARCHAR 36 UNIQUE nullable — UUID v4), `created_at` (DATETIME NOT NULL) e índices `idx_agendamentos_empresa_id`, `idx_agendamentos_profissional_id`, `idx_agendamentos_horario_inicio`
**When** o sistema inicializa
**Then** a tabela está criada com todos os índices

**Given** o usuário autenticado acessa `GET /api/v1/agendamentos?data=2026-06-04` (com filtro opcional `?profissional_id=`)
**When** o backend processa
**Then** retorna agendamentos da empresa filtrados pela data (FR-015), com dados: id, horarioInicio, horarioFim, status, cliente (nome descriptografado, telefone), profissional (nome), tipoAtendimento (nome)
**And** Profissional recebe apenas seus próprios agendamentos (filtro por `profissionalId` do JWT) (FR-005)
**And** Staff e Admin recebem todos da empresa

**Given** o frontend renderiza a lista de agendamentos
**When** os cards são exibidos
**Then** o componente `AgendamentoCard` (UX-DR3) exibe: horário início–fim, nome do cliente, tipo + profissional, borda lateral colorida + badge `StatusBadge` por status (FR-016)
**And** no mobile (≤767px): lista vertical ordenada por horário (FR-021)
**And** no desktop (≥768px): grade horária (FR-021)

**Given** o usuário está na tela de agenda
**When** interage com a navegação
**Then** toggle segmentado `[ Dia | Semana ]` no topo alterna entre visualizações (UX-DR8) (FR-014)
**And** swipe horizontal avança/retrocede dias na visualização diária e semanas na semanal
**And** chips de profissional roláveis abaixo do toggle filtram a agenda (FR-015, FR-017)
**And** botão "Hoje" aparece no header quando a data selecionada não é hoje

**Given** o usuário é Profissional e a agenda carrega
**When** há agendamentos no dia
**Then** o primeiro agendamento exibe badge `⭐ Primeiro do dia` em `brand-50` (UX-DR12) (FR-018)

**Given** há um paciente com status PRESENTE cujo horário é posterior ao próximo PENDENTE/CONFIRMADO
**When** o frontend avalia os cards após receber dados do polling
**Then** o card exibe badge `⚠️ Chegou antes do horário` (UX-DR12) (FR-020)

**Given** o TanStack Query está configurado para a query de agendamentos
**When** a tela de agenda está visível
**Then** `refetchInterval: 30000` a `60000` ms mantém a agenda atualizada automaticamente (FR-019)
**And** o polling ocorre silenciosamente sem indicador de loading visível
**And** se novos itens aparecerem, toast info `"Agenda atualizada"` dura 2s

---

### Story 4.2: Criar Agendamento — FAB, Formulário, Validações e Double-Booking

Como Staff ou Profissional,
Quero criar agendamentos com validação automática de disponibilidade e suporte a double-booking intencional,
Para que a agenda nunca tenha conflitos acidentais e o sistema mostre vagas reais.

**Acceptance Criteria:**

**Given** o usuário autenticado é Staff ou Profissional e está na tela de agenda
**When** a tela renderiza
**Then** o FAB `[ + ]` 56×56px é exibido no canto inferior direito (UX-DR5) (FR-022)
**And** para Profissional, o FAB está presente e ao abrir o Bottom Sheet o campo "Profissional" vem pré-preenchido com o próprio usuário e bloqueado para edição

**Given** o Staff ou Profissional toca no FAB
**When** o gesto é detectado
**Then** o Bottom Sheet sobe com o formulário de novo agendamento (UX-DR6) com campos: Cliente (busca autocomplete), Tipo de Atendimento (seletor — apenas tipos ativos), Profissional (seletor para Staff; bloqueado para Profissional), Data e Hora
**And** no desktop, o mesmo formulário aparece como drawer lateral 380px deslizando da direita com a agenda visível ao fundo

**Given** o Staff preenche Tipo de Atendimento
**When** o tipo é selecionado
**Then** a duração é exibida abaixo do seletor: ex. `"Duração calculada: 30 min"` (FR-025)

**Given** o Staff preenche Profissional, Data e Hora
**When** qualquer desses campos muda
**Then** o sistema exibe feedback inline de disponibilidade (FR-023):
`✅ "Dra. Wanessa disponível às 14:00"` ou
`❌ "Dra. Wanessa não atende às terças. Próximo disponível: quarta 09:00"`

**Given** o Staff submete `POST /api/v1/agendamentos` com dados válidos
**When** o backend valida (FR-023): (1) dia da semana disponível, (2) horário dentro da faixa, (3) janela `[início, início+duração]` não intersecta intervalo configurado
**Then** o agendamento é criado com `status = PENDENTE` e `horario_fim = horario_inicio + duracao_minutos` (FR-025)
**And** retorna HTTP 201
**And** toast `"Agendamento criado ✓"` é exibido e o Bottom Sheet fecha

**Given** o backend detecta que seria o segundo agendamento com janelas sobrepostas para o mesmo profissional (FR-024)
**When** a validação ocorre
**Then** retorna HTTP 409 com `"type": "double-booking"` e dados do agendamento existente sobreposto

**Given** o frontend recebe o 409 de double-booking
**When** o `DialogDoubleBooking` é exibido (UX-DR7)
**Then** mostra: nome do paciente existente, horário e tipo do conflito, e dois botões: `[Cancelar]` (ghost) e `[Agendar mesmo assim]` (âmbar)
**And** `[Agendar mesmo assim]` reenvia `POST /api/v1/agendamentos` com header `X-Confirm-Double-Booking: true`

**Given** seria o terceiro ou mais agendamento com sobreposição para o mesmo profissional (FR-024)
**When** o backend valida
**Then** retorna HTTP 409 com mensagem `"Terceiro agendamento com sobreposição não é permitido"` sem dialog (erro inline)

**Given** validação falha por qualquer motivo (FR-026)
**When** o backend retorna HTTP 409 ou 400
**Then** a mensagem de erro é exibida em linguagem clara (ex: `"Dra. Ana não atende às sextas-feiras."`, `"Intervalo de almoço: 12:00–13:00. Escolha outro horário."`)

---

### Story 4.3: Transições de Status, Histórico e Detalhe do Agendamento

Como Staff, Profissional ou Admin Empresa,
Quero atualizar o status de agendamentos em até 2 toques e ver o histórico completo de cada agendamento,
Para que o estado da agenda reflita a realidade em tempo real.

**Acceptance Criteria:**

**Given** a migration `V9__create_historico_status.sql` foi aplicada criando a tabela `historico_status` com colunas: `id` (BIGINT PK), `agendamento_id` (BIGINT FK NOT NULL), `empresa_id` (BIGINT FK NOT NULL), `status_anterior` (ENUM NOT NULL), `status_novo` (ENUM NOT NULL), `usuario_id` (BIGINT FK NOT NULL → `usuarios.id`), `timestamp` (DATETIME NOT NULL — UTC), `justificativa` (TEXT nullable) e índice `idx_historico_agendamento_id`
**When** o sistema inicializa
**Then** a tabela está criada (FR-057)

**Given** um card de agendamento é renderizado
**When** o status determina a ação primária
**Then** o botão contextual exibe o label correto (FR-027, FR-028):
`PENDENTE` → `[Confirmar]` (Staff/Admin) |
`CONFIRMADO` → `[Chegou]` (Staff/Admin) |
`PRESENTE` → `[Concluir]` (Staff/Admin/Profissional) |
`NOSHOW` → `[Reverter]` (Staff) |
`CONCLUIDO` / `CANCELADO` → sem botão (terminal)

**Given** o usuário toca o botão de ação primária (1 toque)
**When** o dialog de confirmação aparece
**Then** o segundo toque confirma a ação e `PATCH /api/v1/agendamentos/{id}/status` é enviado com `{ "statusNovo": "CONFIRMADO" }` (FR-028)
**And** o backend valida a transição antes de persistir — retorna HTTP 409 se inválida (FR-027)
**And** registra entrada na tabela `historico_status` com `timestamp` UTC, `usuario_id` e `justificativa = null`
**And** o card atualiza o status visualmente via invalidação do TanStack Query
**And** toast `"Status atualizado para Confirmado ✓"` é exibido

**Given** o usuário tenta a transição `PRESENTE → CANCELADO`
**When** o menu `···` → "Cancelar agendamento" é acionado (Staff/Admin)
**Then** um campo de justificativa obrigatório aparece antes de confirmar
**And** o backend persiste a justificativa em `historico_status.justificativa` (FR-057)
**And** retorna HTTP 409 se justificativa ausente

**Given** Staff toca `[Reverter]` em agendamento com status NOSHOW
**When** o dialog de reversão é exibido: `"Reverter NoShow para Presente? O registro de NoShow será mantido no histórico."`
**Then** confirmação via `PATCH /api/v1/agendamentos/{id}/status` com `{ "statusNovo": "PRESENTE" }`
**And** o registro de NoShow é preservado no histórico — não excluído (FR-030)

**Given** qualquer usuário toca no corpo de um card (fora dos botões)
**When** a navegação ocorre
**Then** abre a tela `/agendamento/:id` exibindo detalhe completo (FR-055): nome + telefone do cliente (descriptografados), tipo de atendimento, profissional, data/hora início e fim, status atual e lista de `historico_status` com timestamp formatado no fuso da clínica e nome do usuário responsável

**Given** Staff acessa o menu `···` de um agendamento com status não-terminal
**When** o menu é exibido
**Then** opções disponíveis: "Ver detalhes", "Editar agendamento" (Staff), "Gerar link de cancelamento" (Staff/Admin), "Cancelar agendamento" (Staff/Admin)

**Given** Staff acessa "Editar agendamento" de um agendamento PENDENTE ou CONFIRMADO
**When** submete `PUT /api/v1/agendamentos/{id}` com nova data, hora, tipo ou profissional
**Then** as mesmas validações de FR-023, FR-024, FR-025 são aplicadas (FR-052)
**And** `public_token` existente é invalidado (setado para NULL)
**And** retorna HTTP 200

**Given** a máquina de estados está implementada no backend
**When** `PATCH /api/v1/agendamentos/{id}/status` é chamado diretamente via API (sem intermediação da UI)
**Then** o `AgendamentoStatusValidator` (componente de domínio no backend, ex: `domain/AgendamentoStatusValidator.java`) rejeita com HTTP 409 as seguintes tentativas: `NOSHOW → CONCLUIDO`, `NOSHOW → CONFIRMADO`, `CANCELADO → qualquer status`, `CONCLUIDO → qualquer status`, e qualquer transição enviada por um Profissional exceto `PRESENTE → CONCLUIDO`
**And** a validação ocorre no backend independentemente do estado dos botões na UI — botões desabilitados na UI são UX, não segurança (R2 — state machine aplicada no backend, não apenas no frontend)

---

### Story 4.4: NoShow Automático e Busca de Próximo Disponível

Como sistema e como Staff,
Quero que agendamentos sem check-in sejam marcados automaticamente como NoShow e que o Staff possa encontrar vagas rapidamente,
Para que a agenda reflita os ausentes sem intervenção manual e a criação de agendamentos seja ágil.

**Acceptance Criteria:**

**Given** o `NoShowJob` está configurado com `@Scheduled(fixedDelay = 60000)` em `infrastructure/jobs/NoShowJob.java`
**When** o job executa
**Then** busca todos os agendamentos com `status IN ('PENDENTE', 'CONFIRMADO') AND horario_fim <= NOW() - INTERVAL 30 MINUTE` (UTC) (FR-030)
**And** para cada um encontrado, atualiza `status = 'NOSHOW'`
**And** registra entrada em `historico_status` com `usuario_id = NULL` (ação automática do sistema) e `justificativa = 'NoShow automático'`
**And** invalida o `public_token` (set NULL) — link de autoatendimento expira junto

**Given** o job executa durante um double-booking (dois agendamentos no mesmo slot)
**When** um dos agendamentos já tem status PRESENTE
**Then** apenas o agendamento ainda PENDENTE/CONFIRMADO recebe NoShow — o timer é independente por agendamento (FR-030)

**Given** o job executa e encontra um agendamento com status CANCELADO ou PRESENTE
**When** a query é avaliada
**Then** o agendamento é ignorado (condição `status IN ('PENDENTE', 'CONFIRMADO')`)

**Given** o profissional inativado tem agendamentos futuros
**When** a agenda é carregada (após Epic 2 criar o profissional)
**Then** cards de agendamentos futuros do profissional inativo exibem banner `⚠️ Profissional inativo — redistribuir manualmente` em cor `warning` (FR-009)
**And** os agendamentos NÃO são cancelados automaticamente

**Given** Staff acessa a tela `/buscar` (aba "Buscar" na BottomTabBar — somente Staff)
**When** a tela renderiza
**Then** exibe seletores de filtro: `[ Todos os profissionais ▾ ]` e `[ Qualquer tipo ▾ ]` (UX-DR9) (FR-029)

**Given** Staff acessa `GET /api/v1/agendamentos/proximos-disponiveis?profissional_id=&tipo_id=`
**When** o backend calcula vagas
**Then** retorna no máximo 20 slots disponíveis nos próximos 30 dias, ordenados por data e horário (FR-029)
**And** cada slot contém: data, horário, profissional e tipo de atendimento
**And** se não houver vagas, retorna array vazio com mensagem `"Nenhum horário disponível nos próximos 30 dias"`

**Given** Staff toca `[Agendar]` em uma vaga da lista
**When** o Bottom Sheet abre
**Then** os campos Data, Hora, Profissional e Tipo de Atendimento vêm pré-preenchidos (UX-DR9)
**And** apenas o campo "Cliente" fica em branco para preenchimento

---

### Story 4.5: Link de Autoatendimento Público

Como Staff ou Admin Empresa,
Quero gerar um link único para que o paciente confirme ou cancele sua consulta sem precisar de login,
Para que a taxa de confirmação aumente e os no-shows diminuam.

**Acceptance Criteria:**

**Given** Staff ou Admin acessa o menu `···` de um agendamento e toca "Gerar link de cancelamento"
**When** `POST /api/v1/agendamentos/{id}/gerar-link` é enviado
**Then** o backend gera `UUID.randomUUID()` como `public_token` e persiste em `agendamentos.public_token` (FR-042)
**And** retorna a URL completa: `{APP_FRONTEND_URL}/cancelar/{token}`
**And** o frontend copia a URL para o clipboard automaticamente
**And** toast `"Link de cancelamento copiado ✓"` é exibido

**Given** a rota `/cancelar/:token` é acessada pelo paciente (sem autenticação)
**When** `GET /api/v1/cancelar/{token}` é chamado (única rota sem filtro de tenant)
**Then** o backend busca o agendamento pelo `public_token`
**And** retorna dados públicos: nome do paciente (descriptografado), data/hora (convertida para fuso da clínica), nome do profissional, nome da clínica e status atual

**Given** o agendamento está com status PENDENTE
**When** a `AutoatendimentoPage` renderiza (UX-DR10)
**Then** exibe dois botões: `[✅ Confirmar presença]` (primário, brand-500 cheio) e `[✕ Cancelar consulta]` (secundário, outline error)

**Given** o paciente toca `[Confirmar presença]`
**When** o dialog de confirmação é exibido (`"Confirmar presença?"`)
**Then** segundo toque envia `POST /api/v1/cancelar/{token}/confirmar`
**And** o backend valida que status é PENDENTE e transita para CONFIRMADO
**And** registra em `historico_status` com `usuario_id = NULL` e `justificativa = 'Confirmado via link público'`
**And** a página exibe estado pós-confirmação: `"✅ Presença confirmada! Até [data] às [hora]!"`

**Given** o paciente toca `[Cancelar consulta]`
**When** o dialog é confirmado (`"Cancelar consulta? Esta ação não pode ser desfeita."`)
**Then** `POST /api/v1/cancelar/{token}/cancelar` transita para CANCELADO
**And** `public_token` é invalidado (set NULL)
**And** a página exibe estado pós-cancelamento: `"✅ Consulta cancelada. Obrigado por avisar!"`

**Given** o agendamento já está CONFIRMADO quando o paciente acessa o link
**When** a página renderiza
**Then** botão confirmar aparece desabilitado com label `"Presença já confirmada ✓"` e botão cancelar permanece ativo (FR-042)

**Given** o agendamento está em status PRESENTE, CONCLUIDO ou NOSHOW
**When** a página renderiza
**Then** ambos os botões são desabilitados com mensagem `"Esta consulta já foi atualizada pelo sistema"` (FR-042)

**Given** o `public_token` não existe, já foi invalidado ou o horário do agendamento já passou
**When** `GET /api/v1/cancelar/{token}` é chamado
**Then** retorna HTTP 410 (Gone)
**And** a página exibe: `"⚠️ Este link não está mais disponível. Pode ter expirado ou a consulta já foi cancelada."` (FR-042)

**Given** o job de NoShow executa e marca um agendamento como NOSHOW
**When** o `public_token` existe
**Then** o token é invalidado (set NULL) — link expira junto com o agendamento (FR-042)

**Given** a proteção do endpoint público contra enumeração de tokens
**When** `GET /api/v1/cancelar/{token}` é acessado
**Then** o `public_token` é um UUID v4 gerado via `UUID.randomUUID()` — 122 bits de entropia, computacionalmente inviável de adivinhar ou enumerar (R4 — segurança por entropia do token)
**And** o teste de integração desta story deve incluir: (1) criar agendamentos em duas empresas distintas e gerar tokens para ambos, (2) verificar que cada token retorna apenas dados do seu respectivo agendamento, (3) verificar que um token inexistente retorna HTTP 410 — sem mensagem que revele existência de outros tokens

### Epic 5: Relatórios e Configurações Avançadas

Admin Empresa acessa relatórios de ocupação, performance e NoShow. Configura baseline de no-show estimado para comparação. Todas as listagens com paginação server-side.

---

### Story 5.1: Relatório de Agendamentos — Contadores e Taxa de Ocupação

Como Admin Empresa,
Quero visualizar relatórios de agendamentos por período e profissional com contadores e taxa de ocupação,
Para que eu possa medir o desempenho da clínica e identificar padrões de uso.

**Acceptance Criteria:**

**Given** o Admin Empresa acessa `GET /api/v1/relatorios/agendamentos?dataInicio=2026-06-01&dataFim=2026-06-30` com parâmetro opcional `?profissional_id=`
**When** o backend processa
**Then** retorna contadores por status no período (FR-033): `agendados` (total), `confirmados`, `concluidos`, `cancelados`, `noShows`
**And** retorna taxa de ocupação em duas linhas (FR-034):
  - `ocupacaoConfirmada` = (CONFIRMADO + PRESENTE + CONCLUIDO + NOSHOW) / total slots disponíveis × 100
  - `ocupacaoTotal` = (todos exceto CANCELADO) / total slots disponíveis × 100
**And** retorna dados consolidados e por profissional
**And** apenas Admin Empresa da própria empresa acessa — retorna HTTP 403 para outros perfis (FR-032)

**Given** o cálculo de `total slots disponíveis` é necessário
**When** o backend computa
**Then** soma todos os slots de `duracao_minutos` de cada tipo de atendimento dentro das faixas de `disponibilidades` configuradas para o período, excluindo intervalos

**Given** a tela `/relatorios` renderiza no frontend
**When** o Admin Empresa seleciona período e profissional e toca "Gerar Relatório"
**Then** exibe cards de contadores (agendados, confirmados, concluídos, cancelados, no-shows)
**And** exibe as duas linhas de ocupação (confirmada e total) em formato percentual, consolidado e por profissional

**Given** o Admin Empresa não tem baseline configurado ainda
**When** a taxa de ocupação é exibida
**Then** a referência de baseline aparece como `"—"` com nota `"Configure o baseline em Configurações para comparação após 60 dias"`

---

### Story 5.2: Baseline de No-Show e Lista de Pacientes Recorrentes

Como Admin Empresa,
Quero configurar uma taxa de no-show de referência e ver quais pacientes mais faltam no período,
Para que eu possa comparar a melhora real e agir preventivamente com os pacientes problemáticos.

**Acceptance Criteria:**

**Given** a migration `V10__add_baseline_noshow.sql` foi aplicada adicionando à tabela `empresas`: `baseline_noshow_pct` (DECIMAL(5,2) nullable — valor 0 a 100)
**When** o sistema inicializa
**Then** a coluna está presente com valor NULL por padrão

**Given** o Admin Empresa acessa `/configuracoes/clinica` (já implementado na Story 3.2)
**When** edita o campo "Taxa de no-show estimada (%)" (campo numérico 0–100) e salva
**Then** `PUT /api/v1/configuracoes/clinica` persiste `baseline_noshow_pct` (FR-045)
**And** retorna HTTP 200

**Given** o Admin Empresa acessa o relatório e tem baseline configurado E a clínica tem ≥ 60 dias de operação
**When** a taxa de ocupação é exibida
**Then** o baseline aparece como linha de referência no relatório: `"Baseline configurado: X% · Atual: Y%"` (FR-045)

**Given** o Admin Empresa acessa `GET /api/v1/relatorios/noshow-pacientes?dataInicio=&dataFim=` com parâmetro opcional `?profissional_id=`
**When** o backend processa
**Then** retorna lista paginada (page size 20) de pacientes com ≥ 1 NoShow no período (FR-049, FR-046)
**And** cada item contém: nome do paciente (descriptografado para exibição), quantidade de NoShows, data do último NoShow
**And** ordenado por quantidade de NoShows decrescente

**Given** a tela de relatório renderiza a lista de no-shows
**When** há resultados
**Then** exibe tabela paginada com nome, quantidade de no-shows e data do último no-show
**And** exibe controles de paginação: `← Anterior` / `Próxima →` com indicador `"Página 1 de 5 (92 pacientes)"`

---

### Story 5.3: Paginação Server-Side em Todas as Listagens

Como usuário do DrAgenda,
Quero que todas as listas com crescimento ilimitado tenham paginação eficiente,
Para que o sistema continue performático independente do volume de dados.

**Acceptance Criteria:**

**Given** qualquer endpoint de listagem retorna dados paginados (FR-046)
**When** a API responde
**Then** o envelope de paginação contém: `content` (array), `page` (número atual), `size` (tamanho da página), `totalElements` (total de registros), `totalPages` (total de páginas)
**And** o page size padrão é 20 em todos os endpoints
**And** o cliente pode passar `?page=0&size=20` para controlar a paginação

**Given** os seguintes endpoints devem suportar paginação server-side
**When** são acessados sem parâmetros de paginação
**Then** retornam os primeiros 20 registros com metadados de paginação:
  - `GET /api/v1/clientes` ✓ (já implementado na Story 3.6)
  - `GET /api/v1/profissionais` ✓ (já implementado na Story 2.4)
  - `GET /api/v1/staff` ✓ (já implementado na Story 2.5)
  - `GET /api/v1/relatorios/agendamentos` ✓ (Story 5.1)
  - `GET /api/v1/relatorios/noshow-pacientes` ✓ (Story 5.2)
  - `GET /api/v1/agendamentos/{id}/historico` (histórico de transições de status)

**Given** o frontend renderiza qualquer listagem com `totalPages > 1`
**When** o componente de paginação é exibido
**Then** mostra controles de navegação entre páginas
**And** exibe indicador de posição: `"Página N de T (X registros)"`
**And** ao trocar de página, dispara nova requisição à API com `?page=N`

### Epic 6: Conformidade LGPD e Qualidade

Log de auditoria com retenção configurável, direito ao esquecimento (anonimização), estados de carregamento, estados vazios, banner offline PWA e acessibilidade WCAG 2.1 AA completa.

---

### Story 6.1: Log de Auditoria com Retenção Configurável

Como sistema,
Quero registrar automaticamente todo acesso e modificação de dados de pacientes em um log de auditoria retido por 5 anos,
Para que a clínica esteja em conformidade com a LGPD e tenha rastreabilidade completa de operações sensíveis.

**Acceptance Criteria:**

**Given** a migration `V11__create_audit_log.sql` foi aplicada criando a tabela `audit_log` com colunas: `id` (BIGINT PK), `empresa_id` (BIGINT FK nullable), `usuario_id` (BIGINT FK nullable), `acao` (VARCHAR 100 NOT NULL — ex: `CLIENTE_CRIADO`, `CLIENTE_EDITADO`, `CLIENTE_ANONIMIZADO`, `CLIENTE_LIDO`), `recurso_tipo` (VARCHAR 50 — ex: `CLIENTE`), `recurso_id` (BIGINT nullable), `timestamp` (DATETIME NOT NULL — UTC), `detalhes` (TEXT nullable — JSON com campos alterados, sem valores sensíveis) e índices `idx_audit_empresa_id`, `idx_audit_timestamp`
**When** o sistema inicializa
**Then** a tabela está criada (FR-038)

**Given** qualquer operação de leitura, criação ou modificação de dados de pacientes ocorre via `ClienteService`
**When** a operação é completada com sucesso
**Then** uma entrada é inserida em `audit_log` com `acao`, `recurso_id`, `usuario_id` (do contexto de segurança), `empresa_id` e `timestamp` UTC (FR-038)
**And** `detalhes` registra os campos alterados mas NUNCA os valores (ex: `{"camposAlterados": ["nome", "telefone"]}`) — dados sensíveis nunca entram no log
**And** `recurso_id` armazena apenas o `id` (BIGINT) do cliente — nome, telefone ou qualquer campo PII nunca é gravado em nenhuma coluna do `audit_log` (R5 — log de auditoria sem PII)
**And** teste de integração desta story deve verificar que após criar e editar um cliente com nome de teste, nenhuma linha em `audit_log` com aquele `recurso_id` contém o nome do cliente em nenhuma coluna — confirma que a anonimização futura (Story 6.2) será completa

**Given** a constante `AUDIT_LOG_RETENTION_YEARS` está definida no backend com valor `5`
**When** um job de limpeza diário `@Scheduled` executa
**Then** registros com `timestamp < NOW() - INTERVAL AUDIT_LOG_RETENTION_YEARS YEAR` são excluídos (FR-038)
**And** a constante é configurável via propriedade Spring sem necessidade de redeploy

**Given** qualquer usuário tenta acessar `GET /api/v1/audit-log`
**When** o `@PreAuthorize` avalia
**Then** retorna HTTP 403 para todos os perfis no MVP (log é interno — sem UI de consulta no MVP)

---

### Story 6.2: Direito ao Esquecimento — Anonimização de Pacientes

Como Admin Empresa,
Quero poder anonimizar os dados pessoais de um paciente mediante confirmação explícita,
Para que a clínica possa cumprir o direito ao esquecimento previsto na LGPD.

**Acceptance Criteria:**

**Given** o Admin Empresa acessa a tela de detalhe de um cliente via `/clientes/:id` (Staff vê; Admin Empresa executa)
**When** a tela renderiza para Admin Empresa
**Then** exibe botão "Remover dados pessoais" em cor `error` no rodapé da tela

**Given** o Admin Empresa toca "Remover dados pessoais"
**When** o dialog de confirmação aparece
**Then** exibe: `"Esta ação é irreversível. Os dados pessoais de [nome] serão substituídos por [PACIENTE REMOVIDO]. O histórico de agendamentos será preservado."` com dois botões: `[Cancelar]` e `[Confirmar remoção]` (estilo `error`)

**Given** o Admin Empresa confirma via `DELETE /api/v1/clientes/{id}/anonimizar`
**When** o backend processa (FR-039)
**Then** substitui `nome_enc`, `telefone_enc` e `data_nascimento_enc` pelos valores AES-256 de `[PACIENTE REMOVIDO]`
**And** marca `anonimizado = true` no registro
**And** preserva intactos todos os registros de `agendamentos`, `historico_status` vinculados — apenas dados pessoais são removidos
**And** registra `CLIENTE_ANONIMIZADO` no `audit_log`
**And** retorna HTTP 204

**Given** a anonimização é concluída com sucesso
**When** o `audit_log` é inspecionado para verificar integridade LGPD
**Then** nenhuma linha em `audit_log` relacionada ao `recurso_id` desse cliente contém o nome original do paciente — o nome nunca foi gravado no log, apenas o `cliente_id` numérico (R5 — anonimização completa inclui ausência de PII nos logs)
**And** o teste de integração desta story deve: (1) criar cliente com nome conhecido, (2) executar anonimização, (3) varrer todas as colunas de `audit_log` para `recurso_id` desse cliente e afirmar que nenhuma contém o nome original

**Given** um cliente anonimizado aparece em qualquer listagem ou agendamento
**When** o nome é exibido
**Then** mostra `[PACIENTE REMOVIDO]` no lugar do nome
**And** campos telefone e data de nascimento também exibem `[PACIENTE REMOVIDO]`

**Given** Staff tenta anonimizar via qualquer endpoint
**When** o `@PreAuthorize` avalia
**Then** retorna HTTP 403 (apenas Admin Empresa pode anonimizar)

---

### Story 6.3: Estados de Interface, Acessibilidade e PWA Offline

Como usuário do DrAgenda,
Quero que a interface informe claramente o estado de cada tela, seja acessível independente de limitações visuais e funcione com conexão instável,
Para que o sistema seja utilizável por todos os usuários em qualquer condição de conectividade.

**Acceptance Criteria:**

**Given** qualquer tela com lista de dados está carregando
**When** a requisição está em andamento
**Then** skeleton screens (shimmer) substituem os elementos: 3 cards placeholder na agenda, linhas placeholder em listas (UX-DR11)
**And** os skeletons têm dimensões equivalentes ao conteúdo real (sem layout shift)

**Given** a agenda do dia não tem agendamentos
**When** a lista renderiza
**Then** exibe estado vazio: `"Nenhum agendamento para hoje."` com o FAB visível (Staff/Profissional)

**Given** a lista de clientes está vazia (primeiro acesso)
**When** a tela `/clientes` renderiza
**Then** exibe: `"Nenhum cliente cadastrado ainda."` com botão `"Cadastrar primeiro cliente"`

**Given** a busca de próximo disponível não encontra vagas nos próximos 30 dias
**When** a tela `/buscar` renderiza
**Then** exibe: `"Nenhum horário disponível nos próximos 30 dias para os filtros selecionados. Verifique as configurações de disponibilidade."` com link para `/configuracoes/disponibilidade`

**Given** o dispositivo perde conectividade
**When** o frontend detecta `navigator.onLine === false`
**Then** banner amarelo fixo no topo: `"Sem conexão — algumas ações podem não funcionar"` (UX-DR11)
**And** botões de ação nos cards são desabilitados com tooltip `"Sem conexão"`
**And** a agenda exibe dados do cache do TanStack Query (última sincronização) com timestamp visível

**Given** o dispositivo reconecta
**When** o evento `online` é detectado
**Then** banner desaparece automaticamente e o polling reinicia imediatamente

**Given** o usuário acessa o DrAgenda via browser em um dispositivo mobile pela primeira vez
**When** o Service Worker é registrado
**Then** o PWA é instalável (manifest com `display: standalone`, ícone 192px e 512px)
**And** recursos estáticos (JS, CSS, fontes) são cacheados para funcionamento offline básico

**Given** qualquer componente interativo é renderizado
**When** é acessado via teclado ou leitor de tela
**Then** todos os botões de ícone têm `aria-label` descritivo (ex: FAB → `"Novo agendamento"`, `···` → `"Mais ações para [nome] [horário]"`)
**And** Bottom Sheets e Dialogs implementam focus trap: Tab não escapa do componente enquanto aberto
**And** ao fechar um Bottom Sheet ou Dialog, foco retorna ao elemento que o abriu
**And** mudanças de status são anunciadas via `aria-live="polite"` (ex: `"Status atualizado para Confirmado"`)
**And** todos os campos de formulário têm `<label>` associado via `htmlFor` e `aria-required="true"` quando obrigatório

**Given** qualquer indicador de status é exibido na interface
**When** renderizado
**Then** usa obrigatoriamente cor + ícone + rótulo de texto (nunca apenas cor) — conforme UX-DR2 (WCAG 2.1 AA + daltonismo)
**And** contraste mínimo WCAG 2.1 AA em todos os textos e elementos interativos
**And** área tocável mínima de 44×44px em todos os elementos interativos

### FR Coverage Map

FR-001: Epic 2 — Autenticação JWT com empresa_id
FR-002: Epic 2 — Suporte a 4 perfis de acesso
FR-003: Epic 2 — Isolamento de dados por empresa
FR-004: Epic 4 — Permissões de Staff para agendamentos e clientes
FR-005: Epic 4 — Permissões de Profissional para agendamentos
FR-006: Epic 2 — Admin Sistema cadastra clínicas
FR-007: Epic 2 — Criação de usuários com senha temporária
FR-008: Epic 2 — Cadastro de clínicas pelo Admin Sistema
FR-009: Epic 2 — Cadastro/inativação de profissionais pelo Admin Empresa
FR-010: Epic 2 — Cadastro/inativação de usuários Staff
FR-011: Epic 3 — Cadastro de clientes com consentimento LGPD
FR-012: Epic 3 — Cadastro de tipos de atendimento com duração
FR-013: Epic 3 — Configuração de disponibilidade dos profissionais
FR-014: Epic 4 — Dashboard de agenda com visualização diária e semanal
FR-015: Epic 4 — Filtro da agenda por profissional
FR-016: Epic 4 — Cards de agendamento com cor por status
FR-017: Epic 4 — Filtro de agendamentos por status
FR-018: Epic 4 — Destaque do primeiro agendamento do dia
FR-019: Epic 4 — Polling automático da agenda (30–60s)
FR-020: Epic 4 — Alerta de reordenação de pacientes
FR-021: Epic 4 — Agenda em lista mobile / grade desktop
FR-022: Epic 4 — Criação de agendamentos (Staff e Profissional)
FR-023: Epic 4 — Validação de disponibilidade do profissional
FR-024: Epic 4 — Double-booking com confirmação explícita
FR-025: Epic 4 — Cálculo automático de horário de término
FR-026: Epic 4 — Mensagens de validação em linguagem clara
FR-027: Epic 4 — Transições de status e matriz de permissões
FR-028: Epic 4 — Atualização de status em 2 toques
FR-029: Epic 4 — Busca de próximo horário disponível (Staff)
FR-030: Epic 4 — NoShow automático (+30min) e reversão pelo Staff
FR-031: Epic 4 — Histórico permanente de agendamentos
FR-032: Epic 5 — Relatório de agendamentos por período e profissional
FR-033: Epic 5 — Contadores de agendamentos por status
FR-034: Epic 5 — Taxa de ocupação confirmada e total
FR-035: Epic 3 — Criptografia AES-256 de dados sensíveis (implementação no cadastro)
FR-036: Epic 1 — HTTPS/TLS obrigatório
FR-037: Epic 3 — Aceite LGPD com armazenamento de timestamp/versão/usuario_id
FR-038: Epic 6 — Log de auditoria com retenção de 5 anos (constante configurável)
FR-039: Epic 6 — Direito ao esquecimento: anonimização de dados do paciente
FR-040: Epic 3 — Proibição de armazenar dados clínicos
FR-041: Epic 3 — Configuração de fuso horário da clínica
FR-042: Epic 4 — Link de autoatendimento público (confirmar/cancelar)
FR-043: Epic 2 — Redefinição de senha pelo Admin Empresa
FR-044: Epic 2 — Força troca de senha temporária no primeiro login
FR-045: Epic 5 — Baseline de no-show estimado nas configurações
FR-046: Epic 5 — Paginação server-side em listagens
FR-047: Epic 2 — Logout explícito em todos os perfis
FR-048: Epic 2 — Alteração de senha pelo próprio usuário
FR-049: Epic 5 — Lista paginada de pacientes com NoShow no período
FR-050: Epic 3 — Listagem, busca e edição de clientes pelo Staff
FR-051: Epic 2 — Admin Sistema lista, edita, inativa/reativa clínicas
FR-052: Epic 4 — Edição de agendamentos pelo Staff
FR-053: Epic 3 — Inativação de Tipos de Atendimento
FR-054: Epic 1 — Página pública de política de privacidade e termo de consentimento
FR-055: Epic 4 — Detalhe completo do agendamento (todos os perfis)
FR-056: Epic 2 — Admin Sistema cria Admin Empresa junto com a clínica
FR-057: Epic 4 — Registro de histórico de transições de status
FR-058: Epic 3 — Edição de dados básicos da clínica pelo Admin Empresa

NFR-001: Epic 4 — Performance da agenda < 2s
NFR-002: Epic 1 — Disponibilidade 99,5% (configuração de infra)
NFR-003: Epic 4 — Suporte a múltiplos usuários simultâneos
NFR-004: Epic 2 — Segurança multi-tenant (middleware de autenticação)
NFR-005: Epics 3 e 6 — Conformidade LGPD (consentimento Story 3.5, log auditoria Story 6.1, anonimização Story 6.2)
NFR-006: Epics 4 e 6 — Interface mobile-first ≥ 320px (layout Story 4.1, acessibilidade Story 6.3)
NFR-007: Epic 6 — WCAG 2.1 AA + daltonismo (Story 6.3 — aria, focus trap, contraste, indicadores cor+ícone+rótulo)
NFR-008: Epic 1 — Backup automático diário Railway MySQL add-on, retenção 30 dias (Story 1.1)

UX-DR1: Epic 3 — Design System tokens (cores, tipografia via Tailwind)
UX-DR2: Epic 3 — Componente StatusBadge
UX-DR3: Epic 4 — Componente AgendamentoCard
UX-DR4: Epic 2 — BottomTabBar por perfil
UX-DR5: Epic 4 — FAB flutuante (Staff e Profissional)
UX-DR6: Epic 4 — NovoAgendamentoSheet (Bottom Sheet / Drawer)
UX-DR7: Epic 4 — DialogDoubleBooking
UX-DR8: Epic 4 — Navegação da agenda (toggle + swipe)
UX-DR9: Epic 4 — BuscarDisponivelView
UX-DR10: Epic 4 — AutoatendimentoPage (rota pública)
UX-DR11: Epic 6 — Skeleton screens, estados vazios, banner offline PWA
UX-DR12: Epic 4 — Badge primeiro do dia e alerta de reordenação
UX-DR13: Epic 1 — Wordmark DrAgenda + fonte Inter

## Epic List

### Epic 1: Fundação Técnica
Infrastrutura do monorepo, CI/CD, banco de dados versionado com Flyway, variáveis de ambiente, deploy inicial Vercel + Railway, seed seguro do Admin Sistema e página pública de política de privacidade.
**Resultado para o usuário:** ambiente de desenvolvimento e produção funcionando, pipeline automatizado, banco versionado, sistema acessível via HTTPS.
**FRs cobertos:** FR-036, FR-054
**NFRs:** NFR-002 (disponibilidade 99,5% via Railway), NFR-008 (backup automático diário)
**Adicionais:** Monorepo (`/frontend` + `/backend`), Flyway, CI/CD GitHub Actions (`ci-frontend.yml` + `ci-backend.yml`), variáveis de ambiente canônicas, Springdoc OpenAPI, Logback JSON
**UX-DRs:** UX-DR13 (wordmark + tipografia base)

### Epic 2: Autenticação e Gestão de Usuários
Usuários conseguem fazer login, trocar senha e fazer logout. Admin Sistema cadastra clínicas e cria Admin Empresa. Admin Empresa cadastra Staff e Profissionais. Troca obrigatória de senha temporária no primeiro login.
**Resultado para o usuário:** sistema multi-tenant funcionando, todos os perfis podem autenticar, Admin Sistema gerencia clínicas, Admin Empresa gerencia sua equipe.
**FRs cobertos:** FR-001, FR-002, FR-003, FR-006, FR-007, FR-008, FR-009, FR-010, FR-043, FR-044, FR-047, FR-048, FR-051, FR-056
**NFRs:** NFR-004 (segurança multi-tenant)
**UX-DRs:** UX-DR4 (BottomTabBar por perfil)

### Epic 3: Cadastros Base da Clínica
Admin Empresa configura a clínica: tipos de atendimento, disponibilidade dos profissionais, fuso horário e dados básicos. Staff cadastra e gerencia clientes com consentimento LGPD explícito.
**Resultado para o usuário:** clínica completamente configurada, clientes cadastrados com conformidade LGPD, pronta para receber agendamentos.
**FRs cobertos:** FR-011, FR-012, FR-013, FR-035, FR-037, FR-040, FR-041, FR-050, FR-053, FR-058
**UX-DRs:** UX-DR1 (Design System tokens), UX-DR2 (StatusBadge)

### Epic 4: Agenda e Agendamentos
Usuários visualizam a agenda, criam agendamentos, fazem check-in, atualizam status, buscam próximo horário disponível e veem detalhes completos. Inclui double-booking intencional, NoShow automático e link de autoatendimento público para o paciente confirmar ou cancelar sem login.
**Resultado para o usuário:** fluxo completo de trabalho da clínica operacional — Maria agenda, Wanessa conclui, paciente confirma via link, no-shows são reduzidos.
**FRs cobertos:** FR-004, FR-005, FR-014, FR-015, FR-016, FR-017, FR-018, FR-019, FR-020, FR-021, FR-022, FR-023, FR-024, FR-025, FR-026, FR-027, FR-028, FR-029, FR-030, FR-031, FR-042, FR-052, FR-055, FR-057
**NFRs:** NFR-001, NFR-003, NFR-006
**UX-DRs:** UX-DR3, UX-DR5, UX-DR6, UX-DR7, UX-DR8, UX-DR9, UX-DR10, UX-DR11 parcial, UX-DR12

### Epic 5: Relatórios e Configurações Avançadas
Admin Empresa acessa relatórios de ocupação, performance e NoShow. Configura baseline de no-show estimado para comparação. Todas as listagens com paginação server-side.
**Resultado para o usuário:** André consegue medir o resultado da clínica, identificar padrões e tomar decisões baseadas em dados.
**FRs cobertos:** FR-032, FR-033, FR-034, FR-045, FR-046, FR-049

### Epic 6: Conformidade LGPD e Qualidade
Log de auditoria com retenção configurável, direito ao esquecimento (anonimização), estados de carregamento, estados vazios, banner offline PWA e acessibilidade WCAG 2.1 AA completa.
**Resultado para o usuário:** sistema em conformidade com LGPD, auditavel, acessível para todos os usuários incluindo daltoníicos, com experiência robusta offline.
**FRs cobertos:** FR-038, FR-039
**NFRs:** NFR-005 (LGPD — Stories 6.1 e 6.2), NFR-006 (mobile-first ≥ 320px), NFR-007 (WCAG 2.1 AA + daltonismo)
**UX-DRs:** UX-DR11 (skeleton, estados vazios, offline, PWA)
