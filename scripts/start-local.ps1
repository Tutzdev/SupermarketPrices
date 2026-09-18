param(
    [ValidateSet('Backend', 'Frontend')]
    [string]$Service = 'Backend'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$configurationPath = Join-Path $projectRoot '.local/runtime.json'
if (-not (Test-Path -LiteralPath $configurationPath)) {
    throw 'Configure .local/runtime.json conforme scripts/local-runtime.example.json. Nenhum banco sera criado automaticamente.'
}
$configuration = Get-Content -LiteralPath $configurationPath -Raw | ConvertFrom-Json
Set-Location $projectRoot

if ($Service -eq 'Frontend') {
    $env:VITE_API_BASE_URL = "http://localhost:$($configuration.backendPort)/api/v1"
    Set-Location (Join-Path $projectRoot 'frontend')
    & npm.cmd run dev -- --host localhost --port 5173 --strictPort
    exit $LASTEXITCODE
}

$postgresData = Join-Path $projectRoot $configuration.postgresData
$passwordFile = Join-Path $projectRoot $configuration.passwordFile
if (-not (Test-Path -LiteralPath (Join-Path $postgresData 'PG_VERSION')) -or
        -not (Test-Path -LiteralPath $passwordFile)) {
    throw 'O banco persistente ou sua senha nao foi encontrado. A inicializacao foi interrompida sem criar outro banco.'
}
$postgresControl = Join-Path $configuration.postgresBin 'pg_ctl.exe'
& $postgresControl -D $postgresData status | Out-Null
if ($LASTEXITCODE -ne 0) {
    & $postgresControl -D $postgresData -l (Join-Path $projectRoot '.local/postgres-runtime.log') `
        -o "-h 127.0.0.1 -p $($configuration.databasePort)" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel iniciar o PostgreSQL configurado.' }
}

$env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:$($configuration.databasePort)/$($configuration.databaseName)"
$env:DATABASE_USERNAME = $configuration.databaseUsername
$env:DATABASE_PASSWORD = [IO.File]::ReadAllText($passwordFile)
$env:PORT = [string]$configuration.backendPort
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173'
$env:SPRING_PROFILES_ACTIVE = 'development'
$env:MAIL_ENABLED = 'true'
$env:SPRING_MAIL_HOST = 'localhost'
$env:SPRING_MAIL_PORT = '1025'
$env:ADMIN_BOOTSTRAP_EMAIL = ''
$env:SUBSCRIBER_BOOTSTRAP_EMAIL = ''
$env:DEBUG = 'false'
$env:LOGGING_LEVEL_ORG_HIBERNATE_SQL = 'WARN'
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE -Dspring.devtools.restart.enabled=false".Trim()

Write-Host "Gomo: banco persistente $($configuration.databaseName), porta $($configuration.databasePort); API $($configuration.backendPort)."
& (Join-Path $projectRoot 'mvnw.cmd') spring-boot:run
exit $LASTEXITCODE
