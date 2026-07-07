CREATE TABLE usuarios (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    senha_hash       VARCHAR(255) NOT NULL,
    perfil           ENUM('ADMIN_SISTEMA','ADMIN_EMPRESA','STAFF','PROFISSIONAL') NOT NULL,
    empresa_id       BIGINT       NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    senha_temporaria BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    INDEX idx_usuarios_empresa_id (empresa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_usuarios_email ON usuarios(email);
