# Backlog do Trello — Sprint 1

> Modelo para configurar o board no Trello (`Backlog · A Fazer · Em Progresso · Revisão · Concluído`).
> Crie um card para cada item abaixo. Os cards seguem o padrão `[Critério] Título — UC/RN — Responsável`.

## Configuração do board

| Configuração | Valor |
|--------------|-------|
| **Nome do board** | Mango — Sprint 1 (Unisinos · 2026/1) |
| **Visibilidade** | Workspace (3 devs como membros) |
| **Colunas** | Backlog · A Fazer · Em Progresso · Revisão · Concluído |
| **Etiquetas (Labels)** | 🔵 Produto · 🟣 Processo · 🟠 Configuração · 🟢 Qualidade · 🔴 Bloqueado |
| **Membros** | Max Soares · Lucas Mendes · Lorenzo Oliveira |

## Composição da Sprint 1

| Função | Membros |
|--------|---------|
| **Par da semana** (fatia vertical do UC) | Max Soares + Lucas Mendes |
| **Solo da semana** (fatia complementar + revisão cruzada) | Lorenzo Oliveira |

## Cards (22 itens)

### 🔵 Produto

#### Card 1 — Levantar e listar todos os Casos de Uso
- **Lista inicial:** Concluído
- **Etiquetas:** Produto · UC · Escopo
- **Responsável:** Par (Max + Lucas)
- **Checklist:**
  - [x] UC1 — Catálogo e Busca
  - [x] UC2 — Leitor de Capítulos
  - [x] UC3 — Biblioteca, Histórico e Relatórios
  - [x] UC4 — Tradução Automática
- **Anexo:** `docs/Casos_de_Uso.docx`

#### Card 2 — Detalhar cada UC (atores, fluxos, exceções)
- **Lista inicial:** Concluído
- **Etiquetas:** Produto · Requisitos
- **Responsável:** Par (Max + Lucas)
- **Anexo:** `docs/Casos_de_Uso.docx`

#### Card 3 — Definir as Regras de Negócio (RN) de cada UC
- **Lista inicial:** Concluído
- **Etiquetas:** Produto · RN
- **Responsável:** Par (Max + Lucas)
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
- **Responsável:** Lorenzo (Scrum Master)
- **Checklist:**
  - [ ] Criar workspace
  - [ ] Adicionar Max, Lucas e Lorenzo como membros
  - [ ] Criar 5 colunas
  - [ ] Criar 22 cards (este documento)
  - [ ] Linkar URL no README

#### Card 8 — Sprint Planning
- **Lista inicial:** A Fazer
- **Etiquetas:** Processo · Cerimônia
- **Responsável:** Lorenzo (Scrum Master)
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
- **Responsável:** Lorenzo (Scrum Master)
- **Anexo:** `docs/atas/retro-sprint1.md`

### 🟠 Configuração de Software

#### Card 12 — Criar e configurar repositório no GitHub
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Git
- **Responsável:** Lucas (Owner do repo)
- **Checklist:**
  - [x] Repo público criado em `lucsmends/mango.dev`
  - [ ] 2 colaboradores adicionados (Max, Lorenzo)
  - [x] Branch protection na `main`
  - [x] URL linkada no README

#### Card 13 — Configurar GitFlow (main + develop)
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Git · GitFlow
- **Responsável:** Lucas

#### Card 14 — Documentar fluxo de PRs cruzados
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Git
- **Responsável:** Equipe
- **Anexo:** `CONTRIBUTING.md` §3

#### Card 15 — Estrutura inicial de diretórios (MVC + Service Layer)
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Arquitetura
- **Responsável:** Lorenzo (solo)
- **Anexo:** `src/main/java/com/mango/...`

#### Card 16 — Configurar pom.xml (Maven, Java 21, JavaFX, H2)
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Maven
- **Responsável:** Lorenzo (solo)
- **Anexo:** `pom.xml`

#### Card 17 — Elaborar README.md
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Docs
- **Responsável:** Max
- **Anexo:** `README.md`

#### Card 18 — Criar .gitignore
- **Lista inicial:** Concluído
- **Etiquetas:** Configuração · Git
- **Responsável:** Lorenzo (solo)
- **Anexo:** `.gitignore`

#### Card 19 — Commit inicial dos 3 devs
- **Lista inicial:** Em Progresso
- **Etiquetas:** Configuração · Git
- **Responsável:** Todos
- **Checklist:**
  - [x] Max Soares — 10+ commits
  - [x] Lucas Mendes — 1+ commit (Initial commit)
  - [ ] Lorenzo Oliveira — 1+ commit pendente

### 🟢 Qualidade

#### Card 20 — Elaborar Casos de Teste para UC1
- **Lista inicial:** Concluído
- **Etiquetas:** Qualidade · QA
- **Responsável:** Lorenzo (solo)
- **Anexo:** `docs/Casos_de_Teste.docx` (CT-001 a CT-010)

#### Card 21 — Checklist Sprint 1 com responsáveis e status
- **Lista inicial:** Concluído
- **Etiquetas:** Qualidade · Checklist
- **Responsável:** Equipe
- **Anexo:** `docs/Checklist_Sprint1.xlsx`

#### Card 22 — Revisão cruzada par ↔ solo
- **Lista inicial:** A Fazer
- **Etiquetas:** Qualidade · Revisão
- **Responsável:** Equipe
- **Checklist:**
  - [ ] Lorenzo (solo) validou Casos de Uso UC1-UC4 do Par (Max + Lucas)
  - [ ] Par (Max + Lucas) validou pom.xml, estrutura e Casos de Teste do Lorenzo
  - [ ] Evidência registrada em `docs/atas/revisao-cruzada-sprint1.md`

---

## Como importar rapidamente no Trello

1. Abra o Trello e crie um board chamado **"Mango — Sprint 1 (Unisinos · 2026/1)"**.
2. Crie as 5 colunas: Backlog, A Fazer, Em Progresso, Revisão, Concluído.
3. Crie as 5 etiquetas coloridas (Produto, Processo, Configuração, Qualidade, Bloqueado).
4. Para cada card acima:
   - Crie na coluna indicada em **"Lista inicial"**.
   - Adicione as etiquetas correspondentes.
   - Defina o membro responsável.
   - Cole o conteúdo do checklist na descrição.
5. Cole a URL do board no README.md.

> Dica: o Trello aceita **importação via CSV** em alguns planos. Se quiser, peça um CSV pronto neste formato.
