package com.unaj.senderos.core.ui.sendero

import androidx.compose.ui.semantics.contentDescription
import com.unaj.senderos.core.ui.componentes.accionable
import com.unaj.senderos.core.ui.comun.formatearDistanciaSendero
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cabin
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.unaj.senderos.core.ui.tema.obtenerColorLineaBioma
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.ui.componentes.IndicadorDificultad
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.componentes.obtenerTextoTipoRecorrido
import com.unaj.senderos.core.ui.revisionpuntos.FotoDePunto
import com.unaj.senderos.core.ui.componentes.mapa.MapaRecorrido

data class DatoRecorrido(
    val icono: ImageVector,
    val valor: String,
    val etiqueta: String
)

@Composable
fun DatosDelRecorrido(sendero: Sendero, modifier: Modifier = Modifier) {
    val datos = listOf(
        DatoRecorrido(Icons.Outlined.Straighten, formatearDistanciaSendero(sendero.distanciaKilometros), "Distancia"),
        DatoRecorrido(Icons.Outlined.Schedule, sendero.duracionEstimada, "Duración"),
        DatoRecorrido(Icons.Outlined.Loop, obtenerTextoTipoRecorrido(sendero.tipoRecorrido), "Tipo")
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TituloDeSeccion("Datos del recorrido")

        OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            datos.chunked(3).forEachIndexed { indiceFila, filaDeDatos ->
                if (indiceFila > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    filaDeDatos.forEachIndexed { indiceDato, dato ->
                        if (indiceDato > 0) {
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        CeldaDato(dato, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Dificultad", style = MaterialTheme.typography.bodyMedium)
            IndicadorDificultad(sendero.dificultad, estiloTexto = MaterialTheme.typography.bodyMedium)
        }

        FilaConIcono(Icons.Outlined.Flag, "Punto de inicio: ${sendero.puntoInicio}")
        FilaConIcono(Icons.Outlined.CalendarMonth, "Mejor época: ${sendero.mejorEpoca}")
    }
}

@Composable
fun CeldaDato(dato: DatoRecorrido, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 14.dp, horizontal = 8.dp),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FilaConIcono(icono: ImageVector, texto: String) {
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
fun RecuadroMapa(
    sendero: Sendero,
    modifier: Modifier = Modifier,
    puntoEnfocado: Coordenada? = null,
    claveEnfoque: Int = 0,
    indiceSeleccionado: Int? = null,
    alSeleccionarPunto: (Int?) -> Unit = {},
    alDescargarMapa: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        if (sendero.recorrido.isEmpty()) {
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
                Text(text = "Este sendero todavía no tiene recorrido", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            MapaRecorrido(
                puntos = sendero.recorrido,
                tipoRecorrido = sendero.tipoRecorrido,
                modifier = Modifier.fillMaxSize(),
                usarModoTextura = true,
                colorLinea = obtenerColorLineaBioma(sendero.bioma, MaterialTheme.colorScheme.primary),
                puntoEnfocado = puntoEnfocado,
                claveEnfoque = claveEnfoque,
                indiceSeleccionado = indiceSeleccionado,
                alSeleccionarPunto = alSeleccionarPunto,
                alDescargarMapa = alDescargarMapa
            )
        }
    }
}

@Composable
fun TarjetaPuntoSeleccionado(
    punto: PuntoDeInteres,
    alAmpliarFoto: (String) -> Unit,
    alCerrar: () -> Unit,
    alturaFoto: Dp = 180.dp
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = obtenerIconoTipoPunto(punto.tipo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = punto.texto(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    text = formatearKilometrosDesdeInicio(punto.distanciaDesdeInicioKilometros),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = alCerrar) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }
            if (punto.foto.isNotEmpty()) {
                FotoDePunto(
                    foto = punto.foto,
                    descripcion = "Foto de ${punto.texto()}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                        .height(alturaFoto)
                        .clip(RoundedCornerShape(12.dp))
                        .accionable("Ampliar la foto") { alAmpliarFoto(punto.foto) }
                )
            }
        }
    }
}

fun obtenerIconoTipoPunto(tipo: TipoPunto): ImageVector {
    return when (tipo) {
        TipoPunto.PASO -> Icons.Outlined.Flag
        TipoPunto.MIRADOR -> Icons.Outlined.Terrain
        TipoPunto.AGUA -> Icons.Outlined.WaterDrop
        TipoPunto.REFUGIO -> Icons.Outlined.Cabin
        TipoPunto.CAMPING -> Icons.Outlined.Park
        TipoPunto.PELIGRO -> Icons.Outlined.Warning
        TipoPunto.OTRO -> Icons.Outlined.Star
    }
}

@Composable
fun ListaConVinietas(elementos: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        elementos.forEach { elemento ->
            Text(text = "• $elemento", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ListaDeEtapas(etapas: List<Etapa>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        etapas.forEach { etapa ->
            Text(
                text = "Etapa ${etapa.numero}: ${etapa.nombre} — ${formatearDistanciaSendero(etapa.distanciaKilometros)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
