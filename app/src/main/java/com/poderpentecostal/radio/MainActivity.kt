package com.poderpentecostal.radio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.poderpentecostal.radio.ui.theme.PRUEBRADIOTheme
import com.poderpentecostal.radio.viewmodel.RadioViewModel

/**
 * MainActivity - Pantalla principal de Radio Poder Pentecostal
 * UI simple con logo, controles de reproducción y botones sociales
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // El permiso de notificaciones es opcional, la app funciona sin él
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            PRUEBRADIOTheme {
                RadioScreen()
            }
        }
    }
}

@Composable
fun RadioScreen(viewModel: RadioViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }

    val playerState by viewModel.playerState
    val isConnected by viewModel.isConnected
    val errorMessage by viewModel.errorMessage

    // Variables de estado
    val isPlaying = playerState == RadioViewModel.PlayerState.PLAYING
    val isBuffering = playerState == RadioViewModel.PlayerState.BUFFERING

    // Sincronizar estado periódicamente para asegurar consistencia
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // Cada segundo
            viewModel.syncPlayerState()
        }
    }

    // Mantener pantalla encendida mientras reproduce
    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Vincular servicio al iniciar
    LaunchedEffect(Unit) {
        viewModel.bindService(context)
    }

    // Desvincular al salir
    DisposableEffect(Unit) {
        onDispose {
            viewModel.unbindService(context)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            // Logo
            LogoSection()

            // Estado de conexión y errores
            StatusSection(isConnected, errorMessage)

            // Botón principal Play/Pause
            PlayPauseButton(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onClick = {
                    if (isPlaying) {
                        viewModel.pause()
                    } else {
                        viewModel.play(context)
                    }
                }
            )

            // Controles de volumen
            VolumeControls(audioManager = audioManager)

            // Botones sociales
            SocialButtons()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // Logo desde internet
        val painter = rememberAsyncImagePainter(
            "https://streaminglocucionar.com/portal/images/logos/poderpentecostal.png"
        )

        Image(
            painter = painter,
            contentDescription = "Logo Radio Poder Pentecostal",
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Radio Poder Pentecostal",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0097B2), // Celeste
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatusSection(isConnected: Boolean, errorMessage: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        if (!isConnected) {
            StatusChip(
                text = "Sin conexión a internet",
                color = Color(0xFFFF6B6B)
            )
        } else if (errorMessage != null) {
            StatusChip(
                text = errorMessage,
                color = Color(0xFFFFA500)
            )
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        // Botón principal
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(100.dp),
            containerColor = Color(0xFFFFC107), // Amarillo
            contentColor = Color.Black
        ) {
            // Siempre mostrar el ícono correcto de Play/Pause
            // El buffering se maneja internamente en ExoPlayer
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
fun VolumeControls(audioManager: AudioManager?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón bajar volumen
            VolumeButton(
                icon = Icons.Filled.VolumeDown,
                contentDescription = "Bajar volumen",
                onClick = {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            )


            // Botón subir volumen
            VolumeButton(
                icon = Icons.Filled.VolumeUp,
                contentDescription = "Subir volumen",
                onClick = {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            )
        }
    }
}

@Composable
fun VolumeButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .background(Color(0xFF0097B2).copy(alpha = 0.1f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF0097B2),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SocialButtons() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Síguenos en:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Botón YouTube
            SocialButton(
                text = "YouTube",
                icon = Icons.Filled.PlayArrow,
                backgroundColor = Color(0xFFFF0000),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://youtube.com/@radiopoderpentecostal?si=nmxV94tyvIl-JJAG")
                    )
                    ContextCompat.startActivity(
                        context,
                        intent,
                        null
                    )
                }
            )

            // Botón WhatsApp
            SocialButton(
                text = "WhatsApp",
                icon = Icons.Filled.Email, // Icono más apropiado para mensajería
                backgroundColor = Color(0xFF25D366),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/5491157800291")
                    )
                    ContextCompat.startActivity(
                        context,
                        intent,
                        null
                    )
                }
            )
        }
    }
}

@Composable
fun SocialButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(25.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

