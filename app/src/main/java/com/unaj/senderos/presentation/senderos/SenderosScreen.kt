package com.unaj.senderos.presentation.senderos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.unaj.senderos.presentation.tema.TemaSenderos

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

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is SenderosEfecto.NavegarADetalle -> navegarADetalleActual(efecto.idSendero)
                is SenderosEfecto.NavegarACreacion -> navegarACreacionActual()
            }
        }
    }

    SenderosContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        alNavegarASeccion = alNavegarASeccion
    )
}

@Composable
fun SenderosContenido(
    estadoVisual: SenderosEstadoVisual,
    alProcesarEvento: (SenderosEvento) -> Unit,
    modifier: Modifier = Modifier,
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
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
            else -> ListaSenderos(
                titulo = "Mis senderos",
                senderos = estadoVisual.senderosFiltrados,
                alSeleccionarSendero = { idSendero ->
                    alProcesarEvento(SenderosEvento.AlSeleccionarSendero(idSendero))
                },
                modifier = modificadorCuerpo,
                espacioInferior = PaddingValues(bottom = 96.dp)
            )
        }
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
