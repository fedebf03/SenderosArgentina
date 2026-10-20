package com.unaj.senderos.core.ui.componentes.mapa

import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.tieneCoordenadasValidas
import kotlin.math.hypot
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Projection

// RF4 - Editor de recorrido: un toque selecciona el marcador y arrastrarlo más allá del umbral lo mueve.
// Mientras dura el arrastre el mapa dibuja puntosEnArrastre en lugar de los puntos del estado
internal class ArrastreMarcadores(
    private val vistaMapa: MapView,
    private val densidad: Float,
    private val alRedibujar: () -> Unit
) {
    var puntosEnArrastre: List<PuntoRecorrido>? = null
        private set

    private var indiceArrastrado: Int? = null
    private var posicionInicialToque = PointF()
    private var haySuperadoUmbral = false

    val estaArrastrando: Boolean
        get() = indiceArrastrado != null

    fun olvidarSiNoArrastra() {
        if (indiceArrastrado == null) puntosEnArrastre = null
    }

    fun manejar(evento: MotionEvent, mapa: MapLibreMap, datos: DatosMapa): Boolean {
        return when (evento.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val indice = marcadorCercano(mapa.projection, datos.puntos, evento.x, evento.y)
                if (indice == null) {
                    false
                } else {
                    indiceArrastrado = indice
                    posicionInicialToque = PointF(evento.x, evento.y)
                    haySuperadoUmbral = false
                    true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val indice = indiceArrastrado ?: return false
                arrastrar(indice, evento, mapa, datos.puntos)
                true
            }
            MotionEvent.ACTION_UP -> terminar(fueCancelado = false, datos)
            MotionEvent.ACTION_CANCEL -> terminar(fueCancelado = true, datos)
            else -> indiceArrastrado != null
        }
    }

    fun marcadorCercano(proyeccion: Projection, puntos: List<PuntoRecorrido>, x: Float, y: Float): Int? {
        val radioMaximo = ConfiguracionMapa.RADIO_CAPTURA_MARCADOR_DP * densidad
        var mejorIndice: Int? = null
        var mejorDistancia = Float.MAX_VALUE
        puntos.forEachIndexed { indice, punto ->
            if (!punto.tieneCoordenadasValidas()) return@forEachIndexed
            val posicion = proyeccion.toScreenLocation(LatLng(punto.latitud, punto.longitud))
            val distancia = hypot(posicion.x - x, posicion.y - y)
            if (distancia <= radioMaximo && distancia < mejorDistancia) {
                mejorDistancia = distancia
                mejorIndice = indice
            }
        }
        return mejorIndice
    }

    private fun arrastrar(indice: Int, evento: MotionEvent, mapa: MapLibreMap, puntos: List<PuntoRecorrido>) {
        if (!haySuperadoUmbral) {
            val umbral = ViewConfiguration.get(vistaMapa.context).scaledTouchSlop
            val desplazamiento = hypot(evento.x - posicionInicialToque.x, evento.y - posicionInicialToque.y)
            if (desplazamiento <= umbral) return
            haySuperadoUmbral = true
            mapa.uiSettings.setAllGesturesEnabled(false)
        }
        val coordenada = mapa.projection.fromScreenLocation(PointF(evento.x, evento.y))
        puntosEnArrastre = puntos.mapIndexed { posicion, punto ->
            if (posicion == indice) punto.copy(latitud = coordenada.latitude, longitud = coordenada.longitude) else punto
        }
        alRedibujar()
    }

    // Los gestos del mapa se vuelven a habilitar al redibujar, dentro de aplicarGestos
    private fun terminar(fueCancelado: Boolean, datos: DatosMapa): Boolean {
        val indice = indiceArrastrado ?: return false
        if (!fueCancelado) {
            val destino = puntosEnArrastre?.getOrNull(indice)
            if (haySuperadoUmbral && destino != null) {
                datos.alMoverPunto(indice, destino.latitud, destino.longitud)
            } else {
                datos.alSeleccionarPunto(indice)
            }
        } else {
            puntosEnArrastre = null
        }
        indiceArrastrado = null
        haySuperadoUmbral = false
        alRedibujar()
        return true
    }
}
