$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $projectDir "gerar-instalador.ps1"
$content = Get-Content -LiteralPath $scriptPath -Raw

function Assert-ScriptContains([string]$Pattern, [string]$Message) {
    if ($content -notmatch $Pattern) {
        throw $Message
    }
}

function Assert-ScriptOrder([string[]]$Fragments) {
    $previousIndex = -1
    foreach ($fragment in $Fragments) {
        $currentIndex = $content.IndexOf($fragment, [StringComparison]::Ordinal)
        if ($currentIndex -le $previousIndex) {
            throw "Etapa ausente ou fora de ordem: $fragment"
        }
        $previousIndex = $currentIndex
    }
}

Assert-ScriptContains '\$artifactId\s*=\s*\[string\]\$pom\.project\.artifactId' `
    "O script deve obter o artifactId diretamente do pom.xml."
Assert-ScriptContains 'project\.build\.finalName' `
    "O script deve obter do Maven o nome efetivo do JAR."
Assert-ScriptContains 'Remove-Item\s+-LiteralPath\s+\$publishedArtifact\s+-Force' `
    "O script deve remover artefatos publicados de execucoes anteriores."
Assert-ScriptContains 'Copy-Item\s+-LiteralPath\s+\$expectedJar\s+-Destination\s+\$finalJar' `
    "O script deve publicar o JAR novo na pasta do projeto."
Assert-ScriptContains 'Copy-Item\s+-LiteralPath\s+\$finalJar\s+-Destination\s+\$inputDir' `
    "O instalador deve receber exatamente o JAR novo publicado."
Assert-ScriptContains 'BOOT-INF/classes/' `
    "O script deve validar que o JAR gerado e executavel pelo Spring Boot."
Assert-ScriptContains '\^Main-Class:\\s\+\\S\+' `
    "O script deve validar a classe inicializadora no manifesto do JAR."
Assert-ScriptContains '\^Start-Class:\\s\+\\S\+' `
    "O script deve validar a classe principal da aplicacao no manifesto do JAR."
Assert-ScriptContains '"--main-jar", \(Split-Path -Leaf \$finalJar\)' `
    "O jpackage deve iniciar pelo JAR novo publicado."

Assert-ScriptOrder @(
    'Invoke-Native $maven @("-B", "clean", "package")',
    'Copy-Item -LiteralPath $expectedJar -Destination $finalJar -Force',
    'Copy-Item -LiteralPath $finalJar -Destination $inputDir',
    'Invoke-Native $jpackage $jpackageArgs'
)

Write-Host "[OK] Fluxo JAR -> instalador validado." -ForegroundColor Green
