$ErrorActionPreference = "Stop"

function Read-RequiredText([string]$Prompt) {
    do {
        $value = Read-Host $Prompt
    } while ([string]::IsNullOrWhiteSpace($value))
    return $value.Trim()
}

function Read-RequiredPassword([string]$Prompt) {
    do {
        $secure = Read-Host $Prompt -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $value = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
        if ($value.Length -lt 12) {
            Write-Warning "A senha deve ter pelo menos 12 caracteres."
        }
    } while ($value.Length -lt 12)
    return $value
}

$operatorUser = Read-RequiredText "Usuário do operador"
$adminUser = Read-RequiredText "Usuário do administrador"
if ($operatorUser.Equals($adminUser, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Os usuários operador e administrador devem ser diferentes."
}

$operatorPassword = Read-RequiredPassword "Senha do operador"
do {
    $adminPassword = Read-RequiredPassword "Senha do administrador"
    if ($operatorPassword.Equals($adminPassword, [StringComparison]::Ordinal)) {
        Write-Warning "As senhas do operador e do administrador devem ser diferentes."
    }
} while ($operatorPassword.Equals($adminPassword, [StringComparison]::Ordinal))

[Environment]::SetEnvironmentVariable("APP_USER_USERNAME", $operatorUser, "User")
[Environment]::SetEnvironmentVariable("APP_USER_PASSWORD", $operatorPassword, "User")
[Environment]::SetEnvironmentVariable("APP_ADMIN_USERNAME", $adminUser, "User")
[Environment]::SetEnvironmentVariable("APP_ADMIN_PASSWORD", $adminPassword, "User")

if (-not ([System.Management.Automation.PSTypeName]"NativeEnvironment").Type) {
    Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class NativeEnvironment {
    [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Auto)]
    public static extern IntPtr SendMessageTimeout(
        IntPtr hWnd, uint message, UIntPtr wParam, string lParam,
        uint flags, uint timeout, out UIntPtr result);
}
"@
}

$broadcast = [IntPtr]0xffff
$settingChange = 0x001A
$abortIfHung = 0x0002
$result = [UIntPtr]::Zero
[void][NativeEnvironment]::SendMessageTimeout(
    $broadcast, $settingChange, [UIntPtr]::Zero, "Environment",
    $abortIfHung, 5000, [ref]$result)

$operatorPassword = $null
$adminPassword = $null
Write-Host "Credenciais configuradas para o usuário atual."
