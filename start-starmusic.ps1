# StarMusic 本機啟動腳本：Eureka -> Gateway + 9 個微服務 -> Angular dev server
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$cloud = Join-Path $root 'starmusic-cloud'
$logs = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null

$env:JAVA_HOME = 'F:\Java\jdk-25.0.4.1+1'
# 開發用固定密鑰與測試帳號密碼（可自行修改）
$env:STARMUSIC_JWT_SECRET = 'starmusic-local-dev-secret-0123456789abcdef'
$env:STARMUSIC_ADMIN_PASSWORD = 'admin123'
$env:STARMUSIC_DEMO_PASSWORD = 'starfan123'

$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
$pidFile = Join-Path $logs 'starmusic.pids'

$services = @(
    'star-eureka-server',   # :8761 先啟
    'star-gateway',         # :8080
    'star-video-service',   # :8081
    'star-radio-service',   # :8082
    'star-news-service',    # :8083
    'star-magazine-service',# :8084
    'star-shop-service',    # :8085
    'star-member-service',  # :8086
    'star-search-service',  # :8087
    'star-post-service'     # :8088
)

Remove-Item $pidFile -ErrorAction SilentlyContinue

foreach ($svc in $services) {
    $jar = Join-Path $cloud "$svc\target\$svc-1.0.0.jar"
    if (-not (Test-Path $jar)) { Write-Host "缺少 $jar，先執行 mvn package"; continue }
    $out = Join-Path $logs "$svc.log"
    $p = Start-Process -FilePath $java `
        -ArgumentList "-jar `"$jar`"" `
        -WorkingDirectory (Join-Path $cloud $svc) `
        -RedirectStandardOutput $out -RedirectStandardError "$out.err" `
        -PassThru -WindowStyle Hidden
    "$($p.Id) $svc" | Out-File $pidFile -Append
    Write-Host "啟動 $svc (PID $($p.Id)) -> logs\$svc.log"
    if ($svc -eq 'star-eureka-server') {
        Write-Host '等待 Eureka 就緒...'
        $ok = $false
        for ($i = 0; $i -lt 60; $i++) {
            try { Invoke-WebRequest 'http://localhost:8761' -UseBasicParsing -TimeoutSec 2 | Out-Null; $ok = $true; break }
            catch { Start-Sleep 2 }
        }
        if (-not $ok) { Write-Host 'Eureka 未就緒，請檢查 logs\star-eureka-server.log'; }
    }
}

Write-Host ''
Write-Host '後端啟動完成。Eureka 面板: http://localhost:8761'
Write-Host '前端: 在 starmusic-web 目錄執行 npm start，開啟 http://localhost:4200'
Write-Host '停止: .\stop-starmusic.ps1'
