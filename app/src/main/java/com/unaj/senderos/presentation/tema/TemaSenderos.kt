package com.unaj.senderos.presentation.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.unaj.senderos.domain.model.Dificultad

val CelesteArgentino = Color(0xFF74ACDF)

private val esquemaClaro = lightColorScheme(
    primary = Color(0xFF1F5A8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F5),
    onPrimaryContainer = Color(0xFF0B2A4A),
    secondary = Color(0xFF4F6478),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E4EF),
    onSecondaryContainer = Color(0xFF0B1D2E),
    tertiary = Color(0xFFB86E00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDE6BF),
    onTertiaryContainer = Color(0xFF3D2400),
    background = Color(0xFFF2F6FA),
    onBackground = Color(0xFF14213D),
    surface = Color.White,
    onSurface = Color(0xFF14213D),
    surfaceVariant = Color(0xFFDCE5EE),
    onSurfaceVariant = Color(0xFF4A5A70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FAFD),
    surfaceContainer = Color(0xFFEDF2F7),
    surfaceContainerHigh = Color(0xFFE4ECF4),
    surfaceContainerHighest = Color(0xFFDCE6F0),
    outline = Color(0xFF74808F),
    outlineVariant = Color(0xFFD3DEE9),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun TemaSenderos(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquemaClaro, content = contenido)
}

fun obtenerColorDificultad(dificultad: Dificultad): Color {
    return when (dificultad) {
        Dificultad.BAJA -> Color(0xFF2E7D4F)
        Dificultad.MEDIA -> Color(0xFFC77700)
        Dificultad.ALTA -> Color(0xFFBA1A1A)
    }
}
