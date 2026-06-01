#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo ""
  echo "ERRO: $1" >&2
  echo ""
  exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Aplicador UC2 - Mango"
echo "Este script aplica o UC2, cria commit e faz push usando o Git do seu computador."
echo "Ele nao usa senha/token dentro do script. Se o Git pedir login, faca pela janela oficial do Git."
echo ""

[[ -f pom.xml ]] || fail "Coloque estes arquivos na raiz do projeto mango.dev, na mesma pasta do pom.xml, e rode novamente."
[[ -f mango-uc2.patch ]] || fail "Arquivo mango-uc2.patch nao encontrado na mesma pasta do script."

command -v git >/dev/null 2>&1 || fail "Git nao encontrado."
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "Esta pasta nao parece ser um repositorio Git."

if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  echo "Foram encontradas alteracoes ja existentes em arquivos rastreados:"
  git status --short --untracked-files=no
  fail "Commit/guarde suas alteracoes antes de aplicar o UC2, para evitar conflito."
fi

if [[ -z "$(git config user.name || true)" ]]; then
  read -r -p "Digite seu nome para aparecer como autor do commit: " nome
  [[ -n "$nome" ]] || fail "Nome do autor nao informado."
  git config user.name "$nome"
fi

if [[ -z "$(git config user.email || true)" ]]; then
  read -r -p "Digite seu e-mail do GitHub para aparecer como autor do commit: " email
  [[ -n "$email" ]] || fail "E-mail do autor nao informado."
  git config user.email "$email"
fi

echo ""
echo "Autor configurado neste repositorio:"
echo "Nome : $(git config user.name)"
echo "Email: $(git config user.email)"
echo ""

git fetch origin
git checkout main
git pull origin main

branch_base="implementa-uc2"
branch="$branch_base"
if git show-ref --verify --quiet "refs/heads/$branch"; then
  branch="$branch_base-$(date +%Y%m%d-%H%M%S)"
fi

git checkout -b "$branch"

echo ""
echo "Verificando patch..."
git apply --check --whitespace=nowarn mango-uc2.patch

echo "Aplicando patch..."
git apply --whitespace=nowarn mango-uc2.patch

echo ""
echo "Arquivos alterados:"
git status --short
echo ""

if command -v mvn >/dev/null 2>&1; then
  echo "Compilando com Maven..."
  mvn clean compile
else
  echo "Maven nao foi encontrado neste computador."
  read -r -p "Deseja continuar mesmo assim e criar o commit? Digite S para sim: " continuar
  [[ "$continuar" == "S" || "$continuar" == "s" ]] || fail "Processo cancelado antes do commit."
fi

git add COMO_APLICAR_UC2.md src

if [[ -z "$(git diff --cached --name-only)" ]]; then
  fail "Nao ha alteracoes preparadas para commit."
fi

git commit -m "Implementa UC2 leitor de capitulos"
git push -u origin "$branch"

echo ""
echo "Concluido."
echo "Branch enviada: $branch"
echo "Abra o Pull Request aqui:"
echo "https://github.com/lucsmends/mango.dev/pull/new/$branch"
