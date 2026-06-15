# Mango

> Leitor de mangás desktop em Java, com integração ao MangaDex e tradução automática experimental no estilo *overlay*.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io)
[![Build](https://img.shields.io/badge/build-Maven-C71A36.svg)](https://maven.apache.org)
[![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow.svg)]()
[![License](https://img.shields.io/badge/license-Acad%C3%AAmico-lightgrey.svg)]()

---

## Sumário

1. [Visão geral](#vis%C3%A3o-geral)
2. [Funcionalidades planejadas](#funcionalidades-planejadas)
3. [Stack tecnológica](#stack-tecnol%C3%B3gica)
4. [Arquitetura](#arquitetura)
5. [Estrutura de diretórios](#estrutura-de-diret%C3%B3rios)
6. [Pré-requisitos](#pr%C3%A9-requisitos)
7. [Como configurar o ambiente](#como-configurar-o-ambiente)
8. [Como compilar e executar](#como-compilar-e-executar)
9. [Como executar os testes](#como-executar-os-testes)
10. [Banco de dados](#banco-de-dados)
11. [Cronograma e roadmap](#cronograma-e-roadmap)
12. [Convenções de desenvolvimento](#conven%C3%A7%C3%B5es-de-desenvolvimento)
13. [Documentação adicional](#documenta%C3%A7%C3%A3o-adicional)
14. [Equipe](#equipe)
15. [Licença](#licen%C3%A7a)

---

## Visão geral

O **Mango** é um leitor de mangás desktop desenvolvido como trabalho da disciplina de **Implementação de Software** (UNISINOS — 2026/1). O sistema consome o catálogo público do [MangaDex](https://api.mangadex.org) e oferece uma experiência de leitura confortável com biblioteca pessoal persistente.

O grande diferencial planejado é um pipeline de **tradução automática com overlay** (UC4, **trabalho futuro**): detectar os balões de texto na página, reconhecer o texto japonês, traduzir e renderizar o resultado por cima da imagem original. A arquitetura de referência é o [FrankYomik](https://github.com/akitaonrails/FrankYomik), de Fabio Akita — no qual o cliente leitor consome um **serviço externo** de tradução de mangá (detecção de balão + OCR especializado + tradução por LLM + *inpainting*) via HTTP. Ver [Trabalho futuro](#trabalho-futuro).

> **Aviso acadêmico:** o Mango é um projeto educacional, sem fins comerciais, que apenas consome a API pública do MangaDex. O sistema **não distribui conteúdo protegido por direitos autorais**. Todo conteúdo exibido vem em tempo real dos servidores do MangaDex.

---

## Funcionalidades

| ID  | Caso de Uso                                   | Sprint   | Status                |
|-----|-----------------------------------------------|----------|-----------------------|
| UC1 | Catálogo e busca de mangás                    | 1–2      | ✅ Implementado        |
| UC2 | Leitor de capítulos (página única + zoom)     | 2        | ✅ Implementado        |
| UC3 | Biblioteca pessoal, coleções e histórico      | 3        | ✅ Implementado        |
| UC4 | Tradução automática com overlay (experimental)| 4        | 🚧 Trabalho futuro     |

> O **UC4** foi especificado e planejado, mas **não foi implementado nesta versão**; permanece registrado como trabalho futuro — ver a seção [Trabalho futuro](#trabalho-futuro).

O detalhamento de cada UC, com fluxo principal, fluxos alternativos, exceções e Regras de Negócio, está em [docs/Casos_de_Uso.docx](./docs/Casos_de_Uso.docx).

---

## Stack tecnológica

| Camada                 | Tecnologia                          | Por quê                                                                 |
|------------------------|-------------------------------------|-------------------------------------------------------------------------|
| Linguagem              | Java 21 (LTS)                       | Records, switch patterns, HttpClient nativo                              |
| Build                  | Apache Maven                        | Padrão de mercado, integra com IDEs                                      |
| Interface gráfica      | JavaFX 21                           | Moderna, multiplataforma, suporta FXML e CSS                             |
| Banco de dados         | H2 (embarcado, modo arquivo)        | Persistência local sem servidor externo                                  |
| Acesso a dados         | JDBC + camada Repository            | Baixo acoplamento, didático                                              |
| Cliente HTTP           | `java.net.http.HttpClient`          | Nativo do JDK, sem dependência externa                                   |
| Serialização JSON      | Jackson Databind                    | Integra-se nativamente com records                                       |
| Detecção de balão (UC4)| RT-DETR-v2 (no serviço externo)     | Modelo treinado p/ balões — OCR genérico não recorta o texto do mangá    |
| OCR (UC4)              | manga-ocr (jpn) / EasyOCR (kor)     | OCR **especializado em mangá**; Tesseract genérico falha em fonte/balão  |
| Tradução (UC4)         | LLM (Ollama) ou API (DeepL/Google)  | LLM local dá contexto melhor; API evita GPU — decisão do backend         |
| Integração no cliente  | `HttpClient` → serviço FrankYomik   | O Mango é **cliente HTTP**; o trabalho pesado de ML fica no serviço       |
| Overlay (UC4)          | JavaFX `Pane` sobre a `ImageView`   | Reescala as *bounding boxes* do serviço sobre a página exibida           |
| Testes                 | JUnit 5 + Mockito                   | Padrão da indústria para Java                                            |
| Logging                | SLF4J + Logback                     | Logging estruturado, configurável                                        |
| Controle de versão     | Git + GitHub                        | Pull Requests, branches, futura CI                                       |

---

## Arquitetura

O Mango segue o padrão **MVC com Service Layer**, com clara separação de responsabilidades:

```
┌──────────────────┐    eventos    ┌──────────────────┐
│  View (JavaFX)   │ ─────────────▶│   Controller     │
└──────────────────┘               └────────┬─────────┘
                                            │
                                            ▼
                                   ┌──────────────────┐
                                   │  Service Layer   │  ← regras de negócio
                                   │  (RNs, validações│
                                   │   e orquestração)│
                                   └────────┬─────────┘
                                            │
                            ┌───────────────┼───────────────┐
                            ▼               ▼               ▼
                    ┌─────────────┐ ┌─────────────┐ ┌──────────────┐
                    │ Repository  │ │  MangaDex   │ │   Tradução   │
                    │  (H2 / SQL) │ │  HTTP API   │ │  HTTP API    │
                    └─────────────┘ └─────────────┘ └──────────────┘
```

- **View** (`ui/*.fxml` + `css/`) — telas em FXML estilizadas com CSS.
- **Controller** (`ui/`) — manipula eventos da UI e delega ao Service. Não contém regra de negócio. Ex.: `CatalogoController`, `FichaController`, `LeitorController`.
- **Service** (`service/`) — concentra Regras de Negócio: `CatalogoService` (UC1), `LeitorService` (UC2), `BibliotecaService` e `HistoricoService` (UC3). O `TraducaoService` do UC4 (trabalho futuro) seria um **cliente HTTP de um serviço de tradução externo** (modelo FrankYomik), sem embarcar modelos de ML no aplicativo.
- **Repository** (`db/`) — abstrai o acesso ao banco H2 e ao cache: `CacheBuscaRepository`, `ProgressoRepository`, `BibliotecaRepository`, `ColecaoRepository`.
- **Net** (`net/`) — cliente HTTP do MangaDex (`MangaDexHttp` / `JsonFetcher`) com retry e tratamento de rate limit.
- **Model** (`model/`) — `records` imutáveis: `Manga`, `Capitulo`, `Pagina`, `Progresso`, `LeituraRecente`, `ItemBiblioteca`, `Colecao`, `Genero`, `FiltroBusca`, `ResultadoBusca`.

Exceções de regra de negócio (`RegraNegocioException`, `ApiException`, `MangoException`) são lançadas no Service e traduzidas pelo Controller em mensagens claras para o Leitor.

---

## Estrutura de diretórios

```
mango/
├── pom.xml                       # build Maven
├── README.md
├── .gitignore
├── docs/
│   ├── Plano_de_Projeto.docx
│   ├── Casos_de_Uso.docx
│   ├── Casos_de_Teste.docx
│   └── Estimativas_UCP.xlsx
└── src/
    ├── main/
    │   ├── java/com/mango/
    │   │   ├── Launcher.java     # entry point (evita JavaFX no module-path)
    │   │   ├── MangoApp.java     # Application JavaFX
    │   │   ├── config/           # constantes e parâmetros das regras de negócio
    │   │   ├── model/            # records imutáveis
    │   │   ├── net/              # cliente HTTP do MangaDex
    │   │   ├── db/               # H2: schema, cache, progresso, biblioteca
    │   │   ├── service/          # regras: Catalogo, Leitor, Biblioteca, Historico
    │   │   ├── ui/               # controllers JavaFX (+ Navegador)
    │   │   └── exception/        # exceções de domínio
    │   └── resources/
    │       ├── fxml/             # telas FXML
    │       ├── css/              # estilos
    │       └── db/schema.sql     # schema H2 (idempotente)
    └── test/java/com/mango/
        ├── service/              # testes unitários do Service
        └── repository/           # testes de integração
```

---

## Pré-requisitos

Antes de configurar o ambiente, certifique-se de ter instalado:

- **JDK 21** ([Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21) recomendado).
- **Apache Maven 3.9+** ([download](https://maven.apache.org/download.cgi)).
- **Git** ([download](https://git-scm.com/downloads)).
- **IDE recomendada:** IntelliJ IDEA Community ou VS Code com extensão Java Pack.

> **UC4 (trabalho futuro)** não exige nada instalado no cliente: a detecção de balão, o OCR e a tradução rodariam num **serviço externo** (backend estilo [FrankYomik](https://github.com/akitaonrails/FrankYomik), com GPU). O Mango só precisaria da **URL desse serviço**. Por isso os UCs 1–3 (entregues) não têm nenhuma dependência nativa.

Verifique a instalação:

```bash
java --version    # deve indicar 21
mvn --version     # deve indicar 3.9+
git --version
```

---

## Como configurar o ambiente

```bash
# 1. Clone o repositório
git clone https://github.com/<organizacao>/mango.git
cd mango

# 2. Baixe as dependências
mvn dependency:resolve

# 3. (Trabalho futuro — UC4) apontar a URL do serviço de tradução externo:
# cp .env.example .env
# edite .env e adicione: MANGO_TRADUCAO_URL=http://<host-do-servico-frankyomik>
```

---

## Como compilar e executar

### Build

```bash
mvn clean package
```

O artefato gerado fica em `target/mango-<versao>.jar`.

### Executar pela linha de comando

```bash
mvn javafx:run
```

Ou, após o build:

```bash
java --module-path "$PATH_TO_FX" --add-modules javafx.controls,javafx.fxml \
     -jar target/mango-1.0.0-SNAPSHOT.jar
```

> No primeiro start, o banco H2 é criado em `~/.mango/mango.mv.db`. As tabelas são versionadas pelos scripts em `src/main/resources/db/migrations/`.

### Executar pela IDE

1. Abra o projeto importando o `pom.xml`.
2. Configure a SDK do projeto para o **JDK 21**.
3. Execute a classe `com.mango.Main`.

---

## Como executar os testes

```bash
# Todos os testes
mvn test

# Apenas testes unitários do Service
mvn test -Dtest='*ServiceTest'

# Relatório de cobertura (a partir da Sprint 3, quando JaCoCo for adicionado)
mvn jacoco:report
# abra target/site/jacoco/index.html
```

Testes manuais devem ser executados conforme o documento [docs/Casos_de_Teste.docx](./docs/Casos_de_Teste.docx).

---

## Banco de dados

- **Engine:** H2 em modo arquivo.
- **Local:** `~/.mango/mango.mv.db` (criado automaticamente no primeiro start).
- **Console web (debug):** `http://localhost:8082` quando a aplicação roda em modo `dev`.
- **Migrações:** scripts SQL versionados em `src/main/resources/db/migrations/` (formato `V<numero>__<descricao>.sql`).

Para resetar a base local em desenvolvimento:

```bash
rm -rf ~/.mango
```

---

## Cronograma e roadmap

| Sprint        | Período             | Foco                                            | Entregáveis                                                                   |
|---------------|---------------------|-------------------------------------------------|-------------------------------------------------------------------------------|
| **Sprint 1**  | 05/05 – 11/05/2026  | Planejamento e estruturação                     | Documentos em `/docs`, repositório, README, Trello                            |
| **Sprint 2**  | 12/05 – 18/05/2026  | UC1 completo + parte do UC2                     | Catálogo funcional, esqueleto do leitor, 50% testado                          |
| **Sprint 3**  | 19/05 – 25/05/2026  | UC2 finalizado + UC3                            | Leitor completo, biblioteca pessoal, relatórios, 80% testado                  |
| **Sprint 4**  | 26/05 – 01/06/2026  | UC4 — Tradução automática                       | Pipeline OCR + tradução + overlay funcional                                   |
| **Sprint 5**  | 02/06 – 08/06/2026  | QA cruzado e refinamento                        | Testes unitários e de integração consolidados                                 |
| **Final**     | 09/06 – 15/06/2026  | Apresentação e documentação                     | Software 100%, documentação consolidada, apresentação                         |

---

## Convenções de desenvolvimento

### Git

Modelo **GitFlow simplificado**:

- `main` — código pronto para entrega; merge apenas no final de cada sprint.
- `develop` — branch de integração contínua durante a sprint.
- `feature/<descricao>` — uma branch por funcionalidade.
- `hotfix/<descricao>` — correções urgentes pós-entrega.

### Commits

Formato **Conventional Commits**:

```
feat: adiciona busca por gênero no catálogo
fix: corrige memory leak na pré-carga de páginas
docs: atualiza README com instruções de build
test: adiciona testes de unidade para ContatoService
refactor: extrai TraducaoService de MangaDexService
chore: atualiza versão do Jackson para 2.17
```

### Pull Requests

- Título descritivo + link do card do Trello na descrição.
- **Mínimo de 1 aprovação** do par oposto antes do merge.
- CI (a partir da Sprint 3) deve estar verde.
- Sem auto-merge.

### Estilo de código

- Java 21 com `records` quando o objeto for imutável.
- `@Override`, `final` em parâmetros e variáveis locais quando aplicável.
- Imports organizados; sem `*` imports.
- Tabela de exceções: nunca capturar `Exception` genérica; lance `<Dominio>Exception` específica.

---

## Trabalho futuro

### UC4 — Tradução automática com overlay (não implementado)

O diferencial originalmente planejado do Mango — detectar balões, reconhecer o texto, traduzir e renderizar o resultado por cima da página, em *overlay* — **foi especificado mas não entregue nesta versão**, e fica registrado como evolução futura. A arquitetura de referência é o [FrankYomik](https://github.com/akitaonrails/FrankYomik), de Fabio Akita.

#### Arquitetura proposta (modelo cliente ↔ serviço)

O trabalho pesado de visão computacional e tradução fica num **serviço externo** (com GPU); o Mango atua apenas como **cliente HTTP** que envia a imagem e desenha o resultado. Pipeline do serviço, espelhando o FrankYomik:

1. **Detecção de balões** com **RT-DETR-v2** (modelo treinado para quadrinhos), devolvendo as *bounding boxes*.
2. **OCR especializado**: **manga-ocr** para japonês (mangá) e **EasyOCR** para coreano (webtoon).
3. **Tradução** por **LLM local** (Ollama, ex.: `qwen3:14b`) ou API (DeepL/Google).
4. **Limpeza opcional** do texto original com *inpainting* (**LaMa**) antes de sobrepor.
5. O Mango recebe `(caixa, texto_traduzido)` e desenha um **overlay** em JavaFX (`Pane` sobre a `ImageView`), reescalando as caixas para o tamanho exibido.

#### Lições do protótipo (por que não foi entregue)

Um *spike* descartado tentou resolver tudo no cliente com **OCR genérico (OCR.space) + tradução free (MyMemory)** e mostrou, na prática, por que o UC4 é de alto risco (alinhado ao risco **R1** do Plano):

- **OCR genérico não serve para mangá**: fonte estilizada, texto vertical, onomatopeias e balões irregulares produzem reconhecimento incoerente. É preciso um OCR treinado em mangá (**manga-ocr**) e detecção dedicada de balão (**RT-DETR-v2**).
- **Premissa de idioma**: o leitor já carrega a versão **localizada** do capítulo (RN1.6: pt-br › en › original). O UC4 só faz sentido sobre o **capítulo original em japonês** — exigindo uma busca específica pelas páginas *raw*.
- **Custo de infraestrutura**: a qualidade do FrankYomik depende de **GPU** (o LLM de tradução pede ~9 GB de VRAM), inviável de embarcar no aplicativo desktop e fora do escopo de tempo da disciplina.

Por isso o UC4 permanece como trabalho futuro: a abordagem correta exige um serviço de ML dedicado, não um atalho no cliente.

### Outras evoluções mapeadas

- Testes de integração de repositório contra um H2 em memória (a pasta `src/test/.../repository` está reservada para isso).
- Modos de leitura adicionais (scroll vertical contínuo e página dupla) — hoje há apenas página única.
- Relatórios da biblioteca pessoal e cobertura com JaCoCo.
- Pipeline de CI no GitHub Actions rodando `mvn test` a cada PR.

---

## Documentação adicional

A documentação completa está na pasta [`/docs`](./docs):

| Documento                          | Descrição                                                       |
|------------------------------------|-----------------------------------------------------------------|
| `Plano_de_Projeto.docx`            | Visão geral, escopo, equipe, cronograma, riscos, processo ágil  |
| `Casos_de_Uso.docx`                | UC1–UC4 com fluxos, exceções e Regras de Negócio                |
| `Casos_de_Teste.docx`              | 33 casos de teste cobrindo todos os UCs e RNs                   |
| `Estimativas_UCP.xlsx`             | Cálculo Use Case Points com TCF, ECF e distribuição por sprint  |

---

## Equipe

| Papel              | Membro                  |
|--------------------|-------------------------|
| DEV                | Max Soares              |
| SCRUM MASTER/DEV   | Lorenzo Oliveira        |
| DEV                | Lucas Mendes            | 


---

## Licença

Projeto acadêmico desenvolvido para a disciplina de **Implementação de Software** — UNISINOS — 2026/1. Uso restrito a fins educacionais. As marcas, conteúdos e direitos autorais sobre os mangás exibidos pertencem aos seus respectivos detentores; o Mango apenas consome a API pública do MangaDex em tempo real.

---


