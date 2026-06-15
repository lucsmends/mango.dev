# Ata — Sprint Planning · Sprint 1

| Campo | Valor |
|-------|-------|
| **Sprint** | Sprint 1 — Planejamento e Estruturação |
| **Disciplina** | Implementação de Software — Unisinos — 2026/1 |
| **Data** | 05/05/2026 |
| **Horário** | 19:00 – 19:45 |
| **Duração** | 45 minutos |
| **Local** | [Discord / Sala de aula Unisinos] |
| **Facilitador** | Lorenzo Oliveira (Scrum Master) |
| **Secretário** | Max Soares |

## Participantes

| Membro | Papel na sprint | Presença |
|--------|-----------------|----------|
| Max Soares | Dev · Product Owner | ✅ |
| Lucas Mendes | Dev · Owner do repositório | ⬜ |
| Lorenzo Oliveira | Dev · Scrum Master | ⬜ |

## Sprint Goal

> Entregar todos os artefatos de planejamento e a estrutura inicial do projeto Mango, sem código de produção, garantindo que a equipe esteja pronta para iniciar a Sprint 2 com o UC1 — Catálogo e Busca.

## Itens do backlog selecionados para esta sprint

Total de **22 itens** distribuídos em 4 critérios:

- **Produto (4):** Casos de Uso, RNs, Roadmap MVP.
- **Processo (7):** Plano de Projeto, Estimativas, Trello, Cerimônias.
- **Configuração (8):** Repo, GitFlow, README, pom.xml, .gitignore, estrutura.
- **Qualidade (3):** Casos de Teste UC1, Checklist Sprint 1, Revisão cruzada.

A lista completa, com responsáveis e status, está em [`docs/Checklist_Sprint1.xlsx`](../Checklist_Sprint1.xlsx).

## Divisão de trabalho

Equipe de 3 desenvolvedores: a cada sprint, dois formam o **par da semana** (fatia vertical do UC) e o terceiro fica como **solo** (fatia complementar + revisão cruzada).

| Composição da Sprint 1 | Membros | Foco |
|---|---|---|
| **Par** | Max Soares + Lucas Mendes | Casos de Uso (UC1-UC4), Plano de Projeto, Estimativas, README |
| **Solo** | Lorenzo Oliveira | Configuração do repositório Git, pom.xml, estrutura de diretórios, .gitignore, Casos de Teste UC1 |

A rotação prevista para as próximas sprints:

| Sprint | Par | Solo |
|---|---|---|
| Sprint 2 | Lucas + Lorenzo | Max |
| Sprint 3 | Max + Lorenzo | Lucas |
| Sprint 4 | Max + Lucas | Lorenzo |

## Acordos do time

1. Cerimônias semanais conforme `CONTRIBUTING.md` §6.
2. Todos commitam ao menos 1x na semana (requisito #19).
3. Toda alteração via PR; sem push direto na `develop` ou `main`.
4. Comunicação principal via [Discord / WhatsApp do grupo].
5. Pareamento mínimo de 2x na semana via Meet/Discord.

## Riscos identificados

| Risco | Probabilidade | Mitigação |
|-------|---------------|-----------|
| Atraso na configuração do Git | Média | Configuração na primeira reunião; ajuda síncrona. |
| Subestimação do UC4 | Alta | UC4 fica na Sprint 4; redução de escopo planejada. |
| Conflitos de merge | Média | GitFlow + PRs cruzados + integração toda sexta. |
| **Carga semanal alta com 3 devs (~73 h/sem)** | **Alta** | Priorizar UC1-UC3 firmes; UC4 com escopo flexível; uso intensivo de pareamento e IA para acelerar. |

## Decisões

- ✅ Hospedagem do repositório: **GitHub** (`github.com/lucsmends/mango.dev`).
- ✅ Linguagem dos commits: **português** (Conventional Commits).
- ✅ Idioma do código e dos comentários: **português** para domínio, **inglês** para nomes de classes e métodos.
- ✅ IDE recomendada: **IntelliJ IDEA Community** (alternativa: VS Code com Java Pack).

## Próximos passos

| Ação | Responsável | Prazo |
|------|-------------|-------|
| Criar repositório no GitHub | Lucas | 05/05 |
| Adicionar Max e Lorenzo como Collaborators | Lucas | 05/05 |
| Configurar branch protection em `main` e `develop` | Lucas | 06/05 |
| Criar Trello e cards da Sprint 1 | Lorenzo | 06/05 |
| Primeira Daily | Equipe | 07/05 – 09:00 |

## Assinaturas

- Max Soares — _________________________
- Lucas Mendes — _________________________
- Lorenzo Oliveira — _________________________
