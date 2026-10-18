package com.unaj.senderos.features.senderos.formulario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.GrillaTiposDePunto
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import com.unaj.senderos.core.ui.revisionpuntos.FotoDePunto

private const val LONGITUD_MAXIMA_NOMBRE_PUNTO = 60
private val ALTURA_FOTO_PUNTO = 160.dp

// Mostrar el menú para describir un punto de interés tocado en el mapa del editor: grilla de tipos, nombre y foto opcionales,
// Cancelar y Agregar. Tocar afuera o deslizar hacia abajo lo cierra solo si no se cargó un nombre ni una foto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HojaPuntoDeInteres(
    punto: PuntoDeInteresNuevo,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit
) {
    val hayDatosCargadosActual by rememberUpdatedState(punto.hayDatosCargados)
    val estadoHoja = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { valorNuevo -> valorNuevo != SheetValue.Hidden || !hayDatosCargadosActual }
    )
    ModalBottomSheet(
        onDismissRequest = { if (!hayDatosCargadosActual) alProcesarEvento(FormularioSenderoEvento.AlCancelarPuntoNuevo) },
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
                text = "Punto de interés",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            GrillaTiposDePunto(
                tipoElegido = punto.tipo,
                alElegir = { tipo -> alProcesarEvento(FormularioSenderoEvento.AlElegirTipoPuntoNuevo(tipo)) }
            )
            OutlinedTextField(
                value = punto.nombre,
                onValueChange = { texto ->
                    alProcesarEvento(FormularioSenderoEvento.AlCambiarNombrePuntoNuevo(texto.take(LONGITUD_MAXIMA_NOMBRE_PUNTO)))
                },
                label = { Text("Nombre (opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            SeccionFotoDelPuntoDeInteres(punto, alProcesarEvento, alSacarFoto, alElegirDeGaleria)
            punto.mensajeError?.let { mensaje ->
                Text(text = mensaje, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BotonSecundario(
                    texto = "Cancelar",
                    alTocar = { alProcesarEvento(FormularioSenderoEvento.AlCancelarPuntoNuevo) },
                    esDestructivo = true,
                    modifier = Modifier.weight(1f)
                )
                BotonPrincipal(
                    texto = "Agregar",
                    alTocar = { alProcesarEvento(FormularioSenderoEvento.AlGuardarPuntoNuevo) },
                    estaCargando = punto.estaProcesandoFoto,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Mostrar la foto elegida con el botón para quitarla, o el indicador mientras se copia, y los botones de cámara y galería
@Composable
private fun SeccionFotoDelPuntoDeInteres(
    punto: PuntoDeInteresNuevo,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            punto.estaProcesandoFoto -> Box(
                modifier = Modifier.fillMaxWidth().height(ALTURA_FOTO_PUNTO),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            punto.foto.isNotEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().height(ALTURA_FOTO_PUNTO).clip(RoundedCornerShape(12.dp))
            ) {
                FotoDePunto(
                    foto = punto.foto,
                    descripcion = "Foto elegida para el punto",
                    modifier = Modifier.fillMaxWidth().height(ALTURA_FOTO_PUNTO),
                    contentScale = ContentScale.Crop
                )
                FilledTonalButton(
                    onClick = { alProcesarEvento(FormularioSenderoEvento.AlQuitarFotoPuntoNuevo) },
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
                estaHabilitado = !punto.estaProcesandoFoto,
                modifier = Modifier.weight(1f)
            )
            BotonSecundario(
                texto = "Elegir de la galería",
                alTocar = alElegirDeGaleria,
                icono = { IconoDeBoton(Icons.Outlined.AddPhotoAlternate) },
                estaHabilitado = !punto.estaProcesandoFoto,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
