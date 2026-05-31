param(
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot $EnvFile

if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Arquivo $EnvFile nao encontrado em $projectRoot."
}

Get-Content -LiteralPath $envPath | ForEach-Object {
    $line = $_.Trim()

    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }

    $parts = $line.Split("=", 2)
    if ($parts.Count -ne 2) {
        return
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim().Trim('"').Trim("'")

    if ($name -ne "") {
        Set-Item -Path "Env:$name" -Value $value
    }
}

Write-Host "Variaveis carregadas de $EnvFile"
mvn spring-boot:run
