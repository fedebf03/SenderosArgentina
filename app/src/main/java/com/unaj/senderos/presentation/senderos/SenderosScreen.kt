package com.unaj.senderos.presentation.senderos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.presentation.componentes.BotoneraInferior
import com.unaj.senderos.presentation.componentes.BuscadorSenderos
import com.unaj.senderos.presentation.componentes.CabeceraPrincipal
import com.unaj.senderos.presentation.componentes.CargandoCentrado
import com.unaj.senderos.presentation.componentes.ListaSenderos
import com.unaj.senderos.presentation.componentes.MensajeCentrado
import com.unaj.senderos.presentation.componentes.MensajeVacio
import com.unaj.senderos.presentation.componentes.SeccionPrincipal
import com.unaj.senderos.presentation.comun.PERMISOS_UBICACION
import com.unaj.senderos.presentation.comun.tienePermisoDeUbicacion
import com.unaj.senderos.presentation.tema.TemaSenderos

private const val TAG = "SENDEROS"

// Renderizar Mis Senderos conectando el ViewModel, los permisos y los avisos
@Composable
fun SenderosPantalla(
    viewModel: SenderosViewModel,
    alNavegarADetalle: (String) -> Unit = {},
    alNavegarACreacion: () -> Unit = {},
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val navegarADetalleActual by rememberUpdatedState(alNavegarADetalle)
    val navegarACreacionActual by rememberUpdatedState(alNavegarACreacion)
    val contexto = LocalContext.current
    var tienePermisoUbicacion by remember { mutableStateOf(tienePermisoDeUbicacion(contexto)) }
    val estadoAvisos = remember { SnackbarHostState() }

    val pedirPermisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        tienePermisoUbicacion = respuesta.values.any { otorgado -> otorgado }
        if (tienePermisoUbicacion) {
            viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        } else {
            Log.d(TAG, "Permiso de ubicación denegado")
            viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = false))
            Toast.makeText(contexto, "Sin permiso de ubicación no podemos ordenar por cercanía", Toast.LENGTH_SHORT).show()
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
                // Al activar el orden se pide el permiso si falta, o se refresca la ubicación si ya está otorgado
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

// Renderizar el contenido de Mis Senderos a partir del estado visual
@Composable
fun SenderosContenido(
    estadoVisual: SenderosEstadoVisual,
    alProcesarEvento: (SenderosEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() },
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
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
        bottomBar = { BotoneraInferior(SeccionPrincipal.INICIO, alNavegarASeccion) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { alProcesarEvento(SenderosEvento.AlTocarCrearSendero) },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo sendero") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { espaciadoInterno ->
        val modificadorCuerpo = Modifier.padding(espaciadoInterno)
        when {
            estadoVisual.estaCargando -> CargandoCentrado(modificadorCuerpo)
            estadoVisual.mensajeError != null -> MensajeCentrado(estadoVisual.mensajeError, modificadorCuerpo, esError = true)
            estadoVisual.senderos.isEmpty() -> MensajeVacio(
                icono = Icons.Outlined.Hiking,
                titulo = "Todavía no agregaste senderos",
                detalle = "Tocá «Nuevo sendero» para cargar el primero.",
                modifier = modificadorCuerpo
            )
            estadoVisual.senderosFiltrados.isEmpty() -> MensajeCentrado("No se encontraron senderos", modificadorCuerpo)
            else -> Column(modifier = modificadorCuerpo) {
                FilterChip(
                    selected = estadoVisual.ordenarPorCercania,
                    onClick = { alProcesarEvento(SenderosEvento.AlCambiarOrdenCercania(!estadoVisual.ordenarPorCercania)) },
                    label = { Text("Más cercanos") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                ListaSenderos(
                    titulo = "Mis senderos",
                    senderos = estadoVisual.senderosFiltrados,
                    alSeleccionarSendero = { idSendero ->
                        alProcesarEvento(SenderosEvento.AlSeleccionarSendero(idSendero))
                    },
                    modifier = Modifier.weight(1f),
                    espacioInferior = PaddingValues(bottom = 96.dp),
                    distanciaKilometrosDe = estadoVisual::distanciaKilometrosA
                )
            }
        }
    }
}

// Previsualizar Mis Senderos con senderos de ejemplo
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
