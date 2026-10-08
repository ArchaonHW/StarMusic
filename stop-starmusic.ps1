# 停止所有 StarMusic 後端服務
$logs = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'logs'
$pidFile = Join-Path $logs 'starmusic.pids'
if (Test-Path $pidFile) {
    Get-Content $pidFile | ForEach-Object {
        $id, $name = $_ -split ' ', 2
        Stop-Process -Id ([int]$id) -Force -ErrorAction SilentlyContinue
        Write-Host "已停止 $name (PID $id)"
    }
    Remove-Item $pidFile
} else {
    Write-Host '找不到 PID 檔，嘗試依 jar 名稱停止 java 行程...'
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -match 'star-.*-1\.0\.0\.jar' } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Host "已停止 PID $($_.ProcessId)" }
}
