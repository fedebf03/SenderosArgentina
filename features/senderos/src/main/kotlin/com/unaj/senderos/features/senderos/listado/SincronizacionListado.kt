package com.unaj.senderos.features.senderos.listado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion

// Describir en una frase qué falta sincronizar: lo que se está haciendo ahora y, si no, cuántos senderos esperan subir o fallaron
internal fun describirSincronizacion(estadoVisual: SenderosEstadoVisual): String = when {
    estadoVisual.estadoTrabajoSincronizacion == EstadoTrabajoSincronizacion.EN_CURSO -> "Sincronizando…"
    estadoVisual.estadoTrabajoSincronizacion == EstadoTrabajoSincronizacion.EN_REINTENTO -> "Falló el último intento; se reintenta solo en unos instantes"
    estadoVisual.estadoTrabajoSincronizacion == EstadoTrabajoSincronizacion.EN_COLA ->
        if (estadoVisual.estaSinConexion) "Esperando conexión para sincronizar" else "Sincronización en cola…"
    estadoVisual.cantidadConErrorDeSubida > 0 -> "${estadoVisual.cantidadConErrorDeSubida} con error al subir"
    estadoVisual.cantidadSinSubir > 0 -> "${estadoVisual.cantidadSinSubir} sin subir a tu cuenta"
    else -> "Todo sincronizado con tu cuenta"
}

// Mostrar sobre la lista de Mis senderos si todo está subido, cuántos faltan y el botón para sincronizar a mano
@Composable
internal fun ResumenSincronizacion(estadoVisual: SenderosEstadoVisual, alProcesarEvento: (SenderosEvento) -> Unit) {
    val estaOcupada = estadoVisual.estadoTrabajoSincronizacion == EstadoTrabajoSincronizacion.EN_CURSO
    val hayProblema = estadoVisual.cantidadConErrorDeSubida > 0
    val icono: ImageVector = when {
        hayProblema -> Icons.Outlined.CloudOff
        estadoVisual.cantidadSinSubir > 0 -> Icons.Outlined.CloudUpload
        else -> Icons.Outlined.CloudDone
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (hayProblema) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = describirSincronizacion(estadoVisual),
            style = MaterialTheme.typography.bodySmall,
            color = if (hayProblema) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = { alProcesarEvento(SenderosEvento.AlTocarSincronizar) },
            enabled = !estaOcupada
        ) {
            Icon(imageVector = Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = "Sincronizar ahora", modifier = Modifier.padding(start = 6.dp))
        }
    }
}
