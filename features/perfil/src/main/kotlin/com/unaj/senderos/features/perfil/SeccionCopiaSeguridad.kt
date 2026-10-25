package com.unaj.senderos.features.perfil

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.usecase.AnalisisCopiaSeguridad
import com.unaj.senderos.core.domain.usecase.ModoRestauracion
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TIPO_ARCHIVO_COPIA = "application/json"

@Composable
internal fun SeccionCopiaSeguridad(estadoVisual: PerfilEstadoVisual, alProcesarEvento: (PerfilEvento) -> Unit) {
    val selectorGuardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIPO_ARCHIVO_COPIA)) { uri ->
        if (uri != null) alProcesarEvento(PerfilEvento.AlExportarCopiaSeguridad(uri.toString()))
    }
    val selectorAbrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alProcesarEvento(PerfilEvento.AlImportarCopiaSeguridad(uri.toString()))
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Mis datos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = "Guardá una copia de todos tus senderos en un archivo, por si perdés la cuenta o cambiás de teléfono",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BotonSecundario(
            texto = "Exportar mis senderos",
            alTocar = { selectorGuardar.launch(nombreArchivoCopia()) },
            icono = { IconoDeBoton(Icons.Outlined.FileDownload) },
            estaHabilitado = !estadoVisual.estaProcesandoCopia,
            modifier = Modifier.fillMaxWidth()
        )
        BotonSecundario(
            texto = "Importar copia de seguridad",
            alTocar = { selectorAbrir.launch(arrayOf(TIPO_ARCHIVO_COPIA, "*/*")) },
            icono = { IconoDeBoton(Icons.Outlined.FileUpload) },
            estaHabilitado = !estadoVisual.estaProcesandoCopia,
            modifier = Modifier.fillMaxWidth()
        )
    }
    estadoVisual.copiaPendiente?.let { analisis -> DialogoRestauracion(analisis, alProcesarEvento) }
}

@Composable
private fun DialogoRestauracion(analisis: AnalisisCopiaSeguridad, alProcesarEvento: (PerfilEvento) -> Unit) {
    val nuevos = analisis.nuevos.size
    val repetidos = analisis.existentes.size
    if (repetidos == 0) {
        DialogoRestaurarSoloNuevos(nuevos, alProcesarEvento)
        return
    }
    val avisoCambios = if (analisis.existentesConCambiosSinSubir > 0) {
        " Ojo: ${analisis.existentesConCambiosSinSubir} de los que ya tenés tienen cambios sin subir. Si los reemplazás, se pierden."
    } else {
        ""
    }
    AlertDialog(
        onDismissRequest = { alProcesarEvento(PerfilEvento.AlCancelarRestauracion) },
        title = { Text("Restaurar copia") },
        text = {
            Text(
                "La copia tiene ${nuevos + repetidos} senderos: $nuevos nuevos y $repetidos que ya tenés.$avisoCambios " +
                    "¿Qué hacemos con los que ya tenés?"
            )
        },
        confirmButton = {
            Column {
                TextButton(onClick = { alProcesarEvento(PerfilEvento.AlElegirModoRestauracion(ModoRestauracion.REEMPLAZAR_EXISTENTES)) }) {
                    Text("Reemplazarlos con los de la copia")
                }
                TextButton(onClick = { alProcesarEvento(PerfilEvento.AlElegirModoRestauracion(ModoRestauracion.AGREGAR_COMO_COPIAS)) }) {
                    Text("Agregarlos como copias")
                }
                TextButton(onClick = { alProcesarEvento(PerfilEvento.AlElegirModoRestauracion(ModoRestauracion.SOLO_NUEVOS)) }) {
                    Text("Dejarlos como están")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(PerfilEvento.AlCancelarRestauracion) }) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DialogoRestaurarSoloNuevos(cantidad: Int, alProcesarEvento: (PerfilEvento) -> Unit) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(PerfilEvento.AlCancelarRestauracion) },
        title = { Text("Restaurar copia") },
        text = { Text("Se van a agregar $cantidad senderos de la copia, incluidos los que hayas borrado desde entonces.") },
        confirmButton = {
            TextButton(onClick = { alProcesarEvento(PerfilEvento.AlElegirModoRestauracion(ModoRestauracion.SOLO_NUEVOS)) }) {
                Text("Restaurar")
            }
        },
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(PerfilEvento.AlCancelarRestauracion) }) {
                Text("Cancelar")
            }
        }
    )
}

// El nombre lleva la fecha para distinguir las copias de distintos días
private fun nombreArchivoCopia(): String =
    "senderos-${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.json"
