package com.unaj.senderos.core.ui.componentes.mapa

import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

internal object CapasMapaRecorrido {
    const val ID_FUENTE_LINEA = "fuente-linea-recorrido"
    const val ID_FUENTE_PUNTOS = "fuente-puntos-recorrido"
    const val ID_FUENTE_TRAZADO_CAMINADO = "fuente-trazado-caminado"
    private const val ID_CAPA_TRAZADO_CAMINADO = "capa-trazado-caminado"
    const val ID_FUENTE_PUNTOS_MARCADOS = "fuente-puntos-marcados"
    private const val ID_CAPA_PUNTOS_MARCADOS = "capa-puntos-marcados"
    private const val ID_CAPA_FOTOS_MARCADOS = "capa-fotos-marcados"
    const val ID_FUENTE_POSICION = "fuente-posicion-usuario"
    private const val ID_CAPA_POSICION_HALO = "capa-posicion-usuario-halo"
    private const val ID_CAPA_POSICION = "capa-posicion-usuario"
    private const val ID_CAPA_POSICION_FLECHA = "capa-posicion-usuario-flecha"
    const val ID_CAPA_LINEA = "capa-linea-recorrido"
    private const val ID_CAPA_FLECHAS = "capa-flechas-recorrido"
    private const val ID_CAPA_CIRCULOS = "capa-circulos-recorrido"
    private const val ID_CAPA_NUMEROS = "capa-numeros-recorrido"
    private const val ID_CAPA_TIPOS = "capa-tipos-recorrido"
    private const val ID_CAPA_ROLES = "capa-roles-recorrido"
    private const val ID_CAPA_FOTOS = "capa-fotos-recorrido"

    // Las capas se agregan en orden de dibujo, por lo que lo último que se agrega queda arriba
    fun agregarA(estilo: Style, paleta: PaletaMapa, densidad: Float, colorLinea: Int) {
        IconosMapa.crearTodos(paleta, densidad).forEach { (nombre, imagen) -> estilo.addImage(nombre, imagen) }

        estilo.addSource(GeoJsonSource(ID_FUENTE_LINEA))
        estilo.addSource(GeoJsonSource(ID_FUENTE_PUNTOS))
        estilo.addSource(GeoJsonSource(ID_FUENTE_TRAZADO_CAMINADO))
        estilo.addSource(GeoJsonSource(ID_FUENTE_PUNTOS_MARCADOS))
        estilo.addSource(GeoJsonSource(ID_FUENTE_POSICION))

        estilo.addLayer(
            LineLayer(ID_CAPA_LINEA, ID_FUENTE_LINEA).withProperties(
                PropertyFactory.lineColor(colorLinea),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
        estilo.addLayer(
            SymbolLayer(ID_CAPA_FLECHAS, ID_FUENTE_LINEA).withProperties(
                PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE),
                PropertyFactory.symbolSpacing(70f),
                PropertyFactory.iconImage(NombresImagenMapa.FLECHA),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )

        estilo.addLayer(
            LineLayer(ID_CAPA_TRAZADO_CAMINADO, ID_FUENTE_TRAZADO_CAMINADO).withProperties(
                PropertyFactory.lineColor(paleta.caminado),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )

        val estaSeleccionado = Expression.eq(Expression.get(PropiedadesPunto.SELECCIONADO), true)
        estilo.addLayer(
            CircleLayer(ID_CAPA_CIRCULOS, ID_FUENTE_PUNTOS).withProperties(
                PropertyFactory.circleRadius(
                    Expression.switchCase(estaSeleccionado, Expression.literal(15), Expression.literal(12))
                ),
                PropertyFactory.circleColor(
                    Expression.switchCase(
                        estaSeleccionado,
                        Expression.color(paleta.primario),
                        Expression.color(paleta.contenedorPrimario)
                    )
                ),
                PropertyFactory.circleStrokeColor(paleta.primario),
                PropertyFactory.circleStrokeWidth(2.5f)
            )
        )
        estilo.addLayer(
            SymbolLayer(ID_CAPA_NUMEROS, ID_FUENTE_PUNTOS).withProperties(
                PropertyFactory.textField(Expression.get(PropiedadesPunto.ETIQUETA)),
                PropertyFactory.textFont(arrayOf(ConfiguracionMapa.FUENTE_TEXTO)),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(
                    Expression.switchCase(
                        estaSeleccionado,
                        Expression.color(paleta.sobrePrimario),
                        Expression.color(paleta.sobreContenedorPrimario)
                    )
                ),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )
        )
        estilo.addLayer(
            SymbolLayer(ID_CAPA_TIPOS, ID_FUENTE_PUNTOS)
                .withFilter(Expression.neq(Expression.get(PropiedadesPunto.TIPO), "PASO"))
                .withProperties(
                    PropertyFactory.iconImage(
                        Expression.concat(Expression.literal(NombresImagenMapa.PREFIJO_TIPO), Expression.get(PropiedadesPunto.TIPO))
                    ),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_TOP),
                    PropertyFactory.iconOffset(arrayOf(0f, 14f)),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
        )
        estilo.addLayer(
            SymbolLayer(ID_CAPA_ROLES, ID_FUENTE_PUNTOS)
                .withFilter(Expression.neq(Expression.get(PropiedadesPunto.ROL), RolesPunto.INTERMEDIO))
                .withProperties(
                    PropertyFactory.iconImage(
                        Expression.concat(Expression.literal(NombresImagenMapa.PREFIJO_ROL), Expression.get(PropiedadesPunto.ROL))
                    ),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    PropertyFactory.iconOffset(arrayOf(0f, -14f)),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
        )
        estilo.addLayer(
            SymbolLayer(ID_CAPA_FOTOS, ID_FUENTE_PUNTOS)
                .withFilter(Expression.eq(Expression.get(PropiedadesPunto.FOTO), true))
                .withProperties(
                    PropertyFactory.iconImage(NombresImagenMapa.FOTO),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT),
                    PropertyFactory.iconOffset(arrayOf(8f, -8f)),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
        )
        // Los puntos marcados caminando quedan sobre el trazado y debajo de la posición del usuario
        estilo.addLayer(
            SymbolLayer(ID_CAPA_PUNTOS_MARCADOS, ID_FUENTE_PUNTOS_MARCADOS).withProperties(
                PropertyFactory.iconImage(
                    Expression.concat(Expression.literal(NombresImagenMapa.PREFIJO_TIPO), Expression.get(PropiedadesPunto.TIPO))
                ),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
        estilo.addLayer(
            SymbolLayer(ID_CAPA_FOTOS_MARCADOS, ID_FUENTE_PUNTOS_MARCADOS)
                .withFilter(Expression.eq(Expression.get(PropiedadesPunto.FOTO), true))
                .withProperties(
                    PropertyFactory.iconImage(NombresImagenMapa.FOTO),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT),
                    PropertyFactory.iconOffset(arrayOf(6f, -6f)),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
        )
        // La posición del usuario va arriba de todo. Sin rumbo es un halo con un punto y con rumbo una flecha
        val tieneRumbo = Expression.has(PropiedadesPunto.RUMBO)
        estilo.addLayer(
            CircleLayer(ID_CAPA_POSICION_HALO, ID_FUENTE_POSICION)
                .withFilter(Expression.not(tieneRumbo))
                .withProperties(
                    PropertyFactory.circleRadius(16f),
                    PropertyFactory.circleColor(paleta.primario),
                    PropertyFactory.circleOpacity(0.25f)
                )
        )
        estilo.addLayer(
            CircleLayer(ID_CAPA_POSICION, ID_FUENTE_POSICION)
                .withFilter(Expression.not(tieneRumbo))
                .withProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor(paleta.primario),
                    PropertyFactory.circleStrokeColor(paleta.sobrePrimario),
                    PropertyFactory.circleStrokeWidth(3f)
                )
        )
        estilo.addLayer(
            SymbolLayer(ID_CAPA_POSICION_FLECHA, ID_FUENTE_POSICION)
                .withFilter(tieneRumbo)
                .withProperties(
                    PropertyFactory.iconImage(NombresImagenMapa.FLECHA_NAVEGACION),
                    PropertyFactory.iconRotate(Expression.get(PropiedadesPunto.RUMBO)),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
        )
    }
}
