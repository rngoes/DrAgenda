# DrAgenda

Sistema de agendamentos para clínicas médicas — monorepo com frontend React (Vite) e backend Spring Boot.

## Estrutura

```
dragenda/
├── frontend/   # Vite 5 + React 18 + TypeScript 5 (deploy: Vercel)
└── backend/    # Spring Boot 3.x + Java 21 + Maven (deploy: Railway)
```

## Pré-requisitos

- Java 21 (LTS)
- Node.js 20+ LTS
- Maven 3.9+
- MySQL 8.0+ (local) ou conta Railway (produção)

## Setup Local

### Backend

```bash
cd backend
cp .env.example .env
# Preencher .env com valores reais
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd frontend
cp .env.example .env.local
# Preencher VITE_API_URL
npm install
npm run dev
```

## Convenção de Migrations Flyway (OBRIGATÓRIO)

Toda nova migration usa **timestamp** como prefixo:

```
V{yyyyMMddHHmm}__{descricao}.sql
```

**Exemplo correto:** `V202606041430__create_clientes.sql`

**Nunca use número sequencial** (V1, V2...) — causa conflito em branches paralelas.

Antes de criar uma migration, verifique se já existe outra com timestamp próximo.

## Restore de Backup (Railway MySQL)

1. Acesse o dashboard Railway → seu projeto → MySQL add-on
2. Aba **Backups** → selecione o snapshot desejado
3. Clique em **Restore** e confirme
4. Aguarde a conclusão (tipicamente 2–5 min para bancos pequenos)
5. Reinicie o serviço backend via Railway dashboard

> Backups automáticos são gerados diariamente com retenção mínima de 30 dias (NFR-008).
> Ative em: Railway dashboard → MySQL add-on → Settings → Backups → Enable automatic backups.

## Deploy

- **Frontend → Vercel**: conectar repositório no dashboard Vercel, apontar para `/frontend`, build command `npm run build`, output dir `dist`
- **Backend → Railway**: push para branch principal aciona deploy via `Procfile`

## Variáveis de Ambiente

Ver `backend/.env.example` e `frontend/.env.example` para lista completa.
