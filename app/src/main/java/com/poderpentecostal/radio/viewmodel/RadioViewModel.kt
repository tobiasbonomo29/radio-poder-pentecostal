package com.poderpentecostal.radio.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poderpentecostal.radio.network.NetworkMonitor
import com.poderpentecostal.radio.service.RadioService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel para la MainActivity
 * Gestiona el estado de la radio y la conectividad
 */
class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val _playerState = mutableStateOf(PlayerState.IDLE)
    val playerState: State<PlayerState> = _playerState

    private val _isConnected = mutableStateOf(true)
    val isConnected: State<Boolean> = _isConnected

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private var radioService: RadioService? = null
    private var serviceBound = false
    private val networkMonitor = NetworkMonitor(application)
    private var reconnectJob: Job? = null
    private var wasPlayingBeforeDisconnect = false
    private var connectionLostTimestamp = 0L
    private var hasShownDisconnectMessage = false

    // Guardar contexto para reconexión
    private val appContext: Context = application.applicationContext

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? RadioService.RadioBinder)?.getService()
            radioService = service
            serviceBound = true

            // Actualizar estado inicial basado en el estado real del servicio
            _playerState.value = if (service?.isPlaying() == true) {
                PlayerState.PLAYING
            } else {
                PlayerState.IDLE
            }

            // Configurar listener para eventos del player
            service?.setPlayerListener(object : RadioService.PlayerListener {
                override fun onPlayerReady() {
                    // Solo actualizar si NO estamos buffering
                    // El evento onIsPlayingChanged manejará la transición correcta
                    if (_playerState.value != PlayerState.BUFFERING) {
                        val playing = service.isPlaying()
                        _playerState.value = if (playing) PlayerState.PLAYING else PlayerState.IDLE
                        android.util.Log.d("RadioViewModel", "onPlayerReady: isPlaying=$playing, state=${_playerState.value}")
                    }
                    _errorMessage.value = null
                }

                override fun onPlayerBuffering() {
                    // Solo cambiar a BUFFERING si no estamos ya reproduciendo
                    if (_playerState.value != PlayerState.PLAYING) {
                        _playerState.value = PlayerState.BUFFERING
                        android.util.Log.d("RadioViewModel", "onPlayerBuffering")
                    }
                }

                override fun onPlayerEnded() {
                    _playerState.value = PlayerState.IDLE
                    android.util.Log.d("RadioViewModel", "onPlayerEnded")
                }

                override fun onPlayerIdle() {
                    // Solo cambiar a IDLE si no estamos reproduciendo o buffering
                    if (_playerState.value != PlayerState.PLAYING &&
                        _playerState.value != PlayerState.BUFFERING) {
                        _playerState.value = PlayerState.IDLE
                        android.util.Log.d("RadioViewModel", "onPlayerIdle")
                    }
                }

                override fun onPlayingChanged(isPlaying: Boolean) {
                    // Este es el evento MÁS CONFIABLE
                    // Siempre actualizar basándose en este evento
                    _playerState.value = if (isPlaying) {
                        PlayerState.PLAYING
                    } else {
                        // Si se pausó, ir a PAUSED
                        PlayerState.PAUSED
                    }
                    android.util.Log.d("RadioViewModel", "onPlayingChanged: isPlaying=$isPlaying, newState=${_playerState.value}")
                }

                override fun onPlayerError(error: String) {
                    android.util.Log.e("RadioViewModel", "onPlayerError: $error")
                    _errorMessage.value = "Error en reproducción"
                    _playerState.value = PlayerState.ERROR

                    // Si estábamos reproduciendo antes del error, intentar reconectar
                    if (wasPlayingBeforeDisconnect || _isConnected.value) {
                        android.util.Log.d("RadioViewModel", "Error detectado, iniciando reconexión automática")
                        wasPlayingBeforeDisconnect = true
                        scheduleReconnect(attempt = 2, fastReconnect = true)
                    }
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            radioService = null
            serviceBound = false
        }
    }

    init {
        // Monitorear conectividad
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _isConnected.value = connected
                handleConnectivityChange(connected)
            }
        }
    }

    /**
     * Vincula el servicio de radio
     */
    fun bindService(context: Context) {
        val intent = Intent(context, RadioService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Desvincula el servicio de radio
     */
    fun unbindService(context: Context) {
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }

    /**
     * Inicia la reproducción
     */
    fun play(context: Context) {
        if (!serviceBound) {
            // Iniciar el servicio si no está vinculado
            val intent = Intent(context, RadioService::class.java)
            context.startService(intent)
            bindService(context)

            // Esperar un momento para que el servicio se vincule
            viewModelScope.launch {
                delay(500)
                radioService?.play()
            }
        } else {
            radioService?.play()
        }
        _playerState.value = PlayerState.BUFFERING
    }

    /**
     * Pausa la reproducción
     */
    fun pause() {
        radioService?.pause()
        _playerState.value = PlayerState.PAUSED
    }

    /**
     * Sincroniza el estado del player con el estado real del servicio
     * Previene inconsistencias entre la UI y el reproductor
     */
    fun syncPlayerState() {
        radioService?.let { service ->
            val actuallyPlaying = service.isPlaying()
            val currentState = _playerState.value

            // Si hay desincronización, corregir
            when {
                // Caso 1: El servicio está reproduciendo pero el estado no dice PLAYING
                actuallyPlaying && currentState != PlayerState.PLAYING -> {
                    android.util.Log.d("RadioViewModel", "Corrigiendo estado: servicio reproduciendo pero estado=$currentState")
                    _playerState.value = PlayerState.PLAYING
                }
                // Caso 2: El servicio NO está reproduciendo pero el estado dice PLAYING
                !actuallyPlaying && currentState == PlayerState.PLAYING -> {
                    android.util.Log.d("RadioViewModel", "Corrigiendo estado: servicio pausado pero estado=PLAYING")
                    _playerState.value = PlayerState.PAUSED
                }
                // Caso 3: El servicio NO está reproduciendo y el estado es BUFFERING (puede estar atascado)
                !actuallyPlaying && currentState == PlayerState.BUFFERING -> {
                    // Solo corregir si lleva más de 5 segundos en buffering
                    // (para no interrumpir buffers legítimos)
                    android.util.Log.d("RadioViewModel", "Estado BUFFERING sin reproducción")
                }
            }
        }
    }

    /**
     * Detiene completamente el servicio
     */
    fun stop(context: Context) {
        radioService?.stop()
        unbindService(context)
        _playerState.value = PlayerState.IDLE
    }

    /**
     * Verifica si está reproduciendo
     */
    fun isPlaying(): Boolean {
        return radioService?.isPlaying() ?: false
    }

    /**
     * Maneja cambios en la conectividad
     * NO pausa inmediatamente - deja que el buffer de 10s mantenga la reproducción
     */
    private fun handleConnectivityChange(connected: Boolean) {
        if (!connected) {
            // Perdió conexión - NO pausar inmediatamente
            wasPlayingBeforeDisconnect = isPlaying()
            connectionLostTimestamp = System.currentTimeMillis()

            if (wasPlayingBeforeDisconnect && !hasShownDisconnectMessage) {
                // Mostrar mensaje pero dejar que siga reproduciendo desde el buffer
                _errorMessage.value = "Sin WiFi - reproduciendo desde buffer"
                hasShownDisconnectMessage = true
            }

            reconnectJob?.cancel()

            // NO pausamos, el buffer de 10s seguirá reproduciendo

        } else {
            // Recuperó conexión - RECONECTAR INMEDIATAMENTE
            val disconnectedTime = System.currentTimeMillis() - connectionLostTimestamp

            if (wasPlayingBeforeDisconnect) {
                _errorMessage.value = "WiFi recuperado - reconectando..."

                // SIEMPRE intentar reconexión rápida cuando vuelve el internet
                // No importa cuánto tiempo estuvo desconectado
                scheduleReconnect(attempt = 1, fastReconnect = true)
            } else {
                _errorMessage.value = null
            }

            hasShownDisconnectMessage = false
        }
    }

    /**
     * Programa un reintento de reconexión con backoff
     * Si fastReconnect=true, usa delays más cortos para reconectar rápidamente
     */
    private fun scheduleReconnect(attempt: Int = 1, fastReconnect: Boolean = false) {
        reconnectJob?.cancel()

        if (attempt > 5) {
            _errorMessage.value = "No se pudo reconectar después de 5 intentos. Presiona PLAY para reintentar."
            wasPlayingBeforeDisconnect = false
            return
        }

        // Delays más cortos para reconexión rápida
        val delayMs = if (fastReconnect) {
            when (attempt) {
                1 -> 300L   // 0.3 segundos
                2 -> 800L   // 0.8 segundos
                3 -> 1500L  // 1.5 segundos
                4 -> 3000L  // 3 segundos
                else -> 5000L  // 5 segundos
            }
        } else {
            when (attempt) {
                1 -> 1000L
                2 -> 2000L
                3 -> 3000L
                4 -> 5000L
                else -> 10000L
            }
        }

        reconnectJob = viewModelScope.launch {
            delay(delayMs)

            if (_isConnected.value && wasPlayingBeforeDisconnect) {
                _playerState.value = PlayerState.BUFFERING
                _errorMessage.value = "Reconectando... (intento $attempt/5)"

                // Intentar reconectar y reproducir
                try {
                    if (!serviceBound) {
                        // Si el servicio no está vinculado, reiniciarlo
                        val intent = Intent(appContext, RadioService::class.java)
                        appContext.startService(intent)
                        bindService(appContext)
                        delay(800) // Esperar a que se vincule
                    }

                    radioService?.let { service ->
                        // Estrategia progresiva:
                        // Intento 1: reconnect() normal (rápido)
                        // Intento 2: rebuildPlayer() (reconstruir player sin reiniciar servicio)
                        // Intento 3+: reiniciar servicio completo
                        when {
                            attempt == 1 -> {
                                // Intento rápido: solo reconnect
                                android.util.Log.d("RadioViewModel", "Intento 1: reconnect() normal")
                                service.reconnect()
                                delay(500)
                                service.play()
                            }
                            attempt == 2 -> {
                                // Intento 2: reconstruir el player
                                android.util.Log.d("RadioViewModel", "Intento 2: rebuildPlayer()")
                                service.rebuildPlayer()
                                delay(800)
                                service.play()
                            }
                            else -> {
                                // Intento 3+: Reconexión agresiva - reiniciar servicio completo
                                android.util.Log.d("RadioViewModel", "Intento $attempt: Reiniciando servicio completo")
                                service.stop()
                                delay(300)

                                // Reiniciar el servicio completamente
                                val intent = Intent(appContext, RadioService::class.java)
                                appContext.stopService(intent)
                                delay(500)
                                appContext.startService(intent)

                                // Revincular
                                unbindService(appContext)
                                delay(300)
                                bindService(appContext)
                                delay(1000) // Más tiempo para vincular

                                // Reproducir
                                radioService?.play()
                            }
                        }
                    }

                    // Verificar después de 3 segundos si se reconectó
                    delay(3000)

                    if (_playerState.value == PlayerState.PLAYING) {
                        // ¡Éxito! Conectado y reproduciendo
                        _errorMessage.value = null
                        wasPlayingBeforeDisconnect = false
                        android.util.Log.d("RadioViewModel", "Reconexión exitosa en intento $attempt")
                    } else if (_playerState.value == PlayerState.BUFFERING ||
                               _playerState.value == PlayerState.ERROR ||
                               _playerState.value == PlayerState.IDLE ||
                               _playerState.value == PlayerState.PAUSED) {
                        // Todavía no está reproduciendo - intentar de nuevo
                        android.util.Log.d("RadioViewModel", "Intento $attempt falló. Estado: ${_playerState.value}")
                        scheduleReconnect(attempt + 1, fastReconnect)
                    } else {
                        // Estado inesperado, limpiar
                        wasPlayingBeforeDisconnect = false
                        _errorMessage.value = null
                    }
                } catch (e: Exception) {
                    // Error al reconectar, reintentar
                    android.util.Log.e("RadioViewModel", "Error en intento $attempt: ${e.message}", e)
                    _errorMessage.value = "Error al reconectar: ${e.message}"
                    scheduleReconnect(attempt + 1, fastReconnect)
                }
            } else {
                // No hay conexión
                if (!_isConnected.value) {
                    _errorMessage.value = "Esperando conexión..."
                }
            }
        }
    }

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        reconnectJob?.cancel()
    }

    /**
     * Estados posibles del player
     */
    enum class PlayerState {
        IDLE,
        BUFFERING,
        PLAYING,
        PAUSED,
        ERROR
    }
}

