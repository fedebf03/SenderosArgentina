package com.unaj.senderos.presentation.historial

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.presentation.componentes.IconoSincronizacion
import com.unaj.senderos.presentation.componentes.TituloDeSeccion
import com.unaj.senderos.presentation.recorrido.formatearDistanciaRecorrido
import com.unaj.senderos.presentation.recorrido.formatearTiempoTranscurrido

// Mostrar la sección "Historial de recorridos" con la lista del más reciente al más viejo, o un aviso si está vacía
@Composable
fun HistorialDeRecorridos(
    recorridos: List<RecorridoRealizado>,
    alSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TituloDeSeccion("Historial de recorridos")
        if (recorridos.isEmpty()) {
            Text(
                text = "Todavía no hiciste recorridos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                recorridos.forEachIndexed { indice, recorrido ->
                    if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FilaDeRecorrido(recorrido, alTocar = { alSeleccionar(recorrido.id) })
                }
            }
        }
    }
}

// Mostrar un recorrido con su fecha, sendero, distancia, duración, etiqueta de simulación y estado de sincronización
@Composable
private fun FilaDeRecorrido(recorrido: RecorridoRealizado, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alTocar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = recorrido.nombreSendero,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatearFechaHora(recorrido.inicioMilisegundos),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formatearDistanciaRecorrido(recorrido.distanciaKilometros)} · " +
                    formatearTiempoTranscurrido(recorrido.duracionMilisegundos),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (recorrido.esSimulado) EtiquetaSimulado()
        IconoSincronizacion(recorrido.estadoSincronizacion)
    }
}
