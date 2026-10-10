package com.unaj.senderos.presentation.detalle

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoRecorrido
import kotlin.math.roundToInt

private const val TAG = "DETALLE_SENDERO_PANTALLA"

@Composable
// Coordinar el estado y los efectos de la pantalla de detalle de un sendero
fun DetalleSenderoPantalla(
    viewModel: DetalleSenderoViewModel,
    alVolverAlListado: () -> Unit,
    alNavegarAEdicion: (String) -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverAlListadoActual by rememberUpdatedState(alVolverAlListado)
    val navegarAEdicionActual by rememberUpdatedState(alNavegarAEdicion)
    val contextoActual by rememberUpdatedState(LocalContext.current)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            Log.d(TAG, "Efecto recibido: $efecto")
            when (efecto) {
                is DetalleSenderoEfecto.VolverAlListado -> volverAlListadoActual()
                is DetalleSenderoEfecto.NavegarAEdicion -> navegarAEdicionActual(efecto.idSendero)
                is DetalleSenderoEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
                is DetalleSenderoEfecto.CompartirSendero -> {
                    val intencionCompartir = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, efecto.texto)
                    }
                    contextoActual.startActivity(Intent.createChooser(intencionCompartir, "Compartir sendero"))
                }
            }
        }
    }

    DetalleSenderoContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        estadoAvisos = estadoAvisos
    )
}

@Composable
// Dibujar la pantalla de detalle con cabecera ilustrada, datos, secciones y botón de recorrido
fun DetalleSenderoContenido(
    estadoVisual: DetalleSenderoEstadoVisual,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    BackHandler { alProcesarEvento(DetalleSenderoEvento.AlVolver) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        bottomBar = {
            if (estadoVisual.sendero != null) {
                BotonIniciarRecorrido(alProcesarEvento)
            }
        }
    ) { espaciadoInterno ->
        val sendero = estadoVisual.sendero
        when {
            estadoVisual.estaCargando -> Box(
                Modifier.fillMaxSize().padding(espaciadoInterno),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            estadoVisual.mensajeError != null || sendero == null -> Column(
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
                TextButton(onClick = { alProcesarEvento(DetalleSenderoEvento.AlVolver) }) {
                    Text("Volver")
                }
            }
            else -> CuerpoDetalle(
                estadoVisual = estadoVisual,
                sendero = sendero,
                alProcesarEvento = alProcesarEvento,
                modifier = Modifier.padding(bottom = espaciadoInterno.calculateBottomPadding())
            )
        }
    }

    if (estadoVisual.esDialogoEliminarVisible && estadoVisual.sendero != null) {
        AlertDialog(
            onDismissRequest = { alProcesarEvento(DetalleSenderoEvento.AlCancelarEliminar) },
            title = { Text("Eliminar sendero") },
            text = {
                Text("¿Querés eliminar ${estadoVisual.sendero.nombre}? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = { alProcesarEvento(DetalleSenderoEvento.AlConfirmarEliminar) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { alProcesarEvento(DetalleSenderoEvento.AlCancelarEliminar) }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
// Dibujar el botón principal fijo al pie para iniciar el recorrido
private fun BotonIniciarRecorrido(alProcesarEvento: (DetalleSenderoEvento) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Button(
            onClick = { alProcesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Iniciar recorrido")
        }
    }
}

@Composable
// Dibujar el contenido desplazable: cabecera, datos, mapa reservado, descripción y secciones
private fun CuerpoDetalle(
    estadoVisual: DetalleSenderoEstadoVisual,
    sendero: Sendero,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CabeceraDetalle(estadoVisual, sendero, alProcesarEvento) }
        item {
            DatosDelRecorrido(sendero, modifier = Modifier.padding(horizontal = 24.dp))
        }
        item { RecuadroMapa(modifier = Modifier.padding(horizontal = 24.dp)) }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TituloDeSeccion("Descripción")
                Text(text = sendero.descripcion, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SeccionDesplegable(
                    titulo = "Recomendaciones",
                    estaExpandida = SeccionDetalle.RECOMENDACIONES in estadoVisual.seccionesExpandidas,
                    alAlternar = {
                        alProcesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.RECOMENDACIONES))
                    }
                ) {
                    ListaConVinietas(sendero.recomendaciones)
                }
                HorizontalDivider()
                SeccionDesplegable(
                    titulo = "Servicios en el camino",
                    estaExpandida = SeccionDetalle.SERVICIOS in estadoVisual.seccionesExpandidas,
                    alAlternar = {
                        alProcesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.SERVICIOS))
                    }
                ) {
                    ListaConVinietas(sendero.servicios)
                }
                HorizontalDivider()
                SeccionDesplegable(
                    titulo = "Etapas",
                    estaExpandida = SeccionDetalle.ETAPAS in estadoVisual.seccionesExpandidas,
                    alAlternar = {
                        alProcesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.ETAPAS))
                    }
                ) {
                    ListaDeEtapas(sendero.etapas)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
// Dibujar la cabecera con la ilustración de montañas, las acciones y el nombre del sendero
private fun CabeceraDetalle(
    estadoVisual: DetalleSenderoEstadoVisual,
    sendero: Sendero,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        IlustracionMontanias(modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonCircular(
                icono = Icons.AutoMirrored.Filled.ArrowBack,
                descripcion = "Volver",
                alTocar = { alProcesarEvento(DetalleSenderoEvento.AlVolver) }
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonCircular(
                    icono = if (sendero.esFavorito) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    descripcion = if (sendero.esFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                    alTocar = { alProcesarEvento(DetalleSenderoEvento.AlAlternarFavorito) }
                )
                BotonCircular(
                    icono = Icons.Outlined.Share,
                    descripcion = "Compartir",
                    alTocar = { alProcesarEvento(DetalleSenderoEvento.AlCompartir) }
                )
                Box {
                    BotonCircular(
                        icono = Icons.Filled.MoreVert,
                        descripcion = "Más opciones",
                        alTocar = { alProcesarEvento(DetalleSenderoEvento.AlAbrirMenuOpciones) }
                    )
                    MenuOpciones(estadoVisual.esMenuOpcionesVisible, alProcesarEvento)
                }
            }
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
                    color = MaterialTheme.colorScheme.primary
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

@Composable
// Dibujar el menú desplegable con las opciones de editar y eliminar
private fun MenuOpciones(
    esVisible: Boolean,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit
) {
    DropdownMenu(
        expanded = esVisible,
        onDismissRequest = { alProcesarEvento(DetalleSenderoEvento.AlCerrarMenuOpciones) },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        DropdownMenuItem(
            text = { Text("Editar") },
            leadingIcon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = null) },
            onClick = { alProcesarEvento(DetalleSenderoEvento.AlTocarEditar) }
        )
        DropdownMenuItem(
            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = { alProcesarEvento(DetalleSenderoEvento.AlTocarEliminar) }
        )
    }
}

@Composable
// Dibujar un botón de ícono dentro de un círculo semitransparente para dar contraste sobre la ilustración
private fun BotonCircular(
    icono: ImageVector,
    descripcion: String,
    alTocar: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        IconButton(onClick = alTocar) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
// Dibujar la ilustración de montañas con cielo degradado y tres cordones superpuestos
private fun IlustracionMontanias(modifier: Modifier = Modifier) {
    val colorCieloSuperior = MaterialTheme.colorScheme.primaryContainer
    val colorCieloInferior = MaterialTheme.colorScheme.background
    val colorMontaniaLejana = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val colorMontaniaCercana = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        drawRect(brush = Brush.verticalGradient(listOf(colorCieloSuperior, colorCieloInferior)))

        val ancho = size.width
        val alto = size.height

        val cordonLejano = Path().apply {
            moveTo(0f, alto * 0.70f)
            lineTo(ancho * 0.20f, alto * 0.38f)
            lineTo(ancho * 0.38f, alto * 0.62f)
            lineTo(ancho * 0.60f, alto * 0.30f)
            lineTo(ancho * 0.82f, alto * 0.60f)
            lineTo(ancho, alto * 0.45f)
            lineTo(ancho, alto)
            lineTo(0f, alto)
            close()
        }
        drawPath(path = cordonLejano, color = colorMontaniaLejana)

        val cordonIntermedio = Path().apply {
            moveTo(0f, alto * 0.82f)
            lineTo(ancho * 0.15f, alto * 0.60f)
            lineTo(ancho * 0.33f, alto * 0.78f)
            lineTo(ancho * 0.52f, alto * 0.52f)
            lineTo(ancho * 0.74f, alto * 0.80f)
            lineTo(ancho * 0.90f, alto * 0.64f)
            lineTo(ancho, alto * 0.76f)
            lineTo(ancho, alto)
            lineTo(0f, alto)
            close()
        }
        drawPath(path = cordonIntermedio, color = colorMontaniaLejana)

        val cordonCercano = Path().apply {
            moveTo(0f, alto * 0.92f)
            lineTo(ancho * 0.25f, alto * 0.74f)
            lineTo(ancho * 0.45f, alto * 0.90f)
            lineTo(ancho * 0.68f, alto * 0.72f)
            lineTo(ancho * 0.88f, alto * 0.90f)
            lineTo(ancho, alto * 0.82f)
            lineTo(ancho, alto)
            lineTo(0f, alto)
            close()
        }
        drawPath(path = cordonCercano, color = colorMontaniaCercana)
    }
}

// Representar un dato numérico del recorrido con su ícono, valor y etiqueta
private data class DatoRecorrido(
    val icono: ImageVector,
    val valor: String,
    val etiqueta: String
)

@Composable
// Dibujar el bloque de datos del recorrido: grilla, dificultad, punto de inicio y mejor época
private fun DatosDelRecorrido(sendero: Sendero, modifier: Modifier = Modifier) {
    val datos = listOf(
        DatoRecorrido(Icons.Outlined.Straighten, "${sendero.distanciaKilometros.roundToInt()} km", "Distancia"),
        DatoRecorrido(Icons.AutoMirrored.Filled.TrendingUp, "${sendero.desnivelMetros} m", "Desnivel"),
        DatoRecorrido(Icons.Outlined.Schedule, sendero.duracionEstimada, "Duración"),
        DatoRecorrido(Icons.Filled.ArrowUpward, "${sendero.alturaMaximaMetros} m", "Altura máx."),
        DatoRecorrido(Icons.Filled.ArrowDownward, "${sendero.alturaMinimaMetros} m", "Altura mín."),
        DatoRecorrido(Icons.Outlined.Loop, obtenerTextoTipoRecorrido(sendero.tipoRecorrido), "Tipo")
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TituloDeSeccion("Datos del recorrido")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            datos.chunked(3).forEach { filaDeDatos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filaDeDatos.forEach { dato ->
                        TarjetaDato(dato, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Dificultad", style = MaterialTheme.typography.bodyMedium)
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

        FilaConIcono(Icons.Outlined.Flag, "Punto de inicio: ${sendero.puntoInicio}")
        FilaConIcono(Icons.Outlined.CalendarMonth, "Mejor época: ${sendero.mejorEpoca}")
    }
}

@Composable
// Dibujar una tarjeta con el ícono, el valor y la etiqueta de un dato del recorrido
private fun TarjetaDato(dato: DatoRecorrido, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = dato.icono,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = dato.valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = dato.etiqueta,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
// Dibujar una fila con un ícono decorativo seguido de un texto
private fun FilaConIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(text = texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
// Dibujar el recuadro reservado donde se mostrará el mapa del sendero
private fun RecuadroMapa(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "Mapa disponible próximamente", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
// Dibujar un título de sección con el estilo primario de la app
private fun TituloDeSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
// Dibujar una sección con encabezado clickeable y contenido que se despliega con animación
private fun SeccionDesplegable(
    titulo: String,
    estaExpandida: Boolean,
    alAlternar: () -> Unit,
    contenido: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = alAlternar)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (estaExpandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (estaExpandida) "Contraer $titulo" else "Expandir $titulo",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        AnimatedVisibility(visible = estaExpandida) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                contenido()
            }
        }
    }
}

@Composable
// Dibujar una lista de textos precedidos por una viñeta
private fun ListaConVinietas(elementos: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        elementos.forEach { elemento ->
            Text(text = "• $elemento", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
// Dibujar la lista de etapas con su número, nombre y distancia
private fun ListaDeEtapas(etapas: List<Etapa>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        etapas.forEach { etapa ->
            Text(
                text = "Etapa ${etapa.numero}: ${etapa.nombre} — ${etapa.distanciaKilometros.roundToInt()} km",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Obtener el texto visible de un tipo de recorrido
private fun obtenerTextoTipoRecorrido(tipoRecorrido: TipoRecorrido): String {
    return when (tipoRecorrido) {
        TipoRecorrido.CIRCULAR -> "Circular"
        TipoRecorrido.IDA_Y_VUELTA -> "Ida y vuelta"
        TipoRecorrido.LINEAL -> "Lineal"
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

@Preview(showBackground = true)
@Composable
// Previsualizar el detalle con el sendero de ejemplo Huella Andina
private fun DetalleSenderoContenidoPreview() {
    MaterialTheme {
        DetalleSenderoContenido(
            estadoVisual = DetalleSenderoEstadoVisual(
                sendero = Sendero(
                    id = "huella-andina",
                    idUsuario = "usuario-1",
                    nombre = "Huella Andina",
                    region = "Neuquén, Río Negro y Chubut",
                    distanciaKilometros = 570.0,
                    dificultad = Dificultad.MEDIA,
                    descripcion = "Travesía de largo recorrido que une lagos, bosques andino-patagónicos y pasos de montaña a lo largo de la cordillera.",
                    desnivelMetros = 1200,
                    duracionEstimada = "30 días",
                    alturaMaximaMetros = 1900,
                    alturaMinimaMetros = 700,
                    tipoRecorrido = TipoRecorrido.LINEAL,
                    puntoInicio = "Lago Aluminé, Neuquén",
                    mejorEpoca = "Diciembre a marzo",
                    recomendaciones = listOf("Llevar agua y filtro potabilizador"),
                    servicios = listOf("Campings organizados"),
                    etapas = listOf(Etapa(1, "Lago Aluminé - Lago Ruca Choroi", 21.0))
                ),
                estaCargando = false
            ),
            alProcesarEvento = {}
        )
    }
}
