package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido

// Los puntos van junto con la selección porque eliminar, insertar o invertir corre la posición del punto elegido
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

    // Insertar el punto medio del tramo que termina en el índice dado, para afinar un tramo largo; antes del primero no hace nada
    fun insertarAntes(indice: Int): EdicionRecorrido {
        val anterior = puntos.getOrNull(indice - 1) ?: return this
        val siguiente = puntos.getOrNull(indice) ?: return this
        val puntoMedio = PuntoRecorrido(
            latitud = (anterior.latitud + siguiente.latitud) / 2,
            longitud = (anterior.longitud + siguiente.longitud) / 2
        )
        val nuevaSeleccion = if (seleccion != null && seleccion >= indice) seleccion + 1 else seleccion
        return EdicionRecorrido(puntos.take(indice) + puntoMedio + puntos.drop(indice), nuevaSeleccion)
    }

    // Insertar un punto de interés en el tramo más cercano, sin cambiar el inicio ni el final, igual que los puntos capturados caminando.
    // La selección se suelta porque el punto elegido puede correrse de lugar
    fun insertarPuntoDeInteres(punto: PuntoRecorrido, tipoRecorrido: TipoRecorrido): EdicionRecorrido {
        return EdicionRecorrido(CalculadoraInsercionPuntos.insertar(puntos, tipoRecorrido, listOf(punto)), seleccion = null)
    }

    fun invertir(): EdicionRecorrido {
        if (puntos.size < 2) return this
        return EdicionRecorrido(CalculadoraRecorrido.invertirSentido(puntos), seleccion?.let { posicion -> puntos.size - 1 - posicion })
    }
}
