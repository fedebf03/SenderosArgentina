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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.AddLocationAlt
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.presentation.componentes.TituloDeSeccion
import com.unaj.senderos.presentation.comun.ContenidoRevisionPuntos
import com.unaj.senderos.presentation.comun.RevisionPuntosEstadoVisual
import com.unaj.senderos.presentation.comun.describirCantidadPuntosCapturados
import com.unaj.senderos.presentation.comun.limpiarFotosTemporalesViejas
import com.unaj.senderos.presentation.comun.recordarAccionesDeFoto
import com.unaj.senderos.presentation.mapa.MapaRecorrido
import com.unaj.senderos.presentation.mapa.ModoNavegacion
import com.unaj.senderos.presentation.tema.TemaSenderos
import android.util.Log
import kotlinx.coroutines.delay

private const val TAG = "RECORRIDO"
private const val INTERVALO_RELOJ_MILISEGUNDOS = 1_000L
private const val TEXTO_SIN_SENAL = "—"
private const val TEXTO_NO_DISPONIBLE_EN_EQUIPO = "No disponible en este equipo"

// Mostrar la pantalla "Recorrido en curso" conectada a su ViewModel
@Composable
fun RecorridoPantalla(
    viewModel: RecorridoViewModel,
    alVolverAlDetalle: () -> Unit,
    solicitarCancelacion: Boolean = false,
    alAtenderSolicitudCancelacion: () -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverAlDetalleActual by rememberUpdatedState(alVolverAlDetalle)
    val contexto = LocalContext.current
    val accionesDeFoto = recordarAccionesDeFoto(
        alSeleccionarFoto = { uri -> viewModel.procesarEvento(RecorridoEvento.AlSeleccionarFotoPunto(uri)) },
        alMostrarAviso = { mensaje -> viewModel.procesarEvento(RecorridoEvento.AlFallarFotoPunto(mensaje)) }
    )

    // Actualizar el reloj cada segundo para que el tiempo avance aunque el GPS no emita
    val ahoraMilisegundos by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(INTERVALO_RELOJ_MILISEGUNDOS)
            value = System.currentTimeMillis()
        }
    }

    // Abrir la confirmación de cancelación pedida desde la notificación; nunca cancela sin que el usuario confirme
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
        estadoAvisos = estadoAvisos,
        alSacarFotoPunto = accionesDeFoto.sacarFoto,
        alElegirFotoPuntoDeGaleria = accionesDeFoto.elegirDeGaleria
    )
}

// Renderizar el recorrido en curso a partir del estado visual
@Composable
fun RecorridoContenido(
    estadoVisual: RecorridoEstadoVisual,
    ahoraMilisegundos: Long,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() },
    alSacarFotoPunto: () -> Unit = {},
    alElegirFotoPuntoDeGaleria: () -> Unit = {}
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
    estadoVisual.resumen?.let { resumen -> DialogoResumen(resumen, estadoVisual.revision, alProcesarEvento) }
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
            CartelDeSenal(estadoVisual.calidadSenal)
            TituloDeSeccion("Recorrido en curso")
            Text(text = nombreSendero, style = MaterialTheme.typography.bodyMedium)
            if (estadoVisual.estaDetenido) {
                IndicadorDetenido()
            }
            GrillaDeDatos(estadoVisual, ahoraMilisegundos)
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
            if (estadoVisual.cantidadPuntosCapturados > 0) {
                Text(
                    text = describirCantidadPuntosCapturados(estadoVisual.cantidadPuntosCapturados),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (estadoVisual.puedeAgregarPunto) {
                OutlinedButton(
                    onClick = { alProcesarEvento(RecorridoEvento.AlTocarAgregarPunto) },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.AddLocationAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Agregar punto")
                }
            }
            Button(
                onClick = { alProcesarEvento(RecorridoEvento.AlTocarFinalizar) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = if (estadoVisual.llegoAlFinal) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            ) {
                Text("Finalizar")
            }
            TextButton(
                onClick = { alProcesarEvento(RecorridoEvento.AlTocarCancelarRecorrido) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancelar recorrido")
            }
        }
    }
}

// Mostrar en una grilla de dos columnas las magnitudes del recorrido y de los sensores
@Composable
private fun GrillaDeDatos(estadoVisual: RecorridoEstadoVisual, ahoraMilisegundos: Long) {
    val sinAcelerometro = estadoVisual.faltaAcelerometro
    val confiable = estadoVisual.sonConfiablesLosDatosDeDistancia
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilaDeDatos(
            DatoDeGrilla(
                "Recorrido",
                if (confiable) formatearDistanciaRecorrido(estadoVisual.distanciaRecorridaKilometros) else TEXTO_SIN_SENAL
            ),
            DatoDeGrilla(
                "Restante",
                if (confiable) formatearDistanciaRecorrido(estadoVisual.distanciaRestanteKilometros) else TEXTO_SIN_SENAL
            )
        )
        FilaDeDatos(
            DatoDeGrilla("Tiempo", formatearTiempoTranscurrido(ahoraMilisegundos - estadoVisual.instanteInicioMilisegundos)),
            DatoDeGrilla(
                "Velocidad",
                if (confiable) formatearVelocidad(estadoVisual.velocidadActualMetrosPorSegundo) else TEXTO_SIN_SENAL
            )
        )
        FilaDeDatos(
            DatoDeGrilla(
                "Vel. promedio",
                if (confiable) formatearVelocidadPromedio(estadoVisual.velocidadPromedioKilometrosPorHora) else TEXTO_SIN_SENAL
            ),
            DatoDeGrilla(
                titulo = "Pasos",
                valor = if (sinAcelerometro) TEXTO_NO_DISPONIBLE_EN_EQUIPO else formatearPasosDelPanel(estadoVisual.pasos),
                esNoDisponible = sinAcelerometro
            )
        )
        FilaDeDatos(
            DatoDeGrilla(
                titulo = "Ritmo",
                valor = if (sinAcelerometro) TEXTO_NO_DISPONIBLE_EN_EQUIPO else formatearRitmoDelPanel(estadoVisual.ritmoPasosPorMinuto),
                esNoDisponible = sinAcelerometro
            ),
            armarDatoRumbo(estadoVisual)
        )
    }
}

// Datos de una celda de la grilla: título, valor y si el equipo no puede medirlo
private class DatoDeGrilla(val titulo: String, val valor: String, val esNoDisponible: Boolean = false, val rumboGrados: Float? = null)

// Armar el dato del rumbo: sin rumbo conocido y sin brújula se indica que no está disponible en el equipo
private fun armarDatoRumbo(estadoVisual: RecorridoEstadoVisual): DatoDeGrilla {
    val rumbo = estadoVisual.rumboGrados
    val sinBrujulaNiRumbo = rumbo == null && !estadoVisual.tieneBrujula
    return DatoDeGrilla(
        titulo = "Rumbo",
        valor = if (sinBrujulaNiRumbo) TEXTO_NO_DISPONIBLE_EN_EQUIPO else formatearRumbo(rumbo),
        esNoDisponible = sinBrujulaNiRumbo,
        rumboGrados = rumbo
    )
}

// Mostrar dos datos lado a lado con el mismo ancho
@Composable
private fun FilaDeDatos(izquierdo: DatoDeGrilla, derecho: DatoDeGrilla) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CeldaDelPanel(izquierdo, Modifier.weight(1f))
        CeldaDelPanel(derecho, Modifier.weight(1f))
    }
}

// Mostrar un cartel mientras se busca la señal GPS o cuando se perdió; con buena señal no muestra nada
@Composable
private fun CartelDeSenal(calidadSenal: CalidadSenal) {
    if (calidadSenal == CalidadSenal.BUENA) return
    val estaBuscando = calidadSenal == CalidadSenal.BUSCANDO
    Surface(
        color = if (estaBuscando) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (estaBuscando) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (estaBuscando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Filled.GpsOff, contentDescription = null)
            }
            Text(
                text = if (estaBuscando) {
                    "Buscando señal GPS…"
                } else {
                    "Sin señal GPS: salí al aire libre, lejos de techos y árboles"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Mostrar el aviso de que el usuario está quieto
@Composable
private fun IndicadorDetenido() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Detenido",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// Mostrar una celda de la grilla; el rumbo suma una flecha girada hacia donde se mira
@Composable
private fun CeldaDelPanel(dato: DatoDeGrilla, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = dato.titulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dato.rumboGrados != null) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = "Rumbo ${dato.valor}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).rotate(dato.rumboGrados)
                )
                Spacer(Modifier.width(6.dp))
            }
            if (dato.esNoDisponible) {
                Text(
                    text = dato.valor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(text = dato.valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

// Avisar la llegada al final del sendero y ofrecer finalizar el recorrido o seguir caminando
@Composable
private fun DialogoLlegadaAlFinal(nombreSendero: String, alProcesarEvento: (RecorridoEvento) -> Unit) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(RecorridoEvento.AlSeguirCaminando) },
        title = { Text("¡Llegaste al final!") },
        text = { Text("Terminaste $nombreSendero.") },
        confirmButton = {
            TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlConfirmarFinalizar) }) {
                Text("Finalizar recorrido")
            }
        },
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlSeguirCaminando) }) {
                Text("Seguir caminando")
            }
        }
    )
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

// Pedir confirmación antes de descartar el recorrido sin guardarlo
@Composable
private fun DialogoConfirmarCancelar(alProcesarEvento: (RecorridoEvento) -> Unit) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(RecorridoEvento.AlDescartarCancelacion) },
        title = { Text("Cancelar recorrido") },
        text = { Text("¿Cancelar el recorrido? Se descartará lo recorrido y no quedará en el historial") },
        confirmButton = {
            TextButton(
                onClick = { alProcesarEvento(RecorridoEvento.AlConfirmarCancelarRecorrido) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancelar recorrido")
            }
        },
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(RecorridoEvento.AlDescartarCancelacion) }) {
                Text("Seguir")
            }
        }
    )
}

// Mostrar el resumen con la distancia y el tiempo, y si hay puntos capturados, su revisión con casillas
@Composable
private fun DialogoResumen(
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
                DatoDelPanel("Distancia recorrida", formatearDistanciaRecorrido(resumen.distanciaRecorridaKilometros))
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
