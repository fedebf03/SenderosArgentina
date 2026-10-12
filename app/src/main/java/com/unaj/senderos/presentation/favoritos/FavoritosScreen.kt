package com.unaj.senderos.presentation.favoritos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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

// Renderizar la sección Favoritos conectando el ViewModel y la navegación
@Composable
fun FavoritosPantalla(
    viewModel: FavoritosViewModel,
    alNavegarADetalle: (String) -> Unit = {},
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val navegarADetalleActual by rememberUpdatedState(alNavegarADetalle)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is FavoritosEfecto.NavegarADetalle -> navegarADetalleActual(efecto.idSendero)
            }
        }
    }

    BackHandler { alNavegarASeccion(SeccionPrincipal.INICIO) }

    FavoritosContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        alNavegarASeccion = alNavegarASeccion
    )
}

// Renderizar el contenido de Favoritos a partir del estado visual
@Composable
fun FavoritosContenido(
    estadoVisual: FavoritosEstadoVisual,
    alProcesarEvento: (FavoritosEvento) -> Unit,
    modifier: Modifier = Modifier,
    alNavegarASeccion: (SeccionPrincipal) -> Unit = {}
) {
    val tieneFavoritos = estadoVisual.favoritos.isNotEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            CabeceraPrincipal(
                usuario = estadoVisual.usuario,
                alTocarAvatar = { alNavegarASeccion(SeccionPrincipal.PERFIL) },
                contenidoInferior = if (tieneFavoritos) {
                    {
                        BuscadorSenderos(estadoVisual.textoBusqueda) { texto ->
                            alProcesarEvento(FavoritosEvento.AlCambiarBusqueda(texto))
                        }
                    }
                } else {
                    null
                }
            )
        },
        bottomBar = { BotoneraInferior(SeccionPrincipal.FAVORITOS, alNavegarASeccion) }
    ) { espaciadoInterno ->
        val modificadorCuerpo = Modifier.padding(espaciadoInterno)
        when {
            estadoVisual.estaCargando -> CargandoCentrado(modificadorCuerpo)
            estadoVisual.mensajeError != null -> MensajeCentrado(estadoVisual.mensajeError, modificadorCuerpo, esError = true)
            !tieneFavoritos -> MensajeVacio(
                icono = Icons.Outlined.FavoriteBorder,
                titulo = "Todavía no tenés favoritos",
                detalle = "Abrí un sendero y tocá el corazón para guardarlo acá.",
                modifier = modificadorCuerpo
            )
            estadoVisual.favoritosFiltrados.isEmpty() -> MensajeCentrado("No se encontraron senderos", modificadorCuerpo)
            else -> ListaSenderos(
                titulo = "Favoritos",
                senderos = estadoVisual.favoritosFiltrados,
                alSeleccionarSendero = { idSendero ->
                    alProcesarEvento(FavoritosEvento.AlSeleccionarSendero(idSendero))
                },
                modifier = modificadorCuerpo
            )
        }
    }
}

// Previsualizar Favoritos con senderos de ejemplo
@Preview(showBackground = true)
@Composable
private fun FavoritosContenidoPreview() {
    TemaSenderos {
        FavoritosContenido(
            estadoVisual = FavoritosEstadoVisual(
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

// Previsualizar Favoritos sin ningún sendero marcado
@Preview(showBackground = true)
@Composable
private fun FavoritosVacioPreview() {
    TemaSenderos {
        FavoritosContenido(
            estadoVisual = FavoritosEstadoVisual(
                usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"),
                estaCargando = false
            ),
            alProcesarEvento = {}
        )
    }
}
