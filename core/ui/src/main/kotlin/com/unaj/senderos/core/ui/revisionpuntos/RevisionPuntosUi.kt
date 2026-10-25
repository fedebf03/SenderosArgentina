package com.unaj.senderos.core.ui.revisionpuntos

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.accionable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.unaj.senderos.core.domain.model.PuntoCapturadoConDistancia
import com.unaj.senderos.core.domain.model.esFotoLocal
import com.unaj.senderos.core.ui.sendero.obtenerIconoTipoPunto
import com.unaj.senderos.core.ui.sendero.obtenerTextoTipoPunto
import java.io.File

private val ALTURA_MAXIMA_LISTA_REVISION = 280.dp
private val TAMANO_MINIATURA_REVISION = 48.dp

// La foto puede ser una ruta local, una Uri de contenido o una URL remota
@Composable
fun FotoDePunto(
    foto: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val relleno = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
    val esUriODireccion = foto.startsWith("content://") || foto.startsWith("file://")
    AsyncImage(
        model = if (esFotoLocal(foto) && !esUriODireccion) File(foto) else foto,
        contentDescription = descripcion,
        contentScale = contentScale,
        placeholder = relleno,
        error = relleno,
        modifier = modifier
    )
}

@Composable
fun ContenidoRevisionPuntos(
    estado: RevisionPuntosEstadoVisual,
    alProcesarEvento: (RevisionPuntosEvento) -> Unit,
    modifier: Modifier = Modifier,
    mostrarBotonAgregar: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Elegí los puntos que querés agregar al recorrido del sendero.",
            style = MaterialTheme.typography.bodyMedium
        )
        Column(
            modifier = Modifier
                .heightIn(max = ALTURA_MAXIMA_LISTA_REVISION)
                .verticalScroll(rememberScrollState())
        ) {
            estado.puntosPendientes.forEach { punto ->
                FilaPuntoCapturado(
                    puntoConDistancia = punto,
                    estaMarcado = punto.punto.id in estado.idsMarcados,
                    alAlternar = { alProcesarEvento(RevisionPuntosEvento.AlAlternarPunto(punto.punto.id)) }
                )
            }
        }
        if (mostrarBotonAgregar) {
            BotonPrincipal(
                texto = "Agregar al sendero",
                alTocar = { alProcesarEvento(RevisionPuntosEvento.AlAgregarAlSendero) },
                estaHabilitado = !estado.estaProcesando,
                modifier = Modifier.fillMaxWidth()
            )
        }
        TextButton(
            onClick = { alProcesarEvento(RevisionPuntosEvento.AlTocarDescartarTodos) },
            enabled = !estado.estaProcesando,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Descartar todos")
        }
    }
    if (estado.esConfirmarDescartarTodosVisible) {
        DialogoConfirmarDescartarTodos(alProcesarEvento)
    }
}

@Composable
private fun FilaPuntoCapturado(puntoConDistancia: PuntoCapturadoConDistancia, estaMarcado: Boolean, alAlternar: () -> Unit) {
    val punto = puntoConDistancia.punto
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = estaMarcado, role = Role.Checkbox, onValueChange = { alAlternar() })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = estaMarcado, onCheckedChange = null)
        Icon(
            imageVector = obtenerIconoTipoPunto(punto.tipo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = obtenerTextoTipoPunto(punto.tipo), style = MaterialTheme.typography.titleSmall)
            if (punto.nota.isNotBlank()) {
                Text(
                    text = punto.nota,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            puntoConDistancia.distanciaAlTrazadoMetros?.let { metros ->
                Text(
                    text = describirDistanciaAlTrazado(metros),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (puntoConDistancia.superaDistanciaRecomendada) {
                Text(
                    text = "Lejos del trazado: puede deformar el recorrido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (punto.foto.isNotEmpty()) {
            FotoDePunto(
                foto = punto.foto,
                descripcion = "Foto del punto ${obtenerTextoTipoPunto(punto.tipo)}",
                modifier = Modifier.size(TAMANO_MINIATURA_REVISION).clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun DialogoConfirmarDescartarTodos(alProcesarEvento: (RevisionPuntosEvento) -> Unit) {
    DialogoConfirmacion(
        titulo = "Descartar todos los puntos",
        texto = "¿Querés descartar todos los puntos capturados? Se borrarán y sus fotos no se agregarán al sendero. " +
            "Esta acción no se puede deshacer.",
        textoConfirmar = "Descartar",
        alConfirmar = { alProcesarEvento(RevisionPuntosEvento.AlConfirmarDescartarTodos) },
        textoDescartar = "Conservar puntos",
        alDescartar = { alProcesarEvento(RevisionPuntosEvento.AlCancelarDescartarTodos) },
        esDestructivo = true
    )
}

@Composable
fun DialogoRevisionPuntos(
    estado: RevisionPuntosEstadoVisual,
    alProcesarEvento: (RevisionPuntosEvento) -> Unit
) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(RevisionPuntosEvento.AlCerrar) },
        title = { Text("Puntos por revisar") },
        text = { ContenidoRevisionPuntos(estado, alProcesarEvento, mostrarBotonAgregar = false) },
        confirmButton = {
            BotonPrincipal(
                texto = "Agregar al sendero",
                alTocar = { alProcesarEvento(RevisionPuntosEvento.AlAgregarAlSendero) },
                estaHabilitado = !estado.estaProcesando
            )
        },
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(RevisionPuntosEvento.AlCerrar) }) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun DialogoFotoAmpliada(foto: String, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        text = {
            Box(modifier = Modifier.fillMaxWidth().accionable("Cerrar la foto", alTocar = alCerrar), contentAlignment = Alignment.Center) {
                FotoDePunto(
                    foto = foto,
                    descripcion = "Foto ampliada del punto",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 480.dp),
                    contentScale = ContentScale.Fit
                )
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
    )
}
