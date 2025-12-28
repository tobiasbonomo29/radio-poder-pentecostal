# 🔄 CAMBIAR PROYECTO DE UN REPOSITORIO A OTRO

## 📋 SITUACIÓN ACTUAL

Tu proyecto apunta a: `https://github.com/tobiasbonomo29/RADIO.git`

---

## 🎯 OPCIÓN 1: CAMBIAR LA URL DEL REMOTE (MÁS SIMPLE)

Esta opción mantiene todo tu historial de commits.

### **Paso 1: Ver remote actual**
```powershell
cd E:\PRUEBRADIO
git remote -v
```

### **Paso 2: Cambiar la URL**
```powershell
# Cambiar a un nuevo repositorio
git remote set-url origin https://github.com/TU_USUARIO/NUEVO_REPO.git

# Verificar el cambio
git remote -v
```

### **Paso 3: Subir al nuevo repositorio**
```powershell
git push -u origin main
```

---

## 🔥 OPCIÓN 2: ELIMINAR COMPLETAMENTE EL REMOTE Y AGREGAR UNO NUEVO

### **Paso 1: Eliminar el remote actual**
```powershell
cd E:\PRUEBRADIO
git remote remove origin
```

### **Paso 2: Agregar el nuevo remote**
```powershell
git remote add origin https://github.com/TU_USUARIO/NUEVO_REPO.git
```

### **Paso 3: Verificar**
```powershell
git remote -v
```

### **Paso 4: Subir al nuevo repo**
```powershell
git push -u origin main
```

---

## 🆕 OPCIÓN 3: EMPEZAR DE CERO (NUEVO REPOSITORIO LIMPIO)

Si quieres empezar con un historial limpio:

### **Paso 1: Eliminar el .git actual**
```powershell
cd E:\PRUEBRADIO
Remove-Item -Recurse -Force .git
```

### **Paso 2: Inicializar nuevo repositorio**
```powershell
git init
git add .
git commit -m "🎉 Initial commit - Radio Poder Pentecostal"
git branch -M main
```

### **Paso 3: Conectar con nuevo repo**
```powershell
git remote add origin https://github.com/TU_USUARIO/NUEVO_REPO.git
git push -u origin main
```

---

## 📝 ANTES DE EJECUTAR CUALQUIER OPCIÓN

### **1. Crea el nuevo repositorio en GitHub:**
- Ve a: https://github.com/new
- Repository name: `TU_NUEVO_NOMBRE`
- Public o Private
- ⚠️ NO marques README, .gitignore, ni license
- Click "Create repository"
- **Copia la URL del nuevo repositorio**

### **2. Decide qué opción usar:**
- **Opción 1**: Si solo quieres cambiar la URL (mantiene historial) ✅ RECOMENDADA
- **Opción 2**: Si quieres más control (mantiene historial)
- **Opción 3**: Si quieres empezar con historial limpio

---

## 🎯 COMANDO RÁPIDO (OPCIÓN 1 - RECOMENDADA)

Ejecuta estos 3 comandos:

```powershell
cd E:\PRUEBRADIO

# 1. Cambiar URL del remote
git remote set-url origin https://github.com/TU_USUARIO/NUEVO_REPO.git

# 2. Verificar
git remote -v

# 3. Subir al nuevo repo
git push -u origin main
```

---

## ✅ VERIFICACIÓN

Después de ejecutar los comandos, tu proyecto estará en el nuevo repositorio:
```
https://github.com/TU_USUARIO/NUEVO_REPO
```

---

## 🔍 VER CONFIGURACIÓN ACTUAL

Para ver dónde apunta actualmente tu proyecto:
```powershell
git remote -v
```

---

## 💡 CONSEJOS

1. **Siempre crea el nuevo repositorio en GitHub ANTES de cambiar el remote**
2. **Usa la Opción 1** si solo necesitas cambiar la URL
3. **Usa la Opción 3** si quieres limpiar el historial de commits
4. **Haz backup** antes de eliminar `.git` (si usas Opción 3)

---

## 🆘 PROBLEMAS COMUNES

### "Repository not found"
- Asegúrate de que el nuevo repositorio exista en GitHub
- Verifica que la URL sea correcta

### "Authentication failed"
- Usa un Personal Access Token: https://github.com/settings/tokens
- No uses tu contraseña de GitHub

### "Remote origin already exists" (Opción 2)
```powershell
git remote remove origin
git remote add origin URL_NUEVA
```

---

**¿Cuál es la URL de tu nuevo repositorio?** 

Dímela y ejecuto los comandos automáticamente para ti.

