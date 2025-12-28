# 🎯 PRUEBA ESTO AHORA

## ⚡ 3 PASOS SIMPLES

### 1️⃣ INSTALAR

```powershell
cd E:\PRUEBRADIO
.\gradlew installDebug
```

*Espera 10 segundos a que compile e instale*

---

### 2️⃣ PROBAR

1. Abre la app **Radio Poder Pentecostal**
2. Presiona el botón **PLAY** (amarillo grande)
3. Espera **10 segundos** (que suene la radio)
4. Activa **Modo Avión** en tu celular
5. Espera hasta que se **detenga** la radio (~10 segundos)
6. Desactiva **Modo Avión**

---

### 3️⃣ RESULTADO ESPERADO

**En menos de 1 segundo después de desactivar Modo Avión:**

✅ Debe aparecer: **"WiFi recuperado - reconectando..."**  
✅ Luego: **"Reconectando... (intento 1/5)"**  
✅ **LA RADIO DEBE VOLVER A SONAR SOLA** 🎵  

**SIN TOCAR NADA MÁS**

---

## ✅ SI FUNCIONA

¡Perfecto! La reconexión automática está lista.

## ❌ SI NO FUNCIONA

Envíame los logs:

```powershell
adb logcat | Select-String "RadioViewModel|RadioService"
```

---

**¡Pruébalo ahora mismo!** ⚡

