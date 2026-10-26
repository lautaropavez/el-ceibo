# Humo del JAR ejecutable contra el entorno DEMO (nunca contra la base normal).
# Uso: .\scripts\smoke-jar-demo.ps1 -BaseUrl http://127.0.0.1:8081
# Requiere el JAR ya iniciado con --spring.profiles.active=demo. No recibe ni imprime credenciales.
# Falla si alguna pagina publica no responde 200 o si faltan cabeceras de seguridad.
param([string]$BaseUrl = "http://127.0.0.1:8081")

$fallos = 0
foreach ($ruta in "/", "/login", "/registro", "/libro/biblioteca", "/css/style.css") {
    try {
        $r = Invoke-WebRequest -Uri ($BaseUrl + $ruta) -UseBasicParsing
        $h = $r.Headers
        $ok = ($r.StatusCode -eq 200) -and ($h["X-Content-Type-Options"] -eq "nosniff") -and ($h["X-Frame-Options"] -eq "DENY")
        $estado = $r.StatusCode
    } catch {
        $ok = $false
        $estado = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "sin respuesta" }
    }
    Write-Output ("{0} {1} -> {2}" -f $(if ($ok) { "OK  " } else { "FAIL" }), $ruta, $estado)
    if (-not $ok) { $fallos++ }
}
if ($fallos -gt 0) { exit 1 }
