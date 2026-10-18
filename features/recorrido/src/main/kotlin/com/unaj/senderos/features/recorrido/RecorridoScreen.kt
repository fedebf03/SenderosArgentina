package com.unaj.senderos.features.recorrido

import android.util.Log
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.ui.tema.obtenerColorLineaBioma
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.unaj.senderos.core.ui.sendero.TarjetaPuntoSeleccionado
import com.unaj.senderos.core.ui.sendero.obtenerPuntosDeInteres
import com.unaj.senderos.core.ui.revisionpuntos.DialogoFotoAmpliada
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.ui.componentes.BarraSuperior
import com.unaj.senderos.core.ui.componentes.mapa.MapaRecorrido
import com.unaj.senderos.core.ui.componentes.mapa.ModoNavegacion
import com.unaj.senderos.core.ui.comun.limpiarFotosTemporalesViejas
import com.unaj.senderos.core.ui.comun.recordarAccionesDeFoto
import com.unaj.senderos.core.ui.tema.TemaSenderos
import kotlinx.coroutines.delay

private const val TAG = "RECORRIDO"
private const val INTERVALO_RELOJ_MILISEGUNDOS = 1_000L

private const val FRACCION_ALTURA_PANEL = 0.45f

private const val FRACCION_ANCHO_PANEL = 0.4f
private val ANCHO_MINIMO_PANEL = 300.dp
private val ANCHO_MAXIMO_PANEL = 420.dp
private val TAMANO_BOTON_AGREGAR_PUNTO = 72.dp
private val TAMANO_ICONO_AGREGAR_PUNTO = 32.dp

// Conectar la pantalla del recorrido con su ViewModel: efectos, fotos de los puntos, reloj y pedido de cancelación desde la notificación
@Composable
fun RecorridoPantalla(
    viewModel: RecorridoViewModel,
    alVolverAlDetalle: () -> Unit,
    alNavegarAFormularioDesdeCaminata: (String) -> Unit = {},
    solicitarCancelacion: Boolean = false,
    alAtenderSolicitudCancelacion: () -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverAlDetalleActual by rememberUpdatedState(alVolverAlDetalle)
    val navegarAFormularioActual by rememberUpdatedState(alNavegarAFormularioDesdeCaminata)
    val contexto = LocalContext.current
    val accionesDeFoto = recordarAccionesDeFoto(
        alSeleccionarFoto = { uri -> viewModel.procesarEvento(RecorridoEvento.AlSeleccionarFotoPunto(uri)) },
        alMostrarAviso = { mensaje -> viewModel.procesarEvento(RecorridoEvento.AlFallarFotoPunto(mensaje)) }
    )

    // Actualizar el reloj cada segundo para que el tiempo avance aunque el GPS no emita
    val ahoraMilisegundos = produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(INTERVALO_RELOJ_MILISEGUNDOS)
            value = System.currentTimeMillis()
        }
    }

    // Nunca cancela sin que el usuario confirme
    LaunchedEffect(solicitarCancelacion) {
        if (solicitarCancelacion) {
            viewModel.procesarEvento(RecorridoEvento.AlTocarCancelarRecorrido)
            alAtenderSolicitudCancelacion()
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Pantalla inicializada / rotación completada")
        limpiarFotosTemporalesViejas(contexto)
        viewModel.efectoVisual.collect { efecto ->
            Log.d(TAG, "Efecto recibido: $efecto")
            when (efecto) {
                is RecorridoEfecto.VolverAlDetalle -> volverAlDetalleActual()
                is RecorridoEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
                is RecorridoEfecto.NavegarAFormularioDesdeCaminata -> navegarAFormularioActual(efecto.idCaminata)
            }
        }
    }

    RecorridoContenido(
        estadoVisual = estadoVisual,
        leerAhoraMilisegundos = { ahoraMilisegundos.value },
        alProcesarEvento = { evento ->
            Log.d(TAG, "Evento del usuario: $evento")
            viewModel.procesarEvento(evento)
        },
        estadoAvisos = estadoAvisos,
        alSacarFotoPunto = accionesDeFoto.sacarFoto,
        alElegirFotoPuntoDeGaleria = accionesDeFoto.elegirDeGaleria
    )
}

// Dibujar el recorrido en curso: mapa con el botón "+" para marcar puntos, panel de datos, diálogos y hoja del punto
@Composable
fun RecorridoContenido(
    estadoVisual: RecorridoEstadoVisual,
    leerAhoraMilisegundos: () -> Long,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() },
    alSacarFotoPunto: () -> Unit = {},
    alElegirFotoPuntoDeGaleria: () -> Unit = {}
) {
    BackHandler { alProcesarEvento(RecorridoEvento.AlVolver) }

    var fotoAmpliada by remember { mutableStateOf<String?>(null) }
    val sigueActivo = estadoVisual.fase == FaseRecorrido.EN_CURSO
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        topBar = {
            BarraSuperior(
                titulo = if (estadoVisual.esCaminataLibre) "Caminata libre" else estadoVisual.sendero?.nombre?.ifBlank { null } ?: "Recorrido en curso",
                alVolver = { alProcesarEvento(RecorridoEvento.AlVolver) },
                descripcionVolver = if (sigueActivo) "Volver. El recorrido sigue activo" else "Volver"
            )
        }
    ) { espaciadoInterno ->
        val sendero = estadoVisual.sendero
        val mostrarRecorrido = estadoVisual.fase == FaseRecorrido.EN_CURSO || estadoVisual.resumen != null
        when {
            estadoVisual.fase == FaseRecorrido.NO_ACTIVO && estadoVisual.resumen == null -> MensajeRecorridoInactivo(
                alVolver = { alProcesarEvento(RecorridoEvento.AlVolver) },
                modifier = Modifier.fillMaxSize().padding(espaciadoInterno)
            )
            !mostrarRecorrido || (sendero == null && !estadoVisual.esCaminataLibre) -> Box(
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
                            puntos = sendero?.recorrido.orEmpty(),
                            tipoRecorrido = sendero?.tipoRecorrido ?: TipoRecorrido.LINEAL,
                            modifier = Modifier.fillMaxSize(),
                            colorLinea = obtenerColorLineaBioma(sendero?.bioma ?: Bioma.SIN_DEFINIR, MaterialTheme.colorScheme.primary),
                            posicionUsuario = estadoVisual.posicionUsuario,
                            trazadoCaminado = estadoVisual.trazadoCaminado,
                            indiceSeleccionado = estadoVisual.indicePuntoSeleccionado,
                            alSeleccionarPunto = { indice -> alProcesarEvento(RecorridoEvento.AlSeleccionarPuntoEnMapa(indice)) },
                            modoNavegacion = ModoNavegacion(
                                rumboGrados = estadoVisual.rumboGrados,
                                estaSiguiendo = estadoVisual.estaSiguiendo,
                                alMoverConGesto = { alProcesarEvento(RecorridoEvento.AlMoverMapaConGesto) }
                            )
                        )
                        Column(
                            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DatosFlotantesRecorrido(estadoVisual, leerAhoraMilisegundos)
                            sendero?.let { obtenerPuntosDeInteres(it) }.orEmpty()
                                .firstOrNull { punto -> punto.indice == estadoVisual.indicePuntoSeleccionado }
                                ?.let { punto ->
                                    TarjetaPuntoSeleccionado(
                                        punto = punto,
                                        alAmpliarFoto = { foto -> fotoAmpliada = foto },
                                        alCerrar = { alProcesarEvento(RecorridoEvento.AlSeleccionarPuntoEnMapa(null)) },
                                        alturaFoto = 110.dp
                                    )
                                }
                        }
                        Column(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!estadoVisual.estaSiguiendo) {
                                ExtendedFloatingActionButton(
                                    onClick = { alProcesarEvento(RecorridoEvento.AlTocarCentrar) },
                                    icon = { Icon(imageVector = Icons.Filled.MyLocation, contentDescription = null) },
                                    text = { Text("Centrar") }
                                )
                            }
                            if (estadoVisual.puedeAgregarPunto) {
                                // Grande y siempre a mano para marcar lo que se ve sin frenar, como en Waze: abre la grilla de tipos; la foto es opcional
                                FloatingActionButton(
                                    onClick = { alProcesarEvento(RecorridoEvento.AlTocarAgregarPunto) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(TAMANO_BOTON_AGREGAR_PUNTO)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Marcar un punto de interés",
                                        modifier = Modifier.size(TAMANO_ICONO_AGREGAR_PUNTO)
                                    )
                                }
                            }
                        }
                    }
                }
                val panel: @Composable (Modifier) -> Unit = { modificadorPanel ->
                    PanelRecorrido(
                        estadoVisual = estadoVisual,
                        alProcesarEvento = alProcesarEvento,
                        modifier = modificadorPanel
                    )
                }
                if (maxWidth > maxHeight) {
                    val anchoPanel = (maxWidth * FRACCION_ANCHO_PANEL).coerceIn(ANCHO_MINIMO_PANEL, ANCHO_MAXIMO_PANEL)
                    Row(modifier = Modifier.fillMaxSize()) {
                        mapa(Modifier.weight(1f).fillMaxHeight())
                        panel(Modifier.width(anchoPanel).fillMaxHeight())
                    }
                } else {
                    val alturaMaximaPanel = maxHeight * FRACCION_ALTURA_PANEL
                    Column(modifier = Modifier.fillMaxSize()) {
                        mapa(Modifier.weight(1f).fillMaxWidth())
                        panel(Modifier.fillMaxWidth().heightIn(max = alturaMaximaPanel))
                    }
                }
            }
        }
    }

    if (estadoVisual.esDialogoFinalizarVisible) {
        DialogoConfirmarFinalizar(alProcesarEvento)
    }
    if (estadoVisual.esDialogoCancelarVisible) {
        DialogoConfirmarCancelar(alProcesarEvento)
    }
    if (estadoVisual.esDialogoLlegadaVisible) {
        DialogoLlegadaAlFinal(
            nombreSendero = estadoVisual.nombreSendero.ifBlank { estadoVisual.sendero?.nombre.orEmpty() },
            alProcesarEvento = alProcesarEvento
        )
    }
    if (estadoVisual.esHojaPuntoVisible) {
        HojaCapturaPunto(
            estadoVisual = estadoVisual,
            alProcesarEvento = alProcesarEvento,
            alSacarFoto = alSacarFotoPunto,
            alElegirDeGaleria = alElegirFotoPuntoDeGaleria
        )
    }
    estadoVisual.resumen?.let { resumen ->
        DialogoResumen(resumen, estadoVisual.revision, estadoVisual.esCaminataLibre, estadoVisual.sePuedeConvertirEnSendero, alProcesarEvento)
    }
    fotoAmpliada?.let { foto -> DialogoFotoAmpliada(foto = foto, alCerrar = { fotoAmpliada = null }) }
}

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
@Preview(showBackground = true)
@Composable
private fun RecorridoInactivoPreview() {
    TemaSenderos {
        RecorridoContenido(
            estadoVisual = RecorridoEstadoVisual(fase = FaseRecorrido.NO_ACTIVO),
            leerAhoraMilisegundos = { 0L },
            alProcesarEvento = {}
        )
    }
}
