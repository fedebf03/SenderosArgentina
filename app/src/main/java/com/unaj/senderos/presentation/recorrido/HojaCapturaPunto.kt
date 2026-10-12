package com.unaj.senderos.presentation.recorrido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.presentation.comun.FotoDePunto
import com.unaj.senderos.presentation.detalle.obtenerIconoTipoPunto
import com.unaj.senderos.presentation.formulario.obtenerTextoTipoPunto

private const val LONGITUD_MAXIMA_NOTA = 200

// Tipos que se pueden marcar en el camino: todos menos el simple paso
private val TIPOS_DE_PUNTO_CAPTURABLES: List<TipoPunto> = TipoPunto.entries.filter { tipo -> tipo != TipoPunto.PASO }

// Mostrar la hoja inferior para marcar un punto de interés con tipo, nota y foto opcionales
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HojaCapturaPunto(
    estadoVisual: RecorridoEstadoVisual,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { alProcesarEvento(RecorridoEvento.AlCerrarHojaPunto) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Agregar punto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TIPOS_DE_PUNTO_CAPTURABLES.forEach { tipo ->
                    FilterChip(
                        selected = tipo == estadoVisual.tipoPuntoElegido,
                        onClick = { alProcesarEvento(RecorridoEvento.AlElegirTipoPunto(tipo)) },
                        label = { Text(obtenerTextoTipoPunto(tipo)) },
                        leadingIcon = {
                            Icon(imageVector = obtenerIconoTipoPunto(tipo), contentDescription = null)
                        }
                    )
                }
            }
            OutlinedTextField(
                value = estadoVisual.notaPunto,
                onValueChange = { texto ->
                    alProcesarEvento(RecorridoEvento.AlCambiarNotaPunto(texto.take(LONGITUD_MAXIMA_NOTA)))
                },
                label = { Text("Nota (opcional)") },
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            SeccionFotoDelPunto(estadoVisual, alProcesarEvento, alSacarFoto, alElegirDeGaleria)
            estadoVisual.mensajeErrorPunto?.let { mensaje ->
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = { alProcesarEvento(RecorridoEvento.AlGuardarPunto) },
                enabled = !estadoVisual.estaGuardandoPunto,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (estadoVisual.estaGuardandoPunto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}

// Mostrar la vista previa de la foto elegida y los botones para sacar otra o elegirla de la galería
@Composable
private fun SeccionFotoDelPunto(
    estadoVisual: RecorridoEstadoVisual,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit
) {
    val foto = estadoVisual.fotoPunto
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (foto != null) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))) {
                FotoDePunto(
                    foto = foto,
                    descripcion = "Foto elegida para el punto",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop
                )
                FilledTonalButton(
                    onClick = { alProcesarEvento(RecorridoEvento.AlQuitarFotoPunto) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                ) {
                    Text("Quitar")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = alSacarFoto,
                enabled = !estadoVisual.estaGuardandoPunto,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Sacar foto", modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = alElegirDeGaleria,
                enabled = !estadoVisual.estaGuardandoPunto,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Elegir de la galería", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
