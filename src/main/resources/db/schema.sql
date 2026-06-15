-- =====================================================================
-- Mango — schema dos UC1, UC2 e UC3
-- Aplicado de forma idempotente na inicialização.
-- =====================================================================

CREATE TABLE IF NOT EXISTS cache_busca (
    chave      VARCHAR(600) PRIMARY KEY,
    payload    CLOB         NOT NULL,
    criado_em  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS progresso_leitura (
    manga_id      VARCHAR(64) NOT NULL,
    capitulo_id   VARCHAR(64) NOT NULL,
    pagina_atual  INT         NOT NULL DEFAULT 0,
    total_paginas INT         NOT NULL DEFAULT 0,
    concluido     BOOLEAN     NOT NULL DEFAULT FALSE,
    atualizado_em TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (manga_id, capitulo_id)
);

-- UC3: dados para exibir histórico/continuar lendo com capa e título
ALTER TABLE progresso_leitura ADD COLUMN IF NOT EXISTS titulo     VARCHAR(500);
ALTER TABLE progresso_leitura ADD COLUMN IF NOT EXISTS capa_url   VARCHAR(1000);
ALTER TABLE progresso_leitura ADD COLUMN IF NOT EXISTS cap_numero VARCHAR(40);

-- UC3 — RN3.2: coleções nomeadas da biblioteca pessoal
CREATE TABLE IF NOT EXISTS colecao (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(40) NOT NULL,
    removivel  BOOLEAN     NOT NULL DEFAULT TRUE,
    criada_em  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_colecao_nome UNIQUE (nome)
);

-- UC3 — RN3.1: um mangá não se repete na mesma coleção
CREATE TABLE IF NOT EXISTS item_biblioteca (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    colecao_id    BIGINT       NOT NULL,
    manga_id      VARCHAR(64)  NOT NULL,
    titulo        VARCHAR(500) NOT NULL,
    capa_url      VARCHAR(1000),
    adicionado_em TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_item UNIQUE (colecao_id, manga_id),
    CONSTRAINT fk_item_colecao FOREIGN KEY (colecao_id)
        REFERENCES colecao(id) ON DELETE CASCADE
);

-- UC3 — RN3.3: coleções padrão não removíveis (idempotente)
MERGE INTO colecao (nome, removivel) KEY(nome) VALUES ('Favoritos', FALSE);
MERGE INTO colecao (nome, removivel) KEY(nome) VALUES ('Lendo', FALSE);
MERGE INTO colecao (nome, removivel) KEY(nome) VALUES ('Concluídos', FALSE);
MERGE INTO colecao (nome, removivel) KEY(nome) VALUES ('Quero Ler', FALSE);
