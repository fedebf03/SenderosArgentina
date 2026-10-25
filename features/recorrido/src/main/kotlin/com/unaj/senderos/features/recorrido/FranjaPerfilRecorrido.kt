package com.unaj.senderos.features.recorrido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.ui.componentes.perfilelevacion.GraficoPerfilElevacion
import com.unaj.senderos.core.ui.componentes.perfilelevacion.TEXTO_SUBIDA_RESTANTE
import com.unaj.senderos.core.ui.componentes.descripcionSiSinDato
import com.unaj.senderos.core.ui.comun.TEXTO_SIN_DATO
import com.unaj.senderos.core.ui.comun.formatearAlturaMetros
import kotlin.math.roundToInt

private val ALTURA_GRAFICO_FRANJA = 96.dp

// El perfil queda fijo arriba del panel para verlo mientras se camina.
// No aparece si el sendero no tiene perfil o si el perfil quedó viejo
@Composable
internal fun FranjaPerfilRecorrido(estadoVisual: RecorridoEstadoVisual, modifier: Modifier = Modifier) {
    val perfil = estadoVisual.sendero?.perfilElevacion ?: return
    if (estadoVisual.perfilEstaDesactualizado) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DatoDeLaFranja(
                titulo = "Altura",
                valor = estadoVisual.altitudActualMetros?.let { altura -> formatearAlturaMetros(altura.roundToInt()) } ?: TEXTO_SIN_DATO
            )
            DatoDeLaFranja(
                titulo = TEXTO_SUBIDA_RESTANTE,
                valor = estadoVisual.subidaRestanteMetros?.let { subida -> formatearAlturaMetros(subida) } ?: TEXTO_SIN_DATO
            )
        }
        GraficoPerfilElevacion(
            perfil = perfil,
            bioma = estadoVisual.sendero.bioma,
            esCompacto = true,
            alturaFija = ALTURA_GRAFICO_FRANJA,
            mostrarPosicionEnTexto = false,
            kilometroActual = estadoVisual.kilometroEnTrazado
        )
    }
}

@Composable
private fun DatoDeLaFranja(titulo: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.descripcionSiSinDato(valor)
        )
    }
}
