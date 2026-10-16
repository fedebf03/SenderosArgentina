package com.unaj.senderos.core.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.ui.tema.CelesteArgentino
import com.unaj.senderos.core.ui.tema.obtenerColoresDelBioma

@Composable
fun IlustracionMontanias(
    modifier: Modifier = Modifier,
    espejada: Boolean = false,
    bioma: Bioma = Bioma.SIN_DEFINIR
) {
    val coloresBioma = obtenerColoresDelBioma(bioma)
    val colorCieloSuperior = if (bioma == Bioma.SIN_DEFINIR) CelesteArgentino else coloresBioma.contenedor
    val colorCieloInferior = MaterialTheme.colorScheme.background
    val colorBase = coloresBioma.principal
    val colorMontaniaLejana = colorBase.copy(alpha = 0.55f)
    val colorMontaniaCercana = colorBase

    Canvas(modifier = modifier) {
        drawRect(brush = Brush.verticalGradient(listOf(colorCieloSuperior, colorCieloInferior)))

        val ancho = size.width
        val alto = size.height

        scale(scaleX = if (espejada) -1f else 1f, scaleY = 1f) {
            val cordonLejano = Path().apply {
                moveTo(0f, alto * 0.70f)
                lineTo(ancho * 0.20f, alto * 0.38f)
                lineTo(ancho * 0.38f, alto * 0.62f)
                lineTo(ancho * 0.60f, alto * 0.30f)
                lineTo(ancho * 0.82f, alto * 0.60f)
                lineTo(ancho, alto * 0.45f)
                lineTo(ancho, alto)
                lineTo(0f, alto)
                close()
            }
            drawPath(path = cordonLejano, color = colorMontaniaLejana)

            val cordonIntermedio = Path().apply {
                moveTo(0f, alto * 0.82f)
                lineTo(ancho * 0.15f, alto * 0.60f)
                lineTo(ancho * 0.33f, alto * 0.78f)
                lineTo(ancho * 0.52f, alto * 0.52f)
                lineTo(ancho * 0.74f, alto * 0.80f)
                lineTo(ancho * 0.90f, alto * 0.64f)
                lineTo(ancho, alto * 0.76f)
                lineTo(ancho, alto)
                lineTo(0f, alto)
                close()
            }
            drawPath(path = cordonIntermedio, color = colorMontaniaLejana)

            val cordonCercano = Path().apply {
                moveTo(0f, alto * 0.92f)
                lineTo(ancho * 0.25f, alto * 0.74f)
                lineTo(ancho * 0.45f, alto * 0.90f)
                lineTo(ancho * 0.68f, alto * 0.72f)
                lineTo(ancho * 0.88f, alto * 0.90f)
                lineTo(ancho, alto * 0.82f)
                lineTo(ancho, alto)
                lineTo(0f, alto)
                close()
            }
            drawPath(path = cordonCercano, color = colorMontaniaCercana)
        }
    }
}
