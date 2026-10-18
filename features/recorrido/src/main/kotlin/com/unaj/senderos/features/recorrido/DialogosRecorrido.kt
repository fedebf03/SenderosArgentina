package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.ui.clima.formatearTemperaturaClima
import com.unaj.senderos.core.ui.clima.describirVientoClima
import com.unaj.senderos.core.ui.clima.describirAvisoPronosticoViejo
import com.unaj.senderos.core.ui.clima.describirAntiguedadClima
import com.unaj.senderos.core.ui.comun.formatearTiempoTranscurrido
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.comun.formatearDistanciaCorta
import com.unaj.senderos.core.ui.revisionpuntos.ContenidoRevisionPuntos
import com.unaj.senderos.core.ui.revisionpuntos.RevisionPuntosEstadoVisual
import com.unaj.senderos.core.ui.revisionpuntos.describirCantidadPuntosCapturados

@Composable
internal fun DialogoLlegadaAlFinal(nombreSendero: String, alProcesarEvento: (RecorridoEvento) -> Unit) {
    DialogoConfirmacion(
        titulo = "¡Llegaste al final!",
        texto = "Terminaste $nombreSendero. ¿Querés finalizar el recorrido?",
        textoConfirmar = "Finalizar recorrido",
        alConfirmar = { alProcesarEvento(RecorridoEvento.AlConfirmarFinalizar) },
        textoDescartar = "Seguir caminando",
        alDescartar = { alProcesarEvento(RecorridoEvento.AlSeguirCaminando) }
    )
}

@Composable
internal fun DialogoConfirmarFinalizar(alProcesarEvento: (RecorridoEvento) -> Unit) {
    DialogoConfirmacion(
        titulo = "Finalizar recorrido",
        texto = "¿Querés finalizar el recorrido? Se dejará de seguir tu ubicación.",
        textoConfirmar = "Finalizar",
        alConfirmar = { alProcesarEvento(RecorridoEvento.AlConfirmarFinalizar) },
        textoDescartar = "Seguir caminando",
        alDescartar = { alProcesarEvento(RecorridoEvento.AlCancelarFinalizar) }
    )
}

@Composable
internal fun DialogoConfirmarCancelar(alProcesarEvento: (RecorridoEvento) -> Unit) {
    DialogoConfirmacion(
        titulo = "Cancelar recorrido",
        texto = "¿Querés cancelar el recorrido? Se descartará lo recorrido y no quedará en el historial.",
        textoConfirmar = "Cancelar recorrido",
        alConfirmar = { alProcesarEvento(RecorridoEvento.AlConfirmarCancelarRecorrido) },
        textoDescartar = "Seguir caminando",
        alDescartar = { alProcesarEvento(RecorridoEvento.AlDescartarCancelacion) },
        esDestructivo = true
    )
}

@Composable
internal fun DialogoResumen(
    resumen: ResumenRecorrido,
    revision: RevisionPuntosEstadoVisual,
    esCaminataLibre: Boolean,
    sePuedeConvertir: Boolean,
    alProcesarEvento: (RecorridoEvento) -> Unit
) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(RecorridoEvento.AlCerrarResumen) },
        title = { Text(if (esCaminataLibre) "Caminata finalizada" else "Recorrido finalizado") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoDelPanel("Distancia recorrida", formatearDistanciaCorta(resumen.distanciaRecorridaKilometros))
                DatoDelPanel("Tiempo", formatearTiempoTranscurrido(resumen.tiempoTranscurridoMilisegundos))
                if (esCaminataLibre && !sePuedeConvertir) {
                    Text(
                        text = "La caminata fue muy corta (menos de 50 m) para convertirla en un sendero. Se guardó en tu historial.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (esCaminataLibre) {
                    Text(
                        text = "¿Querés convertir esta caminata en un sendero nuevo? Vas a poder completar sus datos antes de guardarlo." +
                            if (revision.hayPuntosPorRevisar) " Los puntos que marcaste (${revision.puntosPendientes.size}) van incluidos." else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (revision.hayPuntosPorRevisar) {
                    TituloDeSeccion(describirCantidadPuntosCapturados(revision.puntosPendientes.size))
                    ContenidoRevisionPuntos(
                        estado = revision,
                        alProcesarEvento = { evento -> alProcesarEvento(RecorridoEvento.AlRevisarPuntos(evento)) }
                    )
                }
            }
        },
        confirmButton = {
            if (esCaminataLibre && sePuedeConvertir) {
                TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlConvertirEnSendero) }) {
                    Text("Convertir en sendero nuevo")
                }
            } else {
                TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlCerrarResumen) }) {
                    Text("Cerrar")
                }
            }
        },
        dismissButton = if (esCaminataLibre && sePuedeConvertir) {
            { TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlCerrarResumen) }) { Text("Solo guardar") } }
        } else {
            null
        }
    )
}

// Mostrar el clima de donde está el usuario con el detalle de cada aviso, para decidir si sigue, se refugia o vuelve
@Composable
internal fun DialogoClimaRecorrido(estadoVisual: RecorridoEstadoVisual, alCerrar: () -> Unit) {
    val resultado = estadoVisual.clima ?: return
    val actual = resultado.pronostico.actual
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Clima donde estás") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${formatearTemperaturaClima(actual.temperaturaCelsius)} · ${actual.condicion.descripcion}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = describirVientoClima(actual.vientoKilometrosPorHora, actual.rafagasKilometrosPorHora), style = MaterialTheme.typography.bodyMedium)
                estadoVisual.presionHectopascales?.let { presion ->
                    Text(text = "Presión: $presion hPa", style = MaterialTheme.typography.bodyMedium)
                }
                estadoVisual.alertasClima.forEach { alerta ->
                    Column {
                        Text(text = alerta.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        Text(text = alerta.detalle, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    text = if (resultado.estaDesactualizado) {
                        describirAvisoPronosticoViejo(resultado.antiguedadMilisegundos)
                    } else {
                        "Actualizado ${describirAntiguedadClima(resultado.antiguedadMilisegundos)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Entendido") } }
    )
}
