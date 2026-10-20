package com.unaj.senderos.core.ui.sendero

import com.unaj.senderos.core.ui.comun.formatearDistanciaAlUsuario
import com.unaj.senderos.core.ui.comun.formatearDistanciaSendero
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.ui.tema.obtenerColoresDelBioma

// Con este ancho mínimo una pantalla angosta muestra una columna y una tablet reparte las tarjetas en varias
private val ANCHO_MINIMO_TARJETA = 360.dp

@Composable
fun ListaSenderos(
    titulo: String,
    senderos: List<Sendero>,
    alSeleccionarSendero: (String) -> Unit,
    modifier: Modifier = Modifier,
    espacioInferior: PaddingValues = PaddingValues(bottom = 16.dp),
    distanciaKilometrosDe: (Sendero) -> Double? = { null },
    autorDe: (Sendero) -> String? = { null }
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = ANCHO_MINIMO_TARJETA),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 8.dp,
            bottom = espacioInferior.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "$titulo · ${senderos.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        itemsIndexed(senderos, key = { _, sendero -> sendero.id }) { indice, sendero ->
            TarjetaSendero(
                sendero = sendero,
                miniaturaEspejada = indice % 2 == 1,
                alSeleccionar = { alSeleccionarSendero(sendero.id) },
                distanciaKilometros = distanciaKilometrosDe(sendero),
                autor = autorDe(sendero)
            )
        }
    }
}

@Composable
fun CargandoCentrado(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MensajeCentrado(texto: String, modifier: Modifier = Modifier, esError: Boolean = false) {
    Box(modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = texto,
            color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MensajeVacio(icono: ImageVector, titulo: String, detalle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun TarjetaSendero(
    sendero: Sendero,
    alSeleccionar: () -> Unit,
    miniaturaEspejada: Boolean = false,
    distanciaKilometros: Double? = null,
    autor: String? = null
) {
    OutlinedCard(
        onClick = alSeleccionar,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (sendero.bioma != Bioma.SIN_DEFINIR) {
                FranjaBioma(sendero.bioma)
            }
            ContenidoTarjetaSendero(sendero, miniaturaEspejada, distanciaKilometros, autor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FranjaBioma(bioma: Bioma) {
    Box(
        modifier = Modifier
            .width(6.dp)
            .fillMaxHeight()
            .background(obtenerColoresDelBioma(bioma).principal)
    )
}

@Composable
private fun ContenidoTarjetaSendero(
    sendero: Sendero,
    miniaturaEspejada: Boolean,
    distanciaKilometros: Double?,
    autor: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FotoSendero(
            foto = sendero.foto,
            espejada = miniaturaEspejada,
            bioma = sendero.bioma,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = sendero.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconoSincronizacion(sendero.estadoSincronizacion)
                if (sendero.esFavorito) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorito",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = buildAnnotatedString {
                    append(sendero.region)
                    if (sendero.bioma != Bioma.SIN_DEFINIR) {
                        append(" · ")
                        val colorTexto = lerp(obtenerColoresDelBioma(sendero.bioma).principal, Color.Black, 0.15f)
                        withStyle(SpanStyle(color = colorTexto)) { append(obtenerTextoBioma(sendero.bioma)) }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (autor != null) {
                Text(
                    text = "Por $autor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (distanciaKilometros != null) {
                DatoTarjeta(Icons.Outlined.NearMe, formatearDistanciaAlUsuario(distanciaKilometros))
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatoTarjeta(Icons.Outlined.Straighten, formatearDistanciaSendero(sendero.distanciaKilometros))
                if (sendero.duracionEstimada.isNotBlank()) {
                    DatoTarjeta(Icons.Outlined.Schedule, sendero.duracionEstimada)
                }
                Spacer(modifier = Modifier.weight(1f))
                IndicadorDificultad(sendero.dificultad)
            }
        }
    }
}

@Composable
private fun DatoTarjeta(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun IconoSincronizacion(estado: EstadoSincronizacion) {
    when (estado) {
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
}
