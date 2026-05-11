# Backlog do Trello — Sprint 1

> Modelo para configurar o board no Trello (`Backlog · A Fazer · Em Progresso · Revisão · Concluído`).
> Crie um card para cada item abaixo. Os cards seguem o padrão `[Critério] Título — UC/RN — Responsável`.

## Configuração do board

| Configuração | Valor |
|--------------|-------|
| **Nome do board** | Mango — Sprint 1 |
| **Visibilidade** | Workspace (todos os 4 devs como membros) |
| **Colunas** | Backlog · A Fazer · Em Progresso · Revisão · Concluído |
| **Etiquetas (Labels)** | 🔵 Produto · 🟣 Processo · 🟠 Configuração · 🟢 Qualidade · 🔴 Bloqueado |

## Cards (22 itens)

### 🔵 Produto

#### Card 1 — Levantar e listar todos os Casos de Uso
- **Lista inicial:** Concluído
- **Etiquetas:** Produto · UC · Escopo
- **Responsável:** Equipe
- **Checklist:**
  - [x] UC1 — Catálogo e Busca
  - [x] UC2 — Leitor de Capítulos
  - [x] UC3 — Biblioteca, Histórico e Relatórios
  - [x] UC4 — Tradução Automática
- **Anexo:** `docs/Casos_de_Uso.docx`

#### Card 2 — Detalhar cada UC (atores, fluxos, exceções)
- **Lista inicial:** Concluído
- **Etiquetas:** Produto · Requisitos
- **Responsável:** Equipe (Par A: UC1+UC2 · Par B: UC3+UC4)
- **Anexo:** `docs/Casos_de_Uso.docx`

#### Card 3 — Definir as Regras de Negócio (RN) de cada UC
- **Lista inicial:** Concluído
- **Etiquetas:** Produto · RN
- **Responsável:** Equipe
- **Anexo:** `docs/Casos_de_Uso.docx` (apêndice de rastreabilidade)

#### Card 4 — Definir escopo do MVP por sprint
- **Lista inicial:** Concluído
- **Etiquetas:** Produto · MVP
- **Responsável:** Max
- **Anexo:** `docs/Plano_de_Projeto.docx` §8 + `docs/Estimativas_UCP.xlsx`

### 🟣 Processo

#### Card 5 — Criar Plano de Projeto
- **Lista inicial:** Concluído
- **Etiquetas:** Processo · Scrum
- **Responsável:** Max
- **Anexo:** `docs/Plano_de_Projeto.docx`

#### Card 6 — Montar planilha de estimativas UCP
- **Lista inicial:** Concluído
- **Etiquetas:** Processo · Estimativa
- **Responsável:** Max
- **Anexo:** `docs/Estimativas_UCP.xlsx`

#### Card 7 — Configurar Trello da Sprint 1
- **Lista inicial:** Em Progresso
- **Etiquetas:** Processo · Trello
- **Responsável:** Scrum Master
- **Checklist:**
  - [ ] Criar workspace
  - [ ] Adicionar 4 membros
  - [ ] Criar 5 colunas
  - [ ] Criar 22 cards (este documento)
  - [ ] Linkar URL no README

#### Card 8 — Sprint Planning
- **Lista inicial:** A Fazer
- **Etiquetas:** Processo · Cerimônia
- **Responsável:** Scrum Master
- **Anexo:** `docs/atas/planning-sprint1.md`

#### Card 9 — Dailies da Sprint 1
- **Lista inicial:** A Fazer (recorrente)
- **Etiquetas:** Processo · Daily
- **Responsável:** Equipe (rotativo)
- **Anexo:** `docs/atas/dailies-sprint1.md`

#### Card 10 — Sprint Review
- **Lista inicial:** A Fazer
- **Etiquetas:** Processo · Cerimônia
- **Responsável:** Equipe
- **Anexo:** `docs/atas/review-sprint1.md`

#### Card 11 — Sprint Retrospective
- **Lista inicial:** A Fazer
- **Etiquetas:** Processo · Cerimônia
- **Responsável:** Scrum Master
- **Anexo:** `docs/atas/retro-sprint1.md`

### 🟠 Configuração de Software

#### Card 12 — Criar e configurar repositório no GitHub
- **Lista inicial:** Em Progresso
- **Etiquetas:** Configuração · Git
- **Responsável:** Max
- **Checklist:**
  - [ ] Repo público criado
  - [ ] 4 colaboradores adicionados
  - [ ] Branch protection na `main` e `develop`
  - [ ] URL linkada no README

#### Card 13 — Configurar GitFlow (main + develop)
- **Lista inicial:** A Fazer
- **Etiquetas:** Configuração · Git · GitFlow
- **Responsável:** Max

#### Card 14 — Documentar fluxo de PRs cruzados
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Git
- **Responsável:** Equipe
- **Anexo:** `CONTRIBUTING.md` §3

#### Card 15 — Estrutura inicial de diretórios (MVC + Service Layer)
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Arquitetura
- **Responsável:** Equipe
- **Anexo:** `src/main/java/com/mango/...`

#### Card 16 — Configurar pom.xml (Maven, Java 21, JavaFX, H2)
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Maven
- **Responsável:** Equipe
- **Anexo:** `pom.xml`

#### Card 17 — Elaborar README.md
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Docs
- **Responsável:** Max
- **Anexo:** `README.md`

#### Card 18 — Criar .gitignore
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Git
- **Responsável:** Equipe
- **Anexo:** `.gitignore`

#### Card 19 — Commit inicial de todos os 4 devs
- **Lista inicial:** Em Progresso
- **Etiquetas:** Configuração · Git
- **Responsável:** Todos
- **Checklist:**
  - [ ] Max — 1+ commit
  - [ ] [Dev 2] — 1+ commit
  - [ ] [Dev 3] — 1+ commit
  - [ ] [Dev 4] — 1+ commit

### 🟢 Qualidade

#### Card 20 — Elaborar Casos de Teste para UC1
- **Lista inicial:** Concluído
- **Etiquetas:** Qualidade · QA
- **Responsável:** Equipe
- **Anexo:** `docs/Casos_de_Teste.docx` (CT-001 a CT-010)

#### Card 21 — Checklist Sprint 1 com responsáveis e status
- **Lista inicial:** Concluído
- **Etiquetas:** Qualidade · Checklist
- **Responsável:** Equipe
- **Anexo:** `docs/Checklist_Sprint1.xlsx`

#### Card 22 — Revisão cruzada Par A ↔ Par B
- **Lista inicial:** A Fazer
- **Etiquetas:** Qualidade · Revisão
- **Responsável:** Pares
- **Checklist:**
  - [ ] Par B validou Casos de Uso UC1+UC2 do Par A
  - [ ] Par A validou Casos de Uso UC3+UC4 do Par B
  - [ ] Par B validou Plano de Projeto e README do Par A
  - [ ] Par A validou Estimativas e pom.xml do Par B
  - [ ] Evidência registrada em `docs/atas/revisao-cruzada-sprint1.md`

---

## Como importar rapidamente no Trello

1. Abra o Trello e crie um board chamado **"Mango — Sprint 1"**.
2. Crie as 5 colunas: Backlog, A Fazer, Em Progresso, Revisão, Concluído.
3. Crie as 5 etiquetas coloridas (Produto, Processo, Configuração, Qualidade, Bloqueado).
4. Para cada card acima:
   - Crie na coluna indicada em **"Lista inicial"**.
   - Adicione as etiquetas correspondentes.
   - Defina o membro responsável.
   - Cole o conteúdo do checklist na descrição.
5. Cole a URL do board no README.md.

> Dica: o Trello aceita **importação via CSV** em alguns planos. Se quiser, peça um CSV pronto neste formato.
