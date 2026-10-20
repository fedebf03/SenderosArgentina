package com.unaj.senderos.features.perfil

import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.ui.componentes.AvatarUsuario
import com.unaj.senderos.core.ui.componentes.BotoneraInferior
import com.unaj.senderos.core.ui.componentes.CabeceraPrincipal
import com.unaj.senderos.core.ui.componentes.SeccionPrincipal
import com.unaj.senderos.core.ui.tema.TemaSenderos

private const val TAG = "PERFIL"

@Composable
fun PerfilPantalla(
    viewModel: PerfilViewModel,
    alNavegarALogin: () -> Unit = {},
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {},
    alNavegarAHistorialDetalle: (String) -> Unit = {},
    alNavegarAEditarPerfil: () -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val navegarALoginActual by rememberUpdatedState(alNavegarALogin)
    val navegarAHistorialActual by rememberUpdatedState(alNavegarAHistorialDetalle)
    val navegarAEditarPerfilActual by rememberUpdatedState(alNavegarAEditarPerfil)
    val estadoAvisos = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Pantalla inicializada / rotación completada")
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is PerfilEfecto.NavegarALogin -> {
                    Log.d(TAG, "Navegando al inicio de sesión")
                    navegarALoginActual()
                }
                is PerfilEfecto.NavegarAHistorialDetalle -> navegarAHistorialActual(efecto.idRecorrido)
                is PerfilEfecto.NavegarAEditarPerfil -> navegarAEditarPerfilActual()
                is PerfilEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
            }
        }
    }

    BackHandler { alNavegarASeccion(SeccionPrincipal.EXPLORAR) }

    PerfilContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        alNavegarASeccion = alNavegarASeccion,
        estadoAvisos = estadoAvisos
    )
}

@Composable
fun PerfilContenido(
    estadoVisual: PerfilEstadoVisual,
    alProcesarEvento: (PerfilEvento) -> Unit,
    modifier: Modifier = Modifier,
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {},
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        topBar = { CabeceraPrincipal(usuario = estadoVisual.usuario, alTocarAvatar = null) },
        bottomBar = { BotoneraInferior(SeccionPrincipal.PERFIL, alNavegarASeccion) }
    ) { espaciadoInterno ->
        CuerpoPerfil(estadoVisual, alProcesarEvento, Modifier.padding(espaciadoInterno))
    }

    if (estadoVisual.esDialogoCerrarSesionVisible) {
        DialogoConfirmacion(
            titulo = "Cerrar sesión",
            texto = if (estadoVisual.hayRecorridoEnCurso) {
                "¿Querés cerrar la sesión? Hay un recorrido en curso: se guardará y se detendrá."
            } else {
                "¿Querés cerrar la sesión?"
            },
            textoConfirmar = "Cerrar sesión",
            alConfirmar = { alProcesarEvento(PerfilEvento.AlConfirmarCerrarSesion) },
            textoDescartar = if (estadoVisual.hayRecorridoEnCurso) "Seguir en la app" else "Cancelar",
            alDescartar = { alProcesarEvento(PerfilEvento.AlCancelarCerrarSesion) }
        )
    }
}

@Composable
private fun CuerpoPerfil(
    estadoVisual: PerfilEstadoVisual,
    alProcesarEvento: (PerfilEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    val usuario = estadoVisual.usuario
    val nombre = usuario?.nombre.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarUsuario(usuario = usuario, tamano = 88.dp)

        if (nombre.isNotBlank()) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        Text(
            text = usuario?.email.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = if (nombre.isNotBlank()) 4.dp else 16.dp)
        )
        val ubicacion = listOf(usuario?.localidad, usuario?.provincia, usuario?.pais)
            .filter { texto -> !texto.isNullOrBlank() }
            .joinToString(", ")
        if (ubicacion.isNotEmpty()) {
            Text(
                text = ubicacion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        BotonSecundario(
            texto = "Editar perfil",
            alTocar = { alProcesarEvento(PerfilEvento.AlTocarEditarPerfil) },
            icono = { IconoDeBoton(Icons.Outlined.Edit) },
            modifier = Modifier.padding(top = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .toggleable(
                    value = estadoVisual.esModoDemostracion,
                    role = Role.Switch,
                    onValueChange = { activo -> alProcesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo)) }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Modo demostración", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Simula un recorrido sobre el trazado para probar o presentar la app sin caminar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(checked = estadoVisual.esModoDemostracion, onCheckedChange = null)
        }

        SeccionEstadisticas(estadoVisual, modifier = Modifier.padding(top = 24.dp))

        HistorialDeRecorridos(
            recorridos = estadoVisual.recorridos,
            mensajeError = estadoVisual.mensajeError,
            alSeleccionar = { idRecorrido -> alProcesarEvento(PerfilEvento.AlSeleccionarRecorrido(idRecorrido)) }
        )

        SeccionCopiaSeguridad(estadoVisual, alProcesarEvento)

        BotonSecundario(
            texto = "Cerrar sesión",
            alTocar = { alProcesarEvento(PerfilEvento.AlTocarCerrarSesion) },
            icono = { IconoDeBoton(Icons.AutoMirrored.Outlined.Logout) },
            esDestructivo = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilContenidoPreview() {
    TemaSenderos {
        PerfilContenido(
            estadoVisual = PerfilEstadoVisual(
                usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
            ),
            alProcesarEvento = {}
        )
    }
}
