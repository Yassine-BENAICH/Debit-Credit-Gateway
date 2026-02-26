param([int]$Port = 8080, [switch]$Kill)
$ErrorActionPreference = "Stop"
$net = netstat -ano | findstr ":$Port"
if ($net) {
  $procIds = ($net | ForEach-Object { ($_ -split "\s+")[-1] } | Where-Object { $_ -match '^\d+$' } | Select-Object -Unique)
  if ($Kill -and $procIds) {
    foreach ($procId in $procIds) { try { taskkill /F /PID $procId | Out-Null }catch {} }
    Start-Sleep -Seconds 1
  }
}
$env:PORT = $Port.ToString()
Set-Location debit-credit-gateway/backend
mvn spring-boot:run
