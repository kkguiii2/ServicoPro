@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title Gerador do instalador - ControleServico

echo ========================================================
echo   GERADOR DO INSTALADOR CONTROLESERVICO
echo ========================================================
echo.

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0gerar-instalador.ps1" -NoPause
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if not "%EXIT_CODE%"=="0" (
    echo [ERRO] O instalador nao foi gerado.
    echo Consulte a mensagem acima para corrigir o problema.
) else (
    echo [OK] Processo concluido. Os arquivos JAR e MSI estao nesta pasta.
)
echo.
pause
exit /b %EXIT_CODE%
