package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PuntoRecorrido

// Los puntos van junto con la selección porque eliminar o invertir corre la posición del punto elegido
data class EdicionRecorrido(val puntos: List<PuntoRecorrido>, val seleccion: Int?) {

    fun agregar(latitud: Double, longitud: Double): EdicionRecorrido {
        return copy(puntos = puntos + PuntoRecorrido(latitud = latitud, longitud = longitud))
    }

    fun reemplazar(indice: Int, transformar: (PuntoRecorrido) -> PuntoRecorrido): EdicionRecorrido {
        if (indice !in puntos.indices) return this
        return copy(puntos = puntos.mapIndexed { posicion, punto -> if (posicion == indice) transformar(punto) else punto })
    }

    fun mover(indice: Int, latitud: Double, longitud: Double): EdicionRecorrido {
        return reemplazar(indice) { punto -> punto.copy(latitud = latitud, longitud = longitud) }
    }

    fun eliminar(indice: Int): EdicionRecorrido {
        if (indice !in puntos.indices) return this
        val nuevaSeleccion = when {
            seleccion == null || seleccion == indice -> null
            seleccion > indice -> seleccion - 1
            else -> seleccion
        }
        return EdicionRecorrido(puntos.filterIndexed { posicion, _ -> posicion != indice }, nuevaSeleccion)
    }

    fun invertir(): EdicionRecorrido {
        if (puntos.size < 2) return this
        return EdicionRecorrido(CalculadoraRecorrido.invertirSentido(puntos), seleccion?.let { posicion -> puntos.size - 1 - posicion })
    }
}
