@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

echo ========================================================
echo   Instalando ControleServico...
echo ========================================================

set "HASH_FILE="
for /f "delims=" %%F in ('dir /b /a-d /o-d "%~dp0ControleServico-*.msi.sha256" 2^>nul') do if not defined HASH_FILE set "HASH_FILE=%~dp0%%F"
if not defined HASH_FILE (
    echo [ERRO] Nenhum checksum ControleServico-*.msi.sha256 foi encontrado.
    pause
    exit /b 1
)

set "MSI_FILE=%HASH_FILE:.sha256=%"
if not exist "%MSI_FILE%" (
    echo [ERRO] O MSI correspondente nao foi encontrado: "%MSI_FILE%"
    pause
    exit /b 1
)

for /f "tokens=1" %%H in ('type "%HASH_FILE%"') do set "EXPECTED_HASH=%%H"
for /f "delims=" %%H in ('powershell.exe -NoLogo -NoProfile -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath $env:MSI_FILE).Hash"') do set "ACTUAL_HASH=%%H"
if /i not "%EXPECTED_HASH%"=="%ACTUAL_HASH%" (
    echo [ERRO] A verificacao de integridade do MSI falhou.
    pause
    exit /b 1
)

echo Instalador selecionado: "%MSI_FILE%"
echo Checksum SHA-256 validado.
echo Solicitando privilegios de administrador somente para o Windows Installer...
powershell.exe -NoLogo -NoProfile -Command "$argsLine = '/i ' + [char]34 + $env:MSI_FILE + [char]34; $p = Start-Process -FilePath 'msiexec.exe' -ArgumentList $argsLine -Verb RunAs -Wait -PassThru; exit $p.ExitCode"
set "MSI_EXIT=%ERRORLEVEL%"

if "%MSI_EXIT%"=="0" goto :configure
if "%MSI_EXIT%"=="1641" goto :reboot
if "%MSI_EXIT%"=="3010" goto :reboot
echo [ERRO] A instalacao falhou ou foi cancelada. Codigo: %MSI_EXIT%
pause
exit /b %MSI_EXIT%

:reboot
echo [AVISO] Instalacao concluida. O Windows solicitou uma reinicializacao.

:configure
echo Configurando as credenciais para o usuario atual...
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0configurar-credenciais.ps1"
if errorlevel 1 (
    echo [ERRO] O aplicativo foi instalado, mas as credenciais nao foram configuradas.
    echo Execute configurar-credenciais.ps1 antes de iniciar o aplicativo.
    pause
    exit /b 1
)

echo [OK] Instalacao e configuracao concluidas.
pause
exit /b 0
