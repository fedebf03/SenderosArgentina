package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.ResultadoSeguimiento
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import kotlin.math.cos
import kotlin.math.hypot

private const val KILOMETROS_POR_GRADO = 111.195
private const val METROS_POR_KILOMETRO = 1000.0
private const val MOVIMIENTO_MINIMO_KILOMETROS = 0.005
private const val RADIO_AVISO_PUNTO_METROS = 50.0
private const val UMBRAL_DESVIO_METROS = 100.0
private const val UMBRAL_REARME_DESVIO_METROS = 50.0
private const val TOLERANCIA_EMPATE_METROS = 0.5
private const val CERCANIA_FIN_TRAMO_KILOMETROS = 0.005
private const val VELOCIDAD_MINIMA_PARA_RUMBO = 0.5f
private const val DESPLAZAMIENTO_MINIMO_PARA_RUMBO_METROS = 10.0
private const val VENTANA_MINIMA_BUSQUEDA_KILOMETROS = 0.06
private const val FACTOR_VENTANA_POR_DESPLAZAMIENTO = 2.0
private const val FRACCION_MINIMA_PARA_LLEGADA = 0.5
private const val UMBRAL_LLEGADA_KILOMETROS = 0.02

// Proyección de una posición sobre un tramo: distancia perpendicular y fracción recorrida del tramo (0 a 1)
private data class Proyeccion(val distanciaMetros: Double, val fraccion: Double)

object CalculadoraSeguimiento {

    // Crear el estado inicial del seguimiento de un sendero con el trazado completo y la distancia restante total
    fun iniciar(sendero: Sendero, ahora: Long): EstadoSeguimiento {
        val trazado = construirTrazado(sendero.recorrido, sendero.tipoRecorrido)
        return EstadoSeguimiento(
            idSendero = sendero.id,
            nombreSendero = sendero.nombre,
            trazado = trazado,
            indicesRecorridoDelTrazado = construirIndicesDelRecorrido(sendero.recorrido.size, sendero.tipoRecorrido, trazado.size),
            distanciaRestanteKilometros = longitudTotal(trazado),
            indiceProximoPunto = if (trazado.size > 1) 1 else 0,
            instanteInicioMilisegundos = ahora
        )
    }

    // Procesar una nueva lectura de ubicación y devolver el estado actualizado con los avisos generados
    fun actualizar(estado: EstadoSeguimiento, lectura: LecturaUbicacion, ahora: Long): ResultadoSeguimiento {
        val nuevaPosicion = lectura.coordenada
        val tiempoTranscurrido = ahora - estado.instanteInicioMilisegundos
        val trazado = estado.trazado
        if (trazado.size < 2) {
            return ResultadoSeguimiento(
                estado = estado.copy(posicionActual = nuevaPosicion, tiempoTranscurridoMilisegundos = tiempoTranscurrido),
                avisos = emptyList()
            )
        }
        val ultimoTramo = trazado.size - 2
        val indiceTramoAnterior = estado.indiceTramoActual.coerceIn(0, ultimoTramo)
        val largoTotal = longitudTotal(trazado)
        val desplazamientoDesdeLecturaAnteriorKilometros = estado.posicionActual
            ?.let { anterior -> distanciaEnMetros(anterior, nuevaPosicion) / METROS_POR_KILOMETRO } ?: 0.0
        val progresoActualKilometros = largoTotal - estado.distanciaRestanteKilometros
        val limiteDeBusquedaKilometros = progresoActualKilometros + maxOf(
            VENTANA_MINIMA_BUSQUEDA_KILOMETROS,
            FACTOR_VENTANA_POR_DESPLAZAMIENTO * desplazamientoDesdeLecturaAnteriorKilometros
        )
        val (indiceTramo, proyeccion) = buscarTramoMasCercano(trazado, nuevaPosicion, indiceTramoAnterior, limiteDeBusquedaKilometros)
        val largoTramo = largoDelTramo(trazado, indiceTramo)
        val progresoKilometros = (0 until indiceTramo).sumOf { indice -> largoDelTramo(trazado, indice) } +
            proyeccion.fraccion * largoTramo
        val llegoAlFinDelTramo = (1 - proyeccion.fraccion) * largoTramo <= CERCANIA_FIN_TRAMO_KILOMETROS
        val indiceTramoNuevo = if (llegoAlFinDelTramo && indiceTramo < ultimoTramo) indiceTramo + 1 else indiceTramo

        val avisos = mutableListOf<AvisoRecorrido>()
        val puntosAvisados = estado.indicesPuntosAvisados.toMutableSet()
        val rangoDePuntos = indiceTramoAnterior..(indiceTramoNuevo + 1).coerceAtMost(trazado.lastIndex)
        for (indicePunto in rangoDePuntos) {
            val punto = trazado[indicePunto]
            val indiceEnRecorrido = estado.indicesRecorridoDelTrazado.getOrElse(indicePunto) { indicePunto }
            val estaCerca = distanciaEnMetros(nuevaPosicion, punto) <= RADIO_AVISO_PUNTO_METROS
            if (punto.tipo != TipoPunto.PASO && indiceEnRecorrido !in puntosAvisados && estaCerca) {
                puntosAvisados.add(indiceEnRecorrido)
                avisos.add(AvisoRecorrido.CercaDePunto(indice = indiceEnRecorrido, punto = punto))
            }
        }

        var estaDesviado = estado.estaDesviado
        if (!estaDesviado && proyeccion.distanciaMetros > UMBRAL_DESVIO_METROS) {
            estaDesviado = true
            avisos.add(AvisoRecorrido.Desvio)
        } else if (estaDesviado && proyeccion.distanciaMetros < UMBRAL_REARME_DESVIO_METROS) {
            estaDesviado = false
        }

        val distanciaRestante = (largoTotal - progresoKilometros).coerceAtLeast(0.0)

        val ultimaAceptada = estado.ultimaPosicionAceptada
        val desplazamientoKilometros = if (ultimaAceptada == null) {
            0.0
        } else {
            distanciaEnMetros(ultimaAceptada, nuevaPosicion) / METROS_POR_KILOMETRO
        }
        val seAceptaMovimiento = ultimaAceptada == null || desplazamientoKilometros >= MOVIMIENTO_MINIMO_KILOMETROS
        val esSaltoDeSimulacion = estado.esSimulado && (estado.estaDesviado || estaDesviado)
        val distanciaSumada = if (ultimaAceptada != null && seAceptaMovimiento && !esSaltoDeSimulacion) desplazamientoKilometros else 0.0

        val distanciaRecorrida = estado.distanciaRecorridaKilometros + distanciaSumada
        val llegoAlFinal = estado.llegoAlFinal || (
            indiceTramo == ultimoTramo &&
                distanciaRestante < UMBRAL_LLEGADA_KILOMETROS &&
                distanciaRecorrida >= FRACCION_MINIMA_PARA_LLEGADA * largoTotal
            )
        if (llegoAlFinal && !estado.llegoAlFinal) avisos.add(AvisoRecorrido.LlegadaAlFinal)

        val rumboNuevo = determinarRumbo(estado.rumboGrados, lectura, ultimaAceptada, desplazamientoKilometros * METROS_POR_KILOMETRO)

        val nuevoEstado = estado.copy(
            rumboGrados = rumboNuevo,
            velocidadMetrosPorSegundo = lectura.velocidadMetrosPorSegundo,
            velocidadMaximaMetrosPorSegundo = maxOf(estado.velocidadMaximaMetrosPorSegundo, lectura.velocidadMetrosPorSegundo ?: 0f),
            trazadoCaminado = if (seAceptaMovimiento) estado.trazadoCaminado + nuevaPosicion else estado.trazadoCaminado,
            posicionActual = nuevaPosicion,
            ultimaPosicionAceptada = if (seAceptaMovimiento) nuevaPosicion else ultimaAceptada,
            distanciaRecorridaKilometros = distanciaRecorrida,
            distanciaRestanteKilometros = distanciaRestante,
            llegoAlFinal = llegoAlFinal,
            distanciaAlTrazadoMetros = proyeccion.distanciaMetros,
            indiceTramoActual = indiceTramoNuevo,
            indiceProximoPunto = (indiceTramoNuevo + 1).coerceAtMost(trazado.lastIndex),
            estaDesviado = estaDesviado,
            indicesPuntosAvisados = puntosAvisados,
            tiempoTranscurridoMilisegundos = tiempoTranscurrido
        )
        return ResultadoSeguimiento(estado = nuevoEstado, avisos = avisos)
    }

    // Determinar el rumbo: el del GPS si la velocidad es suficiente o desconocida, el geodésico si el desplazamiento supera el ruido, o el último conocido
    private fun determinarRumbo(
        rumboAnterior: Float?,
        lectura: LecturaUbicacion,
        ultimaAceptada: Coordenada?,
        desplazamientoMetros: Double
    ): Float? {
        val rumboGps = lectura.rumboGrados
        val velocidad = lectura.velocidadMetrosPorSegundo
        val umbralDesplazamiento = maxOf(DESPLAZAMIENTO_MINIMO_PARA_RUMBO_METROS, (lectura.precisionMetros ?: 0f).toDouble())
        return when {
            rumboGps != null && (velocidad == null || velocidad >= VELOCIDAD_MINIMA_PARA_RUMBO) -> CalculadoraRecorrido.normalizarRumbo(rumboGps)
            rumboGps == null && ultimaAceptada != null && desplazamientoMetros > umbralDesplazamiento ->
                CalculadoraRecorrido.rumboInicialGrados(ultimaAceptada, lectura.coordenada).toFloat()
            else -> rumboAnterior
        }
    }

    // Construir el trazado a seguir: circular cierra al origen e ida y vuelta agrega el regreso invertido
    private fun construirTrazado(puntos: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): List<PuntoRecorrido> {
        if (puntos.size < 2) return puntos
        return when (tipoRecorrido) {
            TipoRecorrido.LINEAL -> puntos
            TipoRecorrido.CIRCULAR -> puntos + puntos.first()
            TipoRecorrido.IDA_Y_VUELTA -> puntos + puntos.reversed().drop(1)
        }
    }

    // Construir la correspondencia entre cada punto del trazado y su índice en el recorrido del sendero
    private fun construirIndicesDelRecorrido(cantidadPuntos: Int, tipoRecorrido: TipoRecorrido, largoTrazado: Int): List<Int> {
        if (cantidadPuntos < 2) return (0 until largoTrazado).toList()
        return (0 until largoTrazado).map { indice ->
            when {
                indice < cantidadPuntos -> indice
                tipoRecorrido == TipoRecorrido.CIRCULAR -> 0
                else -> 2 * cantidadPuntos - 2 - indice
            }
        }
    }

    // Calcular el largo total del trazado en kilómetros
    private fun longitudTotal(trazado: List<PuntoRecorrido>): Double {
        return (0 until (trazado.size - 1).coerceAtLeast(0)).sumOf { indice -> largoDelTramo(trazado, indice) }
    }

    // Calcular el largo en kilómetros de un tramo reutilizando el Haversine de la calculadora de recorrido
    private fun largoDelTramo(trazado: List<PuntoRecorrido>, indiceTramo: Int): Double {
        val origen = trazado[indiceTramo]
        return CalculadoraRecorrido.distanciaEntre(Coordenada(origen.latitud, origen.longitud), trazado[indiceTramo + 1])
    }

    // Calcular la distancia en metros entre una coordenada y un punto con Haversine
    private fun distanciaEnMetros(desde: Coordenada, hasta: PuntoRecorrido): Double {
        return CalculadoraRecorrido.distanciaEntre(desde, hasta) * METROS_POR_KILOMETRO
    }

    // Calcular la distancia en metros entre dos coordenadas con Haversine
    private fun distanciaEnMetros(desde: Coordenada, hasta: Coordenada): Double {
        return distanciaEnMetros(desde, PuntoRecorrido(hasta.latitud, hasta.longitud))
    }

    // Buscar el tramo más cercano entre el actual y los que empiezan antes del límite de progreso; el más temprano gana los empates
    private fun buscarTramoMasCercano(
        trazado: List<PuntoRecorrido>,
        posicion: Coordenada,
        indiceInicial: Int,
        limiteDeProgresoKilometros: Double
    ): Pair<Int, Proyeccion> {
        var mejorIndice = indiceInicial
        var mejorProyeccion = proyectarSobreTramo(posicion, trazado[indiceInicial], trazado[indiceInicial + 1])
        var inicioDelTramoKilometros = (0 until indiceInicial).sumOf { indice -> largoDelTramo(trazado, indice) }
        inicioDelTramoKilometros += largoDelTramo(trazado, indiceInicial)
        for (indice in (indiceInicial + 1)..(trazado.size - 2)) {
            if (inicioDelTramoKilometros > limiteDeProgresoKilometros) break
            val proyeccion = proyectarSobreTramo(posicion, trazado[indice], trazado[indice + 1])
            if (proyeccion.distanciaMetros < mejorProyeccion.distanciaMetros - TOLERANCIA_EMPATE_METROS) {
                mejorIndice = indice
                mejorProyeccion = proyeccion
            }
            inicioDelTramoKilometros += largoDelTramo(trazado, indice)
        }
        return mejorIndice to mejorProyeccion
    }

    // Proyectar la posición sobre un tramo con una aproximación equirectangular local
    private fun proyectarSobreTramo(posicion: Coordenada, origen: PuntoRecorrido, destino: PuntoRecorrido): Proyeccion {
        val factorLongitud = cos(Math.toRadians(origen.latitud))
        val destinoX = (destino.longitud - origen.longitud) * factorLongitud
        val destinoY = destino.latitud - origen.latitud
        val posicionX = (posicion.longitud - origen.longitud) * factorLongitud
        val posicionY = posicion.latitud - origen.latitud
        val largoCuadrado = destinoX * destinoX + destinoY * destinoY
        val fraccion = if (largoCuadrado == 0.0) {
            0.0
        } else {
            ((posicionX * destinoX + posicionY * destinoY) / largoCuadrado).coerceIn(0.0, 1.0)
        }
        val distanciaGrados = hypot(posicionX - fraccion * destinoX, posicionY - fraccion * destinoY)
        return Proyeccion(distanciaMetros = distanciaGrados * KILOMETROS_POR_GRADO * METROS_POR_KILOMETRO, fraccion = fraccion)
    }
}
