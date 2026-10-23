package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.ui.sendero.ListaDeEtapas
import com.unaj.senderos.core.ui.sendero.ListaConVinietas
import com.unaj.senderos.core.ui.sendero.TarjetaPuntoSeleccionado
import com.unaj.senderos.core.ui.sendero.RecuadroMapa
import com.unaj.senderos.core.ui.sendero.DatosDelRecorrido
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.revisionpuntos.DialogoFotoAmpliada
import com.unaj.senderos.core.ui.revisionpuntos.DialogoRevisionPuntos
import com.unaj.senderos.core.ui.comun.PERMISOS_NOTIFICACIONES
import com.unaj.senderos.core.ui.comun.PERMISOS_UBICACION
import com.unaj.senderos.core.ui.comun.mostrarAvisoDePermiso
import com.unaj.senderos.core.ui.comun.recordarPedidoDePermisos
import com.unaj.senderos.core.ui.revisionpuntos.RevisionPuntosEvento
import com.unaj.senderos.core.ui.comun.tienePermisoDeUbicacionPrecisa
import com.unaj.senderos.core.ui.tema.TemaSenderos

private const val TAG = "DETALLE_SENDERO"

// Si se agrega un elemento antes del mapa en la lista del detalle, este índice tiene que cambiar
private const val INDICE_ELEMENTO_MAPA = 2

private const val CANTIDAD_MINIMA_PUNTOS_PERFIL = 2

@Composable
fun DetalleSenderoPantalla(
    viewModel: DetalleSenderoViewModel,
    alVolverAlListado: () -> Unit,
    alNavegarAEdicion: (String) -> Unit,
    alNavegarARecorrido: (String) -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val volverAlListadoActual by rememberUpdatedState(alVolverAlListado)
    val navegarAEdicionActual by rememberUpdatedState(alNavegarAEdicion)
    val navegarARecorridoActual by rememberUpdatedState(alNavegarARecorrido)
    val contextoActual by rememberUpdatedState(LocalContext.current)
    // El recorrido sigue aunque se niegue el permiso de notificaciones. Solo se avisa lo que se pierde
    val pedirPermisoNotificaciones = recordarPedidoDePermisos(
        permisos = PERMISOS_NOTIFICACIONES,
        explicacion = "Con las notificaciones ves el recorrido en curso y sus avisos aunque la pantalla esté apagada"
    ) { respuesta, negadoParaSiempre ->
        if (respuesta.values.none { concedido -> concedido }) {
            Log.d(TAG, "Permiso de notificaciones denegado")
            alcance.launch {
                estadoAvisos.mostrarAvisoDePermiso(
                    "Sin el permiso de notificaciones no verás los avisos del recorrido con la pantalla apagada",
                    negadoParaSiempre,
                    contextoActual
                )
            }
        }
        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
    }
    val continuarTrasUbicacionPrecisa: () -> Unit = {
        val faltaPermisoNotificaciones = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contextoActual, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (faltaPermisoNotificaciones) {
            pedirPermisoNotificaciones()
        } else {
            viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        }
    }
    val pedirPermisoUbicacion = recordarPedidoDePermisos(
        permisos = PERMISOS_UBICACION,
        explicacion = "Para seguirte durante el recorrido necesitamos tu ubicación precisa"
    ) { resultados, negadoParaSiempre ->
        val esPrecisa = resultados[Manifest.permission.ACCESS_FINE_LOCATION] == true
        when {
            esPrecisa -> continuarTrasUbicacionPrecisa()
            resultados.values.any { concedido -> concedido } ->
                viewModel.procesarEvento(DetalleSenderoEvento.AlConcederSoloUbicacionAproximada)
            negadoParaSiempre -> viewModel.procesarEvento(DetalleSenderoEvento.AlDenegarPermisoUbicacionParaSiempre)
            else -> viewModel.procesarEvento(DetalleSenderoEvento.AlDenegarPermisoUbicacion)
        }
    }

    val abrirSelectorExportacion = recordarSelectoresExportacion(viewModel::procesarEvento)
    val abrirSelectorExportacionActual by rememberUpdatedState(abrirSelectorExportacion)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is DetalleSenderoEfecto.VolverAlListado -> volverAlListadoActual()
                is DetalleSenderoEfecto.NavegarAEdicion -> navegarAEdicionActual(efecto.idSendero)
                is DetalleSenderoEfecto.NavegarARecorrido -> navegarARecorridoActual(efecto.idSendero)
                is DetalleSenderoEfecto.SolicitarPermisoUbicacion -> {
                    if (tienePermisoDeUbicacionPrecisa(contextoActual)) {
                        continuarTrasUbicacionPrecisa()
                    } else {
                        pedirPermisoUbicacion()
                    }
                }
                is DetalleSenderoEfecto.MostrarAviso -> {
                    estadoAvisos.mostrarAvisoDePermiso(efecto.mensaje, efecto.ofrecerAjustes, contextoActual)
                }
                is DetalleSenderoEfecto.ElegirDestinoExportacion -> abrirSelectorExportacionActual(efecto.formato, efecto.nombreArchivo)
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
                BotonIniciarRecorrido(
                    hayRecorridoEnCurso = estadoVisual.hayRecorridoEnCursoEnEsteSendero,
                    esRecorridoSimulado = estadoVisual.esRecorridoEnCursoSimulado,
                    esModoDemostracion = estadoVisual.esModoDemostracion,
                    alProcesarEvento = alProcesarEvento
                )
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

    if (estadoVisual.revision.esRevisionVisible && estadoVisual.hayPuntosPorRevisar) {
        DialogoRevisionPuntos(
            estado = estadoVisual.revision,
            alProcesarEvento = { evento -> alProcesarEvento(DetalleSenderoEvento.AlRevisionPuntos(evento)) }
        )
    }

    estadoVisual.fotoAmpliada?.let { foto ->
        DialogoFotoAmpliada(foto = foto, alCerrar = { alProcesarEvento(DetalleSenderoEvento.AlCerrarFotoAmpliada) })
    }

    estadoVisual.nombreSenderoEnCursoAReemplazar?.let { nombre ->
        DialogoConfirmacion(
            titulo = "Recorrido en curso",
            texto = "Hay un recorrido en curso de $nombre. ¿Querés finalizarlo e iniciar este?",
            textoConfirmar = "Finalizar e iniciar",
            alConfirmar = { alProcesarEvento(DetalleSenderoEvento.AlConfirmarReemplazarRecorrido) },
            textoDescartar = "Seguir con el actual",
            alDescartar = { alProcesarEvento(DetalleSenderoEvento.AlCancelarReemplazarRecorrido) }
        )
    }

    if (estadoVisual.esDialogoSinGpsVisible) {
        DialogoConfirmacion(
            titulo = "Dispositivo sin GPS",
            texto = "El seguimiento usará la ubicación por wifi o red móvil: será impreciso y no funcionará sin señal. " +
                "¿Querés iniciar el recorrido igual?",
            textoConfirmar = "Iniciar igual",
            alConfirmar = { alProcesarEvento(DetalleSenderoEvento.AlIniciarSinGps) },
            textoDescartar = "Cancelar",
            alDescartar = { alProcesarEvento(DetalleSenderoEvento.AlCancelarSinGps) }
        )
    }

    if (estadoVisual.esSugerenciaDescargaVisible) {
        DialogoConfirmacion(
            titulo = "Descargar el mapa",
            texto = "¿Querés descargar el mapa para usarlo sin señal? Podés iniciar el recorrido igual: " +
                "la descarga sigue en segundo plano.",
            textoConfirmar = "Descargar e iniciar",
            alConfirmar = { alProcesarEvento(DetalleSenderoEvento.AlDescargarEIniciar) },
            textoDescartar = "Iniciar sin descargar",
            alDescartar = { alProcesarEvento(DetalleSenderoEvento.AlIniciarSinDescargar) }
        )
    }

    if (estadoVisual.esDialogoEliminarMapaVisible) {
        val estaDescargando = estadoVisual.estadoMapaSinConexion is EstadoMapaSinConexion.Descargando ||
            estadoVisual.estadoMapaSinConexion is EstadoMapaSinConexion.EsperandoConexion
        DialogoConfirmacion(
            titulo = if (estaDescargando) "Cancelar la descarga" else "Eliminar mapa sin conexión",
            texto = if (estaDescargando) {
                "¿Querés cancelar la descarga? Se detendrá y se descartará lo descargado hasta ahora."
            } else {
                "¿Querés eliminar el mapa? Vas a necesitar conexión para ver el mapa de este sendero."
            },
            textoConfirmar = if (estaDescargando) "Detener descarga" else "Eliminar",
            alConfirmar = { alProcesarEvento(DetalleSenderoEvento.AlConfirmarEliminarMapa) },
            textoDescartar = if (estaDescargando) "Seguir descargando" else "Conservar mapa",
            alDescartar = { alProcesarEvento(DetalleSenderoEvento.AlCancelarEliminarMapa) },
            esDestructivo = true
        )
    }

    if (estadoVisual.esDialogoPublicarVisible && estadoVisual.sendero != null) {
        DialogoConfirmacion(
            titulo = "Publicar sendero",
            texto = "Cualquiera que use la app va a poder ver ${estadoVisual.sendero.nombre} y su recorrido, " +
                "incluido dónde empieza. Si después se edita, hay que volver a publicarlo.",
            textoConfirmar = "Publicar",
            alConfirmar = { alProcesarEvento(DetalleSenderoEvento.AlConfirmarPublicar) },
            textoDescartar = "Cancelar",
            alDescartar = { alProcesarEvento(DetalleSenderoEvento.AlCancelarPublicar) }
        )
    }

    if (estadoVisual.esDialogoHacerPrivadoVisible && estadoVisual.sendero != null) {
        DialogoConfirmacion(
            titulo = "Hacer privado",
            texto = "${estadoVisual.sendero.nombre} deja de aparecer en Explorar. " +
                "Las copias que otros ya guardaron se quedan.",
            textoConfirmar = "Hacer privado",
            alConfirmar = { alProcesarEvento(DetalleSenderoEvento.AlConfirmarHacerPrivado) },
            textoDescartar = "Seguir publicado",
            alDescartar = { alProcesarEvento(DetalleSenderoEvento.AlCancelarHacerPrivado) }
        )
    }

    if (estadoVisual.esDialogoExportarVisible) {
        DialogoFormatoExportacion(alProcesarEvento)
    }

    if (estadoVisual.esDialogoEliminarVisible && estadoVisual.sendero != null) {
        DialogoConfirmacion(
            titulo = "Eliminar sendero",
            texto = "¿Querés eliminar ${estadoVisual.sendero.nombre}? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            alConfirmar = { alProcesarEvento(DetalleSenderoEvento.AlConfirmarEliminar) },
            textoDescartar = "Conservar sendero",
            alDescartar = { alProcesarEvento(DetalleSenderoEvento.AlCancelarEliminar) },
            esDestructivo = true
        )
    }
}

@Composable
private fun BotonIniciarRecorrido(
    hayRecorridoEnCurso: Boolean,
    esRecorridoSimulado: Boolean,
    esModoDemostracion: Boolean,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        val textoBoton = when {
            hayRecorridoEnCurso && esRecorridoSimulado -> "Ver simulación en curso"
            hayRecorridoEnCurso -> "Ver recorrido en curso"
            esModoDemostracion -> "Simular recorrido"
            else -> "Iniciar recorrido"
        }
        BotonPrincipal(
            texto = textoBoton,
            alTocar = { alProcesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido) },
            icono = { IconoDeBoton(Icons.AutoMirrored.Filled.DirectionsWalk) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CuerpoDetalle(
    estadoVisual: DetalleSenderoEstadoVisual,
    sendero: Sendero,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    val estadoLista = rememberLazyListState()
    // La última clave se guarda para no repetir el desplazamiento al recrear la pantalla (por ejemplo al girarla)
    var ultimaClaveDesplazada by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(estadoVisual.contadorEnfoque) {
        if (estadoVisual.contadorEnfoque > ultimaClaveDesplazada) {
            ultimaClaveDesplazada = estadoVisual.contadorEnfoque
            if (estadoVisual.debeDesplazarAlMapa) estadoLista.animateScrollToItem(INDICE_ELEMENTO_MAPA)
        }
    }
    LazyColumn(
        state = estadoLista,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CabeceraDetalle(estadoVisual, sendero, alProcesarEvento) }
        item {
            DatosDelRecorrido(sendero, modifier = Modifier.padding(horizontal = 24.dp))
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RecuadroMapa(
                    sendero = sendero,
                    puntoEnfocado = estadoVisual.puntoEnfocado,
                    claveEnfoque = estadoVisual.contadorEnfoque,
                    indiceSeleccionado = estadoVisual.indicePuntoSeleccionado,
                    alSeleccionarPunto = { indice -> alProcesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoEnMapa(indice)) },
                    alDescargarMapa = if (estadoVisual.estadoMapaSinConexion is EstadoMapaSinConexion.NoDescargado) {
                        { alProcesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa) }
                    } else {
                        null
                    }
                )
                estadoVisual.puntosDeInteres.firstOrNull { punto -> punto.indice == estadoVisual.indicePuntoSeleccionado }?.let { punto ->
                    TarjetaPuntoSeleccionado(
                        punto = punto,
                        alAmpliarFoto = { foto -> alProcesarEvento(DetalleSenderoEvento.AlAmpliarFoto(foto)) },
                        alCerrar = { alProcesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoEnMapa(null)) }
                    )
                }
                if (sendero.recorrido.isNotEmpty()) {
                    TarjetaMapaSinConexion(estadoVisual.estadoMapaSinConexion, alProcesarEvento)
                }
            }
        }
        if (sendero.recorrido.size >= CANTIDAD_MINIMA_PUNTOS_PERFIL) {
            item {
                SeccionPerfilElevacion(
                    estadoVisual = estadoVisual,
                    sendero = sendero,
                    alProcesarEvento = alProcesarEvento,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        if (sendero.recorrido.isNotEmpty()) {
            item {
                SeccionClima(
                    estadoVisual = estadoVisual,
                    alProcesarEvento = alProcesarEvento,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        if (estadoVisual.hayPuntosPorRevisar) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TarjetaPuntosPorRevisar(
                        cantidad = estadoVisual.revision.puntosPendientes.size,
                        alRevisar = {
                            alProcesarEvento(DetalleSenderoEvento.AlRevisionPuntos(RevisionPuntosEvento.AlAbrir))
                        }
                    )
                }
            }
        }
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
                if (estadoVisual.puntosDeInteres.isNotEmpty()) {
                    SeccionDesplegable(
                        titulo = "Puntos de interés",
                        estaExpandida = SeccionDetalle.PUNTOS_DE_INTERES in estadoVisual.seccionesExpandidas,
                        alAlternar = {
                            alProcesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.PUNTOS_DE_INTERES))
                        }
                    ) {
                        ListaDePuntosDeInteres(
                            puntos = estadoVisual.puntosDeInteres,
                            alSeleccionar = { indice ->
                                alProcesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(indice))
                            },
                            alAmpliarFoto = { foto -> alProcesarEvento(DetalleSenderoEvento.AlAmpliarFoto(foto)) }
                        )
                    }
                    HorizontalDivider()
                }
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
