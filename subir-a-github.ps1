# Script para subir proyecto a GitHub
# Radio Poder Pentecostal - Android App

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host " SUBIR PROYECTO A GITHUB" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si Git está instalado
try {
    $gitVersion = git --version
    Write-Host "✅ Git encontrado: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Git no está instalado." -ForegroundColor Red
    Write-Host "   Descárgalo desde: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit
}

Write-Host ""

# Verificar configuración de Git
$userName = git config user.name
$userEmail = git config user.email

if (-not $userName -or -not $userEmail) {
    Write-Host "⚙️  Configuración de Git necesaria" -ForegroundColor Yellow
    Write-Host ""

    $nombre = Read-Host "Ingresa tu nombre"
    $email = Read-Host "Ingresa tu email de GitHub"

    git config --global user.name "$nombre"
    git config --global user.email "$email"

    Write-Host "✅ Configuración guardada" -ForegroundColor Green
} else {
    Write-Host "✅ Git configurado como:" -ForegroundColor Green
    Write-Host "   Nombre: $userName"
    Write-Host "   Email:  $userEmail"
}

Write-Host ""
Write-Host "📋 PASOS A SEGUIR:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Ve a GitHub: https://github.com/new" -ForegroundColor White
Write-Host "2. Crea un nuevo repositorio:" -ForegroundColor White
Write-Host "   - Name: radio-poder-pentecostal (o el que prefieras)" -ForegroundColor Gray
Write-Host "   - Public o Private (tú eliges)" -ForegroundColor Gray
Write-Host "   - NO marques 'Add README', 'Add .gitignore', ni 'Add license'" -ForegroundColor Yellow
Write-Host "3. Copia la URL del repositorio" -ForegroundColor White
Write-Host ""

$repoUrl = Read-Host "Pega aquí la URL del repositorio (ejemplo: https://github.com/usuario/repo.git)"

if (-not $repoUrl) {
    Write-Host "❌ URL no válida" -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "🔄 Preparando repositorio..." -ForegroundColor Cyan
Write-Host ""

# Agregar archivos
Write-Host "📁 Agregando archivos..." -ForegroundColor Yellow
git add .

# Verificar qué se agregó
$filesCount = (git status --short | Measure-Object).Count
Write-Host "✅ $filesCount archivos listos" -ForegroundColor Green
Write-Host ""

# Hacer commit
Write-Host "💾 Creando commit inicial..." -ForegroundColor Yellow
git commit -m "🎉 Initial commit - Radio Poder Pentecostal App

✨ Características:
- Streaming de radio en vivo con ExoPlayer
- Buffer de 10 segundos
- Reconexión automática
- Reproducción en segundo plano
- UI moderna con Jetpack Compose
- Notificaciones persistentes
- Control de volumen integrado
- Enlaces a redes sociales"

Write-Host "✅ Commit creado" -ForegroundColor Green
Write-Host ""

# Renombrar rama a main
Write-Host "🔄 Configurando rama principal..." -ForegroundColor Yellow
git branch -M main
Write-Host "✅ Rama 'main' lista" -ForegroundColor Green
Write-Host ""

# Agregar remote
Write-Host "🔗 Conectando con GitHub..." -ForegroundColor Yellow
try {
    git remote add origin $repoUrl
    Write-Host "✅ Conectado con: $repoUrl" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Remote 'origin' ya existe, actualizando..." -ForegroundColor Yellow
    git remote set-url origin $repoUrl
    Write-Host "✅ URL actualizada" -ForegroundColor Green
}
Write-Host ""

# Push
Write-Host "🚀 Subiendo código a GitHub..." -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  Si te pide credenciales:" -ForegroundColor Yellow
Write-Host "   - Usuario: tu usuario de GitHub" -ForegroundColor Gray
Write-Host "   - Password: usa un Personal Access Token (NO tu contraseña)" -ForegroundColor Gray
Write-Host "   - Genera token aquí: https://github.com/settings/tokens" -ForegroundColor Gray
Write-Host ""

$pushResult = git push -u origin main 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "================================" -ForegroundColor Green
    Write-Host "  ✅ ¡ÉXITO!" -ForegroundColor Green
    Write-Host "================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Tu proyecto ya está en GitHub 🎉" -ForegroundColor Green
    Write-Host ""

    # Extraer usuario y repo de la URL
    if ($repoUrl -match "github\.com[:/](.+?)/(.+?)\.git") {
        $usuario = $matches[1]
        $repo = $matches[2]
        $githubUrl = "https://github.com/$usuario/$repo"
        Write-Host "🌐 Visítalo en: $githubUrl" -ForegroundColor Cyan
    }
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Hubo un error al subir el código" -ForegroundColor Red
    Write-Host ""
    Write-Host "Error:" -ForegroundColor Yellow
    Write-Host $pushResult
    Write-Host ""
    Write-Host "💡 Posibles soluciones:" -ForegroundColor Cyan
    Write-Host "1. Verifica que la URL del repositorio sea correcta" -ForegroundColor White
    Write-Host "2. Asegúrate de tener permisos en el repositorio" -ForegroundColor White
    Write-Host "3. Genera un Personal Access Token: https://github.com/settings/tokens" -ForegroundColor White
    Write-Host "4. O usa GitHub CLI: gh auth login" -ForegroundColor White
    Write-Host ""
}

Write-Host ""
Write-Host "Presiona Enter para salir..."
Read-Host

