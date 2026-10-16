package com.unaj.senderos.core.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.unaj.senderos.core.domain.model.Bioma

/**
 * Colores de un bioma: [principal] para líneas, franjas y puntos; [contenedor] para fondos de etiquetas
 * y [sobreContenedor] para el texto que va encima del contenedor.
 */
data class ColoresBioma(val principal: Color, val contenedor: Color, val sobreContenedor: Color)

/** Color de fondo aproximado del estilo de mapa, usado como referencia para verificar el contraste de las líneas. */
internal val FONDO_MAPA_REFERENCIA = Color(0xFFF8F4F0)

/** Crear los colores de un bioma a partir de sus valores ARGB. */
private fun crearColores(principal: Long, contenedor: Long, sobreContenedor: Long): ColoresBioma {
    return ColoresBioma(Color(principal), Color(contenedor), Color(sobreContenedor))
}

private val coloresClaros: Map<Bioma, ColoresBioma> = mapOf(
    Bioma.ALTA_MONTANIA to crearColores(0xFF546E7A, 0xFFDDE5EA, 0xFF1C2B33),
    Bioma.BOSQUE_ANDINO_PATAGONICO to crearColores(0xFF1B5E20, 0xFFD3E8D3, 0xFF0B2B0E),
    Bioma.ESTEPA_PATAGONICA to crearColores(0xFF8D6A00, 0xFFF3E3B8, 0xFF3A2800),
    Bioma.PUNA to crearColores(0xFFB5502E, 0xFFF6D9CF, 0xFF4A1A0E),
    Bioma.YUNGAS to crearColores(0xFF1E8E5A, 0xFFCFEBDC, 0xFF073A22),
    Bioma.MONTE to crearColores(0xFF9C2F2F, 0xFFF2D2D0, 0xFF4A0F0F),
    Bioma.SIERRAS to crearColores(0xFF66741C, 0xFFE4EBC4, 0xFF2A3008),
    Bioma.LITORAL to crearColores(0xFF00796B, 0xFFCDEAE5, 0xFF00302A),
    Bioma.COSTA_MARITIMA to crearColores(0xFF0D6EBD, 0xFFD2E5F8, 0xFF0A2A4A)
)

private val coloresOscuros: Map<Bioma, ColoresBioma> = mapOf(
    Bioma.ALTA_MONTANIA to crearColores(0xFFB0BEC5, 0xFF37474F, 0xFFE3EAEE),
    Bioma.BOSQUE_ANDINO_PATAGONICO to crearColores(0xFF81C784, 0xFF1E3D21, 0xFFD5EDD6),
    Bioma.ESTEPA_PATAGONICA to crearColores(0xFFE0B84A, 0xFF4A3A0A, 0xFFFBEFC9),
    Bioma.PUNA to crearColores(0xFFF0906A, 0xFF5C2A18, 0xFFFBE0D6),
    Bioma.YUNGAS to crearColores(0xFF4CC38A, 0xFF0F4029, 0xFFD3F2E3),
    Bioma.MONTE to crearColores(0xFFEF8A84, 0xFF5A1E1E, 0xFFFBDAD8),
    Bioma.SIERRAS to crearColores(0xFFB5C75A, 0xFF3B4310, 0xFFEEF3CF),
    Bioma.LITORAL to crearColores(0xFF4DB6AC, 0xFF0C4039, 0xFFD3F0EC),
    Bioma.COSTA_MARITIMA to crearColores(0xFF64B5F6, 0xFF12365C, 0xFFD8EAFB)
)

/** Obtener los colores fijos de un bioma, o nulo para [Bioma.SIN_DEFINIR], que usa los colores del tema. */
fun obtenerColoresBioma(bioma: Bioma, esTemaOscuro: Boolean): ColoresBioma? {
    val paleta = if (esTemaOscuro) coloresOscuros else coloresClaros
    return paleta[bioma]
}

/** Obtener los colores del bioma según el tema vigente; sin definir devuelve los colores primarios del tema. */
@Composable
fun obtenerColoresDelBioma(bioma: Bioma): ColoresBioma {
    val esquema = MaterialTheme.colorScheme
    val esTemaOscuro = LocalEsTemaOscuro.current
    return obtenerColoresBioma(bioma, esTemaOscuro)
        ?: ColoresBioma(esquema.primary, esquema.primaryContainer, esquema.onPrimaryContainer)
}

/** Obtener el color de línea del bioma sobre el mapa, que siempre es claro; sin definir usa [colorPorDefecto]. */
fun obtenerColorLineaBioma(bioma: Bioma, colorPorDefecto: Color): Color {
    return obtenerColoresBioma(bioma, esTemaOscuro = false)?.principal ?: colorPorDefecto
}

/** Calcular la relación de contraste WCAG entre dos colores opacos, de 1 a 21. */
internal fun calcularContraste(primero: Color, segundo: Color): Double {
    val luminanciaPrimero = calcularLuminanciaRelativa(primero)
    val luminanciaSegundo = calcularLuminanciaRelativa(segundo)
    val mayor = maxOf(luminanciaPrimero, luminanciaSegundo)
    val menor = minOf(luminanciaPrimero, luminanciaSegundo)
    return (mayor + 0.05) / (menor + 0.05)
}

/** Calcular la luminancia relativa WCAG de un color opaco. */
private fun calcularLuminanciaRelativa(color: Color): Double {
    return 0.2126 * linealizarCanal(color.red) + 0.7152 * linealizarCanal(color.green) + 0.0722 * linealizarCanal(color.blue)
}

/** Convertir un canal sRGB a su valor lineal para el cálculo de luminancia. */
private fun linealizarCanal(canal: Float): Double {
    val valor = canal.toDouble()
    return if (valor <= 0.03928) valor / 12.92 else Math.pow((valor + 0.055) / 1.055, 2.4)
}
