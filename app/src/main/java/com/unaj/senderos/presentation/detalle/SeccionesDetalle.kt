package com.unaj.senderos.presentation.detalle

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import com.unaj.senderos.presentation.componentes.accionable
import com.unaj.senderos.presentation.componentes.BotonSecundario
import com.unaj.senderos.presentation.comun.formatearDistanciaSendero
import com.unaj.senderos.presentation.comun.formatearTamanoMapa
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Cabin
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PinDrop
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
import com.unaj.senderos.presentation.tema.obtenerColorLineaBioma
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.presentation.componentes.IndicadorDificultad
import com.unaj.senderos.presentation.componentes.TituloDeSeccion
import com.unaj.senderos.presentation.componentes.obtenerTextoTipoRecorrido
import com.unaj.senderos.presentation.revisionpuntos.FotoDePunto
import com.unaj.senderos.presentation.revisionpuntos.describirCantidadPuntosPorRevisar
import com.unaj.senderos.presentation.componentes.mapa.MapaRecorrido

private val TAMANO_MINIATURA_PUNTO = 48.dp

internal data class DatoRecorrido(
    val icono: ImageVector,
    val valor: String,
    val etiqueta: String
)

@Composable
internal fun DatosDelRecorrido(sendero: Sendero, modifier: Modifier = Modifier) {
    val datos = listOf(
        DatoRecorrido(Icons.Outlined.Straighten, formatearDistanciaSendero(sendero.distanciaKilometros), "Distancia"),
        DatoRecorrido(Icons.AutoMirrored.Filled.TrendingUp, "${sendero.desnivelMetros} m", "Desnivel"),
        DatoRecorrido(Icons.Outlined.Schedule, sendero.duracionEstimada, "Duración"),
        DatoRecorrido(Icons.Filled.ArrowUpward, "${sendero.alturaMaximaMetros} m", "Altura máxima"),
        DatoRecorrido(Icons.Filled.ArrowDownward, "${sendero.alturaMinimaMetros} m", "Altura mínima"),
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
internal fun CeldaDato(dato: DatoRecorrido, modifier: Modifier = Modifier) {
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
internal fun FilaConIcono(icono: ImageVector, texto: String) {
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
internal fun RecuadroMapa(
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
internal fun TarjetaMapaSinConexion(
    estado: EstadoMapaSinConexion,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Mapa sin conexión", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            when (estado) {
                is EstadoMapaSinConexion.NoDescargado -> BotonSecundario(
                    texto = "Descargar mapa para usar sin señal",
                    alTocar = { alProcesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa) },
                    modifier = Modifier.fillMaxWidth()
                )
                is EstadoMapaSinConexion.Descargando -> ProgresoDeDescarga(
                    texto = describirDescarga(estado),
                    porcentaje = estado.porcentaje,
                    alCancelar = { alProcesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa) }
                )
                is EstadoMapaSinConexion.EsperandoConexion -> ProgresoDeDescarga(
                    texto = describirDescarga(estado),
                    porcentaje = estado.porcentaje,
                    alCancelar = { alProcesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa) }
                )
                is EstadoMapaSinConexion.Disponible -> FilaEstadoMapa(
                    texto = "Disponible sin conexión · ${formatearTamanoMapa(estado.bytes)}",
                    textoAccion = "Eliminar",
                    alAccionar = { alProcesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa) }
                )
                is EstadoMapaSinConexion.Desactualizado -> FilaEstadoMapa(
                    texto = "El recorrido cambió · ${formatearTamanoMapa(estado.bytes)}",
                    textoAccion = "Volver a descargar",
                    alAccionar = { alProcesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa) },
                    textoAccionSecundaria = "Eliminar",
                    alAccionarSecundaria = { alProcesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa) }
                )
                is EstadoMapaSinConexion.ConError -> FilaEstadoMapa(
                    texto = estado.mensaje,
                    textoAccion = "Reintentar",
                    alAccionar = { alProcesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa) },
                    esError = true,
                    textoAccionSecundaria = "Eliminar",
                    alAccionarSecundaria = { alProcesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa) }
                )
            }
        }
    }
}

@Composable
private fun FilaEstadoMapa(
    texto: String,
    textoAccion: String,
    alAccionar: () -> Unit,
    esError: Boolean = false,
    textoAccionSecundaria: String? = null,
    alAccionarSecundaria: () -> Unit = {}
) {
    Column {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            if (textoAccionSecundaria != null) {
                TextButton(onClick = alAccionarSecundaria) { Text(textoAccionSecundaria) }
            }
            TextButton(onClick = alAccionar) { Text(textoAccion) }
        }
    }
}

@Composable
private fun ProgresoDeDescarga(texto: String, porcentaje: Int, alCancelar: () -> Unit) {
    Column {
        Text(text = texto, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { porcentaje.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = alCancelar) { Text("Cancelar descarga") }
        }
    }
}

@Composable
internal fun SeccionDesplegable(
    titulo: String,
    estaExpandida: Boolean,
    alAlternar: () -> Unit,
    contenido: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .accionable(if (estaExpandida) "Contraer" else "Expandir", alTocar = alAlternar)
                .semantics { stateDescription = if (estaExpandida) "Expandido" else "Contraído" }
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
                contentDescription = null,
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
internal fun ListaDePuntosDeInteres(
    puntos: List<PuntoDeInteres>,
    alSeleccionar: (Int) -> Unit,
    alAmpliarFoto: (String) -> Unit = {}
) {
    Column {
        puntos.forEach { punto ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .accionable("Ver en el mapa") { alSeleccionar(punto.indice) }
                        .semantics(mergeDescendants = true) { contentDescription = punto.descripcionAccesible() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = obtenerIconoTipoPunto(punto.tipo),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = punto.texto(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatearKilometrosDesdeInicio(punto.distanciaDesdeInicioKilometros),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (punto.foto.isNotEmpty()) {
                    FotoDePunto(
                        foto = punto.foto,
                        descripcion = "Foto de ${punto.texto()}",
                        modifier = Modifier
                            .size(TAMANO_MINIATURA_PUNTO)
                            .clip(RoundedCornerShape(8.dp))
                            .accionable("Ampliar la foto") { alAmpliarFoto(punto.foto) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun TarjetaPuntoSeleccionado(
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

@Composable
internal fun TarjetaPuntosPorRevisar(cantidad: Int, alRevisar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PinDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = describirCantidadPuntosPorRevisar(cantidad),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            TextButton(onClick = alRevisar) { Text("Revisar") }
        }
    }
}

internal fun obtenerIconoTipoPunto(tipo: TipoPunto): ImageVector {
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
internal fun ListaConVinietas(elementos: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        elementos.forEach { elemento ->
            Text(text = "• $elemento", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun ListaDeEtapas(etapas: List<Etapa>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        etapas.forEach { etapa ->
            Text(
                text = "Etapa ${etapa.numero}: ${etapa.nombre} — ${formatearDistanciaSendero(etapa.distanciaKilometros)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
