# 🚀 GUÍA COMPLETA - SUBIR PROYECTO A GITHUB

## ✅ PREPARACIÓN COMPLETADA

Los siguientes archivos ya están listos:
- ✅ `.gitignore` creado (ignora archivos de build, IDE, etc.)
- ✅ `README.md` actualizado con formato profesional de GitHub
- ✅ Repositorio Git inicializado

---

## 📋 PASOS PARA SUBIR A GITHUB

### **PASO 1: Crear Repositorio en GitHub**

1. Ve a [GitHub](https://github.com)
2. Haz clic en **"New repository"** (botón verde)
3. Configura el repositorio:
   - **Repository name**: `radio-poder-pentecostal` (o el nombre que prefieras)
   - **Description**: "App Android nativa para streaming de Radio Poder Pentecostal"
   - **Visibilidad**: 
     - ✅ **Public** (recomendado para portfolio)
     - ⬜ Private (si no quieres que sea público)
   - ⚠️ **NO marques** "Add a README file"
   - ⚠️ **NO marques** "Add .gitignore"
   - ⚠️ **NO marques** "Choose a license"
4. Haz clic en **"Create repository"**

---

### **PASO 2: Configurar Git Local (Si es la primera vez)**

```powershell
# Configurar tu nombre (solo la primera vez)
git config --global user.name "Tu Nombre"

# Configurar tu email (el de tu cuenta de GitHub)
git config --global user.email "tu-email@ejemplo.com"
```

---

### **PASO 3: Conectar con GitHub y Subir**

Después de crear el repositorio en GitHub, verás una página con instrucciones. Copia la URL del repositorio (algo como `https://github.com/TU_USUARIO/radio-poder-pentecostal.git`).

Ahora ejecuta estos comandos en PowerShell:

```powershell
# 1. Ir a la carpeta del proyecto
cd E:\PRUEBRADIO

# 2. Agregar todos los archivos
git add .

# 3. Hacer el primer commit
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

# 4. Renombrar rama a 'main' (GitHub usa 'main' por defecto)
git branch -M main

# 5. Conectar con GitHub (reemplaza con TU URL)
git remote add origin https://github.com/TU_USUARIO/radio-poder-pentecostal.git

# 6. Subir el código
git push -u origin main
```

---

### **PASO 4: Autenticación (Si te pide credenciales)**

GitHub ya no acepta contraseñas. Necesitas un **Personal Access Token**:

#### **Opción A: Usar GitHub CLI (Recomendado)**

```powershell
# Instalar GitHub CLI
winget install --id GitHub.cli

# Autenticarse
gh auth login

# Seguir las instrucciones en pantalla
```

#### **Opción B: Personal Access Token Manual**

1. Ve a GitHub → Settings → Developer settings
2. Personal access tokens → Tokens (classic)
3. Generate new token (classic)
4. Marca el scope: `repo` (acceso completo a repositorios)
5. Copia el token generado
6. Cuando Git te pida password, pega el token (NO tu contraseña)

---

## 📝 COMANDOS COMPLETOS (COPIA Y PEGA)

```powershell
# Ve a la carpeta del proyecto
cd E:\PRUEBRADIO

# Verifica el estado
git status

# Agrega todos los archivos
git add .

# Verifica qué se agregó
git status

# Haz el commit inicial
git commit -m "🎉 Initial commit - Radio Poder Pentecostal App"

# Renombra la rama a main
git branch -M main

# Conecta con GitHub (REEMPLAZA CON TU URL)
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git

# Sube el código
git push -u origin main
```

**⚠️ IMPORTANTE:** Reemplaza `TU_USUARIO` y `TU_REPOSITORIO` con tus datos reales.

---

## 🔍 VERIFICACIÓN

Después de `git push`, deberías ver algo como:

```
Enumerating objects: 150, done.
Counting objects: 100% (150/150), done.
Delta compression using up to 8 threads
Compressing objects: 100% (120/120), done.
Writing objects: 100% (150/150), 45.67 KiB | 3.05 MiB/s, done.
Total 150 (delta 25), reused 0 (delta 0), pack-reused 0
remote: Resolving deltas: 100% (25/25), done.
To https://github.com/TU_USUARIO/radio-poder-pentecostal.git
 * [new branch]      main -> main
Branch 'main' set up to track remote branch 'main' from 'origin'.
```

---

## 🎉 ¡LISTO!

Tu proyecto ahora está en GitHub. Visita:
```
https://github.com/TU_USUARIO/radio-poder-pentecostal
```

---

## 📚 COMANDOS ÚTILES PARA EL FUTURO

### Hacer cambios y subirlos

```powershell
# Ver archivos modificados
git status

# Agregar cambios
git add .

# Hacer commit
git commit -m "Descripción de los cambios"

# Subir a GitHub
git push
```

### Ver historial

```powershell
git log --oneline
```

### Crear una rama nueva

```powershell
git checkout -b feature/nueva-funcionalidad
```

### Ver ramas

```powershell
git branch
```

---

## 🐛 SOLUCIÓN DE PROBLEMAS

### Error: "remote origin already exists"

```powershell
git remote remove origin
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
```

### Error: "failed to push some refs"

```powershell
# Bajar cambios del remoto primero
git pull origin main --rebase

# Luego subir
git push origin main
```

### Olvidé agregar archivos al .gitignore

```powershell
# Eliminar del tracking pero mantener en disco
git rm --cached nombre-archivo

# O eliminar carpeta completa
git rm -r --cached build/

# Hacer commit
git commit -m "Remove ignored files"
git push
```

---

## 📊 AGREGAR BADGES AL README

GitHub mostrará tu README automáticamente. Para agregar badges (shields), edita el README.md y personaliza las URLs.

---

## ✅ CHECKLIST FINAL

- [ ] Repositorio creado en GitHub
- [ ] Git configurado (nombre y email)
- [ ] `.gitignore` está en el proyecto
- [ ] `git add .` ejecutado
- [ ] `git commit -m "..."` ejecutado
- [ ] `git remote add origin ...` ejecutado
- [ ] `git push -u origin main` ejecutado
- [ ] Código visible en GitHub
- [ ] README.md se ve correctamente en GitHub

---

**¡Tu proyecto ya está en GitHub y listo para compartir!** 🎉

Si tienes problemas con algún paso, avísame y te ayudo a resolverlo.

