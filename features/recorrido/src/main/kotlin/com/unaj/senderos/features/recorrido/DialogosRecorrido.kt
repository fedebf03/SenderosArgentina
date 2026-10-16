package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.ui.comun.formatearTiempoTranscurrido
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
    alProcesarEvento: (RecorridoEvento) -> Unit
) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(RecorridoEvento.AlCerrarResumen) },
        title = { Text("Recorrido finalizado") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoDelPanel("Distancia recorrida", formatearDistanciaCorta(resumen.distanciaRecorridaKilometros))
                DatoDelPanel("Tiempo", formatearTiempoTranscurrido(resumen.tiempoTranscurridoMilisegundos))
                if (revision.hayPuntosPorRevisar) {
                    TituloDeSeccion(describirCantidadPuntosCapturados(revision.puntosPendientes.size))
                    ContenidoRevisionPuntos(
                        estado = revision,
                        alProcesarEvento = { evento -> alProcesarEvento(RecorridoEvento.AlRevisarPuntos(evento)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlCerrarResumen) }) {
                Text("Cerrar")
            }
        }
    )
}
