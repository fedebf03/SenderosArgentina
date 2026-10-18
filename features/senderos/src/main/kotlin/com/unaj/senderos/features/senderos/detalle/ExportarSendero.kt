package com.unaj.senderos.features.senderos.detalle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.usecase.FormatoExportacion
import com.unaj.senderos.core.ui.componentes.accionable

// Preparar un selector "Guardar como" por formato (el tipo de archivo se fija al registrarlo) y devolver la función que abre el
// del formato pedido con el nombre sugerido; el archivo elegido vuelve al ViewModel como AlExportarSendero
@Composable
internal fun recordarSelectoresExportacion(alProcesarEvento: (DetalleSenderoEvento) -> Unit): (FormatoExportacion, String) -> Unit {
    val procesarActual by rememberUpdatedState(alProcesarEvento)
    val selectores = FormatoExportacion.entries.associateWith { formato ->
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(formato.tipoMime)) { uri ->
            if (uri != null) procesarActual(DetalleSenderoEvento.AlExportarSendero(formato, uri.toString()))
        }
    }
    return { formato, nombreArchivo -> selectores.getValue(formato).launch(nombreArchivo) }
}

// Mostrar los formatos de exportación con una explicación breve de para qué sirve cada uno
@Composable
internal fun DialogoFormatoExportacion(alProcesarEvento: (DetalleSenderoEvento) -> Unit) {
    val opciones = listOf(
        FormatoExportacion.GPX to ("GPX" to "Para Wikiloc, Strava, Garmin y la mayoría de las apps y relojes"),
        FormatoExportacion.CSV to ("CSV" to "Latitud, longitud y altura; se abre con Excel"),
        FormatoExportacion.JSON to ("JSON" to "Copia completa del sendero para volver a importarla en Senderos")
    )
    AlertDialog(
        onDismissRequest = { alProcesarEvento(DetalleSenderoEvento.AlCerrarExportar) },
        title = { Text("Exportar sendero") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                opciones.forEach { (formato, textos) ->
                    val (titulo, descripcion) = textos
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .accionable("Exportar en $titulo") { alProcesarEvento(DetalleSenderoEvento.AlElegirFormatoExportacion(formato)) }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(text = titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(DetalleSenderoEvento.AlCerrarExportar) }) {
                Text("Cancelar")
            }
        }
    )
}
