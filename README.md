# Mango

Leitor de mangás desktop em Java 21 + JavaFX, com integração à API pública do
MangaDex. Implementa o **UC1 (Catálogo e Busca)** e o **UC2 (Leitor de Capítulos)**.

## Como rodar

```bash
mvn clean javafx:run     # roda o app
mvn test                 # roda os testes (sem rede e sem banco)
```

O banco H2 local fica em `~/.mango`. Precisa de Java 21, Maven 3.9+ e internet.

## Funcionalidades

- Catálogo com busca por título e filtro de gênero, paginação e cache de buscas.
- Ficha do mangá com sinopse e lista de capítulos (pt-br com fallback en).
- Leitor de capítulos: página única, navegação por botões/teclado, zoom,
  "ir para página", pré-carga, e retomada do ponto de leitura.

## Estrutura

```
src/main/java/com/mango/
├── MangoApp.java / Launcher.java     # bootstrap
├── config/                           # constantes das regras de negócio
├── model/                            # records imutáveis
├── net/                              # cliente HTTP do MangaDex (retry/rate limit)
├── db/                               # H2: schema, cache, progresso
├── service/                          # CatalogoService (UC1), LeitorService (UC2)
└── ui/                               # controllers JavaFX
src/main/resources/{fxml,css,db}
src/test/java/com/mango/service/
```
