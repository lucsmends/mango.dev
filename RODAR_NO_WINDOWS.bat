@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aplicar_uc2_commit_push.ps1"
pause
