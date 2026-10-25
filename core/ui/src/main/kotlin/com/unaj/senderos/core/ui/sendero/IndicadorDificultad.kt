package com.unaj.senderos.core.ui.sendero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.ui.tema.obtenerColorDificultad

private val ANCHO_BARRAS = 16.dp
private val ALTO_BARRAS = 14.dp
private val SEPARACION_ENTRE_BARRAS = 2.dp
private val RADIO_ESQUINA_BARRA = 1.dp
private val SEPARACION_BARRAS_TEXTO = 6.dp
private val ALTURAS_RELATIVAS_BARRAS = listOf(0.4f, 0.7f, 1f)

@Composable
fun IndicadorDificultad(
    dificultad: Dificultad,
    modifier: Modifier = Modifier,
    mostrarTexto: Boolean = true,
    estiloTexto: TextStyle = MaterialTheme.typography.bodySmall
) {
    val texto = obtenerTextoDificultad(dificultad)
    val modificadorAccesible = if (mostrarTexto) {
        modifier.semantics(mergeDescendants = true) { contentDescription = "Dificultad ${texto.lowercase()}" }
    } else {
        modifier
    }
    Row(
        modifier = modificadorAccesible,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SEPARACION_BARRAS_TEXTO)
    ) {
        BarrasDeDificultad(dificultad)
        if (mostrarTexto) {
            Text(text = texto, style = estiloTexto, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BarrasDeDificultad(dificultad: Dificultad) {
    val nivel = dificultad.ordinal + 1
    val colorActivo = obtenerColorDificultad(dificultad)
    val colorInactivo = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = Modifier.size(width = ANCHO_BARRAS, height = ALTO_BARRAS)) {
        val separacion = SEPARACION_ENTRE_BARRAS.toPx()
        val anchoBarra = (size.width - separacion * (ALTURAS_RELATIVAS_BARRAS.size - 1)) / ALTURAS_RELATIVAS_BARRAS.size
        ALTURAS_RELATIVAS_BARRAS.forEachIndexed { indice, alturaRelativa ->
            val alturaBarra = size.height * alturaRelativa
            drawRoundRect(
                color = if (indice < nivel) colorActivo else colorInactivo,
                topLeft = Offset(x = indice * (anchoBarra + separacion), y = size.height - alturaBarra),
                size = Size(width = anchoBarra, height = alturaBarra),
                cornerRadius = CornerRadius(RADIO_ESQUINA_BARRA.toPx())
            )
        }
    }
}
