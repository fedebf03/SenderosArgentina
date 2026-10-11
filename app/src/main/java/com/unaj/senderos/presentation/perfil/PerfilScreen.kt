package com.unaj.senderos.presentation.perfil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
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
import com.unaj.senderos.presentation.tema.TemaSenderos

@Composable
fun PerfilPantalla(
    viewModel: PerfilViewModel,
    alNavegarALogin: () -> Unit = {},
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val navegarALoginActual by rememberUpdatedState(alNavegarALogin)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is PerfilEfecto.NavegarALogin -> navegarALoginActual()
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

        OutlinedButton(
            onClick = { alProcesarEvento(PerfilEvento.AlCerrarSesion) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
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
