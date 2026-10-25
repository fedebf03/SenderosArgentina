package com.unaj.senderos.core.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.unaj.senderos.core.domain.model.Dificultad

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

private val esquemaOscuro = darkColorScheme(
    primary = Color(0xFFA0CAFD),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFB8C8DA),
    onSecondary = Color(0xFF23323F),
    secondaryContainer = Color(0xFF394857),
    onSecondaryContainer = Color(0xFFD4E4F6),
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFF653E00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE0E2E8),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE0E2E8),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    surfaceContainerLowest = Color(0xFF0B0E12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

// Los componentes que no usan el esquema de Material (como la paleta de biomas) consultan aquí si el tema es oscuro
val LocalEsTemaOscuro = staticCompositionLocalOf { false }

@Composable
fun TemaSenderos(esTemaOscuro: Boolean = false, contenido: @Composable () -> Unit) {
    val esquema = if (esTemaOscuro) esquemaOscuro else esquemaClaro
    CompositionLocalProvider(LocalEsTemaOscuro provides esTemaOscuro) {
        MaterialTheme(colorScheme = esquema, content = contenido)
    }
}

fun obtenerColorDificultad(dificultad: Dificultad): Color {
    return when (dificultad) {
        Dificultad.BAJA -> Color(0xFF2E7D4F)
        Dificultad.MEDIA -> Color(0xFFC77700)
        Dificultad.ALTA -> Color(0xFFBA1A1A)
    }
}
