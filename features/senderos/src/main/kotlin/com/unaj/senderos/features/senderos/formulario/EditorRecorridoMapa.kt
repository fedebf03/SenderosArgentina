package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.ui.sendero.obtenerTextoTipoPunto
import com.unaj.senderos.core.ui.componentes.BarraSuperior
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import com.unaj.senderos.core.ui.comun.formatearKilometros
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import com.unaj.senderos.core.ui.tema.obtenerColorLineaBioma
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoCamera
import com.unaj.senderos.core.ui.revisionpuntos.FotoDePunto
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.unaj.senderos.core.ui.comun.recordarAccionesDeFoto
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.ui.componentes.mapa.MapaRecorrido
import com.unaj.senderos.core.ui.tema.TemaSenderos

private val ANCHO_PANEL_PUNTO_HORIZONTAL = 340.dp
private val ALTURA_MAXIMA_PANEL_PUNTO = 440.dp
private val ALTURA_FOTO_PANEL_PUNTO = 120.dp

// RF4 - Editor de recorrido: se abre a pantalla completa sobre el formulario, con el mapa editable y el punto tocado
@Composable
fun EditorRecorridoMapa(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { alProcesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa) }

    var esAyudaVisible by rememberSaveable { mutableStateOf(true) }
    val accionesFotoPuntoExistente = recordarAccionesDeFoto(
        alSeleccionarFoto = { uri ->
            estadoVisual.indicePuntoSeleccionado?.let { indice ->
                alProcesarEvento(FormularioSenderoEvento.AlSeleccionarFotoDePunto(indice, uri))
            }
        },
        alMostrarAviso = { mensaje -> alProcesarEvento(FormularioSenderoEvento.AlFallarFotoDePunto(mensaje)) }
    )
    val accionesFotoPunto = recordarAccionesDeFoto(
        alSeleccionarFoto = { uri -> alProcesarEvento(FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo(uri)) },
        alMostrarAviso = { mensaje -> alProcesarEvento(FormularioSenderoEvento.AlFallarFotoPuntoNuevo(mensaje)) }
    )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraSuperior(
                titulo = "Recorrido en el mapa",
                alVolver = { alProcesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa) },
                descripcionVolver = "Volver al formulario",
                acciones = {
                    BotonSecundario(
                        texto = "Listo",
                        alTocar = { alProcesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
            SelectorModoEditor(estadoVisual.modoEditor, alProcesarEvento)
            if (esAyudaVisible) AyudaDelEditor(modo = estadoVisual.modoEditor, alOcultar = { esAyudaVisible = false })
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val esHorizontal = maxWidth > maxHeight
                MapaRecorrido(
                    puntos = estadoVisual.recorrido,
                    tipoRecorrido = estadoVisual.tipoRecorrido,
                    modifier = Modifier.fillMaxSize(),
                    esEditable = true,
                    colorLinea = obtenerColorLineaBioma(estadoVisual.bioma, MaterialTheme.colorScheme.primary),
                    mostrarControlesDeZoom = true,
                    indiceSeleccionado = estadoVisual.indicePuntoSeleccionado,
                    alAgregarPunto = { latitud, longitud ->
                        alProcesarEvento(FormularioSenderoEvento.AlAgregarPunto(latitud, longitud))
                    },
                    alMoverPunto = { indice, latitud, longitud ->
                        alProcesarEvento(FormularioSenderoEvento.AlMoverPunto(indice, latitud, longitud))
                    },
                    alSeleccionarPunto = { indice -> alProcesarEvento(FormularioSenderoEvento.AlSeleccionarPunto(indice)) }
                )
                val indiceSeleccionado = estadoVisual.indicePuntoSeleccionado
                val puntoSeleccionado = indiceSeleccionado?.let { indice -> estadoVisual.recorrido.getOrNull(indice) }
                if (indiceSeleccionado != null && puntoSeleccionado != null) {
                    PanelPuntoSeleccionado(
                        indice = indiceSeleccionado,
                        punto = puntoSeleccionado,
                        estaCopiandoFoto = estadoVisual.indicePuntoCopiandoFoto == indiceSeleccionado,
                        alProcesarEvento = alProcesarEvento,
                        alSacarFoto = accionesFotoPuntoExistente.sacarFoto,
                        alElegirDeGaleria = accionesFotoPuntoExistente.elegirDeGaleria,
                        modifier = if (esHorizontal) {
                            Modifier.align(Alignment.CenterEnd).width(ANCHO_PANEL_PUNTO_HORIZONTAL).padding(12.dp)
                        } else {
                            Modifier.align(Alignment.BottomCenter).padding(12.dp)
                        }
                    )
                }
            }
            BarraInferiorEditor(estadoVisual, alProcesarEvento)
        }
    }
    estadoVisual.puntoDeInteresNuevo?.let { punto ->
        HojaPuntoDeInteres(
            punto = punto,
            alProcesarEvento = alProcesarEvento,
            alSacarFoto = accionesFotoPunto.sacarFoto,
            alElegirDeGaleria = accionesFotoPunto.elegirDeGaleria
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorModoEditor(modo: ModoEditorRecorrido, alProcesarEvento: (FormularioSenderoEvento) -> Unit) {
    val opciones = listOf(
        ModoEditorRecorrido.TRAZAR_CAMINO to "Trazar camino",
        ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES to "Punto de interés"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        opciones.forEachIndexed { posicion, (modoOpcion, texto) ->
            SegmentedButton(
                selected = modo == modoOpcion,
                onClick = { alProcesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(modoOpcion)) },
                shape = SegmentedButtonDefaults.itemShape(index = posicion, count = opciones.size)
            ) {
                Text(texto)
            }
        }
    }
}

@Composable
private fun AyudaDelEditor(modo: ModoEditorRecorrido, alOcultar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 24.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (modo == ModoEditorRecorrido.TRAZAR_CAMINO) {
                    "Tocá el mapa para agregar puntos. Arrastrá un punto para moverlo o tocalo para editarlo."
                } else {
                    "Tocá el lugar del mapa donde está lo que querés marcar: agua, un mirador, un refugio…"
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = alOcultar) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Ocultar la ayuda")
            }
        }
    }
}

@Composable
private fun BarraInferiorEditor(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Distancia del mapa: ${formatearKilometros(estadoVisual.distanciaCalculadaKilometros)} km " +
                "(${estadoVisual.recorrido.size} puntos)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        BotonSecundario(
            texto = "Invertir sentido",
            alTocar = { alProcesarEvento(FormularioSenderoEvento.AlInvertirSentido) },
            estaHabilitado = estadoVisual.recorrido.size >= 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// El nombre, el tipo y la foto se aplican al instante. Así no se pierden si el panel se cierra con la X o se sale del editor
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelPuntoSeleccionado(
    indice: Int,
    punto: PuntoRecorrido,
    estaCopiandoFoto: Boolean,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().heightIn(max = ALTURA_MAXIMA_PANEL_PUNTO),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Punto ${indice + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlSeleccionarPunto(null)) }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar el panel del punto")
                    }
                }
                OutlinedTextField(
                    value = punto.nombre,
                    onValueChange = { texto ->
                        alProcesarEvento(FormularioSenderoEvento.AlCambiarNombrePunto(indice, texto.take(LONGITUD_MAXIMA_NOMBRE_PUNTO)))
                    },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoPunto.entries.forEach { tipoDisponible ->
                        FilterChip(
                            selected = punto.tipo == tipoDisponible,
                            onClick = { alProcesarEvento(FormularioSenderoEvento.AlCambiarTipoPunto(indice, tipoDisponible)) },
                            label = { Text(obtenerTextoTipoPunto(tipoDisponible)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                FotoDelPuntoSeleccionado(
                    indice = indice,
                    foto = punto.foto,
                    estaCopiandoFoto = estaCopiandoFoto,
                    alProcesarEvento = alProcesarEvento,
                    alSacarFoto = alSacarFoto,
                    alElegirDeGaleria = alElegirDeGaleria
                )
                if (indice > 0) {
                    TextButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlInsertarPuntoAntes(indice)) }) {
                        Text("Insertar punto antes")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BotonSecundario(
                    texto = "Eliminar",
                    alTocar = { alProcesarEvento(FormularioSenderoEvento.AlEliminarPunto(indice)) },
                    icono = { IconoDeBoton(Icons.Outlined.Delete) },
                    esDestructivo = true,
                    modifier = Modifier.weight(1f)
                )
                BotonPrincipal(
                    texto = "Listo",
                    alTocar = { alProcesarEvento(FormularioSenderoEvento.AlSeleccionarPunto(null)) },
                    icono = { IconoDeBoton(Icons.Filled.Check) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FotoDelPuntoSeleccionado(
    indice: Int,
    foto: String,
    estaCopiandoFoto: Boolean,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit
) {
    when {
        estaCopiandoFoto -> Box(modifier = Modifier.fillMaxWidth().height(ALTURA_FOTO_PANEL_PUNTO), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        foto.isNotEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().height(ALTURA_FOTO_PANEL_PUNTO).clip(RoundedCornerShape(12.dp))
        ) {
            FotoDePunto(
                foto = foto,
                descripcion = "Foto del punto ${indice + 1}",
                modifier = Modifier.fillMaxWidth().height(ALTURA_FOTO_PANEL_PUNTO),
                contentScale = ContentScale.Crop
            )
            FilledTonalButton(
                onClick = { alProcesarEvento(FormularioSenderoEvento.AlQuitarFotoDePunto(indice)) },
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
            estaHabilitado = !estaCopiandoFoto,
            modifier = Modifier.weight(1f)
        )
        BotonSecundario(
            texto = "Galería",
            alTocar = alElegirDeGaleria,
            icono = { IconoDeBoton(Icons.Outlined.AddPhotoAlternate) },
            estaHabilitado = !estaCopiandoFoto,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPanelPuntoSeleccionado() {
    TemaSenderos {
        PanelPuntoSeleccionado(
            indice = 1,
            punto = PuntoRecorrido(-40.5, -71.3, TipoPunto.AGUA, "Arroyo"),
            estaCopiandoFoto = false,
            alProcesarEvento = {},
            alSacarFoto = {},
            alElegirDeGaleria = {}
        )
    }
}
