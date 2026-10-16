package com.unaj.senderos.features.mapa

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.ui.componentes.BotoneraInferior
import com.unaj.senderos.core.ui.componentes.CabeceraPrincipal
import com.unaj.senderos.core.ui.componentes.CargandoCentrado
import com.unaj.senderos.core.ui.componentes.MensajeCentrado
import com.unaj.senderos.core.ui.componentes.SeccionPrincipal
import com.unaj.senderos.core.ui.componentes.TarjetaSendero
import com.unaj.senderos.core.ui.comun.PERMISOS_UBICACION
import com.unaj.senderos.core.ui.comun.tienePermisoDeUbicacion
import com.unaj.senderos.core.ui.componentes.mapa.MapaDeSenderos

@Composable
fun MapaSenderosPantalla(
    viewModel: MapaSenderosViewModel,
    alNavegarADetalle: (String) -> Unit = {},
    alNavegarADetallePublico: (String) -> Unit = {},
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val navegarADetalleActual by rememberUpdatedState(alNavegarADetalle)
    val navegarADetallePublicoActual by rememberUpdatedState(alNavegarADetallePublico)
    val contexto = LocalContext.current
    val estadoAvisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    var tienePermisoUbicacion by remember { mutableStateOf(tienePermisoDeUbicacion(contexto)) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        tienePermisoUbicacion = respuesta.values.any { otorgado -> otorgado }
        if (!tienePermisoUbicacion) {
            alcance.launch { estadoAvisos.showSnackbar("Sin permiso de ubicación no podemos mostrar dónde estás") }
        }
    }
    val solicitarUbicacion = {
        viewModel.procesarEvento(MapaSenderosEvento.AlSolicitarUbicacion)
        pedirPermiso.launch(PERMISOS_UBICACION)
    }

    // Se pregunta una sola vez por sesión al entrar; después el botón de ubicación permite volver a pedirlo
    LaunchedEffect(Unit) {
        if (!tienePermisoUbicacion && !estadoVisual.seSolicitoUbicacion) solicitarUbicacion()
    }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is MapaSenderosEfecto.NavegarADetalle -> navegarADetalleActual(efecto.idSendero)
                is MapaSenderosEfecto.NavegarADetallePublico -> navegarADetallePublicoActual(efecto.idSendero)
            }
        }
    }

    BackHandler {
        if (estadoVisual.senderoSeleccionado != null) {
            viewModel.procesarEvento(MapaSenderosEvento.AlSeleccionarSendero(null))
        } else {
            alNavegarASeccion(SeccionPrincipal.EXPLORAR)
        }
    }

    MapaSenderosContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        alNavegarASeccion = alNavegarASeccion,
        tienePermisoUbicacion = tienePermisoUbicacion,
        alPedirUbicacion = solicitarUbicacion,
        estadoAvisos = estadoAvisos,
        alMostrarAviso = { mensaje -> alcance.launch { estadoAvisos.showSnackbar(mensaje) } }
    )
}

@Composable
fun MapaSenderosContenido(
    estadoVisual: MapaSenderosEstadoVisual,
    alProcesarEvento: (MapaSenderosEvento) -> Unit,
    modifier: Modifier = Modifier,
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {},
    tienePermisoUbicacion: Boolean = false,
    alPedirUbicacion: () -> Unit = {},
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() },
    alMostrarAviso: (String) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        topBar = {
            CabeceraPrincipal(
                usuario = estadoVisual.usuario,
                alTocarAvatar = { alNavegarASeccion(SeccionPrincipal.PERFIL) }
            )
        },
        bottomBar = { BotoneraInferior(SeccionPrincipal.MAPA, alNavegarASeccion) }
    ) { espaciadoInterno ->
        val modificadorCuerpo = Modifier.padding(espaciadoInterno)
        when {
            estadoVisual.estaCargando -> CargandoCentrado(modificadorCuerpo)
            estadoVisual.mensajeError != null -> MensajeCentrado(estadoVisual.mensajeError, modificadorCuerpo, esError = true)
            else -> Box(modifier = modificadorCuerpo.fillMaxSize()) {
                MapaDeSenderos(
                    senderos = estadoVisual.senderosEnMapa,
                    idSeleccionado = estadoVisual.idSeleccionado,
                    alSeleccionarSendero = { idSendero ->
                        alProcesarEvento(MapaSenderosEvento.AlSeleccionarSendero(idSendero))
                    },
                    mostrarUbicacion = tienePermisoUbicacion,
                    alPedirUbicacion = alPedirUbicacion,
                    estaSinConexion = estadoVisual.estaSinConexion,
                    alMostrarAviso = alMostrarAviso,
                    modifier = Modifier.fillMaxSize()
                )

                val senderoSeleccionado = estadoVisual.senderoSeleccionado
                AnimatedVisibility(
                    visible = senderoSeleccionado != null,
                    enter = slideInVertically { altura -> altura } + fadeIn(),
                    exit = slideOutVertically { altura -> altura } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    if (senderoSeleccionado != null) {
                        TarjetaSendero(
                            sendero = senderoSeleccionado,
                            alSeleccionar = {
                                alProcesarEvento(MapaSenderosEvento.AlAbrirDetalle(senderoSeleccionado.id))
                            },
                            autor = estadoVisual.nombreAutorDe(senderoSeleccionado)
                        )
                    }
                }
            }
        }
    }
}
