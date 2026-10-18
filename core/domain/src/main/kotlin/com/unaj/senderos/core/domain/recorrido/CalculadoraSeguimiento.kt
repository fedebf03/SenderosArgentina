package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.ResultadoSeguimiento
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
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
private const val UMBRAL_LLEGADA_METROS = 20.0
private const val UMBRAL_LLEGADA_MAXIMO_METROS = 40.0
private const val PRECISION_MAXIMA_ACEPTABLE_METROS = 30f
private const val FACTOR_PRECISION_TRAZADO_CAMINADO = 2f
private const val TOPE_PRECISION_TRAZADO_METROS = 50f
private const val VIGENCIA_LECTURA_VALIDA_MILISEGUNDOS = 15_000L
private const val ESPERA_PRIMERA_LECTURA_MILISEGUNDOS = 30_000L
private const val VELOCIDAD_PLAUSIBLE_METROS_POR_SEGUNDO = 3.0
private const val DISTANCIA_MAXIMA_PRIMER_TRAMO_CIRCULAR_METROS = 30.0

// Proyección de una posición sobre un tramo: distancia perpendicular y fracción recorrida del tramo (0 a 1)
internal data class Proyeccion(val distanciaMetros: Double, val fraccion: Double)

object CalculadoraSeguimiento {

    fun iniciar(sendero: Sendero, ahora: Long): EstadoSeguimiento {
        val trazado = construirTrazado(sendero.recorrido, sendero.tipoRecorrido)
        return EstadoSeguimiento(
            idSendero = sendero.id,
            nombreSendero = sendero.nombre,
            trazado = trazado,
            indicesRecorridoDelTrazado = construirIndicesDelRecorrido(sendero.recorrido.size, sendero.tipoRecorrido, trazado.size),
            distanciaRestanteKilometros = longitudTotal(trazado),
            indiceProximoPunto = if (trazado.size > 1) 1 else 0,
            instanteInicioMilisegundos = ahora,
            esCircular = sendero.tipoRecorrido == TipoRecorrido.CIRCULAR && sendero.recorrido.size >= 2
        )
    }

    // Iniciar el seguimiento de una caminata libre: sin trazado, solo con el identificador y el nombre de la caminata
    fun iniciarCaminataLibre(idCaminata: String, nombre: String, ahora: Long): EstadoSeguimiento = EstadoSeguimiento(
        idSendero = idCaminata,
        nombreSendero = nombre,
        trazado = emptyList(),
        instanteInicioMilisegundos = ahora,
        esCaminataLibre = true
    )

    fun actualizar(
        estado: EstadoSeguimiento,
        lectura: LecturaUbicacion,
        ahora: Long,
        precisionMaximaMetros: Float = PRECISION_MAXIMA_ACEPTABLE_METROS
    ): ResultadoSeguimiento {
        val nuevaPosicion = lectura.coordenada
        val tiempoTranscurrido = ahora - estado.instanteInicioMilisegundos
        val trazado = estado.trazado
        if ((lectura.precisionMetros ?: 0f) > precisionMaximaMetros) {
            // Lectura imprecisa: no mueve la posición ni suma distancia (sí puede sumarse al trazado caminado); hace correr el tiempo, toma la velocidad del GPS (sigue siendo útil) y reevalúa la señal
            val sinMover = estado.copy(
                tiempoTranscurridoMilisegundos = tiempoTranscurrido,
                velocidadMetrosPorSegundo = lectura.velocidadMetrosPorSegundo
            )
            val conTrazado = extenderTrazadoCaminado(sinMover, lectura, esValida = false, precisionMaximaMetros = precisionMaximaMetros)
            return ResultadoSeguimiento(estado = reevaluarSenal(conTrazado, ahora), avisos = emptyList())
        }
        if (estado.esCaminataLibre) return ResultadoSeguimiento(estado = actualizarCaminataLibre(estado, lectura, ahora, precisionMaximaMetros), avisos = emptyList())
        if (trazado.size < 2) {
            return ResultadoSeguimiento(
                estado = estado.copy(
                    posicionActual = nuevaPosicion,
                    tiempoTranscurridoMilisegundos = tiempoTranscurrido,
                    calidadSenal = CalidadSenal.BUENA,
                    instanteUltimaLecturaValidaMilisegundos = ahora
                ),
                avisos = emptyList()
            )
        }
        val ultimoTramo = trazado.size - 2
        val indiceTramoAnterior = estado.indiceTramoActual.coerceIn(0, ultimoTramo)
        val largoTotal = longitudTotal(trazado)
        val esPrimeraLectura = estado.posicionActual == null
        val desplazamientoDesdeLecturaAnteriorKilometros = estado.posicionActual
            ?.let { anterior -> distanciaEnMetros(anterior, nuevaPosicion) / METROS_POR_KILOMETRO } ?: 0.0
        val desplazamientoParaVentanaKilometros =
            minOf(desplazamientoDesdeLecturaAnteriorKilometros, desplazamientoPlausibleKilometros(estado, ahora))
        val progresoActualKilometros = largoTotal - estado.distanciaRestanteKilometros
        val limiteDeBusquedaKilometros = if (esPrimeraLectura) {
            Double.POSITIVE_INFINITY
        } else {
            progresoActualKilometros + maxOf(
                VENTANA_MINIMA_BUSQUEDA_KILOMETROS,
                FACTOR_VENTANA_POR_DESPLAZAMIENTO * desplazamientoParaVentanaKilometros
            )
        }
        val (indiceTramoBuscado, proyeccionBuscada) = buscarTramoMasCercano(trazado, nuevaPosicion, indiceTramoAnterior, limiteDeBusquedaKilometros)
        val (indiceTramo, proyeccion) = if (esPrimeraLectura && estado.esCircular && indiceTramoBuscado == ultimoTramo) {
            val proyeccionPrimerTramo = proyectarSobreTramo(nuevaPosicion, trazado[0], trazado[1])
            if (proyeccionPrimerTramo.distanciaMetros < DISTANCIA_MAXIMA_PRIMER_TRAMO_CIRCULAR_METROS) {
                0 to proyeccionPrimerTramo
            } else {
                indiceTramoBuscado to proyeccionBuscada
            }
        } else {
            indiceTramoBuscado to proyeccionBuscada
        }
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
                distanciaRestante < umbralDeLlegadaKilometros(lectura) &&
                distanciaRecorrida >= FRACCION_MINIMA_PARA_LLEGADA * largoTotal
            )
        if (llegoAlFinal && !estado.llegoAlFinal) avisos.add(AvisoRecorrido.LlegadaAlFinal)

        val rumboNuevo = determinarRumbo(estado.rumboGrados, lectura, ultimaAceptada, desplazamientoKilometros * METROS_POR_KILOMETRO)

        val nuevoEstado = estado.copy(
            rumboGrados = rumboNuevo,
            velocidadMetrosPorSegundo = lectura.velocidadMetrosPorSegundo,
            velocidadMaximaMetrosPorSegundo = maxOf(estado.velocidadMaximaMetrosPorSegundo, lectura.velocidadMetrosPorSegundo ?: 0f),
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
            tiempoTranscurridoMilisegundos = tiempoTranscurrido,
            instanteUltimaLecturaMilisegundos = ahora,
            instanteUltimaLecturaValidaMilisegundos = ahora,
            calidadSenal = CalidadSenal.BUENA
        )
        val conTrazado = extenderTrazadoCaminado(nuevoEstado, lectura, esValida = true, precisionMaximaMetros = precisionMaximaMetros)
        return ResultadoSeguimiento(estado = conTrazado, avisos = avisos)
    }

    // Registrar una lectura válida de una caminata libre: suma la distancia desde la última posición aceptada (5 m o más), guarda velocidad,
    // rumbo y trazado caminado; no calcula tramo, restante, desvío ni llegada porque no hay trazado
    private fun actualizarCaminataLibre(
        estado: EstadoSeguimiento,
        lectura: LecturaUbicacion,
        ahora: Long,
        precisionMaximaMetros: Float
    ): EstadoSeguimiento {
        val nuevaPosicion = lectura.coordenada
        val ultimaAceptada = estado.ultimaPosicionAceptada
        val desplazamientoKilometros = ultimaAceptada?.let { anterior -> distanciaEnMetros(anterior, nuevaPosicion) / METROS_POR_KILOMETRO } ?: 0.0
        val seAceptaMovimiento = ultimaAceptada == null || desplazamientoKilometros >= MOVIMIENTO_MINIMO_KILOMETROS
        val distanciaSumada = if (ultimaAceptada != null && seAceptaMovimiento) desplazamientoKilometros else 0.0
        val nuevoEstado = estado.copy(
            rumboGrados = determinarRumbo(estado.rumboGrados, lectura, ultimaAceptada, desplazamientoKilometros * METROS_POR_KILOMETRO),
            velocidadMetrosPorSegundo = lectura.velocidadMetrosPorSegundo,
            velocidadMaximaMetrosPorSegundo = maxOf(estado.velocidadMaximaMetrosPorSegundo, lectura.velocidadMetrosPorSegundo ?: 0f),
            posicionActual = nuevaPosicion,
            ultimaPosicionAceptada = if (seAceptaMovimiento) nuevaPosicion else ultimaAceptada,
            distanciaRecorridaKilometros = estado.distanciaRecorridaKilometros + distanciaSumada,
            tiempoTranscurridoMilisegundos = ahora - estado.instanteInicioMilisegundos,
            instanteUltimaLecturaMilisegundos = ahora,
            instanteUltimaLecturaValidaMilisegundos = ahora,
            calidadSenal = CalidadSenal.BUENA
        )
        return extenderTrazadoCaminado(nuevoEstado, lectura, esValida = true, precisionMaximaMetros = precisionMaximaMetros)
    }

    // Extender el trazado caminado con la lectura si su precisión no supera el límite del trazado: siempre la primera; después, una válida
    // separada 5 m o más del último punto, o una imprecisa separada al menos su propia precisión (para no dibujar el error del GPS con el
    // usuario quieto). La lectura que no entra queda como pendiente para cerrar el trazado; no toca distancia, tramo ni avisos
    private fun extenderTrazadoCaminado(
        estado: EstadoSeguimiento,
        lectura: LecturaUbicacion,
        esValida: Boolean,
        precisionMaximaMetros: Float
    ): EstadoSeguimiento {
        val precision = lectura.precisionMetros ?: 0f
        if (precision > limitePrecisionTrazadoMetros(precisionMaximaMetros)) return estado
        val posicion = lectura.coordenada
        val ultimoDelTrazado = estado.trazadoCaminado.lastOrNull()
        val separacionMinimaMetros = if (esValida) {
            MOVIMIENTO_MINIMO_KILOMETROS * METROS_POR_KILOMETRO
        } else {
            maxOf(MOVIMIENTO_MINIMO_KILOMETROS * METROS_POR_KILOMETRO, precision.toDouble())
        }
        val seAgrega = ultimoDelTrazado == null || distanciaEnMetros(ultimoDelTrazado, posicion) >= separacionMinimaMetros
        return if (seAgrega) {
            estado.copy(
                trazadoCaminado = estado.trazadoCaminado + posicion,
                ultimaPosicionConocida = null
            )
        } else {
            estado.copy(ultimaPosicionConocida = posicion)
        }
    }

    // Calcular el límite de precisión del trazado: el doble del umbral del equipo con tope de 50 m, sin bajar nunca del propio umbral
    private fun limitePrecisionTrazadoMetros(precisionMaximaMetros: Float): Float =
        maxOf(precisionMaximaMetros, minOf(FACTOR_PRECISION_TRAZADO_CAMINADO * precisionMaximaMetros, TOPE_PRECISION_TRAZADO_METROS))

    // Cerrar el trazado caminado con la última lectura que no había entrado, para que termine donde terminó el usuario
    fun cerrarTrazadoCaminado(estado: EstadoSeguimiento): List<Coordenada> {
        val pendiente = estado.ultimaPosicionConocida ?: return estado.trazadoCaminado
        return if (pendiente == estado.trazadoCaminado.lastOrNull()) estado.trazadoCaminado else estado.trazadoCaminado + pendiente
    }

    // Evaluar la calidad de la señal con el reloj: en simulación siempre BUENA; BUENA si la última lectura válida tiene 15 s o menos;
    // DEBIL si pasó más; sin ninguna lectura válida, BUSCANDO hasta los 30 s del inicio y DEBIL después
    fun evaluarCalidad(estado: EstadoSeguimiento, ahora: Long): CalidadSenal {
        if (estado.esSimulado) return CalidadSenal.BUENA
        val ultimaValida = estado.instanteUltimaLecturaValidaMilisegundos
        return if (ultimaValida == null) {
            if (ahora - estado.instanteInicioMilisegundos > ESPERA_PRIMERA_LECTURA_MILISEGUNDOS) CalidadSenal.DEBIL else CalidadSenal.BUSCANDO
        } else if (ahora - ultimaValida > VIGENCIA_LECTURA_VALIDA_MILISEGUNDOS) {
            CalidadSenal.DEBIL
        } else {
            CalidadSenal.BUENA
        }
    }

    // Si pasa a DEBIL, deja la velocidad desconocida porque ya no es confiable
    fun reevaluarSenal(estado: EstadoSeguimiento, ahora: Long): EstadoSeguimiento {
        val calidad = evaluarCalidad(estado, ahora)
        return estado.copy(
            calidadSenal = calidad,
            velocidadMetrosPorSegundo = if (calidad == CalidadSenal.DEBIL) null else estado.velocidadMetrosPorSegundo
        )
    }

    private fun desplazamientoPlausibleKilometros(estado: EstadoSeguimiento, ahora: Long): Double {
        val instanteAnterior = estado.instanteUltimaLecturaMilisegundos ?: return Double.POSITIVE_INFINITY
        val segundos = ((ahora - instanteAnterior) / 1000.0).coerceAtLeast(0.0)
        return VELOCIDAD_PLAUSIBLE_METROS_POR_SEGUNDO * segundos * estado.multiplicadorSimulacion / METROS_POR_KILOMETRO
    }

    private fun umbralDeLlegadaKilometros(lectura: LecturaUbicacion): Double {
        val metros = maxOf(UMBRAL_LLEGADA_METROS, (lectura.precisionMetros ?: 0f).toDouble())
        return minOf(metros, UMBRAL_LLEGADA_MAXIMO_METROS) / METROS_POR_KILOMETRO
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

    internal fun construirTrazado(puntos: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): List<PuntoRecorrido> {
        if (puntos.size < 2) return puntos
        return when (tipoRecorrido) {
            TipoRecorrido.LINEAL -> puntos
            TipoRecorrido.CIRCULAR -> puntos + puntos.first()
            TipoRecorrido.IDA_Y_VUELTA -> puntos + puntos.reversed().drop(1)
        }
    }

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

    private fun longitudTotal(trazado: List<PuntoRecorrido>): Double {
        return (0 until (trazado.size - 1).coerceAtLeast(0)).sumOf { indice -> largoDelTramo(trazado, indice) }
    }

    private fun largoDelTramo(trazado: List<PuntoRecorrido>, indiceTramo: Int): Double {
        val origen = trazado[indiceTramo]
        return CalculadoraRecorrido.distanciaEntre(Coordenada(origen.latitud, origen.longitud), trazado[indiceTramo + 1])
    }

    private fun distanciaEnMetros(desde: Coordenada, hasta: PuntoRecorrido): Double {
        return CalculadoraRecorrido.distanciaEntre(desde, hasta) * METROS_POR_KILOMETRO
    }

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

    // Usa una aproximación equirectangular local
    internal fun proyectarSobreTramo(posicion: Coordenada, origen: PuntoRecorrido, destino: PuntoRecorrido): Proyeccion {
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
