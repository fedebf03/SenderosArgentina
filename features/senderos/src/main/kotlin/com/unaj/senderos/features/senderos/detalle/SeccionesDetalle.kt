package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.ui.sendero.formatearKilometrosDesdeInicio
import com.unaj.senderos.core.ui.sendero.PuntoDeInteres
import com.unaj.senderos.core.ui.sendero.obtenerIconoTipoPunto
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import com.unaj.senderos.core.ui.componentes.accionable
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.comun.formatearTamanoMapa
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.ui.revisionpuntos.FotoDePunto
import com.unaj.senderos.core.ui.revisionpuntos.describirCantidadPuntosPorRevisar

private val TAMANO_MINIATURA_PUNTO = 48.dp

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
