package com.unaj.senderos.features.senderos.formulario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.ui.componentes.GraficoPerfilElevacion
import com.unaj.senderos.core.ui.comun.formatearKilometros

// Indicar debajo de la distancia si sale del mapa o, si se escribió a mano, ofrecer volver a usar la del mapa
@Composable
internal fun AyudaDistanciaDelMapa(estadoVisual: FormularioSenderoEstadoVisual, alProcesarEvento: (FormularioSenderoEvento) -> Unit) {
    if (estadoVisual.recorrido.size < 2) return
    if (estadoVisual.distanciaEsManual) {
        TextButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlUsarDistanciaDelMapa) }) {
            Text("Usar la del mapa (${formatearKilometros(estadoVisual.distanciaCalculadaKilometros)} km)")
        }
    } else {
        Text(
            text = "Distancia calculada con el mapa",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Mostrar el perfil de elevación del trazado que se está dibujando: el gráfico si está al día, el indicador mientras se calcula,
// o el motivo por el que falta con el botón para calcularlo. Con menos de dos puntos no muestra nada
@Composable
internal fun PerfilEnFormulario(estadoVisual: FormularioSenderoEstadoVisual, alProcesarEvento: (FormularioSenderoEvento) -> Unit) {
    if (estadoVisual.recorrido.size < 2) return
    val perfil = estadoVisual.perfilElevacion
    val aviso = estadoVisual.avisoPerfil
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            estadoVisual.estaCalculandoPerfil -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Calculando el perfil de elevación…", style = MaterialTheme.typography.bodyMedium)
            }
            perfil != null && !estadoVisual.perfilEstaDesactualizado -> GraficoPerfilElevacion(
                perfil = perfil,
                bioma = estadoVisual.bioma,
                esCompacto = true
            )
            aviso != null -> MensajeConAccion(texto = aviso, textoAccion = "Reintentar", alTocar = {
                alProcesarEvento(FormularioSenderoEvento.AlCalcularPerfil)
            })
            perfil != null -> MensajeConAccion(
                texto = "El recorrido cambió desde que se calculó el perfil",
                textoAccion = "Calcular ahora",
                alTocar = { alProcesarEvento(FormularioSenderoEvento.AlCalcularPerfil) }
            )
            else -> MensajeConAccion(
                texto = "Todavía no se calculó el perfil de elevación",
                textoAccion = "Calcular perfil",
                alTocar = { alProcesarEvento(FormularioSenderoEvento.AlCalcularPerfil) }
            )
        }
    }
}

// Mostrar un texto breve con un botón de texto para resolverlo
@Composable
private fun MensajeConAccion(texto: String, textoAccion: String, alTocar: () -> Unit) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
    TextButton(onClick = alTocar) {
        Text(textoAccion)
    }
}
