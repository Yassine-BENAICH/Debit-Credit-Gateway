$listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, 9001)
$listener.Start()
Write-Host "LISTENING on port 9001"
$client = $listener.AcceptTcpClient()
Write-Host "CONNECTED"
$stream = $client.GetStream()
$buffer = New-Object byte[] 1024
$read = $stream.Read($buffer, 0, 1024)
Write-Host "RECEIVED $read bytes"
$data = [System.Text.Encoding]::ASCII.GetString($buffer, 0, $read)
Write-Host "DATA: $data"
$resp = $data.Replace("0100", "0110")
$respBytes = [System.Text.Encoding]::ASCII.GetBytes($resp)
$stream.Write($respBytes, 0, $respBytes.Length)
Write-Host "SENT RESPONSE"
$client.Close()
$listener.Stop()
