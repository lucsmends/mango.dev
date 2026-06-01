-- =====================================================================
-- Mango — Schema inicial (V1)
-- Derivado do modelo dos Casos de Uso (UC1–UC3).
-- Executado de forma idempotente pelo SchemaInitializer na inicialização.
-- =====================================================================

CREATE TABLE IF NOT EXISTS colecao (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(30)  NOT NULL,
    cor           VARCHAR(7),
    removivel     BOOLEAN      NOT NULL DEFAULT TRUE,
    criada_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_colecao_nome UNIQUE (nome)            -- RN3.2
);

CREATE TABLE IF NOT EXISTS item_biblioteca (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    manga_id      VARCHAR(64)  NOT NULL,                -- UUID do MangaDex
    titulo        VARCHAR(500) NOT NULL,
    capa_url      VARCHAR(1000),
    colecao_id    BIGINT       NOT NULL,
    adicionado_em TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_colecao FOREIGN KEY (colecao_id) REFERENCES colecao(id),
    CONSTRAINT uk_item_colecao UNIQUE (manga_id, colecao_id)   -- RN3.1
);

CREATE TABLE IF NOT EXISTS historico_leitura (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    manga_id      VARCHAR(64)  NOT NULL,
    capitulo_id   VARCHAR(64)  NOT NULL,
    pagina_atual  INT          NOT NULL DEFAULT 0,
    total_paginas INT          NOT NULL DEFAULT 0,
    concluido     BOOLEAN      NOT NULL DEFAULT FALSE,  -- RN2.3
    atualizado_em TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_historico UNIQUE (manga_id, capitulo_id)
);

-- Cache de buscas do catálogo (RN1.2): payload JSON por combinação de filtros,
-- com TTL de 30 minutos validado em código.
CREATE TABLE IF NOT EXISTS cache_busca (
    chave_filtros VARCHAR(600) PRIMARY KEY,
    payload_json  CLOB         NOT NULL,
    criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- RN3.3: coleções padrão não removíveis. MERGE evita duplicar em re-execuções.
MERGE INTO colecao (nome, cor, removivel) KEY(nome) VALUES ('Lendo',      '#4F86C6', FALSE);
MERGE INTO colecao (nome, cor, removivel) KEY(nome) VALUES ('Concluídos', '#5CB85C', FALSE);
MERGE INTO colecao (nome, cor, removivel) KEY(nome) VALUES ('Quero Ler',  '#F0AD4E', FALSE);
