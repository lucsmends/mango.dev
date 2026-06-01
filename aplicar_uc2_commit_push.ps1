$ErrorActionPreference = "Stop"

function Parar($mensagem) {
    Write-Host ""
    Write-Host "ERRO: $mensagem" -ForegroundColor Red
    Write-Host ""
    Read-Host "Pressione Enter para sair"
    exit 1
}

function Rodar($comando, $argumentos) {
    Write-Host "> $comando $argumentos" -ForegroundColor DarkGray
    & $comando @argumentos
    if ($LASTEXITCODE -ne 0) {
        Parar "O comando falhou: $comando $argumentos"
    }
}

$PastaScript = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $PastaScript

Write-Host "Aplicador UC2 - Mango" -ForegroundColor Cyan
Write-Host "Este script aplica o UC2, cria commit e faz push usando o Git do seu computador."
Write-Host "Ele nao usa senha/token dentro do script. Se o Git pedir login, faca pelo navegador/janela oficial do Git."
Write-Host ""

if (!(Test-Path "pom.xml")) {
    Parar "Coloque estes arquivos na raiz do projeto mango.dev, na mesma pasta do pom.xml, e rode novamente."
}

if (!(Test-Path "mango-uc2.patch")) {
    Parar "Arquivo mango-uc2.patch nao encontrado na mesma pasta do script."
}

try {
    git --version | Out-Null
} catch {
    Parar "Git nao encontrado. Instale o Git ou abra pelo Git Bash/terminal da sua IDE."
}

git rev-parse --is-inside-work-tree | Out-Null
if ($LASTEXITCODE -ne 0) {
    Parar "Esta pasta nao parece ser um repositorio Git. Abra a pasta clonada do GitHub."
}

$mudancasRastreadas = git status --porcelain --untracked-files=no
if ($mudancasRastreadas) {
    Write-Host "Foram encontradas alteracoes ja existentes em arquivos rastreados:" -ForegroundColor Yellow
    git status --short --untracked-files=no
    Parar "Commit/guarde suas alteracoes antes de aplicar o UC2, para evitar conflito."
}

$nome = git config user.name
$email = git config user.email

if ([string]::IsNullOrWhiteSpace($nome)) {
    $nome = Read-Host "Digite seu nome para aparecer como autor do commit"
    if ([string]::IsNullOrWhiteSpace($nome)) { Parar "Nome do autor nao informado." }
    Rodar git @("config", "user.name", $nome)
}

if ([string]::IsNullOrWhiteSpace($email)) {
    $email = Read-Host "Digite seu e-mail do GitHub para aparecer como autor do commit"
    if ([string]::IsNullOrWhiteSpace($email)) { Parar "E-mail do autor nao informado." }
    Rodar git @("config", "user.email", $email)
}

Write-Host ""
Write-Host "Autor configurado neste repositorio:" -ForegroundColor Cyan
Write-Host "Nome : $(git config user.name)"
Write-Host "Email: $(git config user.email)"
Write-Host ""

Rodar git @("fetch", "origin")
Rodar git @("checkout", "main")
Rodar git @("pull", "origin", "main")

$branchBase = "implementa-uc2"
$branch = $branchBase
git show-ref --verify --quiet "refs/heads/$branch"
if ($LASTEXITCODE -eq 0) {
    $sufixo = Get-Date -Format "yyyyMMdd-HHmmss"
    $branch = "$branchBase-$sufixo"
}

Rodar git @("checkout", "-b", $branch)

Write-Host ""
Write-Host "Verificando patch..." -ForegroundColor Cyan
Rodar git @("apply", "--check", "--whitespace=nowarn", "mango-uc2.patch")

Write-Host "Aplicando patch..." -ForegroundColor Cyan
Rodar git @("apply", "--whitespace=nowarn", "mango-uc2.patch")

Write-Host ""
Write-Host "Arquivos alterados:" -ForegroundColor Cyan
git status --short
Write-Host ""

$mvnExiste = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvnExiste) {
    Write-Host "Compilando com Maven..." -ForegroundColor Cyan
    & mvn clean compile
    if ($LASTEXITCODE -ne 0) {
        Parar "A compilacao falhou. Corrija os erros antes de commitar/pushar."
    }
} else {
    Write-Host "Maven nao foi encontrado neste computador." -ForegroundColor Yellow
    $continuarSemMaven = Read-Host "Deseja continuar mesmo assim e criar o commit? Digite S para sim"
    if ($continuarSemMaven -ne "S" -and $continuarSemMaven -ne "s") {
        Parar "Processo cancelado antes do commit."
    }
}

Rodar git @("add", "COMO_APLICAR_UC2.md", "src")

$staged = git diff --cached --name-only
if (!$staged) {
    Parar "Nao ha alteracoes preparadas para commit."
}

Rodar git @("commit", "-m", "Implementa UC2 leitor de capitulos")
Rodar git @("push", "-u", "origin", $branch)

Write-Host ""
Write-Host "Concluido." -ForegroundColor Green
Write-Host "Branch enviada: $branch" -ForegroundColor Green
Write-Host "Abra o Pull Request aqui:" -ForegroundColor Cyan
Write-Host "https://github.com/lucsmends/mango.dev/pull/new/$branch"
Write-Host ""
Read-Host "Pressione Enter para sair"
