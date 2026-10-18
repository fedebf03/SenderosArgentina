package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.ui.clima.formatearTemperaturaClima
import com.unaj.senderos.core.ui.clima.elegirIconoClima
import androidx.compose.material.icons.filled.Warning
import com.unaj.senderos.core.ui.comun.formatearTiempoTranscurrido
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import com.unaj.senderos.core.ui.componentes.accionable
import com.unaj.senderos.core.ui.componentes.descripcionSiSinDato
import com.unaj.senderos.core.ui.comun.TEXTO_NO_DISPONIBLE
import com.unaj.senderos.core.ui.comun.TEXTO_SIN_DATO
import com.unaj.senderos.core.ui.comun.formatearDistanciaCorta
import com.unaj.senderos.core.ui.comun.formatearPasos
import com.unaj.senderos.core.ui.comun.formatearRitmo
import com.unaj.senderos.core.ui.comun.formatearVelocidad
import com.unaj.senderos.core.ui.comun.formatearVelocidadDesdeMetrosPorSegundo
import com.unaj.senderos.core.ui.revisionpuntos.describirCantidadPuntosCapturados
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.ui.componentes.GraficoPerfilElevacion
import com.unaj.senderos.core.ui.componentes.TEXTO_SUBIDA_RESTANTE
import com.unaj.senderos.core.ui.comun.formatearAlturaMetros


// Los datos y avisos se desplazan y las acciones quedan fijas abajo; el panel tiene altura máxima y, si los datos no entran, se resuelve con el desplazamiento
@Composable
internal fun PanelRecorrido(
    estadoVisual: RecorridoEstadoVisual,
    alProcesarEvento: (RecorridoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FranjaPerfilRecorrido(estadoVisual)
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CartelDeSenal(estadoVisual.calidadSenal)
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

// Mostrar las acciones fijas del panel: finalizar y cancelar el recorrido
@Composable
private fun AccionesDelPanel(estadoVisual: RecorridoEstadoVisual, alProcesarEvento: (RecorridoEvento) -> Unit) {
    // Marcar puntos se hace con el botón flotante del mapa; al llegar al final, Finalizar suma una bandera para marcar que es el paso siguiente
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonPrincipal(
            texto = "Finalizar",
            alTocar = { alProcesarEvento(RecorridoEvento.AlTocarFinalizar) },
            icono = if (estadoVisual.llegoAlFinal) {
                { IconoDeBoton(Icons.Filled.Flag) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth()
        )
        BotonSecundario(
            texto = "Cancelar recorrido",
            alTocar = { alProcesarEvento(RecorridoEvento.AlTocarCancelarRecorrido) },
            icono = { IconoDeBoton(Icons.Filled.Close) },
            modifier = Modifier.fillMaxWidth(),
            esDestructivo = true
        )
    }
}

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

// Mostrar recorrido, restante, tiempo y velocidad en una tarjeta translúcida sobre el mapa, para que se vean aunque el panel de abajo no entre
@Composable
internal fun DatosFlotantesRecorrido(
    estadoVisual: RecorridoEstadoVisual,
    leerAhoraMilisegundos: () -> Long,
    modifier: Modifier = Modifier,
    alTocarClima: () -> Unit = {}
) {
    val confiable = estadoVisual.sonConfiablesLosDatosDeDistancia
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 4.dp
    ) {
        Column {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CeldaDelPanel(
                DatoDeGrilla(
                    "Recorrido",
                    if (confiable) formatearDistanciaCorta(estadoVisual.distanciaRecorridaKilometros) else TEXTO_SIN_DATO
                ),
                Modifier.weight(1f)
            )
            if (!estadoVisual.esCaminataLibre) {
                CeldaDelPanel(
                    DatoDeGrilla(
                        "Restante",
                        if (confiable) formatearDistanciaCorta(estadoVisual.distanciaRestanteKilometros) else TEXTO_SIN_DATO
                    ),
                    Modifier.weight(1f)
                )
            }
            CeldaTiempo(estadoVisual.instanteInicioMilisegundos, leerAhoraMilisegundos, Modifier.weight(1f))
            CeldaDelPanel(
                DatoDeGrilla(
                    "Velocidad",
                    if (confiable) formatearVelocidadDesdeMetrosPorSegundo(estadoVisual.velocidadActualMetrosPorSegundo) else TEXTO_SIN_DATO
                ),
                Modifier.weight(1f)
            )
        }
        ClimaDelRecorrido(estadoVisual, alTocarClima)
        }
    }
}

// Mostrar debajo de los datos el icono del clima con la temperatura de donde está el usuario y, si hay, el primer aviso; tocarlo abre el detalle.
// Sin clima (sin conexión y sin copia guardada) no se dibuja nada
@Composable
private fun ClimaDelRecorrido(estadoVisual: RecorridoEstadoVisual, alTocar: () -> Unit) {
    val resultado = estadoVisual.clima
    val presion = estadoVisual.presionHectopascales
    if (resultado == null && presion == null) return
    val actual = resultado?.pronostico?.actual
    val alertas = estadoVisual.alertasClima
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .accionable("Ver el clima", alTocar = alTocar)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (resultado != null && actual != null) {
            Icon(
                imageVector = elegirIconoClima(actual.condicion, actual.esDeDia),
                contentDescription = actual.condicion.descripcion,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = formatearTemperaturaClima(actual.temperaturaCelsius), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (resultado.estaDesactualizado) "${actual.condicion.descripcion} · sin conexión" else actual.condicion.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        if (alertas.isNotEmpty()) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = alertas.first().titulo + if (alertas.size > 1) " +${alertas.size - 1}" else "",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (presion != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$presion hPa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Presión atmosférica: $presion hectopascales" }
            )
        }
    }
}

// Es la única celda que lee el reloj, así que solo ella se recompone cada segundo
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

private class DatoDeGrilla(val titulo: String, val valor: String, val esNoDisponible: Boolean = false, val rumboGrados: Float? = null)

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

@Composable
private fun FilaDeDatos(izquierdo: DatoDeGrilla, derecho: DatoDeGrilla) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CeldaDelPanel(izquierdo, Modifier.weight(1f))
        CeldaDelPanel(derecho, Modifier.weight(1f))
    }
}

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
