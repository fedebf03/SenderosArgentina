package com.unaj.senderos.features.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.comun.TEXTO_NO_DISPONIBLE
import com.unaj.senderos.core.ui.comun.formatearDistanciaCorta
import com.unaj.senderos.core.ui.comun.formatearPasos
import com.unaj.senderos.core.ui.comun.formatearVelocidad

private const val TITULO_ESTADISTICAS = "Mis estadísticas"
private const val TEXTO_SIN_RECORRIDOS = "Todavía no hay recorridos reales para mostrar estadísticas. Los recorridos de menos de 50 m y los simulados no cuentan."

// Mostrar los totales del usuario a partir de su historial: cantidad, distancia, tiempo, velocidad, el recorrido más largo y los pasos
@Composable
internal fun SeccionEstadisticas(estadoVisual: PerfilEstadoVisual, modifier: Modifier = Modifier) {
    val estadisticas = estadoVisual.estadisticas
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloDeSeccion(TITULO_ESTADISTICAS)
        if (estadisticas.cantidadRecorridos == 0) {
            Text(text = TEXTO_SIN_RECORRIDOS, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                val datos = listOf(
                    "Recorridos" to estadisticas.cantidadRecorridos.toString(),
                    "Distancia total" to formatearDistanciaCorta(estadisticas.distanciaTotalKilometros),
                    "Tiempo total" to describirTiempoTotal(estadisticas.tiempoTotalMilisegundos),
                    "Velocidad promedio" to formatearVelocidad(estadisticas.velocidadPromedioKilometrosPorHora),
                    "Recorrido más largo" to formatearDistanciaCorta(estadisticas.mayorDistanciaKilometros),
                    "Pasos" to formatearPasos(estadisticas.pasosTotales, TEXTO_NO_DISPONIBLE)
                )
                datos.forEachIndexed { indice, (etiqueta, valor) ->
                    if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = etiqueta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                }
            }
        }
        val aclaracion = describirSimuladosIgnorados(estadisticas.cantidadSimuladosIgnorados)
        if (aclaracion.isNotEmpty() && estadisticas.cantidadRecorridos > 0) {
            Text(text = aclaracion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
