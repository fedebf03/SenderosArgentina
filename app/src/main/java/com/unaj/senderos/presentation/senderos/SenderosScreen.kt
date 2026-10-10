package com.unaj.senderos.presentation.senderos

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario

private const val TAG = "SENDEROS_PANTALLA"

@Composable
// Coordinar el estado y los efectos de la pantalla de senderos del usuario
fun SenderosPantalla(
    viewModel: SenderosViewModel,
    alNavegarALogin: () -> Unit = {},
    alNavegarADetalle: (String) -> Unit = {},
    alNavegarACreacion: () -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val navegarALoginActual by rememberUpdatedState(alNavegarALogin)
    val navegarADetalleActual by rememberUpdatedState(alNavegarADetalle)
    val navegarACreacionActual by rememberUpdatedState(alNavegarACreacion)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            Log.d(TAG, "Efecto recibido: $efecto")
            when (efecto) {
                is SenderosEfecto.NavegarALogin -> navegarALoginActual()
                is SenderosEfecto.NavegarADetalle -> navegarADetalleActual(efecto.idSendero)
                is SenderosEfecto.NavegarACreacion -> navegarACreacionActual()
                is SenderosEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
            }
        }
    }

    SenderosContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        estadoAvisos = estadoAvisos
    )
}

@Composable
// Dibujar la pantalla de senderos con cabecera, listado y botonera inferior
fun SenderosContenido(
    estadoVisual: SenderosEstadoVisual,
    alProcesarEvento: (SenderosEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        topBar = { CabeceraSenderos(estadoVisual, alProcesarEvento) },
        bottomBar = { BotoneraInferior(estadoVisual.seccionSeleccionada, alProcesarEvento) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { alProcesarEvento(SenderosEvento.AlTocarCrearSendero) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Crear sendero")
            }
        },
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) }
    ) { espaciadoInterno ->
        CuerpoSenderos(
            estadoVisual = estadoVisual,
            alProcesarEvento = alProcesarEvento,
            modifier = Modifier.padding(espaciadoInterno)
        )
    }
}

@Composable
// Dibujar la cabecera con avatar, buscador y botón de notificaciones
private fun CabeceraSenderos(
    estadoVisual: SenderosEstadoVisual,
    alProcesarEvento: (SenderosEvento) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AvatarConMenu(estadoVisual, alProcesarEvento)

        OutlinedTextField(
            value = estadoVisual.textoBusqueda,
            onValueChange = { texto -> alProcesarEvento(SenderosEvento.AlCambiarBusqueda(texto)) },
            placeholder = { Text("Buscar senderos") },
            leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = { alProcesarEvento(SenderosEvento.AlTocarNotificaciones) }) {
            Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notificaciones")
        }
    }
}

@Composable
// Dibujar el avatar del usuario y su menú desplegable de perfil
private fun AvatarConMenu(
    estadoVisual: SenderosEstadoVisual,
    alProcesarEvento: (SenderosEvento) -> Unit
) {
    val usuario = estadoVisual.usuario

    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { alProcesarEvento(SenderosEvento.AlTocarAvatar) },
            contentAlignment = Alignment.Center
        ) {
            if (usuario != null && usuario.fotoUrl.isNotEmpty()) {
                AsyncImage(
                    model = usuario.fotoUrl,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = obtenerInicial(usuario),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        DropdownMenu(
            expanded = estadoVisual.esMenuPerfilVisible,
            onDismissRequest = { alProcesarEvento(SenderosEvento.AlCerrarMenuPerfil) },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text("Cerrar sesión") },
                leadingIcon = {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                },
                onClick = { alProcesarEvento(SenderosEvento.AlCerrarSesion) }
            )
        }
    }
}

// Obtener la inicial en mayúscula del nombre o, si está vacío, del correo del usuario
private fun obtenerInicial(usuario: Usuario?): String {
    val texto = usuario?.nombre?.ifBlank { usuario.email } ?: ""
    return texto.trim().take(1).uppercase()
}

@Composable
// Dibujar el cuerpo según esté cargando, haya error, no haya resultados o haya senderos
private fun CuerpoSenderos(
    estadoVisual: SenderosEstadoVisual,
    alProcesarEvento: (SenderosEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    val senderosFiltrados = estadoVisual.senderosFiltrados

    when {
        estadoVisual.estaCargando -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        estadoVisual.mensajeError != null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = estadoVisual.mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        senderosFiltrados.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No se encontraron senderos", style = MaterialTheme.typography.bodyMedium)
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Mis senderos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(senderosFiltrados, key = { sendero -> sendero.id }) { sendero ->
                TarjetaSendero(
                    sendero = sendero,
                    alSeleccionar = { alProcesarEvento(SenderosEvento.AlSeleccionarSendero(sendero.id)) }
                )
            }
        }
    }
}

@Composable
// Dibujar la tarjeta clickeable con los datos de un sendero
private fun TarjetaSendero(sendero: Sendero, alSeleccionar: () -> Unit) {
    Card(
        onClick = alSeleccionar,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = sendero.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                when (sendero.estadoSincronizacion) {
                    EstadoSincronizacion.PENDIENTE -> Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = "Pendiente de sincronizar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    EstadoSincronizacion.CON_ERROR -> Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = "Error al sincronizar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    EstadoSincronizacion.SINCRONIZADO -> Unit
                }
                if (sendero.esFavorito) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorito",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = sendero.region, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "%.0f km".format(sendero.distanciaKilometros),
                    style = MaterialTheme.typography.bodySmall
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = obtenerTextoDificultad(sendero.dificultad),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// Obtener el texto visible de una dificultad
private fun obtenerTextoDificultad(dificultad: Dificultad): String {
    return when (dificultad) {
        Dificultad.BAJA -> "Baja"
        Dificultad.MEDIA -> "Media"
        Dificultad.ALTA -> "Alta"
    }
}

@Composable
// Dibujar la botonera inferior con las cinco secciones de navegación
private fun BotoneraInferior(
    seccionSeleccionada: SeccionNavegacion,
    alProcesarEvento: (SenderosEvento) -> Unit
) {
    val coloresItem = NavigationBarItemDefaults.colors(
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        ItemBotonera(SeccionNavegacion.INICIO, "Inicio", Icons.Outlined.Home, seccionSeleccionada, coloresItem, alProcesarEvento)
        ItemBotonera(SeccionNavegacion.MAPA, "Mapa", Icons.Outlined.Map, seccionSeleccionada, coloresItem, alProcesarEvento)

        NavigationBarItem(
            selected = seccionSeleccionada == SeccionNavegacion.NAVEGAR,
            onClick = {
                alProcesarEvento(SenderosEvento.AlSeleccionarSeccion(SeccionNavegacion.NAVEGAR))
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = "Navegar",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            colors = coloresItem
        )

        ItemBotonera(SeccionNavegacion.FAVORITOS, "Favoritos", Icons.Outlined.FavoriteBorder, seccionSeleccionada, coloresItem, alProcesarEvento)
        ItemBotonera(SeccionNavegacion.PERFIL, "Perfil", Icons.Outlined.Person, seccionSeleccionada, coloresItem, alProcesarEvento)
    }
}

@Composable
// Dibujar un ítem de la botonera inferior con ícono y etiqueta
private fun RowScope.ItemBotonera(
    seccion: SeccionNavegacion,
    etiqueta: String,
    icono: ImageVector,
    seccionSeleccionada: SeccionNavegacion,
    colores: NavigationBarItemColors,
    alProcesarEvento: (SenderosEvento) -> Unit
) {
    NavigationBarItem(
        selected = seccionSeleccionada == seccion,
        onClick = { alProcesarEvento(SenderosEvento.AlSeleccionarSeccion(seccion)) },
        icon = { Icon(imageVector = icono, contentDescription = null) },
        label = { Text(etiqueta) },
        colors = colores
    )
}

@Preview(showBackground = true)
@Composable
// Previsualizar la pantalla de senderos con datos de ejemplo
private fun SenderosContenidoPreview() {
    MaterialTheme {
        SenderosContenido(
            estadoVisual = SenderosEstadoVisual(
                usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"),
                senderos = listOf(
                    Sendero("huella-andina", "usuario-1", "Huella Andina", "Neuquén, Río Negro y Chubut", 570.0, Dificultad.MEDIA),
                    Sendero("huella-neuquina", "usuario-1", "Huella Neuquina", "Neuquén", 120.0, Dificultad.MEDIA)
                ),
                estaCargando = false
            ),
            alProcesarEvento = {}
        )
    }
}
