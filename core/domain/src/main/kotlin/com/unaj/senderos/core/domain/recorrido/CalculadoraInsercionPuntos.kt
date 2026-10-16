package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido

private const val METROS_POR_KILOMETRO = 1000.0

// El orden de la declaración es el orden dentro del mismo hueco
private enum class Zona { ANTES_DEL_INICIO, DENTRO_DEL_TRAMO, DESPUES_DEL_FINAL }

private data class Asignacion(val hueco: Int, val zona: Zona, val valorOrden: Double, val orden: Int, val punto: PuntoRecorrido)

object CalculadoraInsercionPuntos {

    // Insertar cada punto nuevo en el tramo más cercano del recorrido; en el mismo tramo se ordenan a lo largo del tramo.
    // El tramo i va del punto i al siguiente; en un circular el último tramo es el de cierre (último a primero).
    // En un recorrido LINEAL o IDA_Y_VUELTA nunca cambia el inicio ni el final: un punto antes del inicio se inserta como segundo vértice
    // y uno después del final como anteúltimo; varios en un extremo se ordenan por distancia al extremo, acercándose al final.
    // Con menos de dos puntos no hay tramos y los nuevos se agregan al final en orden de captura.
    fun insertar(
        puntos: List<PuntoRecorrido>,
        tipoRecorrido: TipoRecorrido,
        nuevos: List<PuntoRecorrido>
    ): List<PuntoRecorrido> {
        if (nuevos.isEmpty()) return puntos
        if (puntos.size < 2) return puntos + nuevos
        val esCircular = tipoRecorrido == TipoRecorrido.CIRCULAR
        val cantidadTramos = if (esCircular) puntos.size else puntos.size - 1
        val gruposPorHueco = nuevos
            .mapIndexed { orden, nuevo -> asignar(puntos, cantidadTramos, esCircular, orden, nuevo) }
            .groupBy { asignacion -> asignacion.hueco }
        val resultado = mutableListOf<PuntoRecorrido>()
        puntos.forEachIndexed { indice, punto ->
            resultado.add(punto)
            resultado.addAll(ordenar(gruposPorHueco[indice]))
        }
        return resultado
    }

    // Calcular para cada punto nuevo su distancia en metros al trazado (tramo más cercano, con cierre si es circular);
    // null si el recorrido está vacío y con un solo vértice se mide a ese vértice
    fun distanciasAlTrazadoMetros(
        puntos: List<PuntoRecorrido>,
        tipoRecorrido: TipoRecorrido,
        nuevos: List<PuntoRecorrido>
    ): List<Double?> {
        return nuevos.map { nuevo ->
            val posicion = Coordenada(nuevo.latitud, nuevo.longitud)
            when {
                puntos.isEmpty() -> null
                puntos.size == 1 -> CalculadoraRecorrido.distanciaEntre(posicion, puntos.first()) * METROS_POR_KILOMETRO
                else -> {
                    val cantidadTramos = if (tipoRecorrido == TipoRecorrido.CIRCULAR) puntos.size else puntos.size - 1
                    (0 until cantidadTramos).minOf { tramo -> proyectar(puntos, tramo, posicion).distanciaMetros }
                }
            }
        }
    }

    private fun asignar(
        puntos: List<PuntoRecorrido>,
        cantidadTramos: Int,
        esCircular: Boolean,
        orden: Int,
        nuevo: PuntoRecorrido
    ): Asignacion {
        val posicion = Coordenada(nuevo.latitud, nuevo.longitud)
        var mejorTramo = 0
        var mejorProyeccion = proyectar(puntos, 0, posicion)
        for (tramo in 1 until cantidadTramos) {
            val proyeccion = proyectar(puntos, tramo, posicion)
            if (proyeccion.distanciaMetros < mejorProyeccion.distanciaMetros) {
                mejorTramo = tramo
                mejorProyeccion = proyeccion
            }
        }
        val esTramoReal = !esCircular && !esDegenerado(puntos, mejorTramo)
        val estaAntesDelInicio = esTramoReal && mejorTramo == 0 && mejorProyeccion.fraccion == 0.0
        val estaDespuesDelFinal = esTramoReal && mejorTramo == cantidadTramos - 1 && mejorProyeccion.fraccion == 1.0
        return when {
            estaAntesDelInicio ->
                Asignacion(0, Zona.ANTES_DEL_INICIO, distanciaAlVertice(posicion, puntos.first()), orden, nuevo)
            estaDespuesDelFinal ->
                Asignacion(puntos.size - 2, Zona.DESPUES_DEL_FINAL, -distanciaAlVertice(posicion, puntos.last()), orden, nuevo)
            else -> Asignacion(mejorTramo, Zona.DENTRO_DEL_TRAMO, mejorProyeccion.fraccion, orden, nuevo)
        }
    }

    private fun esDegenerado(puntos: List<PuntoRecorrido>, tramo: Int): Boolean {
        val origen = puntos[tramo]
        val destino = puntos[(tramo + 1) % puntos.size]
        return origen.latitud == destino.latitud && origen.longitud == destino.longitud
    }

    private fun distanciaAlVertice(posicion: Coordenada, vertice: PuntoRecorrido): Double {
        return CalculadoraRecorrido.distanciaEntre(posicion, vertice) * METROS_POR_KILOMETRO
    }

    private fun proyectar(puntos: List<PuntoRecorrido>, tramo: Int, posicion: Coordenada): Proyeccion {
        val origen = puntos[tramo]
        val destino = puntos[(tramo + 1) % puntos.size]
        return CalculadoraSeguimiento.proyectarSobreTramo(posicion, origen, destino)
    }

    private fun ordenar(asignaciones: List<Asignacion>?): List<PuntoRecorrido> {
        return asignaciones.orEmpty()
            .sortedWith(
                compareBy<Asignacion> { asignacion -> asignacion.zona }
                    .thenBy { asignacion -> asignacion.valorOrden }
                    .thenBy { asignacion -> asignacion.orden }
            )
            .map { asignacion -> asignacion.punto }
    }
}
