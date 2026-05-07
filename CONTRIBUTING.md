# Guia de Contribuição — Mango

Este documento define as regras de colaboração no repositório Mango. Todos os membros da equipe devem segui-las antes de abrir um Pull Request.

---

## 1. Modelo de Branches (GitFlow simplificado)

| Branch              | Origem    | Destino       | Propósito                                                                 |
|---------------------|-----------|---------------|---------------------------------------------------------------------------|
| `main`              | —         | —             | Versão estável, **protegida**. Recebe merge apenas no final de cada sprint via PR. |
| `develop`           | `main`    | `main`        | Integração contínua durante a sprint. Recebe PRs das `feature/*`.         |
| `feature/<nome>`    | `develop` | `develop`     | Uma por funcionalidade ou tarefa do Trello.                               |
| `hotfix/<nome>`     | `main`    | `main` + `develop` | Correções urgentes pós-entrega.                                      |
| `release/<versão>`  | `develop` | `main` + `develop` | Estabilização final antes de uma entrega (apenas Sprint 4).          |

### Convenção de nome de branch

```
feature/uc1-busca-por-titulo
feature/uc2-leitor-pagina-unica
hotfix/email-validacao
```

- Use **kebab-case** (palavras com `-`).
- Prefixe com o **UC** que a branch atende, quando aplicável.
- Mantenha **uma branch por card do Trello**.

---

## 2. Convenção de Commits — Conventional Commits

Todo commit deve seguir o padrão:

```
<tipo>(<escopo opcional>): <descrição curta no imperativo>
```

### Tipos aceitos

| Tipo       | Quando usar                                                              |
|------------|--------------------------------------------------------------------------|
| `feat`     | Nova funcionalidade visível ao usuário                                   |
| `fix`      | Correção de bug                                                          |
| `docs`     | Alteração apenas de documentação                                         |
| `test`     | Adição ou correção de testes                                             |
| `refactor` | Mudança no código sem alterar comportamento externo                      |
| `style`    | Formatação, espaços, vírgulas (sem mudar lógica)                         |
| `chore`    | Tarefas de manutenção (deps, build, configs)                             |
| `perf`     | Melhoria de performance                                                  |
| `ci`       | Configuração de integração contínua                                      |

### Exemplos

```
feat(uc1): adiciona filtro de gênero no catálogo
fix(uc2): corrige memory leak na pré-carga de páginas
docs: atualiza README com instruções de Tesseract
test(service): adiciona testes para MangaDexService
refactor(repository): extrai BaseRepository
chore: bumpa versão do Jackson para 2.17
```

### Regras adicionais

- Mensagem em **português ou inglês** (escolha um e mantenha consistente).
- Linha de assunto com **no máximo 72 caracteres**.
- Use o **imperativo** ("adiciona", não "adicionado").
- Sem ponto final no assunto.
- Corpo opcional, separado por linha em branco, explicando o **porquê**.

---

## 3. Fluxo de Pull Requests Cruzados

A regra principal do nosso processo é: **nenhum código entra na `develop` sem ser revisado pelo par oposto**. Isso garante que todos os membros conheçam o sistema inteiro e evita silos de conhecimento.

### Quem revisa o quê

| PR aberto por… | Revisado e aprovado por… |
|----------------|---------------------------|
| Par A          | Pelo menos 1 membro do **Par B** |
| Par B          | Pelo menos 1 membro do **Par A** |

> **Exceção:** PRs apenas de documentação ou `.gitignore`/configs podem ser aprovados por qualquer membro do par oposto (não exigem revisão profunda).

### Checklist do autor (antes de abrir o PR)

- [ ] Branch atualizada com a `develop` mais recente (`git pull origin develop && git rebase develop` ou merge).
- [ ] Build local passa (`mvn clean package`).
- [ ] Testes locais passam (`mvn test`).
- [ ] Sem warnings novos no compilador.
- [ ] Caso de teste correspondente foi executado manualmente.
- [ ] Título do PR segue Conventional Commits.
- [ ] Descrição do PR contém o **link do card do Trello** e o **escopo** da mudança.
- [ ] Marcou o membro do par oposto como **reviewer**.

### Template de descrição do PR

```markdown
## O que muda
Breve descrição (1-3 linhas) do que este PR entrega.

## Link do Trello
[Card #NN — Título do card](https://trello.com/c/abc123)

## Caso(s) de Uso e Regra(s) de Negócio
- UC: UC1
- RN: RN1.1, RN1.2

## Como testar
1. Passo 1
2. Passo 2
3. Resultado esperado

## Checklist
- [ ] Build passou
- [ ] Testes passaram
- [ ] Caso de teste manual executado
- [ ] Documentação atualizada (se aplicável)

## Capturas de tela (se UI)
```

### Checklist do revisor

- [ ] Código segue o padrão MVC + Service Layer (sem regra de negócio no Controller).
- [ ] Nomes claros, sem abreviações obscuras.
- [ ] Tratamento de exceções consistente (nada de `catch (Exception e)` genérico).
- [ ] Testes cobrem as RNs declaradas.
- [ ] Sem código morto, sem `System.out.println` (use logger).
- [ ] Sem credenciais ou chaves no código.
- [ ] Imports organizados, sem `*`.
- [ ] Se a UI mudou, o reviewer rodou o app e validou visualmente.

### Tempo máximo de revisão

- **24 horas** para PRs durante a semana (segunda a quinta).
- PRs abertos sexta podem aguardar até segunda — **mas** não devem bloquear a entrega; nesse caso, pedir revisão no canal do grupo.

### Política de aprovação

- ✅ **Approve** — código pronto para merge.
- 💬 **Comment** — sugestões não bloqueantes (autor decide aplicar).
- ❌ **Request changes** — bloqueia o merge; autor deve responder/ajustar.

### Como resolver conflitos de merge

```bash
git checkout feature/minha-branch
git fetch origin
git rebase origin/develop
# resolver conflitos nos arquivos
git add <arquivos>
git rebase --continue
git push --force-with-lease
```

Use sempre `--force-with-lease`, **nunca** `--force` puro (evita sobrescrever trabalho de terceiros).

---

## 4. Branch Protection (configurar no GitHub)

Aplicar nas branches `main` e `develop`:

- ✅ Require a pull request before merging
- ✅ Require approvals: **1 mínimo**
- ✅ Dismiss stale pull request approvals when new commits are pushed
- ✅ Require review from Code Owners (opcional)
- ✅ Require status checks to pass before merging (a partir da Sprint 3, quando houver CI)
- ✅ Require branches to be up to date before merging
- ✅ Require conversation resolution before merging
- ❌ Do not allow bypassing the above settings
- ❌ Do not allow force pushes
- ❌ Do not allow deletions

Onde configurar: **Settings → Branches → Add rule**.

---

## 5. Definition of Done (DoD)

Um item só é considerado **Concluído** quando atende a TODOS os critérios abaixo:

- [ ] Código implementado em branch `feature/*` criada a partir de `develop`.
- [ ] Caso de teste manual executado e passou.
- [ ] PR aberto com descrição completa.
- [ ] PR revisado e aprovado por pelo menos 1 membro do par oposto.
- [ ] PR mergeado em `develop` sem conflitos.
- [ ] Card do Trello movido para **Concluído** com link do PR.
- [ ] Documentação atualizada (README, Casos de Uso, Casos de Teste) quando aplicável.

---

## 6. Ritmo Semanal de Trabalho

| Dia         | Atividade                                          | Duração     |
|-------------|----------------------------------------------------|-------------|
| Segunda     | Sprint Planning + criação dos cards no Trello       | 45 min      |
| Terça a Quinta | Daily Stand-up + desenvolvimento em pares       | 10 min daily + pareamento |
| Sexta-feira (manhã) | Integração: merge das `feature/*` na `develop` + testes manuais | ~2h |
| Sexta-feira (tarde) | Sprint Review + Sprint Retrospective       | 60 min      |

---

## 7. Como ajudar quem está começando

Se você é o reviewer e o autor está com dificuldade, **prefira pareamento síncrono** a uma fila longa de comentários no PR. Cinco minutos no Meet costumam resolver o que dez idas e voltas de comentário levariam horas.

Lembre-se: o objetivo do PR cruzado **não é apenas filtrar bugs**, é **espalhar o conhecimento da arquitetura** por toda a equipe. Faça perguntas se algo não estiver claro — provavelmente outros terão a mesma dúvida no futuro.

---

> Em caso de dúvida sobre um ponto deste guia, abra uma Issue no repositório ou levante na próxima Daily.
