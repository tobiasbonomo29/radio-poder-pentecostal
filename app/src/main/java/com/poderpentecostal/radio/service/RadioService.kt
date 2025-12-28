package com.poderpentecostal.radio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.poderpentecostal.radio.MainActivity
import com.poderpentecostal.radio.R
import java.io.File

/**
 * Foreground Service para reproducción continua de radio
 * Gestiona ExoPlayer con cache y reconexión automática
 */
class RadioService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "radio_playback_channel"
        const val STREAM_URL = "http://69.61.116.28:9425/stream"

        // Configuración de buffer - 10 segundos mínimo para seguir reproduciendo sin conexión
        private const val MIN_BUFFER_MS = 10000 // 10 segundos - audio continúa sin WiFi
        private const val MAX_BUFFER_MS = 30000 // 30 segundos
        private const val BUFFER_FOR_PLAYBACK_MS = 2500 // 2.5 segundos para empezar a reproducir
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000 // 5 segundos después de rebuffering

        // Cache
        private const val CACHE_SIZE = 50 * 1024 * 1024L // 50 MB
    }

    private var exoPlayer: ExoPlayer? = null
    private var simpleCache: SimpleCache? = null
    private val binder = RadioBinder()
    private var playerListener: PlayerListener? = null
    private var audioManager: AudioManager? = null

    // Audio Focus
    private var audioFocusRequest: Any? = null

    inner class RadioBinder : Binder() {
        fun getService(): RadioService = this@RadioService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initializeCache()
        initializePlayer()
    }

    /**
     * Inicializa el cache para streaming
     */
    private fun initializeCache() {
        val cacheDir = File(cacheDir, "radio-cache")
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
        simpleCache = SimpleCache(cacheDir, cacheEvictor)
    }

    /**
     * Inicializa ExoPlayer con configuración optimizada para radio streaming
     * Buffer de 10 segundos para seguir reproduciendo sin conexión
     */
    private fun initializePlayer() {
        // Configurar LoadControl para mantener buffer de 10 segundos
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,              // minBufferMs - mínimo antes de empezar
                MAX_BUFFER_MS,              // maxBufferMs - máximo a mantener
                BUFFER_FOR_PLAYBACK_MS,     // bufferForPlaybackMs - para iniciar reproducción
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS // bufferForPlaybackAfterRebufferMs - después de rebuffering
            )
            .setPrioritizeTimeOverSizeThresholds(true) // Priorizar tiempo sobre tamaño
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()
            .apply {
                // Configurar listener para cambios de estado
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                playerListener?.onPlayerReady()
                                updateNotification(isPlaying)
                            }
                            Player.STATE_BUFFERING -> {
                                playerListener?.onPlayerBuffering()
                            }
                            Player.STATE_ENDED -> {
                                playerListener?.onPlayerEnded()
                            }
                            Player.STATE_IDLE -> {
                                playerListener?.onPlayerIdle()
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateNotification(isPlaying)
                        playerListener?.onPlayingChanged(isPlaying)
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playerListener?.onPlayerError(error.message ?: "Error desconocido")
                    }
                })

                // Configurar la fuente de datos con cache
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("RadioPoderPentecostal/1.0")
                    .setConnectTimeoutMs(10000)
                    .setReadTimeoutMs(10000)
                    .setAllowCrossProtocolRedirects(true) // Permitir redirecciones

                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(simpleCache!!)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                // Preparar el stream
                val mediaItem = MediaItem.fromUri(STREAM_URL)
                val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)

                setMediaSource(mediaSource)
                prepare()
            }
    }

    /**
     * Inicia la reproducción del stream
     */
    fun play() {
        if (requestAudioFocus()) {
            exoPlayer?.play()
            startForeground(NOTIFICATION_ID, createNotification(true))
        }
    }

    /**
     * Pausa la reproducción
     */
    fun pause() {
        exoPlayer?.pause()
        updateNotification(false)
        abandonAudioFocus()
    }

    /**
     * Detiene completamente el servicio
     */
    fun stop() {
        exoPlayer?.stop()
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Retorna si está reproduciendo actualmente
     */
    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false

    /**
     * Reconecta el stream (útil después de pérdida de conexión)
     * Reconstruye completamente el mediaSource y reconecta
     */
    fun reconnect() {
        exoPlayer?.let { player ->
            try {
                val wasPlaying = player.isPlaying
                val playerState = player.playbackState

                android.util.Log.d("RadioService", "reconnect() - Estado actual: $playerState")

                // Si el player está en estado de ERROR, reconstruir completamente
                if (playerState == Player.STATE_IDLE || playerState == Player.STATE_ENDED) {
                    android.util.Log.d("RadioService", "Player en mal estado, reconstruyendo completamente")
                    rebuildPlayer()
                    return
                }

                // Detener reproducción actual
                player.stop()

                // Reconstruir la fuente de datos
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("RadioPoderPentecostal/1.0")
                    .setConnectTimeoutMs(10000)
                    .setReadTimeoutMs(10000)
                    .setAllowCrossProtocolRedirects(true)

                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(simpleCache!!)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                // Recrear mediaSource
                val mediaItem = MediaItem.fromUri(STREAM_URL)
                val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)

                // Configurar y preparar nuevo stream
                player.setMediaSource(mediaSource)
                player.prepare()

                // Si estaba reproduciendo, reanudar
                if (wasPlaying) {
                    player.play()
                }
            } catch (e: Exception) {
                android.util.Log.e("RadioService", "Error en reconnect(), reconstruyendo player", e)
                rebuildPlayer()
            }
        } ?: run {
            // Si exoPlayer es null, reconstruir
            android.util.Log.d("RadioService", "ExoPlayer es null, reconstruyendo")
            rebuildPlayer()
        }
    }

    /**
     * Reconstruye completamente el ExoPlayer desde cero
     * Usado cuando el player está en un estado irrecuperable
     */
    fun rebuildPlayer() {
        android.util.Log.d("RadioService", "Reconstruyendo ExoPlayer completamente")

        try {
            // Liberar el player existente
            exoPlayer?.release()
            exoPlayer = null

            // Recrear el player desde cero
            initializePlayer()

            android.util.Log.d("RadioService", "ExoPlayer reconstruido exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("RadioService", "Error al reconstruir player", e)
            playerListener?.onPlayerError("Error al reconstruir player: ${e.message}")
        }
    }

    /**
     * Establece el listener para eventos del player
     */
    fun setPlayerListener(listener: PlayerListener) {
        this.playerListener = listener
    }

    /**
     * Solicita Audio Focus
     */
    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> play()
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            return audioManager?.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager?.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> play()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    /**
     * Abandona el Audio Focus
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager?.abandonAudioFocusRequest(it as android.media.AudioFocusRequest)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    /**
     * Crea el canal de notificación (necesario en Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Crea la notificación para el Foreground Service
     */
    private fun createNotification(isPlaying: Boolean): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(
                if (isPlaying) getString(R.string.notification_playing)
                else getString(R.string.notification_paused)
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Actualiza la notificación con el estado actual
     */
    private fun updateNotification(isPlaying: Boolean) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(isPlaying))
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        simpleCache?.release()
        simpleCache = null
        abandonAudioFocus()
    }

    /**
     * Interface para comunicar eventos del player a la UI
     */
    interface PlayerListener {
        fun onPlayerReady()
        fun onPlayerBuffering()
        fun onPlayerEnded()
        fun onPlayerIdle()
        fun onPlayingChanged(isPlaying: Boolean)
        fun onPlayerError(error: String)
    }
}

