package com.unaj.senderos.presentation.recorrido

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.presentation.componentes.BotonPrincipal
import com.unaj.senderos.presentation.componentes.BotonSecundario
import com.unaj.senderos.presentation.componentes.IconoDeBoton
import com.unaj.senderos.presentation.componentes.accionable
import com.unaj.senderos.presentation.componentes.descripcionSiSinDato
import com.unaj.senderos.presentation.comun.TEXTO_NO_DISPONIBLE
import com.unaj.senderos.presentation.comun.TEXTO_SIN_DATO
import com.unaj.senderos.presentation.comun.formatearDistanciaCorta
import com.unaj.senderos.presentation.comun.formatearPasos
import com.unaj.senderos.presentation.comun.formatearRitmo
import com.unaj.senderos.presentation.comun.formatearVelocidad
import com.unaj.senderos.presentation.comun.formatearVelocidadDesdeMetrosPorSegundo
import com.unaj.senderos.presentation.revisionpuntos.describirCantidadPuntosCapturados

// Ancho disponible mínimo para mostrar "Agregar punto" y "Finalizar" lado a lado, y escala de fuente máxima para ello
private val ANCHO_MINIMO_BOTONES_LADO_A_LADO = 340.dp
private const val ESCALA_FUENTE_MAXIMA_LADO_A_LADO = 1.3f

// Mostrar el panel del recorrido: todo el bloque de datos y avisos se desplaza, y las acciones quedan fijas abajo;
// el panel tiene una altura máxima y, si los datos no entran, se resuelve con el desplazamiento
@Composable
internal fun PanelRecorrido(
    estadoVisual: RecorridoEstadoVisual,
    leerAhoraMilisegundos: () -> Long,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CartelDeSenal(estadoVisual.calidadSenal)
                GrillaCompacta(estadoVisual, leerAhoraMilisegundos)
                if (estadoVisual.esSimulado) {
                    ControlesSimulacion(estadoVisual.multiplicadorSimulacion, alProcesarEvento)
                }
                AnimatedVisibility(visible = estadoVisual.estaDetenido) { IndicadorDetenido() }
                estadoVisual.proximoPunto?.let { punto ->
                    DatoDelPanel("Próximo punto", obtenerEtiquetaPunto(punto), Modifier.fillMaxWidth())
                }
                AnimatedVisibility(visible = estadoVisual.estaDesviado) { AvisoDeDesvio() }
                if (estadoVisual.cantidadPuntosCapturados > 0) {
                    Text(
                        text = describirCantidadPuntosCapturados(estadoVisual.cantidadPuntosCapturados),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SeccionMasDatos(estadoVisual)
            }
            AccionesDelPanel(estadoVisual, alProcesarEvento)
        }
    }
}

// Mostrar las acciones fijas del panel: "Agregar punto" y "Finalizar" lado a lado si entran con su ícono, apiladas
// si el ancho es escaso o la fuente es grande, y "Cancelar recorrido" debajo
@Composable
private fun AccionesDelPanel(estadoVisual: RecorridoEstadoVisual, alProcesarEvento: (RecorridoEvento) -> Unit) {
    val escalaFuente = LocalDensity.current.fontScale
    val botonAgregarPunto: @Composable (Modifier) -> Unit = { modificadorBoton ->
        if (estadoVisual.puedeAgregarPunto) {
            BotonSecundario(
                texto = "Agregar punto",
                alTocar = { alProcesarEvento(RecorridoEvento.AlTocarAgregarPunto) },
                icono = { IconoDeBoton(Icons.Outlined.AddLocationAlt) },
                modifier = modificadorBoton
            )
        }
    }
    // Al llegar al final el botón es el mismo, pero suma una bandera para marcar que es el paso siguiente
    val botonFinalizar: @Composable (Modifier) -> Unit = { modificadorBoton ->
        BotonPrincipal(
            texto = "Finalizar",
            alTocar = { alProcesarEvento(RecorridoEvento.AlTocarFinalizar) },
            icono = if (estadoVisual.llegoAlFinal) {
                { IconoDeBoton(Icons.Filled.Flag) }
            } else {
                null
            },
            modifier = modificadorBoton
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val debenApilarse = estadoVisual.puedeAgregarPunto &&
                (maxWidth < ANCHO_MINIMO_BOTONES_LADO_A_LADO || escalaFuente > ESCALA_FUENTE_MAXIMA_LADO_A_LADO)
            if (debenApilarse) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    botonAgregarPunto(Modifier.fillMaxWidth())
                    botonFinalizar(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    botonAgregarPunto(Modifier.weight(1f))
                    botonFinalizar(Modifier.weight(1f))
                }
            }
        }
        TextButton(
            onClick = { alProcesarEvento(RecorridoEvento.AlTocarCancelarRecorrido) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cancelar recorrido")
        }
    }
}

// Mostrar el aviso de que el usuario se alejó del trazado del sendero
@Composable
private fun AvisoDeDesvio() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Te alejaste del sendero. Volvé al trazado para seguir la ruta.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// Mostrar los cuatro datos principales, siempre visibles: distancia recorrida y restante, tiempo y velocidad
@Composable
private fun GrillaCompacta(estadoVisual: RecorridoEstadoVisual, leerAhoraMilisegundos: () -> Long) {
    val confiable = estadoVisual.sonConfiablesLosDatosDeDistancia
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilaDeDatos(
            DatoDeGrilla(
                "Recorrido",
                if (confiable) formatearDistanciaCorta(estadoVisual.distanciaRecorridaKilometros) else TEXTO_SIN_DATO
            ),
            DatoDeGrilla(
                "Restante",
                if (confiable) formatearDistanciaCorta(estadoVisual.distanciaRestanteKilometros) else TEXTO_SIN_DATO
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaTiempo(estadoVisual.instanteInicioMilisegundos, leerAhoraMilisegundos, Modifier.weight(1f))
            CeldaDelPanel(
                DatoDeGrilla(
                    "Velocidad",
                    if (confiable) formatearVelocidadDesdeMetrosPorSegundo(estadoVisual.velocidadActualMetrosPorSegundo) else TEXTO_SIN_DATO
                ),
                Modifier.weight(1f)
            )
        }
    }
}

// Mostrar la celda del tiempo transcurrido; es la única que lee el reloj, así que solo ella se recompone cada segundo
@Composable
private fun CeldaTiempo(instanteInicioMilisegundos: Long, leerAhoraMilisegundos: () -> Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Tiempo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatearTiempoTranscurrido(leerAhoraMilisegundos() - instanteInicioMilisegundos),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Mostrar la sección expandible "Más datos" con la velocidad promedio y los datos de los sensores
@Composable
private fun SeccionMasDatos(estadoVisual: RecorridoEstadoVisual) {
    var estaExpandida by rememberSaveable { mutableStateOf(false) }
    val sinAcelerometro = estadoVisual.faltaAcelerometro
    val confiable = estadoVisual.sonConfiablesLosDatosDeDistancia
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .accionable(if (estaExpandida) "Contraer más datos" else "Expandir más datos") {
                    estaExpandida = !estaExpandida
                }
                .semantics { stateDescription = if (estaExpandida) "Expandido" else "Contraído" }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Más datos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (estaExpandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        AnimatedVisibility(visible = estaExpandida) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilaDeDatos(
                    DatoDeGrilla(
                        "Velocidad promedio",
                        if (confiable) formatearVelocidad(estadoVisual.velocidadPromedioKilometrosPorHora) else TEXTO_SIN_DATO
                    ),
                    DatoDeGrilla(
                        titulo = "Pasos",
                        valor = if (sinAcelerometro) TEXTO_NO_DISPONIBLE else formatearPasos(estadoVisual.pasos),
                        esNoDisponible = sinAcelerometro
                    )
                )
                FilaDeDatos(
                    DatoDeGrilla(
                        titulo = "Ritmo",
                        valor = if (sinAcelerometro) TEXTO_NO_DISPONIBLE else formatearRitmo(estadoVisual.ritmoPasosPorMinuto),
                        esNoDisponible = sinAcelerometro
                    ),
                    armarDatoRumbo(estadoVisual)
                )
            }
        }
    }
}

// Datos de una celda de la grilla: título, valor y si el dispositivo no puede medirlo
private class DatoDeGrilla(val titulo: String, val valor: String, val esNoDisponible: Boolean = false, val rumboGrados: Float? = null)

// Armar el dato del rumbo: sin rumbo conocido y sin brújula se indica que no está disponible en el dispositivo
private fun armarDatoRumbo(estadoVisual: RecorridoEstadoVisual): DatoDeGrilla {
    val rumbo = estadoVisual.rumboGrados
    val sinBrujulaNiRumbo = rumbo == null && !estadoVisual.tieneBrujula
    return DatoDeGrilla(
        titulo = "Rumbo",
        valor = if (sinBrujulaNiRumbo) TEXTO_NO_DISPONIBLE else formatearRumbo(rumbo),
        esNoDisponible = sinBrujulaNiRumbo,
        rumboGrados = rumbo
    )
}

// Mostrar dos datos lado a lado con el mismo ancho
@Composable
private fun FilaDeDatos(izquierdo: DatoDeGrilla, derecho: DatoDeGrilla) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CeldaDelPanel(izquierdo, Modifier.weight(1f))
        CeldaDelPanel(derecho, Modifier.weight(1f))
    }
}

// Mostrar un cartel mientras se busca la señal GPS o cuando se perdió; con buena señal se oculta con una animación
@Composable
private fun CartelDeSenal(calidadSenal: CalidadSenal) {
    AnimatedVisibility(visible = calidadSenal != CalidadSenal.BUENA) {
        val estaBuscando = calidadSenal == CalidadSenal.BUSCANDO
        Surface(
            color = if (estaBuscando) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (estaBuscando) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (estaBuscando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Filled.GpsOff, contentDescription = null)
                }
                Text(
                    text = if (estaBuscando) {
                        "Buscando señal GPS…"
                    } else {
                        "Sin señal GPS: salí al aire libre, lejos de techos y árboles"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Mostrar el aviso de que el usuario está quieto
@Composable
private fun IndicadorDetenido() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Detenido",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// Mostrar una celda de la grilla; el rumbo suma una flecha girada hacia donde se mira
@Composable
private fun CeldaDelPanel(dato: DatoDeGrilla, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = dato.titulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dato.rumboGrados != null) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).rotate(dato.rumboGrados)
                )
                Spacer(Modifier.width(6.dp))
            }
            if (dato.esNoDisponible) {
                Text(
                    text = dato.valor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = dato.valor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.descripcionSiSinDato(dato.valor)
                )
            }
        }
    }
}

// Mostrar el cartel de simulación con el selector de velocidad y el botón para simular un desvío
@Composable
private fun ControlesSimulacion(multiplicador: Int, alProcesarEvento: (RecorridoEvento) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Simulación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Velocidad", style = MaterialTheme.typography.bodyMedium)
                MULTIPLICADORES_SIMULACION.forEach { valor ->
                    FilterChip(
                        selected = valor == multiplicador,
                        onClick = { alProcesarEvento(RecorridoEvento.AlSeleccionarMultiplicador(valor)) },
                        label = { Text("×$valor") }
                    )
                }
            }
            BotonSecundario(
                texto = "Simular desvío",
                alTocar = { alProcesarEvento(RecorridoEvento.AlTocarSimularDesvio) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Mostrar un dato del panel con su título y su valor destacado
@Composable
internal fun DatoDelPanel(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
