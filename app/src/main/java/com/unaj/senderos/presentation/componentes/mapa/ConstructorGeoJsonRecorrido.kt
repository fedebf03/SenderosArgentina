package com.unaj.senderos.presentation.componentes.mapa

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.tieneCoordenadasValidas
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

fun PuntoRecorrido.tieneCoordenadasValidas(): Boolean {
    return Coordenada(latitud, longitud).tieneCoordenadasValidas()
}

object PropiedadesPunto {
    const val INDICE = "indice"
    const val ETIQUETA = "etiqueta"
    const val TIPO = "tipo"
    const val ROL = "rol"
    const val SELECCIONADO = "seleccionado"
    const val RUMBO = "rumbo"
}

object RolesPunto {
    const val INTERMEDIO = "intermedio"
    const val INICIO = "inicio"
    const val FIN = "fin"
    const val INICIO_Y_FIN = "inicioFin"
}

object ConstructorGeoJsonRecorrido {

    fun crearLinea(puntos: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): FeatureCollection {
        val puntosValidos = puntos.filter { punto -> punto.tieneCoordenadasValidas() }
        if (puntosValidos.size < 2) return FeatureCollection.fromFeatures(emptyList<Feature>())
        val coordenadas = puntosValidos.map { punto -> Point.fromLngLat(punto.longitud, punto.latitud) }
        val coordenadasConCierre = if (tipoRecorrido == TipoRecorrido.CIRCULAR) {
            coordenadas + coordenadas.first()
        } else {
            coordenadas
        }
        return FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(coordenadasConCierre)))
    }

    fun crearTrazadoCaminado(trazado: List<Coordenada>): FeatureCollection {
        val validas = trazado.filter { coordenada -> coordenada.tieneCoordenadasValidas() }
        if (validas.size < 2) return FeatureCollection.fromFeatures(emptyList<Feature>())
        val coordenadas = validas.map { coordenada -> Point.fromLngLat(coordenada.longitud, coordenada.latitud) }
        return FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(coordenadas)))
    }

    fun crearPuntos(
        puntos: List<PuntoRecorrido>,
        tipoRecorrido: TipoRecorrido,
        indiceSeleccionado: Int?
    ): FeatureCollection {
        val elementos = puntos.mapIndexedNotNull { indice, punto ->
            if (!punto.tieneCoordenadasValidas()) return@mapIndexedNotNull null
            val elemento = Feature.fromGeometry(Point.fromLngLat(punto.longitud, punto.latitud))
            elemento.addNumberProperty(PropiedadesPunto.INDICE, indice)
            elemento.addStringProperty(PropiedadesPunto.ETIQUETA, (indice + 1).toString())
            elemento.addStringProperty(PropiedadesPunto.TIPO, punto.tipo.name)
            elemento.addStringProperty(PropiedadesPunto.ROL, obtenerRol(indice, puntos.size, tipoRecorrido))
            elemento.addBooleanProperty(PropiedadesPunto.SELECCIONADO, indice == indiceSeleccionado)
            elemento
        }
        return FeatureCollection.fromFeatures(elementos)
    }

    fun crearPosicion(posicion: Coordenada?, rumboGrados: Float? = null): FeatureCollection {
        if (posicion == null || !posicion.tieneCoordenadasValidas()) return FeatureCollection.fromFeatures(emptyList<Feature>())
        val elemento = Feature.fromGeometry(Point.fromLngLat(posicion.longitud, posicion.latitud))
        if (rumboGrados != null && rumboGrados.isFinite()) {
            elemento.addNumberProperty(PropiedadesPunto.RUMBO, rumboGrados)
        }
        return FeatureCollection.fromFeature(elemento)
    }

    fun obtenerRol(indice: Int, cantidad: Int, tipoRecorrido: TipoRecorrido): String {
        return when {
            cantidad >= 2 && tipoRecorrido == TipoRecorrido.CIRCULAR && indice == 0 -> RolesPunto.INICIO_Y_FIN
            indice == 0 -> RolesPunto.INICIO
            indice == cantidad - 1 && cantidad >= 2 && tipoRecorrido != TipoRecorrido.CIRCULAR -> RolesPunto.FIN
            else -> RolesPunto.INTERMEDIO
        }
    }
}
