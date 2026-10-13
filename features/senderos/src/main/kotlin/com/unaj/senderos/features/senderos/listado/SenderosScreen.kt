package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.presentation.componentes.BannerAviso
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.SenderoPublico
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.presentation.componentes.BotoneraInferior
import com.unaj.senderos.presentation.componentes.BuscadorSenderos
import com.unaj.senderos.presentation.componentes.CabeceraPrincipal
import com.unaj.senderos.presentation.componentes.CargandoCentrado
import com.unaj.senderos.presentation.componentes.ChipsDificultad
import com.unaj.senderos.presentation.componentes.ListaSenderos
import com.unaj.senderos.presentation.componentes.MensajeCentrado
import com.unaj.senderos.presentation.componentes.MensajeVacio
import com.unaj.senderos.presentation.componentes.SeccionPrincipal
import com.unaj.senderos.presentation.comun.PERMISOS_UBICACION
import com.unaj.senderos.presentation.comun.tienePermisoDeUbicacion
import com.unaj.senderos.presentation.tema.TemaSenderos

private const val TAG = "SENDEROS"

@Composable
fun SenderosPantalla(
    viewModel: SenderosViewModel,
    alNavegarADetalle: (String) -> Unit = {},
    alNavegarADetallePublico: (String) -> Unit = {},
    alNavegarACreacion: () -> Unit = {},
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val navegarADetalleActual by rememberUpdatedState(alNavegarADetalle)
    val navegarADetallePublicoActual by rememberUpdatedState(alNavegarADetallePublico)
    val navegarACreacionActual by rememberUpdatedState(alNavegarACreacion)
    val contexto = LocalContext.current
    var tienePermisoUbicacion by remember { mutableStateOf(tienePermisoDeUbicacion(contexto)) }
    val estadoAvisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    val pedirPermisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        tienePermisoUbicacion = respuesta.values.any { otorgado -> otorgado }
        if (tienePermisoUbicacion) {
            viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        } else {
            Log.d(TAG, "Permiso de ubicación denegado")
            viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = false))
            alcance.launch { estadoAvisos.showSnackbar("Sin permiso de ubicación no podemos ordenar por cercanía") }
        }
    }
    val pedirPermisoNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        Log.d(TAG, "Permiso de notificaciones otorgado: $otorgado")
    }

    // Si ya hay permiso al entrar y todavía no hay ubicación, se obtiene; si no hay permiso, se pide recién al activar "Más cercanos"
    LaunchedEffect(Unit) {
        if (tienePermisoUbicacion && estadoVisual.ubicacionActual == null) viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
    }

    // Se pide el permiso de notificaciones una sola vez por sesión (la marca vive en el ViewModel) y solo desde Android 13
    LaunchedEffect(Unit) {
        val hayQuePedirNotificaciones = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (hayQuePedirNotificaciones && !estadoVisual.seSolicitoPermisoNotificaciones) {
            viewModel.procesarEvento(SenderosEvento.AlSolicitarPermisoNotificaciones)
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is SenderosEfecto.NavegarADetalle -> navegarADetalleActual(efecto.idSendero)
                is SenderosEfecto.NavegarADetallePublico -> navegarADetallePublicoActual(efecto.idSendero)
                is SenderosEfecto.NavegarACreacion -> navegarACreacionActual()
                is SenderosEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
            }
        }
    }

    SenderosContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = { evento ->
            viewModel.procesarEvento(evento)
            if (evento is SenderosEvento.AlCambiarOrdenCercania && evento.activado) {
                if (tienePermisoUbicacion) {
                    viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
                } else {
                    pedirPermisoUbicacion.launch(PERMISOS_UBICACION)
                }
            }
        },
        estadoAvisos = estadoAvisos,
        alNavegarASeccion = alNavegarASeccion
    )
}

@Composable
fun SenderosContenido(
    estadoVisual: SenderosEstadoVisual,
    alProcesarEvento: (SenderosEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() },
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    val esExplorar = estadoVisual.modo == ModoListado.EXPLORAR
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        topBar = {
            CabeceraPrincipal(
                usuario = estadoVisual.usuario,
                alTocarAvatar = { alNavegarASeccion(SeccionPrincipal.PERFIL) }
            ) {
                BuscadorSenderos(estadoVisual.textoBusqueda) { texto ->
                    alProcesarEvento(SenderosEvento.AlCambiarBusqueda(texto))
                }
            }
        },
        bottomBar = {
            BotoneraInferior(
                if (esExplorar) SeccionPrincipal.EXPLORAR else SeccionPrincipal.MIS_SENDEROS,
                alNavegarASeccion
            )
        },
        floatingActionButton = {
            if (!esExplorar) {
                ExtendedFloatingActionButton(
                    onClick = { alProcesarEvento(SenderosEvento.AlTocarCrearSendero) },
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo sendero") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { espaciadoInterno ->
        Column(modifier = Modifier.padding(espaciadoInterno)) {
            AnimatedVisibility(visible = estadoVisual.estaSinConexion) {
                BannerAviso(
                    texto = if (esExplorar) {
                        "Sin conexión: puede que veas senderos desactualizados"
                    } else {
                        "Sin conexión: los cambios se sincronizarán después"
                    },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
            val modificadorCuerpo = Modifier.weight(1f)
            when {
                estadoVisual.estaCargando -> CargandoCentrado(modificadorCuerpo)
                estadoVisual.mensajeError != null -> MensajeCentrado(estadoVisual.mensajeError, modificadorCuerpo, esError = true)
                esExplorar && estadoVisual.hayErrorEnPublicos && estadoVisual.estaVacio -> MensajeCentrado(
                    "No pudimos cargar los senderos públicos",
                    modificadorCuerpo,
                    esError = true
                )
                estadoVisual.estaVacio -> if (esExplorar) {
                    MensajeVacio(
                        icono = Icons.Outlined.Explore,
                        titulo = "Todavía no hay senderos publicados",
                        detalle = "Los senderos que se publiquen aparecen acá.",
                        modifier = modificadorCuerpo
                    )
                } else {
                    MensajeVacio(
                        icono = Icons.Outlined.Hiking,
                        titulo = "Todavía no agregaste senderos",
                        detalle = "Tocá «Nuevo sendero» para cargar el primero.",
                        modifier = modificadorCuerpo
                    )
                }
                else -> Column(modifier = modificadorCuerpo) {
                    FiltrosSenderos(estadoVisual, alProcesarEvento)
                    if (estadoVisual.senderosFiltrados.isEmpty()) {
                        MensajeCentrado(
                            if (estadoVisual.textoBusqueda.isBlank()) "No hay senderos con esos filtros" else "No se encontraron senderos",
                            Modifier.weight(1f)
                        )
                    } else {
                        ListaSenderos(
                            titulo = if (esExplorar) "Senderos públicos" else "Mis senderos",
                            senderos = estadoVisual.senderosFiltrados,
                            alSeleccionarSendero = { idSendero ->
                                alProcesarEvento(SenderosEvento.AlSeleccionarSendero(idSendero))
                            },
                            modifier = Modifier.weight(1f),
                            espacioInferior = PaddingValues(bottom = 96.dp),
                            distanciaKilometrosDe = estadoVisual::distanciaKilometrosA,
                            autorDe = estadoVisual::nombreAutorDe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltrosSenderos(estadoVisual: SenderosEstadoVisual, alProcesarEvento: (SenderosEvento) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (estadoVisual.modo == ModoListado.MIS_SENDEROS) {
            FilterChip(
                selected = estadoVisual.soloFavoritos,
                onClick = { alProcesarEvento(SenderosEvento.AlAlternarFavoritos) },
                label = { Text("Favoritos") }
            )
            FilterChip(
                selected = estadoVisual.soloGuardados,
                onClick = { alProcesarEvento(SenderosEvento.AlAlternarGuardados) },
                label = { Text("Guardados") }
            )
        }
        FilterChip(
            selected = estadoVisual.ordenarPorCercania,
            onClick = { alProcesarEvento(SenderosEvento.AlCambiarOrdenCercania(!estadoVisual.ordenarPorCercania)) },
            label = { Text("Más cercanos") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        ChipsDificultad(
            seleccionadas = estadoVisual.dificultadesSeleccionadas,
            alAlternar = { dificultad -> alProcesarEvento(SenderosEvento.AlAlternarDificultad(dificultad)) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SenderosContenidoPreview() {
    TemaSenderos {
        SenderosContenido(
            estadoVisual = SenderosEstadoVisual(
                usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"),
                senderos = listOf(
                    Sendero("huella-andina", "usuario-1", "Huella Andina", "Neuquén, Río Negro y Chubut", 570.0, Dificultad.MEDIA, esFavorito = true),
                    Sendero("huella-neuquina", "usuario-1", "Huella Neuquina", "Neuquén", 120.0, Dificultad.BAJA)
                ),
                estaCargando = false
            ),
            alProcesarEvento = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplorarContenidoPreview() {
    TemaSenderos {
        SenderosContenido(
            estadoVisual = SenderosEstadoVisual(
                modo = ModoListado.EXPLORAR,
                usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"),
                senderosPublicos = listOf(
                    SenderoPublico(
                        Sendero("puerto-canoa", "senderosargentina", "Puerto Canoa - Aila", "Neuquén", 8.2, Dificultad.BAJA),
                        nombreAutor = "SenderosArgentina"
                    )
                ),
                estaCargando = false
            ),
            alProcesarEvento = {}
        )
    }
}
