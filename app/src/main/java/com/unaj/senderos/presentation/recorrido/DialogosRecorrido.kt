package com.unaj.senderos.presentation.recorrido

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
import com.unaj.senderos.presentation.componentes.DialogoConfirmacion
import com.unaj.senderos.presentation.componentes.TituloDeSeccion
import com.unaj.senderos.presentation.comun.formatearDistanciaCorta
import com.unaj.senderos.presentation.revisionpuntos.ContenidoRevisionPuntos
import com.unaj.senderos.presentation.revisionpuntos.RevisionPuntosEstadoVisual
import com.unaj.senderos.presentation.revisionpuntos.describirCantidadPuntosCapturados

// Avisar la llegada al final del sendero y ofrecer finalizar el recorrido o seguir caminando
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

// Pedir confirmación antes de finalizar el recorrido
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

// Pedir confirmación antes de descartar el recorrido sin guardarlo
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

// Mostrar el resumen con la distancia y el tiempo, y si hay puntos capturados, su revisión con casillas
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
