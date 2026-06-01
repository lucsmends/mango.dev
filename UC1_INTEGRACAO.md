# UC1 — Catálogo e Busca · guia de integração

Implementação da fatia vertical completa do **UC1** + a **Fundação** compartilhada
que faltava no backlog (a "Fase 0"). Código Java 21, seguindo a arquitetura
MVC + Service + Repository do projeto.

## Arquivos criados

```
src/main/java/com/mango/
├── Launcher.java                     # fix do empacotamento fat-jar (shade + JavaFX)
├── Main.java                         # bootstrap: inicializa o H2 e carrega o catálogo
├── exception/
│   ├── MangoException.java           # base do domínio
│   ├── MangaDexException.java        # falhas de API (EX1–EX3)
│   ├── PersistenciaException.java    # falhas de H2
│   └── RegraNegocioException.java    # violações de RN (ex.: RN1.1)
├── model/
│   ├── Manga.java                    # record imutável
│   ├── Capitulo.java
│   ├── FiltroBusca.java              # termo + paginação + chave de cache
│   └── ResultadoBusca.java           # paginação (total, totalPaginas, doCache)
├── repository/
│   ├── Database.java                 # conexão + schema idempotente
│   └── CacheBuscaRepository.java     # cache de buscas (RN1.2)
├── service/
│   └── MangaDexService.java          # busca, ficha, capítulos (RN1.1/1.3/1.6)
├── util/
│   ├── AppConfig.java                # config central (caminhos, API, constantes de RN)
│   └── HttpJsonClient.java           # GET com retry/backoff (RN1.4) + 429 (RN1.5)
└── controller/
    ├── CatalogoController.java       # busca assíncrona + grade de cards + paginação
    └── FichaController.java          # ficha + lista de capítulos

src/main/resources/
├── fxml/catalogo.fxml
├── fxml/ficha.fxml
├── css/styles.css
└── db/migration/V1__schema_inicial.sql

src/test/java/com/mango/service/
└── MangaDexServiceTest.java          # RN1.1, RN1.2/FA2, RN1.3, EX4 (mockado)
```

## Como integrar no repositório

Estes arquivos seguem exatamente o layout do `mango.dev`. Basta **copiar as pastas
`src/` para a raiz do clone** (elas se mesclam com a estrutura existente, substituindo
o `Main.java` stub). Nenhuma dependência nova é necessária — usa apenas o que já está
no `pom.xml` (JavaFX, H2, Jackson, SLF4J; JUnit 5 + Mockito no teste).

**Único ajuste no `pom.xml`** (necessário só para o fat-jar; o `mvn javafx:run` já funciona sem ele):
no `maven-shade-plugin`, troque a `mainClass` para o Launcher:

```xml
<mainClass>com.mango.Launcher</mainClass>
```

## Como rodar

```bash
mvn clean javafx:run
```

Na primeira execução o banco H2 é criado em `~/.mango/mango.mv.db` com o schema e as
três coleções padrão (RN3.3). Para testar: abra o app, digite um título (ex.: "naruto")
e Enter — ou busque com o campo vazio para ver os populares. Clique num card para abrir
a ficha com a lista de capítulos.

```bash
mvn test            # roda os testes do MangaDexService
```

## O que já está coberto (rastreabilidade)

| Regra | Onde | Situação |
|-------|------|----------|
| RN1.1 termo mínimo (2 chars) | `MangaDexService.validarTermo` | ✅ + teste |
| RN1.2 cache de busca (30 min) | `CacheBuscaRepository` | ✅ + teste |
| RN1.3 paginação (20/página) | `AppConfig.TAMANHO_PAGINA` + `ResultadoBusca` | ✅ + teste |
| RN1.4 retentativas em 5xx (backoff) | `HttpJsonClient.getJson` | ✅ |
| RN1.5 rate limit 429 (Retry-After) | `HttpJsonClient.getJson` | ✅ |
| RN1.6 idioma pt-br + fallback en | `MangaDexService.listarCapitulos` | ✅ |
| FA1 busca sem termo = populares | `montarUrlBusca` | ✅ |
| FA2 indicação de cache | `ResultadoBusca.doCache` + UI | ✅ |
| FA3 paginação | botões Anterior/Próxima | ✅ |
| EX1 sem conexão | `HttpJsonClient` → mensagem amigável | ✅ |
| EX4 resultado vazio | `CatalogoController.renderizar` | ✅ |

## Próximos passos do UC1 (ainda abertos)

1. **MNG-32 — filtros de gênero e autor.** A busca por título + paginação está pronta;
   falta resolver os IDs: gêneros via `GET /manga/tag` (mapeia nome → UUID, usado em
   `includedTags[]`) e autor via `GET /author` (`authors[]`). O gancho está marcado com
   `// NOTA (MNG-32)` em `MangaDexService.montarUrlBusca`.
2. **Endpoint para o UC2 (Lucas).** O contrato que o leitor vai consumir para obter as
   páginas é `GET /at-home/server/{chapterId}` → `{baseUrl, chapter:{hash, data[]}}`, com
   a URL de cada página montada como `{baseUrl}/data/{hash}/{file}`. Vale alinhar isso
   com o Lucas, já que o `Capitulo.id` daqui é a entrada do leitor dele.
3. **Cache de capa em disco** (otimização opcional, fora do escopo mínimo do UC1).

## Notas de design

- **Threading:** toda chamada de rede roda em `Task` JavaFX em thread daemon; a UI nunca
  trava e há `ProgressIndicator` visível durante a busca (requisito não-funcional do UC1).
- **Injeção de dependência:** `MangaDexService` tem construtor que recebe `HttpJsonClient`
  e `CacheBuscaRepository`, justamente para os testes mockarem rede e banco (MNG-58).
- **Migrations:** o `Database` aplica o `V1__schema_inicial.sql` de forma idempotente
  (`CREATE TABLE IF NOT EXISTS` + `MERGE`). Se preferirem Flyway depois, é só adicionar a
  dependência e mover o script para `classpath:db/migration` — o nome já segue o padrão.
