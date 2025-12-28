# Script para cambiar el proyecto a otro repositorio
# Radio Poder Pentecostal - Android App

Write-Host ""
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host " CAMBIAR PROYECTO A OTRO REPOSITORIO" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Mostrar configuración actual
Write-Host "📋 CONFIGURACIÓN ACTUAL:" -ForegroundColor Yellow
Write-Host ""
$currentRemote = git remote get-url origin 2>$null
if ($currentRemote) {
    Write-Host "Remote actual: $currentRemote" -ForegroundColor White
} else {
    Write-Host "No hay remote configurado" -ForegroundColor Red
}
Write-Host ""

# Opciones
Write-Host "¿Qué deseas hacer?" -ForegroundColor Cyan
Write-Host ""
Write-Host "1️⃣  Cambiar la URL del remote (mantiene historial) - RECOMENDADO" -ForegroundColor Green
Write-Host "2️⃣  Eliminar y agregar nuevo remote (mantiene historial)" -ForegroundColor Yellow
Write-Host "3️⃣  Empezar de cero con nuevo repositorio (historial limpio)" -ForegroundColor Red
Write-Host "4️⃣  Solo ver la configuración actual" -ForegroundColor Gray
Write-Host "5️⃣  Cancelar" -ForegroundColor Gray
Write-Host ""

$opcion = Read-Host "Elige una opción (1-5)"

Write-Host ""

switch ($opcion) {
    "1" {
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host " OPCIÓN 1: CAMBIAR URL DEL REMOTE" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host ""

        $nuevaUrl = Read-Host "Ingresa la URL del nuevo repositorio"

        if ($nuevaUrl) {
            Write-Host ""
            Write-Host "🔄 Cambiando remote a: $nuevaUrl" -ForegroundColor Yellow
            git remote set-url origin $nuevaUrl

            Write-Host "✅ Remote actualizado" -ForegroundColor Green
            Write-Host ""
            Write-Host "📋 Nueva configuración:" -ForegroundColor Cyan
            git remote -v
            Write-Host ""
            Write-Host "🚀 Para subir al nuevo repositorio ejecuta:" -ForegroundColor White
            Write-Host "   git push -u origin main" -ForegroundColor Cyan
        } else {
            Write-Host "❌ No ingresaste una URL" -ForegroundColor Red
        }
    }

    "2" {
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host " OPCIÓN 2: ELIMINAR Y AGREGAR REMOTE" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host ""

        $nuevaUrl = Read-Host "Ingresa la URL del nuevo repositorio"

        if ($nuevaUrl) {
            Write-Host ""
            Write-Host "🗑️  Eliminando remote actual..." -ForegroundColor Yellow
            git remote remove origin 2>$null
            Write-Host "✅ Remote eliminado" -ForegroundColor Green

            Write-Host ""
            Write-Host "➕ Agregando nuevo remote..." -ForegroundColor Yellow
            git remote add origin $nuevaUrl
            Write-Host "✅ Remote agregado" -ForegroundColor Green

            Write-Host ""
            Write-Host "📋 Nueva configuración:" -ForegroundColor Cyan
            git remote -v
            Write-Host ""
            Write-Host "🚀 Para subir al nuevo repositorio ejecuta:" -ForegroundColor White
            Write-Host "   git push -u origin main" -ForegroundColor Cyan
        } else {
            Write-Host "❌ No ingresaste una URL" -ForegroundColor Red
        }
    }

    "3" {
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Red
        Write-Host " OPCIÓN 3: EMPEZAR DE CERO" -ForegroundColor Red
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Red
        Write-Host ""
        Write-Host "⚠️  ADVERTENCIA: Esto eliminará todo el historial de Git" -ForegroundColor Red
        Write-Host ""

        $confirmar = Read-Host "¿Estás seguro? Escribe 'SI' para confirmar"

        if ($confirmar -eq "SI") {
            $nuevaUrl = Read-Host "Ingresa la URL del nuevo repositorio"

            if ($nuevaUrl) {
                Write-Host ""
                Write-Host "🗑️  Eliminando repositorio Git actual..." -ForegroundColor Yellow
                Remove-Item -Recurse -Force .git -ErrorAction SilentlyContinue
                Write-Host "✅ Repositorio eliminado" -ForegroundColor Green

                Write-Host ""
                Write-Host "🆕 Inicializando nuevo repositorio..." -ForegroundColor Yellow
                git init
                git add .
                git commit -m "🎉 Initial commit - Radio Poder Pentecostal"
                git branch -M main
                Write-Host "✅ Repositorio inicializado" -ForegroundColor Green

                Write-Host ""
                Write-Host "➕ Conectando con GitHub..." -ForegroundColor Yellow
                git remote add origin $nuevaUrl
                Write-Host "✅ Conectado" -ForegroundColor Green

                Write-Host ""
                Write-Host "📋 Configuración:" -ForegroundColor Cyan
                git remote -v
                Write-Host ""
                Write-Host "🚀 Para subir al nuevo repositorio ejecuta:" -ForegroundColor White
                Write-Host "   git push -u origin main" -ForegroundColor Cyan
            } else {
                Write-Host "❌ No ingresaste una URL" -ForegroundColor Red
            }
        } else {
            Write-Host "❌ Operación cancelada" -ForegroundColor Yellow
        }
    }

    "4" {
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host " CONFIGURACIÓN ACTUAL" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host ""

        Write-Host "📍 Remote:" -ForegroundColor Yellow
        git remote -v
        Write-Host ""

        Write-Host "🌿 Rama actual:" -ForegroundColor Yellow
        git branch --show-current
        Write-Host ""

        Write-Host "📝 Último commit:" -ForegroundColor Yellow
        git log --oneline -1
        Write-Host ""
    }

    "5" {
        Write-Host "❌ Operación cancelada" -ForegroundColor Yellow
    }

    default {
        Write-Host "❌ Opción inválida" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 RECORDATORIO IMPORTANTE:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Antes de hacer 'git push', asegúrate de que el repositorio" -ForegroundColor White
Write-Host "exista en GitHub:" -ForegroundColor White
Write-Host ""
Write-Host "   1. Ve a: https://github.com/new" -ForegroundColor Cyan
Write-Host "   2. Crea el repositorio (sin README, sin .gitignore)" -ForegroundColor Cyan
Write-Host "   3. Luego ejecuta: git push -u origin main" -ForegroundColor Cyan
Write-Host ""
Write-Host "Presiona Enter para salir..."
Read-Host

