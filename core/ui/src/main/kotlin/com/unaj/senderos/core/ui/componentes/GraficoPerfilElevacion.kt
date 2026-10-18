package com.unaj.senderos.core.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.configuracion.ConfiguracionElevacion
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.ui.comun.formatearAlturaMetros
import com.unaj.senderos.core.ui.comun.formatearKilometros
import com.unaj.senderos.core.ui.tema.obtenerColoresDelBioma
import kotlin.math.max

private val SEPARACION_ETIQUETA_Y = 8.dp
private val MARGEN_DERECHO = 14.dp
private val MARGEN_SUPERIOR = 28.dp
private val SEPARACION_ETIQUETA_X = 4.dp
private val MARGEN_INFERIOR_EXTRA = 6.dp
private val RADIO_MARCA_INTERES = 11.dp
private val TAMANO_ICONO_MARCA = 14.dp
private val DESPLAZAMIENTO_MARCA_SOBRE_CURVA = 18.dp
private val GROSOR_LINEA = 2.dp
private val RADIO_PUNTO = 5.dp
private val RADIO_PUNTO_ACTUAL = 6.dp
private val ALTURA_MINIMA_NORMAL = 170.dp
private val ALTURA_MAXIMA_NORMAL = 280.dp
private val ALTURA_MINIMA_COMPACTA = 120.dp
private val ALTURA_MAXIMA_COMPACTA = 160.dp
private const val PROPORCION_ALTURA_NORMAL = 0.38f
private const val PROPORCION_ALTURA_COMPACTA = 0.28f
private const val PASOS_DE_LA_GUIA_ACCESIBLE = 20
private const val TEXTO_ESTAS_ACA = "Estás acá"
private const val TEXTO_AYUDA_TOQUE = "Tocá o arrastrá el gráfico para ver la altura de cada punto"

// Marca de un punto de interés ubicada en su kilómetro del perfil
data class MarcaDelPerfil(
    val kilometros: Double,
    val icono: ImageVector
)

// Área rectangular del gráfico en píxeles y conversión de datos a posiciones
private class AreaDelGrafico(
    val izquierda: Float,
    val derecha: Float,
    val arriba: Float,
    val abajo: Float,
    val escala: EscalaPerfil
) {
    // Convertir un kilómetro en su posición horizontal
    fun posicionX(kilometros: Double): Float {
        if (escala.kilometrosTotales <= 0.0) return izquierda
        val fraccion = (kilometros / escala.kilometrosTotales).coerceIn(0.0, 1.0)
        return izquierda + (derecha - izquierda) * fraccion.toFloat()
    }

    // Convertir una altitud en su posición vertical
    fun posicionY(altitudMetros: Double): Float {
        val rango = escala.altitudMaximaEjeMetros - escala.altitudMinimaEjeMetros
        if (rango <= 0.0) return abajo
        val fraccion = ((altitudMetros - escala.altitudMinimaEjeMetros) / rango).coerceIn(0.0, 1.0)
        return abajo - (abajo - arriba) * fraccion.toFloat()
    }
}

// Colores y textos ya resueltos para dibujar sin leer el tema dentro del lienzo
private class PaletaDelGrafico(
    val linea: Color,
    val cuadricula: Color,
    val texto: Color,
    val fondoMarca: Color,
    val guia: Color,
    val posicionActual: Color
)

// Dibujar el perfil de elevación: área con degradé del color del bioma, ejes en km y m, puntos de interés,
// guía vertical al tocar o arrastrar y, si se indica, la marca "Estás acá". Con alturaFija el gráfico ocupa esa altura (franja del recorrido)
// y, si no se muestra la posición en el texto, debajo solo aparece la lectura del punto tocado
@Composable
fun GraficoPerfilElevacion(
    perfil: PerfilElevacion,
    bioma: Bioma,
    modifier: Modifier = Modifier,
    marcasDeInteres: List<MarcaDelPerfil> = emptyList(),
    kilometroActual: Double? = null,
    esCompacto: Boolean = false,
    alturaFija: Dp? = null,
    mostrarPosicionEnTexto: Boolean = true,
    alSeleccionarKilometro: ((Double) -> Unit)? = null
) {
    val coloresBioma = obtenerColoresDelBioma(bioma)
    val esquema = MaterialTheme.colorScheme
    val paleta = PaletaDelGrafico(
        linea = coloresBioma.principal,
        cuadricula = esquema.outlineVariant,
        texto = esquema.onSurfaceVariant,
        fondoMarca = esquema.surface,
        guia = esquema.onSurface,
        posicionActual = esquema.primary
    )
    val escala = remember(perfil) { calcularEscalaPerfil(perfil.puntos) }
    val puntos = perfil.puntos
    var indiceSeleccionado by rememberSaveable { mutableStateOf<Int?>(null) }
    val indiceVigente = indiceSeleccionado?.takeIf { indice -> indice in puntos.indices }
    val descripcionResumen = remember(perfil) { describirResumenPerfil(perfil) }
    val medidorTexto = rememberTextMeasurer()
    val estiloEtiqueta = MaterialTheme.typography.labelSmall.copy(color = paleta.texto)
    val etiquetasAltitud = remember(escala) { escala.marcasAltitudMetros.map { marca -> formatearAlturaMetros(marca) } }
    val etiquetasKilometros = remember(escala) {
        escala.marcasKilometros.mapIndexed { posicion, marca ->
            val texto = formatearKilometros(marca)
            if (posicion == escala.marcasKilometros.lastIndex) "$texto km" else texto
        }
    }
    val medidasAltitud = remember(etiquetasAltitud, estiloEtiqueta) {
        etiquetasAltitud.map { etiqueta -> medidorTexto.measure(etiqueta, estiloEtiqueta) }
    }
    val medidasKilometros = remember(etiquetasKilometros, estiloEtiqueta) {
        etiquetasKilometros.map { etiqueta -> medidorTexto.measure(etiqueta, estiloEtiqueta) }
    }
    val medidaEstasAca = remember(estiloEtiqueta) { medidorTexto.measure(TEXTO_ESTAS_ACA, estiloEtiqueta) }
    val pintoresMarcas = marcasDeInteres.map { marca -> rememberVectorPainter(marca.icono) }
    val densidad = LocalDensity.current
    val izquierdaPixeles = with(densidad) {
        (medidasAltitud.maxOfOrNull { medida -> medida.size.width } ?: 0) + SEPARACION_ETIQUETA_Y.toPx()
    }
    val margenDerechoPixeles = with(densidad) { MARGEN_DERECHO.toPx() }
    val margenSuperiorPixeles = with(densidad) { MARGEN_SUPERIOR.toPx() }
    val alturaEtiquetaX = medidasKilometros.maxOfOrNull { medida -> medida.size.height } ?: 0
    val margenInferiorPixeles = with(densidad) { alturaEtiquetaX + SEPARACION_ETIQUETA_X.toPx() + MARGEN_INFERIOR_EXTRA.toPx() }

    // Seleccionar el punto más cercano a una posición horizontal y avisar a quien escucha cuando se pide
    val seleccionarEnPosicion: (Float, Float, Boolean) -> Unit = { posicionX, anchoTotal, debeAvisar ->
        val kilometros = convertirPosicionAKilometros(
            posicionX = posicionX,
            izquierda = izquierdaPixeles,
            derecha = anchoTotal - margenDerechoPixeles,
            kilometrosTotales = escala.kilometrosTotales
        )
        val indice = buscarIndicePuntoMasCercano(puntos, kilometros)
        indiceSeleccionado = indice
        if (debeAvisar && indice != null) alSeleccionarKilometro?.invoke(puntos[indice].distanciaKilometros)
    }
    val seleccionarActual by rememberUpdatedState(seleccionarEnPosicion)
    // Mover la guía una cantidad de puntos para quien no puede arrastrar (lector de pantalla)
    val moverGuia: (Int) -> Boolean = { desplazamiento ->
        val paso = max(1, puntos.size / PASOS_DE_LA_GUIA_ACCESIBLE)
        val actual = indiceVigente ?: if (desplazamiento > 0) -paso else puntos.size
        val nuevo = (actual + desplazamiento * paso).coerceIn(0, puntos.lastIndex)
        indiceSeleccionado = nuevo
        alSeleccionarKilometro?.invoke(puntos[nuevo].distanciaKilometros)
        true
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val altura = alturaFija ?: if (esCompacto) {
                (maxWidth * PROPORCION_ALTURA_COMPACTA).coerceIn(ALTURA_MINIMA_COMPACTA, ALTURA_MAXIMA_COMPACTA)
            } else {
                (maxWidth * PROPORCION_ALTURA_NORMAL).coerceIn(ALTURA_MINIMA_NORMAL, ALTURA_MAXIMA_NORMAL)
            }
            val anchoPixeles = constraints.maxWidth.toFloat()
            val altoPixeles = with(densidad) { altura.roundToPx() }.toFloat()
            val area = remember(escala, izquierdaPixeles, margenDerechoPixeles, margenSuperiorPixeles, margenInferiorPixeles, anchoPixeles, altoPixeles) {
                AreaDelGrafico(
                    izquierda = izquierdaPixeles,
                    derecha = anchoPixeles - margenDerechoPixeles,
                    arriba = margenSuperiorPixeles,
                    abajo = altoPixeles - margenInferiorPixeles,
                    escala = escala
                )
            }
            val trazados = remember(perfil, area) { crearTrazadosDelPerfil(area, perfil) }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(altura)
                    .semantics {
                        contentDescription = descripcionResumen
                        role = Role.Image
                        customActions = listOf(
                            CustomAccessibilityAction("Mover la guía hacia adelante") { moverGuia(1) },
                            CustomAccessibilityAction("Mover la guía hacia atrás") { moverGuia(-1) }
                        )
                    }
                    .pointerInput(escala, izquierdaPixeles) {
                        detectTapGestures { posicion -> seleccionarActual(posicion.x, size.width.toFloat(), true) }
                    }
                    .pointerInput(escala, izquierdaPixeles) {
                        var ultimaPosicionX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { posicion ->
                                ultimaPosicionX = posicion.x
                                seleccionarActual(posicion.x, size.width.toFloat(), false)
                            },
                            onDragEnd = { seleccionarActual(ultimaPosicionX, size.width.toFloat(), true) },
                            onDragCancel = { seleccionarActual(ultimaPosicionX, size.width.toFloat(), true) }
                        ) { cambio, _ ->
                            cambio.consume()
                            ultimaPosicionX = cambio.position.x
                            seleccionarActual(cambio.position.x, size.width.toFloat(), false)
                        }
                    }
            ) {
                dibujarEjes(area, paleta, medidasAltitud, medidasKilometros)
                dibujarPerfil(area, trazados, paleta)
                dibujarMarcasDeInteres(area, perfil, marcasDeInteres, pintoresMarcas, paleta)
                kilometroActual?.let { kilometros ->
                    dibujarPosicionActual(area, perfil, kilometros, paleta, medidaEstasAca)
                }
                indiceVigente?.let { indice -> dibujarGuia(area, perfil, indice, paleta) }
            }
        }
        val textoLectura = describirLectura(perfil, indiceVigente, kilometroActual.takeIf { mostrarPosicionEnTexto }, esCompacto)
        if (textoLectura != null) {
            Text(
                text = textoLectura,
                style = MaterialTheme.typography.bodyMedium,
                color = if (indiceVigente != null) esquema.onSurface else esquema.onSurfaceVariant,
                modifier = if (indiceVigente != null) {
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                } else {
                    Modifier
                }
            )
        }
        Text(
            text = ConfiguracionElevacion.TEXTO_ATRIBUCION,
            style = MaterialTheme.typography.bodySmall,
            color = esquema.onSurfaceVariant
        )
    }
}

// Elegir el texto bajo el gráfico: el punto tocado, la posición actual o la ayuda de uso
private fun describirLectura(
    perfil: PerfilElevacion,
    indiceSeleccionado: Int?,
    kilometroActual: Double?,
    esCompacto: Boolean
): String? {
    if (indiceSeleccionado != null) {
        val punto = perfil.puntos[indiceSeleccionado]
        return describirPuntoDelPerfil(punto.distanciaKilometros, punto.altitudMetros)
    }
    if (kilometroActual != null) {
        val altitud = interpolarAltitud(perfil.puntos, kilometroActual) ?: return null
        return describirPosicionActual(kilometroActual, altitud)
    }
    return if (esCompacto) null else TEXTO_AYUDA_TOQUE
}

// Dibujar la cuadrícula con las etiquetas de altura a la izquierda y de kilómetros abajo
private fun DrawScope.dibujarEjes(
    area: AreaDelGrafico,
    paleta: PaletaDelGrafico,
    medidasAltitud: List<TextLayoutResult>,
    medidasKilometros: List<TextLayoutResult>
) {
    val grosor = 1.dp.toPx()
    area.escala.marcasAltitudMetros.forEachIndexed { posicion, marca ->
        val y = area.posicionY(marca.toDouble())
        drawLine(paleta.cuadricula, Offset(area.izquierda, y), Offset(area.derecha, y), strokeWidth = grosor)
        val medida = medidasAltitud.getOrNull(posicion) ?: return@forEachIndexed
        drawText(
            textLayoutResult = medida,
            topLeft = Offset(area.izquierda - SEPARACION_ETIQUETA_Y.toPx() - medida.size.width, y - medida.size.height / 2f)
        )
    }
    area.escala.marcasKilometros.forEachIndexed { posicion, marca ->
        val x = area.posicionX(marca)
        drawLine(paleta.cuadricula.copy(alpha = 0.5f), Offset(x, area.arriba), Offset(x, area.abajo), strokeWidth = grosor)
        val medida = medidasKilometros.getOrNull(posicion) ?: return@forEachIndexed
        val izquierdaTexto = (x - medida.size.width / 2f).coerceIn(0f, max(0f, size.width - medida.size.width))
        drawText(medida, topLeft = Offset(izquierdaTexto, area.abajo + SEPARACION_ETIQUETA_X.toPx()))
    }
    drawLine(paleta.texto.copy(alpha = 0.6f), Offset(area.izquierda, area.abajo), Offset(area.derecha, area.abajo), strokeWidth = grosor)
}

// Trazados del perfil ya armados: la línea superior y el área rellena bajo ella
private class TrazadosDelPerfil(val linea: Path, val relleno: Path)

// Armar los trazados del perfil una sola vez por perfil y tamaño, para no rearmarlos al dibujar ni al arrastrar
private fun crearTrazadosDelPerfil(area: AreaDelGrafico, perfil: PerfilElevacion): TrazadosDelPerfil {
    val puntos = perfil.puntos
    val linea = Path()
    puntos.forEachIndexed { indice, punto ->
        val x = area.posicionX(punto.distanciaKilometros)
        val y = area.posicionY(punto.altitudMetros)
        if (indice == 0) linea.moveTo(x, y) else linea.lineTo(x, y)
    }
    val relleno = Path()
    if (puntos.isNotEmpty()) {
        relleno.addPath(linea)
        relleno.lineTo(area.posicionX(puntos.last().distanciaKilometros), area.abajo)
        relleno.lineTo(area.posicionX(puntos.first().distanciaKilometros), area.abajo)
        relleno.close()
    }
    return TrazadosDelPerfil(linea, relleno)
}

// Dibujar el área con degradé y la línea superior del perfil
private fun DrawScope.dibujarPerfil(area: AreaDelGrafico, trazados: TrazadosDelPerfil, paleta: PaletaDelGrafico) {
    drawPath(
        path = trazados.relleno,
        brush = Brush.verticalGradient(
            colors = listOf(paleta.linea.copy(alpha = 0.45f), paleta.linea.copy(alpha = 0.05f)),
            startY = area.arriba,
            endY = area.abajo
        )
    )
    drawPath(
        path = trazados.linea,
        color = paleta.linea,
        style = Stroke(width = GROSOR_LINEA.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// Dibujar el ícono de cada punto de interés sobre la curva, en su kilómetro
private fun DrawScope.dibujarMarcasDeInteres(
    area: AreaDelGrafico,
    perfil: PerfilElevacion,
    marcas: List<MarcaDelPerfil>,
    pintores: List<Painter>,
    paleta: PaletaDelGrafico
) {
    marcas.forEachIndexed { posicion, marca ->
        if (marca.kilometros < 0.0 || marca.kilometros > area.escala.kilometrosTotales) return@forEachIndexed
        val pintor = pintores.getOrNull(posicion) ?: return@forEachIndexed
        val altitud = interpolarAltitud(perfil.puntos, marca.kilometros) ?: return@forEachIndexed
        val x = area.posicionX(marca.kilometros)
        val radio = RADIO_MARCA_INTERES.toPx()
        val y = (area.posicionY(altitud) - DESPLAZAMIENTO_MARCA_SOBRE_CURVA.toPx()).coerceAtLeast(radio)
        drawCircle(paleta.fondoMarca, radius = radio, center = Offset(x, y))
        drawCircle(paleta.linea, radius = radio, center = Offset(x, y), style = Stroke(width = 1.dp.toPx()))
        val lado = TAMANO_ICONO_MARCA.toPx()
        translate(left = x - lado / 2f, top = y - lado / 2f) {
            with(pintor) { draw(size = Size(lado, lado), colorFilter = ColorFilter.tint(paleta.linea)) }
        }
    }
}

// Dibujar la marca "Estás acá": guía punteada, punto sobre la curva y rótulo arriba
private fun DrawScope.dibujarPosicionActual(
    area: AreaDelGrafico,
    perfil: PerfilElevacion,
    kilometros: Double,
    paleta: PaletaDelGrafico,
    medidaRotulo: TextLayoutResult
) {
    val altitud = interpolarAltitud(perfil.puntos, kilometros) ?: return
    val x = area.posicionX(kilometros)
    val y = area.posicionY(altitud)
    drawLine(
        color = paleta.posicionActual,
        start = Offset(x, area.arriba),
        end = Offset(x, area.abajo),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
    )
    dibujarPuntoConAnillo(Offset(x, y), RADIO_PUNTO_ACTUAL.toPx(), paleta.posicionActual, paleta.fondoMarca)
    val izquierdaRotulo = (x - medidaRotulo.size.width / 2f).coerceIn(0f, max(0f, size.width - medidaRotulo.size.width))
    drawText(medidaRotulo, color = paleta.posicionActual, topLeft = Offset(izquierdaRotulo, 2.dp.toPx()))
}

// Dibujar la guía vertical y el punto del perfil seleccionado
private fun DrawScope.dibujarGuia(area: AreaDelGrafico, perfil: PerfilElevacion, indice: Int, paleta: PaletaDelGrafico) {
    val punto = perfil.puntos[indice]
    val x = area.posicionX(punto.distanciaKilometros)
    val y = area.posicionY(punto.altitudMetros)
    drawLine(paleta.guia.copy(alpha = 0.8f), Offset(x, area.arriba), Offset(x, area.abajo), strokeWidth = 1.5.dp.toPx())
    dibujarPuntoConAnillo(Offset(x, y), RADIO_PUNTO.toPx(), paleta.linea, paleta.fondoMarca)
}

// Dibujar un círculo relleno con un anillo de otro color para que se distinga sobre el área
private fun DrawScope.dibujarPuntoConAnillo(centro: Offset, radio: Float, relleno: Color, anillo: Color) {
    drawCircle(anillo, radius = radio + 2.dp.toPx(), center = centro)
    drawCircle(relleno, radius = radio, center = centro)
}
