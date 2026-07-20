param(
    [switch]$NoPause
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot
$appName = "ControleServico"
$productName = "Controle de Servico"
$vendor = "Gestao TI"
$upgradeUuid = "5EA3A3B5-28E9-4B81-A587-4070DF8D7598"
$wixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip"
$wixSha256 = "6AC824E1642D6F7277D0ED7EA09411A508F6116BA6FAE0AA5F2C7DAA2FF43D31"

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "[....] $Message" -ForegroundColor Cyan
}

function Find-Executable([string[]]$Names) {
    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }
    return $null
}

function Find-JPackage {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += Join-Path $env:JAVA_HOME "bin\jpackage.exe"
    }
    $candidates += Get-ChildItem -Path "$env:ProgramFiles\Java\jdk-*\bin\jpackage.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | ForEach-Object FullName
    $candidates += Get-ChildItem -Path "$env:ProgramFiles\Eclipse Adoptium\jdk-*\bin\jpackage.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | ForEach-Object FullName

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            return $candidate
        }
    }

    $jpackage = Find-Executable @("jpackage.exe", "jpackage")
    if ($jpackage) {
        return $jpackage
    }
    return $null
}

function Invoke-Native([string]$Executable, [string[]]$Arguments, [string]$FailureMessage) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FailureMessage Codigo de saida: $LASTEXITCODE"
    }
}

function Ensure-Wix {
    $candle = Find-Executable @("candle.exe")
    $light = Find-Executable @("light.exe")
    if ($candle -and $light) {
        $wixBin = Split-Path -Parent $candle
        $env:PATH = "$wixBin;$env:PATH"
        Write-Host "[OK] WiX encontrado em: $wixBin" -ForegroundColor Green
        return
    }

    $toolsDir = Join-Path $projectDir ".tools"
    $downloadsDir = Join-Path $toolsDir "downloads"
    $wixDir = Join-Path $toolsDir "wix314"
    $wixArchive = Join-Path $downloadsDir "wix314-binaries.zip"
    New-Item -ItemType Directory -Path $downloadsDir -Force | Out-Null

    $validArchive = $false
    if (Test-Path -LiteralPath $wixArchive -PathType Leaf) {
        $archiveHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $wixArchive).Hash
        $validArchive = $archiveHash -eq $wixSha256
        if (-not $validArchive) {
            Remove-Item -LiteralPath $wixArchive -Force
        }
    }

    if (-not $validArchive) {
        Write-Step "Baixando WiX 3.14.1 (necessario para criar MSI)"
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -UseBasicParsing -Uri $wixUrl -OutFile $wixArchive
        $archiveHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $wixArchive).Hash
        if ($archiveHash -ne $wixSha256) {
            Remove-Item -LiteralPath $wixArchive -Force
            throw "O checksum do WiX baixado e invalido. Download removido por seguranca."
        }
    }

    if (Test-Path -LiteralPath $wixDir) {
        Remove-Item -LiteralPath $wixDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $wixDir -Force | Out-Null
    Expand-Archive -LiteralPath $wixArchive -DestinationPath $wixDir -Force

    $candleFiles = @(Get-ChildItem -LiteralPath $wixDir -Filter "candle.exe" -Recurse)
    $lightFiles = @(Get-ChildItem -LiteralPath $wixDir -Filter "light.exe" -Recurse)
    if (($candleFiles.Count -eq 0) -or ($lightFiles.Count -eq 0)) {
        throw "O pacote do WiX foi extraido, mas candle.exe/light.exe nao foram encontrados."
    }

    $wixBin = $candleFiles[0].Directory.FullName
    $env:PATH = "$wixBin;$env:PATH"
    Write-Host "[OK] WiX preparado em: $wixBin" -ForegroundColor Green
}

try {
    if ($env:OS -ne "Windows_NT") {
        throw "A geracao MSI deve ser executada no Windows."
    }

    Set-Location -LiteralPath $projectDir
    $pomPath = Join-Path $projectDir "pom.xml"
    $iconPath = Join-Path $projectDir "setting.ico"
    $pngPath = Join-Path $projectDir "setting.png"

    foreach ($requiredFile in @($pomPath, $iconPath, $pngPath)) {
        if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "Arquivo obrigatorio nao encontrado: $requiredFile"
        }
        if ((Get-Item -LiteralPath $requiredFile).Length -eq 0) {
            throw "Arquivo obrigatorio esta vazio: $requiredFile"
        }
    }

    [xml]$pom = Get-Content -LiteralPath $pomPath -Raw
    $artifactId = [string]$pom.project.artifactId
    $version = [string]$pom.project.version
    if ([string]::IsNullOrWhiteSpace($artifactId) -or [string]::IsNullOrWhiteSpace($version)) {
        throw "Nao foi possivel ler o artifactId e a versao do pom.xml."
    }

    $maven = Find-Executable @("mvn.cmd", "mvn.exe", "mvn")
    if (-not $maven) {
        throw "Maven nao encontrado. Instale o Maven 3.9+ e adicione seu bin ao PATH."
    }
    $jpackage = Find-JPackage
    if (-not $jpackage) {
        throw "jpackage nao encontrado. Instale um JDK 17+ e configure JAVA_HOME/PATH."
    }

    $jdkHome = Split-Path -Parent (Split-Path -Parent $jpackage)
    $java = Join-Path $jdkHome "bin\java.exe"
    $jarTool = Join-Path $jdkHome "bin\jar.exe"
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
        throw "O java.exe correspondente ao jpackage nao foi encontrado em: $java"
    }
    if (-not (Test-Path -LiteralPath $jarTool -PathType Leaf)) {
        throw "O jar.exe correspondente ao jpackage nao foi encontrado em: $jarTool"
    }
    $jpackageVersionOutput = & $jpackage --version 2>&1
    $jpackageVersionExitCode = $LASTEXITCODE
    $jpackageVersionText = ($jpackageVersionOutput | Select-Object -First 1).ToString().Trim()
    if ($jpackageVersionExitCode -ne 0 -or $jpackageVersionText -notmatch '^(\d+)') {
        throw "Nao foi possivel determinar a versao do jpackage."
    }
    if ([int]$Matches[1] -lt 17) {
        throw "JDK 17+ obrigatorio. Versao encontrada: $jpackageVersionText"
    }
    $env:JAVA_HOME = $jdkHome
    $env:PATH = "$(Join-Path $jdkHome 'bin');$env:PATH"

    Write-Host "[OK] Maven: $maven" -ForegroundColor Green
    Write-Host "[OK] jpackage: $jpackage" -ForegroundColor Green
    Write-Host "[OK] JDK: $jpackageVersionText ($jdkHome)" -ForegroundColor Green
    Write-Host "[OK] Versao: $version" -ForegroundColor Green
    Write-Host "[OK] Icones: setting.ico e setting.png" -ForegroundColor Green

    $effectiveFinalNameOutput = & $maven "-q" "help:evaluate" "-Dexpression=project.build.finalName" "-DforceStdout" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "O Maven nao conseguiu determinar o nome efetivo do JAR."
    }
    $effectiveFinalName = ($effectiveFinalNameOutput | Select-Object -Last 1).ToString().Trim()
    if ([string]::IsNullOrWhiteSpace($effectiveFinalName)) {
        throw "O Maven retornou um nome vazio para o JAR."
    }

    $targetDir = Join-Path $projectDir "target"
    $workDir = Join-Path $projectDir ".installer"
    $inputDir = Join-Path $workDir "input"
    $tempDir = Join-Path $workDir "jpackage-temp"
    $outputDir = Join-Path $projectDir "dist\installer"
    $finalJar = Join-Path $projectDir "$appName-$version.jar"
    $finalMsi = Join-Path $projectDir "$appName-$version.msi"
    $hashFile = "$finalMsi.sha256"

    foreach ($publishedArtifact in @($finalJar, $finalMsi, $hashFile)) {
        if (Test-Path -LiteralPath $publishedArtifact) {
            Remove-Item -LiteralPath $publishedArtifact -Force
        }
    }
    foreach ($directory in @($workDir, $outputDir)) {
        if (Test-Path -LiteralPath $directory) {
            Remove-Item -LiteralPath $directory -Recurse -Force
        }
    }

    Write-Step "Gerando o novo JAR da aplicacao e executando os testes"
    Invoke-Native $maven @("-B", "clean", "package") "O Maven nao conseguiu gerar a aplicacao."

    $expectedJar = Join-Path $targetDir "$effectiveFinalName.jar"
    if (-not (Test-Path -LiteralPath $expectedJar -PathType Leaf)) {
        throw "O Maven terminou sem criar o JAR esperado: $expectedJar"
    }
    if ((Get-Item -LiteralPath $expectedJar).Length -eq 0) {
        throw "O JAR gerado esta vazio: $expectedJar"
    }

    $jarEntries = @(& $jarTool "--list" "--file" $expectedJar)
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel validar o conteudo do JAR gerado."
    }
    $hasManifest = $jarEntries -contains "META-INF/MANIFEST.MF"
    $hasBootClasses = @($jarEntries | Where-Object { $_.StartsWith("BOOT-INF/classes/") }).Count -gt 0
    if (-not $hasManifest -or -not $hasBootClasses) {
        throw "O artefato gerado nao e um JAR executavel do Spring Boot: $expectedJar"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jarArchive = [System.IO.Compression.ZipFile]::OpenRead($expectedJar)
    $manifestReader = $null
    $manifestContent = ""
    try {
        $manifestEntry = $jarArchive.GetEntry("META-INF/MANIFEST.MF")
        if (-not $manifestEntry) {
            throw "O manifesto nao foi encontrado no JAR gerado."
        }
        $manifestReader = [System.IO.StreamReader]::new($manifestEntry.Open())
        $manifestContent = $manifestReader.ReadToEnd()
    }
    finally {
        if ($manifestReader) {
            $manifestReader.Dispose()
        }
        $jarArchive.Dispose()
    }
    if ($manifestContent -notmatch '(?m)^Main-Class:\s+\S+' -or
        $manifestContent -notmatch '(?m)^Start-Class:\s+\S+') {
        throw "O manifesto do JAR nao define Main-Class e Start-Class."
    }

    Copy-Item -LiteralPath $expectedJar -Destination $finalJar -Force
    Write-Host "[OK] Novo JAR gerado: $finalJar" -ForegroundColor Green

    Ensure-Wix

    New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

    $jcefDependency = @($pom.project.dependencies.dependency |
        Where-Object { $_.artifactId -eq "jcefmaven" })
    if ($jcefDependency.Count -ne 1 -or [string]::IsNullOrWhiteSpace([string]$jcefDependency[0].version)) {
        throw "Nao foi possivel determinar a versao do jcefmaven no pom.xml."
    }
    $jcefVersion = [string]$jcefDependency[0].version
    $jcefCacheDir = Join-Path $projectDir ".tools\jcef-$jcefVersion-$env:PROCESSOR_ARCHITECTURE"
    $classPathFile = Join-Path $targetDir "jcef-classpath.txt"
    $requiredJcefFiles = @("build_meta.json", "libcef.dll", "jcef.dll", "jcef_helper.exe", "icudtl.dat")
    if (Test-Path -LiteralPath $jcefCacheDir) {
        $invalidCache = $requiredJcefFiles | Where-Object {
            -not (Test-Path -LiteralPath (Join-Path $jcefCacheDir $_) -PathType Leaf)
        }
        if ($invalidCache -or -not (Test-Path -LiteralPath (Join-Path $jcefCacheDir "locales") -PathType Container)) {
            Write-Warning "O cache JCEF esta incompleto e sera recriado."
            Remove-Item -LiteralPath $jcefCacheDir -Recurse -Force
        }
    }

    Write-Step "Preparando o runtime JCEF para uso offline"
    Invoke-Native $maven @("-B", "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath", "-Dmdep.outputFile=target/jcef-classpath.txt") `
        "O Maven nao conseguiu montar o classpath do JCEF."
    $dependencyClassPath = (Get-Content -LiteralPath $classPathFile -Raw).Trim()
    if ([string]::IsNullOrWhiteSpace($dependencyClassPath)) {
        throw "O classpath do JCEF foi gerado vazio."
    }
    $runtimeClassPath = "$(Join-Path $targetDir 'classes');$dependencyClassPath"
    Invoke-Native $java @(
        "-cp", $runtimeClassPath,
        "com.empresa.controleservico.JcefRuntimeInstaller",
        $jcefCacheDir
    ) "Nao foi possivel preparar o runtime offline do JCEF."

    Copy-Item -LiteralPath $finalJar -Destination $inputDir
    Copy-Item -LiteralPath $pngPath -Destination $inputDir
    Copy-Item -LiteralPath $jcefCacheDir -Destination (Join-Path $inputDir "jcef-runtime") -Recurse

    Write-Step "Gerando o pacote MSI"
    $jpackageArgs = @(
        "--type", "msi",
        "--input", $inputDir,
        "--dest", $outputDir,
        "--temp", $tempDir,
        "--name", $appName,
        "--app-version", $version,
        "--vendor", $vendor,
        "--description", $productName,
        "--main-jar", (Split-Path -Leaf $finalJar),
        "--icon", $iconPath,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Dstdout.encoding=UTF-8",
        "--java-options", "-Dstderr.encoding=UTF-8",
        "--java-options", "-Dsun.stdout.encoding=UTF-8",
        "--java-options", "-Dsun.stderr.encoding=UTF-8",
        "--java-options", "-Dspring.profiles.active=local",
        "--java-options", "-Djava.awt.headless=false",
        "--win-dir-chooser",
        "--win-menu",
        "--win-menu-group", $productName,
        "--win-shortcut",
        "--win-upgrade-uuid", $upgradeUuid
    )
    Invoke-Native $jpackage $jpackageArgs "O jpackage nao conseguiu criar o MSI."

    $generatedFiles = @(Get-ChildItem -LiteralPath $outputDir -Filter "*.msi" |
        Sort-Object LastWriteTime -Descending)
    if ($generatedFiles.Count -eq 0) {
        throw "O jpackage terminou sem criar um arquivo MSI."
    }

    Copy-Item -LiteralPath $generatedFiles[0].FullName -Destination $finalMsi -Force
    $finalHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $finalMsi).Hash
    Set-Content -LiteralPath $hashFile -Encoding ASCII -Value "$finalHash  $(Split-Path -Leaf $finalMsi)"
    $sizeMb = [Math]::Round((Get-Item -LiteralPath $finalMsi).Length / 1MB, 2)

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "[OK] JAR E INSTALADOR GERADOS COM SUCESSO" -ForegroundColor Green
    Write-Host "JAR: $finalJar"
    Write-Host "Instalador: $finalMsi"
    Write-Host "Checksum: $hashFile"
    Write-Host "Tamanho: $sizeMb MB"
    Write-Host "Icone MSI/atalhos: setting.ico"
    Write-Host "Icone da janela: setting.png"
    Write-Host "========================================================" -ForegroundColor Green
    exit 0
}
catch {
    Write-Host ""
    Write-Host "[ERRO] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
finally {
    if (-not $NoPause) {
        Write-Host ""
        Read-Host "Pressione ENTER para fechar"
    }
}
