package com.unaj.senderos.presentation.recorrido

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.presentation.componentes.TituloDeSeccion
import com.unaj.senderos.presentation.mapa.MapaRecorrido
import com.unaj.senderos.presentation.mapa.ModoNavegacion
import com.unaj.senderos.presentation.tema.TemaSenderos
import android.util.Log
import kotlinx.coroutines.delay

private const val TAG = "RECORRIDO"
private const val INTERVALO_RELOJ_MILISEGUNDOS = 1_000L

// Mostrar la pantalla "Recorrido en curso" conectada a su ViewModel
@Composable
fun RecorridoPantalla(
    viewModel: RecorridoViewModel,
    alVolverAlDetalle: () -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverAlDetalleActual by rememberUpdatedState(alVolverAlDetalle)

    // Actualizar el reloj cada segundo para que el tiempo avance aunque el GPS no emita
    val ahoraMilisegundos by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(INTERVALO_RELOJ_MILISEGUNDOS)
            value = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Pantalla inicializada / rotación completada")
        viewModel.efectoVisual.collect { efecto ->
            Log.d(TAG, "Efecto recibido: $efecto")
            when (efecto) {
                is RecorridoEfecto.VolverAlDetalle -> volverAlDetalleActual()
                is RecorridoEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
            }
        }
    }

    RecorridoContenido(
        estadoVisual = estadoVisual,
        ahoraMilisegundos = ahoraMilisegundos,
        alProcesarEvento = { evento ->
            Log.d(TAG, "Evento del usuario: $evento")
            viewModel.procesarEvento(evento)
        },
        estadoAvisos = estadoAvisos
    )
}

// Renderizar el recorrido en curso a partir del estado visual
@Composable
fun RecorridoContenido(
    estadoVisual: RecorridoEstadoVisual,
    ahoraMilisegundos: Long,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    BackHandler { alProcesarEvento(RecorridoEvento.AlVolver) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) }
    ) { espaciadoInterno ->
        val sendero = estadoVisual.sendero
        val mostrarRecorrido = estadoVisual.fase == FaseRecorrido.EN_CURSO || estadoVisual.resumen != null
        when {
            estadoVisual.fase == FaseRecorrido.NO_ACTIVO && estadoVisual.resumen == null -> MensajeRecorridoInactivo(
                alVolver = { alProcesarEvento(RecorridoEvento.AlVolver) },
                modifier = Modifier.fillMaxSize().padding(espaciadoInterno)
            )
            !mostrarRecorrido || sendero == null -> Box(
                modifier = Modifier.fillMaxSize().padding(espaciadoInterno),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(text = "Iniciando el recorrido", style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(espaciadoInterno)) {
                val mapa: @Composable (Modifier) -> Unit = { modificadorMapa ->
                    Box(modifier = modificadorMapa) {
                        MapaRecorrido(
                            puntos = sendero.recorrido,
                            tipoRecorrido = sendero.tipoRecorrido,
                            modifier = Modifier.fillMaxSize(),
                            posicionUsuario = estadoVisual.posicionUsuario,
                            modoNavegacion = ModoNavegacion(
                                rumboGrados = estadoVisual.rumboGrados,
                                estaSiguiendo = estadoVisual.estaSiguiendo,
                                alMoverConGesto = { alProcesarEvento(RecorridoEvento.AlMoverMapaConGesto) }
                            )
                        )
                        if (!estadoVisual.estaSiguiendo) {
                            ExtendedFloatingActionButton(
                                onClick = { alProcesarEvento(RecorridoEvento.AlTocarCentrar) },
                                icon = { Icon(imageVector = Icons.Filled.MyLocation, contentDescription = null) },
                                text = { Text("Centrar") },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            )
                        }
                    }
                }
                val panel: @Composable (Modifier) -> Unit = { modificadorPanel ->
                    PanelRecorrido(
                        nombreSendero = sendero.nombre,
                        estadoVisual = estadoVisual,
                        ahoraMilisegundos = ahoraMilisegundos,
                        alProcesarEvento = alProcesarEvento,
                        modifier = modificadorPanel
                    )
                }
                if (maxWidth > maxHeight) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        mapa(Modifier.weight(1f).fillMaxHeight())
                        panel(Modifier.width(320.dp).fillMaxHeight())
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        mapa(Modifier.weight(1f).fillMaxWidth())
                        panel(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    if (estadoVisual.esDialogoFinalizarVisible) {
        DialogoConfirmarFinalizar(alProcesarEvento)
    }
    estadoVisual.resumen?.let { resumen -> DialogoResumen(resumen, alProcesarEvento) }
}

// Mostrar que no hay un recorrido activo con un botón para volver al detalle
@Composable
private fun MensajeRecorridoInactivo(alVolver: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "El recorrido no está activo",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = alVolver) {
            Text("Volver")
        }
    }
}

// Mostrar el panel con las magnitudes del recorrido, el próximo punto, el desvío y el botón Finalizar
@Composable
private fun PanelRecorrido(
    nombreSendero: String,
    estadoVisual: RecorridoEstadoVisual,
    ahoraMilisegundos: Long,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (estadoVisual.esSimulado) {
                ControlesSimulacion(estadoVisual.multiplicadorSimulacion, alProcesarEvento)
            }
            TituloDeSeccion("Recorrido en curso")
            Text(text = nombreSendero, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatoDelPanel("Recorrido", formatearDistanciaRecorrido(estadoVisual.distanciaRecorridaKilometros), Modifier.weight(1f))
                DatoDelPanel("Restante", formatearDistanciaRecorrido(estadoVisual.distanciaRestanteKilometros), Modifier.weight(1f))
                DatoDelPanel(
                    titulo = "Tiempo",
                    valor = formatearTiempoTranscurrido(ahoraMilisegundos - estadoVisual.instanteInicioMilisegundos),
                    modifier = Modifier.weight(1f)
                )
            }
            estadoVisual.proximoPunto?.let { punto ->
                DatoDelPanel("Próximo punto", obtenerEtiquetaPunto(punto), Modifier.fillMaxWidth())
            }
            if (estadoVisual.estaDesviado) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Te alejaste del sendero. Volvé al trazado para seguir la ruta.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Button(
                onClick = { alProcesarEvento(RecorridoEvento.AlTocarFinalizar) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Finalizar")
            }
        }
    }
}

// Mostrar el cartel de simulación con el selector de velocidad y el botón para simular un desvío
@Composable
private fun ControlesSimulacion(multiplicador: Int, alProcesarEvento: (RecorridoEvento) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "SIMULACIÓN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Velocidad", style = MaterialTheme.typography.bodyMedium)
                MULTIPLICADORES_SIMULACION.forEach { valor ->
                    FilterChip(
                        selected = valor == multiplicador,
                        onClick = { alProcesarEvento(RecorridoEvento.AlSeleccionarMultiplicador(valor)) },
                        label = { Text("×$valor") }
                    )
                }
            }
            OutlinedButton(
                onClick = { alProcesarEvento(RecorridoEvento.AlTocarSimularDesvio) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simular desvío")
            }
        }
    }
}

// Mostrar un dato del panel con su título y su valor destacado
@Composable
private fun DatoDelPanel(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// Pedir confirmación antes de finalizar el recorrido
@Composable
private fun DialogoConfirmarFinalizar(alProcesarEvento: (RecorridoEvento) -> Unit) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(RecorridoEvento.AlCancelarFinalizar) },
        title = { Text("Finalizar recorrido") },
        text = { Text("¿Querés finalizar el recorrido? Se dejará de seguir tu ubicación.") },
        confirmButton = {
            TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlConfirmarFinalizar) }) {
                Text("Finalizar")
            }
        },
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlCancelarFinalizar) }) {
                Text("Cancelar")
            }
        }
    )
}

// Mostrar el resumen con la distancia recorrida y el tiempo al finalizar
@Composable
private fun DialogoResumen(resumen: ResumenRecorrido, alProcesarEvento: (RecorridoEvento) -> Unit) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(RecorridoEvento.AlCerrarResumen) },
        title = { Text("Recorrido finalizado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DatoDelPanel("Distancia recorrida", formatearDistanciaRecorrido(resumen.distanciaRecorridaKilometros))
                DatoDelPanel("Tiempo", formatearTiempoTranscurrido(resumen.tiempoTranscurridoMilisegundos))
            }
        },
        confirmButton = {
            TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlCerrarResumen) }) {
                Text("Cerrar")
            }
        }
    )
}

// Previsualizar la pantalla cuando no hay un recorrido activo
@Preview(showBackground = true)
@Composable
private fun RecorridoInactivoPreview() {
    TemaSenderos {
        RecorridoContenido(
            estadoVisual = RecorridoEstadoVisual(fase = FaseRecorrido.NO_ACTIVO),
            ahoraMilisegundos = 0L,
            alProcesarEvento = {}
        )
    }
}
