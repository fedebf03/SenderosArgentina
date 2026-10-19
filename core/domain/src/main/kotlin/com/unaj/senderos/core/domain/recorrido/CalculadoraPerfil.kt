package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.tieneCoordenadasValidas
import com.unaj.senderos.core.domain.recorrido.CalculadoraCorredor
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraSeguimiento
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val METROS_POR_KILOMETRO = 1000.0
private const val RADIO_SUAVIZADO = 2
private const val DISTANCIA_MINIMA_PENDIENTE_METROS = 1.0

// Coordenada del trazado con la distancia acumulada desde su inicio
data class MuestraTrazado(
    val coordenada: Coordenada,
    val distanciaKilometros: Double
)

object CalculadoraPerfil {
    const val PASO_METROS = 50.0
    const val MAXIMO_PUNTOS = 500

    // Calcular la firma del trazado planificado completo (con la vuelta si es circular o de ida y vuelta)
    fun firmaTrazado(recorrido: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): String {
        return CalculadoraCorredor.firmaTrazado(construirTrazadoValido(recorrido, tipoRecorrido))
    }

    // Indicar si el sendero tiene un trazado medible y su perfil falta o fue calculado con otro trazado
    fun necesitaPerfil(sendero: Sendero): Boolean {
        val puntosValidos = sendero.recorrido.filter(::esPuntoValido)
        if (CalculadoraRecorrido.distanciaKilometros(puntosValidos, TipoRecorrido.LINEAL) <= 0.0) return false
        val perfil = sendero.perfilElevacion ?: return true
        return perfil.firmaTrazado != firmaTrazado(sendero.recorrido, sendero.tipoRecorrido)
    }

    // Muestrear el trazado planificado en tramos iguales de a lo sumo pasoMetros, incluyendo el inicio y el fin;
    // si hiciera falta superar maximoPuntos, el paso crece. Devuelve vacío si el trazado no tiene largo
    fun muestrearTrazado(
        recorrido: List<PuntoRecorrido>,
        tipoRecorrido: TipoRecorrido,
        pasoMetros: Double = PASO_METROS,
        maximoPuntos: Int = MAXIMO_PUNTOS
    ): List<MuestraTrazado> {
        val trazado = construirTrazadoValido(recorrido, tipoRecorrido)
        if (trazado.size < 2) return emptyList()
        val acumuladas = CalculadoraRecorrido.distanciasAcumuladasKilometros(trazado)
        val total = acumuladas.last()
        if (total <= 0.0) return emptyList()
        val tramos = ceil(total * METROS_POR_KILOMETRO / pasoMetros).toInt().coerceIn(1, max(maximoPuntos - 1, 1))
        var indiceTramo = 0
        return (0..tramos).map { indiceMuestra ->
            if (indiceMuestra == tramos) {
                MuestraTrazado(Coordenada(trazado.last().latitud, trazado.last().longitud), total)
            } else {
                val distancia = total * indiceMuestra / tramos
                while (indiceTramo < trazado.size - 2 && acumuladas[indiceTramo + 1] < distancia) indiceTramo++
                MuestraTrazado(interpolar(trazado, acumuladas, indiceTramo, distancia), distancia)
            }
        }
    }

    // Armar el perfil con las altitudes del modelo digital (una por muestra). El desnivel y la pendiente se miden sobre
    // una curva suavizada con promedio móvil de 5 muestras (unos 250 m mientras el paso sea de 50 m; el paso crece
    // recién en trazados de más de ~25 km), porque el modelo de 90 m de resolución oscila de a metros entre muestras
    // vecinas y sumarlas directo inflaría el desnivel. Las alturas máxima y mínima salen de los valores sin suavizar
    // y los puntos del perfil conservan las altitudes originales
    fun calcular(muestras: List<MuestraTrazado>, altitudes: List<Double>, firmaTrazado: String): PerfilElevacion {
        require(muestras.isNotEmpty()) { "Hacen falta muestras para calcular el perfil" }
        require(muestras.size == altitudes.size) { "Debe haber una altitud por cada muestra" }
        val suavizadas = suavizarAltitudes(altitudes)
        var desnivelPositivo = 0.0
        var desnivelNegativo = 0.0
        var pendienteMaxima = 0.0
        for (indice in 1 until suavizadas.size) {
            val diferencia = suavizadas[indice] - suavizadas[indice - 1]
            if (diferencia > 0) desnivelPositivo += diferencia else desnivelNegativo -= diferencia
            val tramoMetros = (muestras[indice].distanciaKilometros - muestras[indice - 1].distanciaKilometros) * METROS_POR_KILOMETRO
            if (tramoMetros >= DISTANCIA_MINIMA_PENDIENTE_METROS) {
                pendienteMaxima = max(pendienteMaxima, abs(diferencia) / tramoMetros * 100.0)
            }
        }
        return PerfilElevacion(
            puntos = muestras.indices.map { indice -> PuntoPerfil(muestras[indice].distanciaKilometros, altitudes[indice]) },
            desnivelPositivoMetros = desnivelPositivo.roundToInt(),
            desnivelNegativoMetros = desnivelNegativo.roundToInt(),
            alturaMaximaMetros = altitudes.max().roundToInt(),
            alturaMinimaMetros = altitudes.min().roundToInt(),
            pendienteMaximaPorcentaje = pendienteMaxima,
            firmaTrazado = firmaTrazado
        )
    }

    fun aplicarAlSendero(sendero: Sendero, perfil: PerfilElevacion): Sendero {
        return sendero.copy(
            perfilElevacion = perfil,
            desnivelMetros = perfil.desnivelPositivoMetros,
            alturaMaximaMetros = perfil.alturaMaximaMetros,
            alturaMinimaMetros = perfil.alturaMinimaMetros
        )
    }

    // Calcular los metros de subida que faltan desde un kilómetro del trazado hasta el final, con el mismo suavizado
    // que el desnivel del perfil. Un kilómetro fuera del trazado se limita a sus extremos; un perfil sin tramos da 0
    fun calcularSubidaRestanteMetros(perfil: PerfilElevacion, kilometroActual: Double): Int {
        val puntos = perfil.puntos
        if (puntos.size < 2) return 0
        val suavizadas = suavizarAltitudes(puntos.map { punto -> punto.altitudMetros })
        val kilometro = kilometroActual.coerceIn(puntos.first().distanciaKilometros, puntos.last().distanciaKilometros)
        val indiceSiguiente = puntos.indexOfFirst { punto -> punto.distanciaKilometros > kilometro }
        if (indiceSiguiente <= 0) return 0
        val anterior = puntos[indiceSiguiente - 1]
        val largoTramo = puntos[indiceSiguiente].distanciaKilometros - anterior.distanciaKilometros
        val fraccion = if (largoTramo <= 0.0) 0.0 else (kilometro - anterior.distanciaKilometros) / largoTramo
        val altitudActual = suavizadas[indiceSiguiente - 1] + (suavizadas[indiceSiguiente] - suavizadas[indiceSiguiente - 1]) * fraccion
        var subida = max(0.0, suavizadas[indiceSiguiente] - altitudActual)
        for (indice in indiceSiguiente + 1 until suavizadas.size) {
            subida += max(0.0, suavizadas[indice] - suavizadas[indice - 1])
        }
        return subida.roundToInt()
    }

    // Indicar si las coordenadas del punto son finitas y están dentro del rango geográfico
    private fun esPuntoValido(punto: PuntoRecorrido): Boolean {
        return Coordenada(punto.latitud, punto.longitud).tieneCoordenadasValidas()
    }

    // Construir el trazado completo con la misma regla que el seguimiento, descartando puntos con coordenadas inválidas
    private fun construirTrazadoValido(recorrido: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): List<PuntoRecorrido> {
        return CalculadoraSeguimiento.construirTrazado(recorrido.filter(::esPuntoValido), tipoRecorrido)
    }

    // Ubicar por interpolación lineal la coordenada a una distancia dada dentro del tramo indicado
    private fun interpolar(
        trazado: List<PuntoRecorrido>,
        acumuladas: List<Double>,
        indiceTramo: Int,
        distancia: Double
    ): Coordenada {
        val origen = trazado[indiceTramo]
        val destino = trazado[indiceTramo + 1]
        val largoTramo = acumuladas[indiceTramo + 1] - acumuladas[indiceTramo]
        val fraccion = if (largoTramo <= 0.0) 0.0 else ((distancia - acumuladas[indiceTramo]) / largoTramo).coerceIn(0.0, 1.0)
        return Coordenada(
            latitud = origen.latitud + (destino.latitud - origen.latitud) * fraccion,
            longitud = origen.longitud + (destino.longitud - origen.longitud) * fraccion
        )
    }

    // Suavizar con promedio móvil centrado de 5 muestras cuya ventana se achica en los extremos para no mover el inicio ni el fin
    fun suavizarAltitudes(altitudes: List<Double>): List<Double> {
        return altitudes.indices.map { indice ->
            val radio = min(RADIO_SUAVIZADO, min(indice, altitudes.lastIndex - indice))
            altitudes.subList(indice - radio, indice + radio + 1).average()
        }
    }
}
