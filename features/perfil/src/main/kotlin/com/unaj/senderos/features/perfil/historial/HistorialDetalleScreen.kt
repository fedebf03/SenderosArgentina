package com.unaj.senderos.features.perfil.historial

import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import androidx.compose.material.icons.outlined.Delete
import com.unaj.senderos.core.ui.componentes.EtiquetaSimulado
import com.unaj.senderos.core.ui.comun.formatearFechaHora
import com.unaj.senderos.core.ui.componentes.BarraSuperior
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.comun.formatearDistanciaCorta
import com.unaj.senderos.core.ui.comun.formatearPasos
import com.unaj.senderos.core.ui.comun.formatearRitmo
import com.unaj.senderos.core.ui.comun.formatearVelocidad
import com.unaj.senderos.core.ui.comun.TEXTO_NO_DISPONIBLE
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.unaj.senderos.core.ui.tema.obtenerColorLineaBioma
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.componentes.mapa.MapaRecorrido
import com.unaj.senderos.core.ui.comun.formatearTiempoTranscurrido
import com.unaj.senderos.core.ui.tema.TemaSenderos

private const val TAG = "HISTORIAL"

@Composable
fun HistorialDetallePantalla(
    viewModel: HistorialDetalleViewModel,
    alVolverAlPerfil: () -> Unit,
    alNavegarAFormularioDesdeCaminata: (String) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val volverAlPerfilActual by rememberUpdatedState(alVolverAlPerfil)
    val navegarAFormularioActual by rememberUpdatedState(alNavegarAFormularioDesdeCaminata)

    LaunchedEffect(Unit) {
        Log.d(TAG, "Pantalla inicializada / rotación completada")
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is HistorialDetalleEfecto.VolverAlPerfil -> volverAlPerfilActual()
                is HistorialDetalleEfecto.NavegarAFormularioDesdeCaminata -> navegarAFormularioActual(efecto.idCaminata)
            }
        }
    }

    HistorialDetalleContenido(estadoVisual = estadoVisual, alProcesarEvento = viewModel::procesarEvento)
}

@Composable
fun HistorialDetalleContenido(
    estadoVisual: HistorialDetalleEstadoVisual,
    alProcesarEvento: (HistorialDetalleEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { alProcesarEvento(HistorialDetalleEvento.AlVolver) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = estadoVisual.recorrido?.nombreSendero ?: "Recorrido",
                alVolver = { alProcesarEvento(HistorialDetalleEvento.AlVolver) },
                descripcionVolver = "Volver al perfil"
            )
        }
    ) { espaciadoInterno ->
        val recorrido = estadoVisual.recorrido
        when {
            estadoVisual.estaCargando -> Box(
                Modifier.fillMaxSize().padding(espaciadoInterno),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            recorrido == null -> Column(
                modifier = Modifier.fillMaxSize().padding(espaciadoInterno).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = estadoVisual.mensajeError ?: "No se encontró el recorrido",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = { alProcesarEvento(HistorialDetalleEvento.AlVolver) }) {
                    Text("Volver")
                }
            }
            else -> CuerpoHistorial(estadoVisual, recorrido, alProcesarEvento, Modifier.padding(espaciadoInterno))
        }
    }
}

@Composable
private fun CuerpoHistorial(
    estadoVisual: HistorialDetalleEstadoVisual,
    recorrido: RecorridoRealizado,
    alProcesarEvento: (HistorialDetalleEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recorrido.esSimulado) EtiquetaSimulado()

        OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(280.dp)) {
            MapaRecorrido(
                puntos = estadoVisual.sendero?.recorrido.orEmpty(),
                tipoRecorrido = estadoVisual.sendero?.tipoRecorrido ?: TipoRecorrido.LINEAL,
                modifier = Modifier.fillMaxSize(),
                usarModoTextura = true,
                colorLinea = estadoVisual.sendero?.let { sendero ->
                    obtenerColorLineaBioma(sendero.bioma, MaterialTheme.colorScheme.primary)
                },
                trazadoCaminado = recorrido.trazadoCaminado
            )
        }
        if (estadoVisual.sendero == null && !estadoVisual.esCaminataLibre) {
            Text(
                text = "El sendero ya no existe: solo se muestra el trazado caminado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (estadoVisual.sePuedeConvertirEnSendero) {
            BotonPrincipal(
                texto = "Convertir en sendero nuevo",
                alTocar = { alProcesarEvento(HistorialDetalleEvento.AlConvertirEnSendero) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        TituloDeSeccion("Datos del recorrido")
        OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            val datos = listOf(
                "Inicio" to formatearFechaHora(recorrido.inicioMilisegundos),
                "Fin" to formatearFechaHora(recorrido.finMilisegundos),
                "Duración" to formatearTiempoTranscurrido(recorrido.duracionMilisegundos),
                "Distancia" to formatearDistanciaCorta(recorrido.distanciaKilometros),
                "Velocidad promedio" to formatearVelocidad(recorrido.velocidadPromedioKilometrosPorHora),
                "Velocidad máxima" to formatearVelocidad(recorrido.velocidadMaximaKilometrosPorHora),
                "Pasos" to formatearPasos(recorrido.pasos, TEXTO_NO_DISPONIBLE),
                "Ritmo" to formatearRitmo(recorrido.ritmoPasosPorMinuto, TEXTO_NO_DISPONIBLE)
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
                    Text(
                        text = valor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        estadoVisual.errorAlEliminar?.let { mensaje ->
            Text(text = mensaje, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        BotonSecundario(
            texto = "Eliminar recorrido",
            alTocar = { alProcesarEvento(HistorialDetalleEvento.AlTocarEliminar) },
            icono = { IconoDeBoton(Icons.Outlined.Delete) },
            esDestructivo = true,
            estaHabilitado = !estadoVisual.estaEliminando,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (estadoVisual.esDialogoEliminarVisible) {
        DialogoConfirmacion(
            titulo = "Eliminar recorrido",
            texto = if (estadoVisual.esCaminataLibre) {
                "¿Querés eliminar esta caminata? Se borra también de tu cuenta en la nube y se descartan los puntos que marcaste si todavía no los pasaste a un sendero. No se puede deshacer."
            } else {
                "¿Querés eliminar este recorrido? Se borra también de tu cuenta en la nube. No se puede deshacer."
            },
            textoConfirmar = "Eliminar",
            alConfirmar = { alProcesarEvento(HistorialDetalleEvento.AlConfirmarEliminar) },
            textoDescartar = "Cancelar",
            alDescartar = { alProcesarEvento(HistorialDetalleEvento.AlCancelarEliminar) },
            esDestructivo = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorialDetalleInexistentePreview() {
    TemaSenderos {
        HistorialDetalleContenido(
            estadoVisual = HistorialDetalleEstadoVisual(estaCargando = false, mensajeError = "No se encontró el recorrido"),
            alProcesarEvento = {}
        )
    }
}
