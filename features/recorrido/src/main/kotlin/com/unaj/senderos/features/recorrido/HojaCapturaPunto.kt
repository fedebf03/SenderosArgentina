package com.unaj.senderos.features.recorrido

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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

// En el orden de la grilla, de a tres por fila: lo más urgente (agua y peligro) primero y "Otro" al final
private val TIPOS_DE_PUNTO_CAPTURABLES: List<TipoPunto> = listOf(
    TipoPunto.AGUA, TipoPunto.PELIGRO, TipoPunto.MIRADOR,
    TipoPunto.REFUGIO, TipoPunto.CAMPING, TipoPunto.OTRO
)
private const val COLUMNAS_GRILLA_TIPOS = 3

// Mostrar la hoja para describir el punto marcado: grilla de tipos, nota opcional, foto, Cancelar y Guardar.
// Tocar afuera o deslizar hacia abajo la cierra solo si no se escribió una nota ni se eligió una foto, para no perderlas por un toque sin querer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaCapturaPunto(
    estadoVisual: RecorridoEstadoVisual,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit
) {
    val hayDatosCargados = estadoVisual.notaPunto.isNotBlank() || estadoVisual.fotoPunto != null
    val hayDatosCargadosActual by rememberUpdatedState(hayDatosCargados)
    val estadoHoja = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { valorNuevo -> valorNuevo != SheetValue.Hidden || !hayDatosCargadosActual }
    )
    ModalBottomSheet(
        onDismissRequest = { if (!hayDatosCargadosActual) alProcesarEvento(RecorridoEvento.AlCerrarHojaPunto) },
        sheetState = estadoHoja
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
                text = "¿Qué viste?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            GrillaTiposDePunto(
                tipoElegido = estadoVisual.tipoPuntoElegido,
                alElegir = { tipo -> alProcesarEvento(RecorridoEvento.AlElegirTipoPunto(tipo)) }
            )
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BotonSecundario(
                    texto = "Cancelar",
                    alTocar = { alProcesarEvento(RecorridoEvento.AlCerrarHojaPunto) },
                    estaHabilitado = !estadoVisual.estaGuardandoPunto,
                    esDestructivo = true,
                    modifier = Modifier.weight(1f)
                )
                BotonPrincipal(
                    texto = "Guardar",
                    alTocar = { alProcesarEvento(RecorridoEvento.AlGuardarPunto) },
                    estaCargando = estadoVisual.estaGuardandoPunto,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Mostrar los tipos como baldosas grandes, fáciles de tocar caminando o con guantes; la elegida queda resaltada
@Composable
private fun GrillaTiposDePunto(tipoElegido: TipoPunto, alElegir: (TipoPunto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
        TIPOS_DE_PUNTO_CAPTURABLES.chunked(COLUMNAS_GRILLA_TIPOS).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                fila.forEach { tipo ->
                    BaldosaTipoDePunto(
                        tipo = tipo,
                        estaElegido = tipo == tipoElegido,
                        alTocar = { alElegir(tipo) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Dibujar una baldosa con el ícono y el nombre del tipo; se anuncia como opción seleccionable para el lector de pantalla
@Composable
private fun BaldosaTipoDePunto(tipo: TipoPunto, estaElegido: Boolean, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        selected = estaElegido,
        onClick = alTocar,
        shape = RoundedCornerShape(16.dp),
        color = if (estaElegido) esquema.primaryContainer else esquema.surfaceContainerHigh,
        contentColor = if (estaElegido) esquema.onPrimaryContainer else esquema.onSurface,
        border = if (estaElegido) BorderStroke(2.dp, esquema.primary) else null,
        modifier = modifier.heightIn(min = 88.dp).semantics { role = Role.RadioButton }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(imageVector = obtenerIconoTipoPunto(tipo), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(
                text = obtenerTextoTipoPunto(tipo),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (estaElegido) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
