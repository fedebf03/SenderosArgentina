package com.unaj.senderos.presentation.detalle

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.presentation.componentes.TituloDeSeccion
import com.unaj.senderos.presentation.tema.TemaSenderos

@Composable
fun DetalleSenderoPantalla(
    viewModel: DetalleSenderoViewModel,
    alVolverAlListado: () -> Unit,
    alNavegarAEdicion: (String) -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverAlListadoActual by rememberUpdatedState(alVolverAlListado)
    val navegarAEdicionActual by rememberUpdatedState(alNavegarAEdicion)
    val contextoActual by rememberUpdatedState(LocalContext.current)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is DetalleSenderoEfecto.VolverAlListado -> volverAlListadoActual()
                is DetalleSenderoEfecto.NavegarAEdicion -> navegarAEdicionActual(efecto.idSendero)
                is DetalleSenderoEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
                is DetalleSenderoEfecto.CompartirSendero -> {
                    val intencionCompartir = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, efecto.texto)
                    }
                    contextoActual.startActivity(Intent.createChooser(intencionCompartir, "Compartir sendero"))
                }
            }
        }
    }

    DetalleSenderoContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        estadoAvisos = estadoAvisos
    )
}

@Composable
fun DetalleSenderoContenido(
    estadoVisual: DetalleSenderoEstadoVisual,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    BackHandler { alProcesarEvento(DetalleSenderoEvento.AlVolver) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        bottomBar = {
            if (estadoVisual.sendero != null) {
                BotonIniciarRecorrido(alProcesarEvento)
            }
        }
    ) { espaciadoInterno ->
        val sendero = estadoVisual.sendero
        when {
            estadoVisual.estaCargando -> Box(
                Modifier.fillMaxSize().padding(espaciadoInterno),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            estadoVisual.mensajeError != null || sendero == null -> Column(
                modifier = Modifier.fillMaxSize().padding(espaciadoInterno).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = estadoVisual.mensajeError ?: "No se encontró el sendero",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = { alProcesarEvento(DetalleSenderoEvento.AlVolver) }) {
                    Text("Volver")
                }
            }
            else -> CuerpoDetalle(
                estadoVisual = estadoVisual,
                sendero = sendero,
                alProcesarEvento = alProcesarEvento,
                modifier = Modifier.padding(bottom = espaciadoInterno.calculateBottomPadding())
            )
        }
    }

    if (estadoVisual.esDialogoEliminarVisible && estadoVisual.sendero != null) {
        AlertDialog(
            onDismissRequest = { alProcesarEvento(DetalleSenderoEvento.AlCancelarEliminar) },
            title = { Text("Eliminar sendero") },
            text = {
                Text("¿Querés eliminar ${estadoVisual.sendero.nombre}? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = { alProcesarEvento(DetalleSenderoEvento.AlConfirmarEliminar) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { alProcesarEvento(DetalleSenderoEvento.AlCancelarEliminar) }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun BotonIniciarRecorrido(alProcesarEvento: (DetalleSenderoEvento) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Button(
            onClick = { alProcesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Iniciar recorrido")
        }
    }
}

@Composable
private fun CuerpoDetalle(
    estadoVisual: DetalleSenderoEstadoVisual,
    sendero: Sendero,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CabeceraDetalle(estadoVisual, sendero, alProcesarEvento) }
        item {
            DatosDelRecorrido(sendero, modifier = Modifier.padding(horizontal = 24.dp))
        }
        item { RecuadroMapa(sendero = sendero, modifier = Modifier.padding(horizontal = 24.dp)) }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TituloDeSeccion("Descripción")
                Text(text = sendero.descripcion, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SeccionDesplegable(
                    titulo = "Recomendaciones",
                    estaExpandida = SeccionDetalle.RECOMENDACIONES in estadoVisual.seccionesExpandidas,
                    alAlternar = {
                        alProcesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.RECOMENDACIONES))
                    }
                ) {
                    ListaConVinietas(sendero.recomendaciones)
                }
                HorizontalDivider()
                SeccionDesplegable(
                    titulo = "Servicios en el camino",
                    estaExpandida = SeccionDetalle.SERVICIOS in estadoVisual.seccionesExpandidas,
                    alAlternar = {
                        alProcesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.SERVICIOS))
                    }
                ) {
                    ListaConVinietas(sendero.servicios)
                }
                HorizontalDivider()
                SeccionDesplegable(
                    titulo = "Etapas",
                    estaExpandida = SeccionDetalle.ETAPAS in estadoVisual.seccionesExpandidas,
                    alAlternar = {
                        alProcesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.ETAPAS))
                    }
                ) {
                    ListaDeEtapas(sendero.etapas)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalleSenderoContenidoPreview() {
    TemaSenderos {
        DetalleSenderoContenido(
            estadoVisual = DetalleSenderoEstadoVisual(
                sendero = Sendero(
                    id = "huella-andina",
                    idUsuario = "usuario-1",
                    nombre = "Huella Andina",
                    region = "Neuquén, Río Negro y Chubut",
                    distanciaKilometros = 570.0,
                    dificultad = Dificultad.MEDIA,
                    descripcion = "Travesía de largo recorrido que une lagos, bosques andino-patagónicos y pasos de montaña a lo largo de la cordillera.",
                    desnivelMetros = 1200,
                    duracionEstimada = "30 días",
                    alturaMaximaMetros = 1900,
                    alturaMinimaMetros = 700,
                    tipoRecorrido = TipoRecorrido.LINEAL,
                    puntoInicio = "Lago Aluminé, Neuquén",
                    mejorEpoca = "Diciembre a marzo",
                    recomendaciones = listOf("Llevar agua y filtro potabilizador"),
                    servicios = listOf("Campings organizados"),
                    etapas = listOf(Etapa(1, "Lago Aluminé - Lago Ruca Choroi", 21.0))
                ),
                estaCargando = false
            ),
            alProcesarEvento = {}
        )
    }
}
