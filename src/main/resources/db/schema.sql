-- =====================================================================
-- Mango — schema dos UC1 e UC2
-- Aplicado de forma idempotente na inicialização.
-- =====================================================================

-- RN1.2: cache de buscas por combinação de filtros (TTL validado em código)
CREATE TABLE IF NOT EXISTS cache_busca (
    chave      VARCHAR(600) PRIMARY KEY,
    payload    CLOB         NOT NULL,
    criado_em  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- RN2.2 / RN2.3 / FA5: progresso de leitura por capítulo
CREATE TABLE IF NOT EXISTS progresso_leitura (
    manga_id      VARCHAR(64) NOT NULL,
    capitulo_id   VARCHAR(64) NOT NULL,
    pagina_atual  INT         NOT NULL DEFAULT 0,
    total_paginas INT         NOT NULL DEFAULT 0,
    concluido     BOOLEAN     NOT NULL DEFAULT FALSE,
    atualizado_em TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (manga_id, capitulo_id)
);
