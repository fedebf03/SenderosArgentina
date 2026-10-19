package com.unaj.senderos.features.senderos.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.clima.AlertaClima
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.clima.PronosticoDia
import com.unaj.senderos.core.ui.clima.describirAvisoPronosticoViejo
import com.unaj.senderos.core.ui.clima.describirDiaClima
import com.unaj.senderos.core.ui.clima.describirProbabilidadLluvia
import com.unaj.senderos.core.ui.clima.describirVientoClima
import com.unaj.senderos.core.ui.clima.elegirIconoClima
import com.unaj.senderos.core.ui.clima.formatearTemperaturaClima
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import java.time.LocalDate

private const val TITULO_SECCION_CLIMA = "Clima en el punto de inicio"
private const val TEXTO_CONSULTANDO_CLIMA = "Consultando el clima…"
private val TAMANO_ICONO_ACTUAL = 44.dp
private val TAMANO_ICONO_DIA = 28.dp

// Mostrar el clima del punto de inicio del sendero: avisos, clima de ahora, próximos días y, solo si el dato quedó viejo, el aviso
@Composable
internal fun SeccionClima(
    estadoVisual: DetalleSenderoEstadoVisual,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier,
    hoy: String = LocalDate.now().toString()
) {
    val resultado = estadoVisual.clima
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloDeSeccion(TITULO_SECCION_CLIMA)
        when {
            resultado == null && estadoVisual.estaCargandoClima -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(text = TEXTO_CONSULTANDO_CLIMA, style = MaterialTheme.typography.bodyMedium)
            }
            resultado == null -> {
                Text(
                    text = estadoVisual.errorClima ?: "No se pudo consultar el clima",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                BotonSecundario(
                    texto = "Reintentar",
                    alTocar = { alProcesarEvento(DetalleSenderoEvento.AlActualizarClima) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                AvisosDelClima(estadoVisual.alertasClimaAhora, estadoVisual.alertasClimaPorDia, hoy)
                ClimaDeAhora(resultado.pronostico.actual)
                DiasDelPronostico(resultado.pronostico.dias, hoy)
                if (resultado.estaDesactualizado) {
                    Text(
                        text = describirAvisoPronosticoViejo(resultado.antiguedadMilisegundos),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Listar los avisos de ahora y los de cada día; sin avisos no se dibuja nada
@Composable
private fun AvisosDelClima(ahora: Set<AlertaClima>, porDia: Map<String, Set<AlertaClima>>, hoy: String) {
    if (ahora.isEmpty() && porDia.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ahora.forEach { alerta -> FilaDeAviso("Ahora", alerta) }
            porDia.forEach { (fecha, alertas) ->
                alertas.forEach { alerta -> FilaDeAviso(describirDiaClima(fecha, hoy), alerta) }
            }
        }
    }
}

@Composable
private fun FilaDeAviso(cuando: String, alerta: AlertaClima) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(imageVector = Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(text = "$cuando · ${alerta.titulo}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = alerta.detalle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ClimaDeAhora(actual: com.unaj.senderos.core.domain.clima.ClimaActual) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = elegirIconoClima(actual.condicion, actual.esDeDia),
                contentDescription = actual.condicion.descripcion,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(TAMANO_ICONO_ACTUAL)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formatearTemperaturaClima(actual.temperaturaCelsius), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = actual.condicion.descripcion, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = describirVientoClima(actual.vientoKilometrosPorHora, actual.rafagasKilometrosPorHora),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                actual.sensacionTermicaCelsius?.let { sensacion ->
                    Text(
                        text = "Sensación ${formatearTemperaturaClima(sensacion)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Mostrar los próximos días en una fila de columnas del mismo ancho, con su icono, máxima, mínima y probabilidad de lluvia
@Composable
private fun DiasDelPronostico(dias: List<PronosticoDia>, hoy: String) {
    if (dias.isEmpty()) return
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
            dias.forEach { dia ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${describirDiaClima(dia.fecha, hoy)}: ${dia.condicion.descripcion}, máxima " +
                                "${formatearTemperaturaClima(dia.temperaturaMaximaCelsius)}, mínima ${formatearTemperaturaClima(dia.temperaturaMinimaCelsius)}"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = describirDiaClima(dia.fecha, hoy), style = MaterialTheme.typography.labelMedium)
                    Icon(
                        imageVector = elegirIconoClima(dia.condicion, esDeDia = true),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(TAMANO_ICONO_DIA)
                    )
                    Text(text = formatearTemperaturaClima(dia.temperaturaMaximaCelsius), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatearTemperaturaClima(dia.temperaturaMinimaCelsius),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = describirProbabilidadLluvia(dia.probabilidadLluviaPorcentaje),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
