package com.unaj.senderos.presentation.formulario

import com.unaj.senderos.presentation.componentes.BarraSuperior
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import com.unaj.senderos.presentation.componentes.BotonPrincipal
import com.unaj.senderos.presentation.componentes.BotonSecundario
import com.unaj.senderos.presentation.componentes.IconoDeBoton
import com.unaj.senderos.presentation.comun.formatearKilometros
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import com.unaj.senderos.presentation.tema.obtenerColorLineaBioma
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.presentation.componentes.mapa.MapaRecorrido
import com.unaj.senderos.presentation.tema.TemaSenderos

private val ANCHO_PANEL_PUNTO_HORIZONTAL = 340.dp

@Composable
fun EditorRecorridoMapa(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { alProcesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa) }

    var esAyudaVisible by rememberSaveable { mutableStateOf(true) }

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
            if (esAyudaVisible) AyudaDelEditor(alOcultar = { esAyudaVisible = false })
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
                        alProcesarEvento = alProcesarEvento,
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
}

@Composable
private fun AyudaDelEditor(alOcultar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 24.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tocá el mapa para agregar puntos. Arrastrá un punto para moverlo o tocalo para editarlo.",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelPuntoSeleccionado(
    indice: Int,
    punto: PuntoRecorrido,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    var nombre by remember(indice, punto.nombre) { mutableStateOf(punto.nombre) }
    var tipo by remember(indice, punto.tipo) { mutableStateOf(punto.tipo) }
    Card(
        modifier = modifier.fillMaxWidth().heightIn(max = 320.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
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
                value = nombre,
                onValueChange = { texto -> nombre = texto },
                label = { Text("Nombre (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoPunto.entries.forEach { tipoDisponible ->
                    FilterChip(
                        selected = tipo == tipoDisponible,
                        onClick = { tipo = tipoDisponible },
                        label = { Text(obtenerTextoTipoPunto(tipoDisponible)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
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
                    texto = "Guardar",
                    alTocar = {
                        if (nombre != punto.nombre) {
                            alProcesarEvento(FormularioSenderoEvento.AlCambiarNombrePunto(indice, nombre))
                        }
                        if (tipo != punto.tipo) {
                            alProcesarEvento(FormularioSenderoEvento.AlCambiarTipoPunto(indice, tipo))
                        }
                        alProcesarEvento(FormularioSenderoEvento.AlSeleccionarPunto(null))
                    },
                    icono = { IconoDeBoton(Icons.Filled.Check) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

internal fun obtenerTextoTipoPunto(tipo: TipoPunto): String {
    return when (tipo) {
        TipoPunto.PASO -> "Paso"
        TipoPunto.MIRADOR -> "Mirador"
        TipoPunto.AGUA -> "Agua"
        TipoPunto.REFUGIO -> "Refugio"
        TipoPunto.CAMPING -> "Camping"
        TipoPunto.PELIGRO -> "Peligro"
    }
}

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPanelPuntoSeleccionado() {
    TemaSenderos {
        PanelPuntoSeleccionado(
            indice = 1,
            punto = PuntoRecorrido(-40.5, -71.3, TipoPunto.AGUA, "Arroyo"),
            alProcesarEvento = {}
        )
    }
}
