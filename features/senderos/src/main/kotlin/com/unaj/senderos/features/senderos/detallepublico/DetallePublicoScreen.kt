package com.unaj.senderos.features.explorar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoPublico
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.FotoSendero
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.componentes.BotonCircular
import com.unaj.senderos.core.ui.sendero.DatosDelRecorrido
import com.unaj.senderos.core.ui.sendero.ListaConVinietas
import com.unaj.senderos.core.ui.sendero.ListaDeEtapas
import com.unaj.senderos.core.ui.sendero.RecuadroMapa
import com.unaj.senderos.core.ui.tema.TemaSenderos

@Composable
fun DetallePublicoPantalla(
    viewModel: DetallePublicoViewModel,
    alVolverAExplorar: () -> Unit,
    alNavegarAMiSendero: (String) -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverActual by rememberUpdatedState(alVolverAExplorar)
    val navegarAMiSenderoActual by rememberUpdatedState(alNavegarAMiSendero)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is DetallePublicoEfecto.VolverAExplorar -> volverActual()
                is DetallePublicoEfecto.NavegarAMiSendero -> navegarAMiSenderoActual(efecto.idSendero)
                is DetallePublicoEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
            }
        }
    }

    DetallePublicoContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        estadoAvisos = estadoAvisos
    )
}

@Composable
fun DetallePublicoContenido(
    estadoVisual: DetallePublicoEstadoVisual,
    alProcesarEvento: (DetallePublicoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    BackHandler { alProcesarEvento(DetallePublicoEvento.AlVolver) }

    val publico = estadoVisual.senderoPublico
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        bottomBar = {
            if (publico != null && !estadoVisual.esPropio) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val yaGuardado = estadoVisual.idCopia != null
                    BotonPrincipal(
                        texto = if (yaGuardado) "Ver mi copia" else "Guardar en mis senderos",
                        alTocar = {
                            alProcesarEvento(
                                if (yaGuardado) DetallePublicoEvento.AlVerMiCopia else DetallePublicoEvento.AlGuardarEnMisSenderos
                            )
                        },
                        icono = {
                            Icon(
                                imageVector = if (yaGuardado) Icons.Outlined.Bookmark else Icons.Outlined.Download,
                                contentDescription = null
                            )
                        },
                        estaCargando = estadoVisual.estaGuardando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    ) { espaciadoInterno ->
        when {
            estadoVisual.estaCargando -> Box(
                Modifier.fillMaxSize().padding(espaciadoInterno),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            estadoVisual.mensajeError != null || publico == null -> Column(
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
                TextButton(onClick = { alProcesarEvento(DetallePublicoEvento.AlVolver) }) { Text("Volver") }
            }
            else -> CuerpoDetallePublico(
                publico = publico,
                esPropio = estadoVisual.esPropio,
                alVolver = { alProcesarEvento(DetallePublicoEvento.AlVolver) },
                modifier = Modifier.padding(bottom = espaciadoInterno.calculateBottomPadding())
            )
        }
    }
}

@Composable
private fun CuerpoDetallePublico(
    publico: SenderoPublico,
    esPropio: Boolean,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sendero = publico.sendero
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { CabeceraPublica(sendero, alVolver) }
        item {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (esPropio) "Publicado por vos" else "Publicado por ${publico.nombreAutor}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { DatosDelRecorrido(sendero, modifier = Modifier.padding(horizontal = 24.dp)) }
        item { RecuadroMapa(sendero = sendero, modifier = Modifier.padding(horizontal = 24.dp)) }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TituloDeSeccion("Descripción")
                Text(text = sendero.descripcion, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (sendero.recomendaciones.isNotEmpty()) {
            item { SeccionDeLista("Recomendaciones") { ListaConVinietas(sendero.recomendaciones) } }
        }
        if (sendero.servicios.isNotEmpty()) {
            item { SeccionDeLista("Servicios en el camino") { ListaConVinietas(sendero.servicios) } }
        }
        if (sendero.etapas.isNotEmpty()) {
            item { SeccionDeLista("Etapas") { ListaDeEtapas(sendero.etapas) } }
        }
    }
}

@Composable
private fun SeccionDeLista(titulo: String, contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TituloDeSeccion(titulo)
        contenido()
    }
}

@Composable
private fun CabeceraPublica(sendero: Sendero, alVolver: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        FotoSendero(foto = sendero.foto, modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            BotonCircular(
                icono = Icons.AutoMirrored.Filled.ArrowBack,
                descripcion = "Volver",
                alTocar = alVolver
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sendero.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sendero.region,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetallePublicoContenidoPreview() {
    TemaSenderos {
        DetallePublicoContenido(
            estadoVisual = DetallePublicoEstadoVisual(
                senderoPublico = SenderoPublico(
                    sendero = Sendero(
                        id = "huella-andina",
                        idUsuario = "usuario-2",
                        nombre = "Huella Andina",
                        region = "Neuquén, Río Negro y Chubut",
                        distanciaKilometros = 570.0,
                        dificultad = Dificultad.MEDIA,
                        descripcion = "Travesía de largo recorrido por la cordillera."
                    ),
                    nombreAutor = "Luis"
                ),
                estaCargando = false
            ),
            alProcesarEvento = {}
        )
    }
}
