package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.Sendero
import java.util.Locale
import kotlin.math.roundToInt

private val LOCALE_DISTANCIA: Locale = Locale.forLanguageTag("es-AR")

// Renderizar la lista de senderos con su título y la distancia al usuario cuando se conoce
@Composable
fun ListaSenderos(
    titulo: String,
    senderos: List<Sendero>,
    alSeleccionarSendero: (String) -> Unit,
    modifier: Modifier = Modifier,
    espacioInferior: PaddingValues = PaddingValues(bottom = 16.dp),
    distanciaKilometrosDe: (Sendero) -> Double? = { null }
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 8.dp,
            bottom = espacioInferior.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
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
                distanciaKilometros = distanciaKilometrosDe(sendero)
            )
        }
    }
}

// Renderizar un indicador de carga centrado
@Composable
fun CargandoCentrado(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// Renderizar un mensaje de texto centrado, en color de error si corresponde
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

// Renderizar el aviso de lista vacía con ícono, título y detalle
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

// Renderizar la tarjeta de un sendero con foto, datos, estado de sincronización y distancia opcional
@Composable
fun TarjetaSendero(
    sendero: Sendero,
    alSeleccionar: () -> Unit,
    miniaturaEspejada: Boolean = false,
    distanciaKilometros: Double? = null
) {
    OutlinedCard(
        onClick = alSeleccionar,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FotoSendero(
                foto = sendero.foto,
                espejada = miniaturaEspejada,
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
                    text = sendero.region,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (distanciaKilometros != null) {
                    DatoTarjeta(Icons.Outlined.NearMe, formatearDistanciaAlUsuario(distanciaKilometros))
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatoTarjeta(Icons.Outlined.Straighten, "%.0f km".format(sendero.distanciaKilometros))
                    if (sendero.duracionEstimada.isNotBlank()) {
                        DatoTarjeta(Icons.Outlined.Schedule, sendero.duracionEstimada)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    ChipDificultad(sendero.dificultad)
                }
            }
        }
    }
}

// Formatear la distancia al usuario: metros bajo 1 km, un decimal hasta 10 km y entero a partir de allí
fun formatearDistanciaAlUsuario(distanciaKilometros: Double): String {
    val metros = (distanciaKilometros * 1000).roundToInt()
    if (metros < 1000) return "a $metros m de vos"
    val kilometrosConDecimal = (distanciaKilometros * 10).roundToInt() / 10.0
    if (kilometrosConDecimal < 10) return "a ${"%.1f".format(LOCALE_DISTANCIA, kilometrosConDecimal)} km de vos"
    return "a ${distanciaKilometros.roundToInt()} km de vos"
}

// Renderizar un dato breve de la tarjeta con ícono y texto
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

// Renderizar el ícono del estado de sincronización cuando el sendero no está sincronizado
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
