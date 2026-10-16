package com.unaj.senderos.features.recorrido

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
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
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.ui.revisionpuntos.FotoDePunto
import com.unaj.senderos.core.ui.sendero.obtenerIconoTipoPunto
import com.unaj.senderos.core.ui.sendero.obtenerTextoTipoPunto

private const val LONGITUD_MAXIMA_NOTA = 200

private val TIPOS_DE_PUNTO_CAPTURABLES: List<TipoPunto> = TipoPunto.entries.filter { tipo -> tipo != TipoPunto.PASO }

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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
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
            BotonPrincipal(
                texto = "Guardar",
                alTocar = { alProcesarEvento(RecorridoEvento.AlGuardarPunto) },
                estaCargando = estadoVisual.estaGuardandoPunto,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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
            BotonSecundario(
                texto = "Sacar foto",
                alTocar = alSacarFoto,
                icono = { IconoDeBoton(Icons.Outlined.PhotoCamera) },
                estaHabilitado = !estadoVisual.estaGuardandoPunto,
                modifier = Modifier.weight(1f)
            )
            BotonSecundario(
                texto = "Elegir de la galería",
                alTocar = alElegirDeGaleria,
                icono = { IconoDeBoton(Icons.Outlined.AddPhotoAlternate) },
                estaHabilitado = !estadoVisual.estaGuardandoPunto,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
