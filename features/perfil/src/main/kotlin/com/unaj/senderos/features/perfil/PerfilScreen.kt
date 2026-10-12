package com.unaj.senderos.presentation.perfil

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.presentation.componentes.AvatarUsuario
import com.unaj.senderos.presentation.componentes.BotoneraInferior
import com.unaj.senderos.presentation.componentes.CabeceraPrincipal
import com.unaj.senderos.presentation.componentes.SeccionPrincipal
import com.unaj.senderos.presentation.historial.HistorialDeRecorridos
import com.unaj.senderos.presentation.tema.TemaSenderos

private const val TAG = "PERFIL"

// Mostrar el perfil conectado a su ViewModel y atender la navegación al inicio de sesión
@Composable
fun PerfilPantalla(
    viewModel: PerfilViewModel,
    alNavegarALogin: () -> Unit = {},
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {},
    alNavegarAHistorialDetalle: (String) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val navegarALoginActual by rememberUpdatedState(alNavegarALogin)
    val navegarAHistorialActual by rememberUpdatedState(alNavegarAHistorialDetalle)

    LaunchedEffect(Unit) {
        Log.d(TAG, "Pantalla inicializada / rotación completada")
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is PerfilEfecto.NavegarALogin -> {
                    Log.d(TAG, "Navegando al inicio de sesión")
                    navegarALoginActual()
                }
                is PerfilEfecto.NavegarAHistorialDetalle -> navegarAHistorialActual(efecto.idRecorrido)
            }
        }
    }

    BackHandler { alNavegarASeccion(SeccionPrincipal.INICIO) }

    PerfilContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        alNavegarASeccion = alNavegarASeccion
    )
}

// Renderizar el perfil con su cabecera y la botonera inferior
@Composable
fun PerfilContenido(
    estadoVisual: PerfilEstadoVisual,
    alProcesarEvento: (PerfilEvento) -> Unit,
    modifier: Modifier = Modifier,
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = { CabeceraPrincipal(usuario = estadoVisual.usuario, alTocarAvatar = null) },
        bottomBar = { BotoneraInferior(SeccionPrincipal.PERFIL, alNavegarASeccion) }
    ) { espaciadoInterno ->
        CuerpoPerfil(estadoVisual, alProcesarEvento, Modifier.padding(espaciadoInterno))
    }
}

// Mostrar los datos del usuario, el modo demostración y el cierre de sesión
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
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
            Switch(
                checked = estadoVisual.esModoDemostracion,
                onCheckedChange = { activo -> alProcesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo)) }
            )
        }

        HistorialDeRecorridos(
            recorridos = estadoVisual.recorridos,
            alSeleccionar = { idRecorrido -> alProcesarEvento(PerfilEvento.AlSeleccionarRecorrido(idRecorrido)) }
        )

        OutlinedButton(
            onClick = { alProcesarEvento(PerfilEvento.AlCerrarSesion) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }
}

// Previsualizar el perfil de un usuario de ejemplo
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
