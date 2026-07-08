import { TERMO_VERSAO_VIGENTE } from '../shared/lib/constants'

export default function PoliticaPage() {
  return (
    <div className="min-h-screen bg-[var(--bg-base)]">
      <div className="max-w-2xl mx-auto px-4 py-8">

        <h1 className="font-brand font-medium text-2xl text-brand-500 mb-8">
          <span className="font-light text-[var(--text-primary)]">Dr</span>Agenda
        </h1>

        <h2 className="text-xl font-semibold text-[var(--text-primary)] mb-6">
          Política de Privacidade e Termo de Consentimento LGPD
        </h2>

        <section className="mb-6">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            1. Dados que coletamos
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Coletamos nome completo, telefone e data de nascimento para fins de agendamento de
            consultas. Não armazenamos diagnósticos, procedimentos clínicos ou qualquer
            informação de saúde além dos dados de agendamento.
          </p>
        </section>

        <section className="mb-6">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            2. Como usamos seus dados
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Seus dados são utilizados exclusivamente para gerenciar seus agendamentos de consulta
            na clínica. Não compartilhamos seus dados com terceiros. Todos os dados sensíveis
            são criptografados em repouso (AES-256) e a comunicação ocorre via HTTPS.
          </p>
        </section>

        <section className="mb-6">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            3. Seus direitos (LGPD)
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Conforme a Lei Geral de Proteção de Dados (Lei nº 13.709/2018), você tem direito a:
            acessar seus dados, corrigir informações incorretas, solicitar a exclusão
            (anonimização) dos seus dados pessoais e revogar o consentimento a qualquer momento.
            Entre em contato com a clínica para exercer seus direitos.
          </p>
        </section>

        <section className="mb-8">
          <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">
            4. Contato
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
            Para dúvidas ou solicitações sobre seus dados pessoais, entre em contato diretamente
            com a clínica responsável pelo seu agendamento.
          </p>
        </section>

        <footer className="border-t border-[var(--border)] pt-4">
          <p className="text-xs text-[var(--text-disabled)]">
            Versão {TERMO_VERSAO_VIGENTE} — vigência a partir de 04/06/2026
          </p>
        </footer>

      </div>
    </div>
  )
}
