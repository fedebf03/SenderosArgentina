package com.unaj.senderos.presentation.mapa

import android.annotation.SuppressLint
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.presentation.tema.CelesteArgentino
import kotlin.math.abs
import kotlin.math.hypot
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val TAG = "MAPA"

private const val ID_FUENTE_LINEA = "fuente-linea-recorrido"
private const val ID_FUENTE_PUNTOS = "fuente-puntos-recorrido"
private const val ID_CAPA_LINEA = "capa-linea-recorrido"
private const val ID_CAPA_FLECHAS = "capa-flechas-recorrido"
private const val ID_CAPA_CIRCULOS = "capa-circulos-recorrido"
private const val ID_CAPA_NUMEROS = "capa-numeros-recorrido"
private const val ID_CAPA_TIPOS = "capa-tipos-recorrido"
private const val ID_CAPA_ROLES = "capa-roles-recorrido"

@Composable
fun MapaRecorrido(
    puntos: List<PuntoRecorrido>,
    tipoRecorrido: TipoRecorrido,
    modifier: Modifier = Modifier,
    esEditable: Boolean = false,
    indiceSeleccionado: Int? = null,
    sonGestosHabilitados: Boolean = true,
    usarModoTextura: Boolean = false,
    alAgregarPunto: (Double, Double) -> Unit = { _, _ -> },
    alMoverPunto: (Int, Double, Double) -> Unit = { _, _, _ -> },
    alSeleccionarPunto: (Int?) -> Unit = {}
) {
    val contexto = LocalContext.current
    val densidad = LocalDensity.current.density
    val esquemaDeColores = MaterialTheme.colorScheme
    val propietarioDelCiclo = LocalLifecycleOwner.current

    // MapLibre se inicializa de forma perezosa, solo cuando se muestra un mapa (la llamada es idempotente)
    // Se recrea si cambia el propietario del ciclo para no reutilizar un MapView ya destruido
    val vistaMapa = remember(propietarioDelCiclo, usarModoTextura) {
        MapLibre.getInstance(contexto.applicationContext)
        MapView(contexto, MapLibreMapOptions.createFromAttributes(contexto).textureMode(usarModoTextura))
    }
    val controlador = remember(vistaMapa) {
        val paleta = PaletaMapa(
            primario = esquemaDeColores.primary.toArgb(),
            sobrePrimario = esquemaDeColores.onPrimary.toArgb(),
            contenedorPrimario = CelesteArgentino.toArgb(),
            sobreContenedorPrimario = esquemaDeColores.onPrimaryContainer.toArgb(),
            error = esquemaDeColores.error.toArgb(),
            sobreError = esquemaDeColores.onError.toArgb()
        )
        ControladorMapaRecorrido(vistaMapa, paleta, densidad)
    }

    SideEffect {
        controlador.actualizar(
            DatosMapa(
                puntos = puntos,
                tipoRecorrido = tipoRecorrido,
                indiceSeleccionado = indiceSeleccionado,
                esEditable = esEditable,
                sonGestosHabilitados = sonGestosHabilitados,
                alAgregarPunto = alAgregarPunto,
                alMoverPunto = alMoverPunto,
                alSeleccionarPunto = alSeleccionarPunto
            )
        )
    }

    VincularCicloDeVidaMapa(vistaMapa, alIniciar = controlador::iniciar, alLiberar = controlador::liberar)

    Box(modifier = modifier) {
        AndroidView(factory = { vistaMapa }, modifier = Modifier.fillMaxSize())
        if (controlador.haFalladoLaCarga) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mapa no disponible sin conexión",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

private data class DatosMapa(
    val puntos: List<PuntoRecorrido> = emptyList(),
    val tipoRecorrido: TipoRecorrido = TipoRecorrido.LINEAL,
    val indiceSeleccionado: Int? = null,
    val esEditable: Boolean = false,
    val sonGestosHabilitados: Boolean = true,
    val alAgregarPunto: (Double, Double) -> Unit = { _, _ -> },
    val alMoverPunto: (Int, Double, Double) -> Unit = { _, _, _ -> },
    val alSeleccionarPunto: (Int?) -> Unit = {}
)

private class ControladorMapaRecorrido(
    private val vistaMapa: MapView,
    private val paleta: PaletaMapa,
    private val densidad: Float
) {
    var haFalladoLaCarga by mutableStateOf(false)
        private set

    private var mapa: MapLibreMap? = null
    private var estilo: Style? = null
    private var datos = DatosMapa()
    private var fueEncuadrado = false
    private var puntosEncuadrados: List<PuntoRecorrido>? = null

    private var indiceArrastrado: Int? = null
    private var puntosEnArrastre: List<PuntoRecorrido>? = null
    private var posicionInicialToque = PointF()
    private var haySuperadoUmbral = false

    private val escuchaFalloDeCarga = MapView.OnDidFailLoadingMapListener { descripcion ->
        Log.e(TAG, "Falló la carga del mapa: $descripcion")
        haFalladoLaCarga = true
    }

    private val escuchaClicEnMapa = MapLibreMap.OnMapClickListener { posicion ->
        if (datos.esEditable) {
            datos.alAgregarPunto(posicion.latitude, posicion.longitude)
            true
        } else {
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun iniciar() {
        vistaMapa.setOnTouchListener { _, evento -> manejarToque(evento) }
        vistaMapa.addOnDidFailLoadingMapListener(escuchaFalloDeCarga)
        vistaMapa.getMapAsync { mapaListo -> alEstarListoElMapa(mapaListo) }
    }

    // Quitar los detectores para no retener el controlador desde la vista
    fun liberar() {
        vistaMapa.setOnTouchListener(null)
        vistaMapa.removeOnDidFailLoadingMapListener(escuchaFalloDeCarga)
        mapa?.removeOnMapClickListener(escuchaClicEnMapa)
        mapa = null
        estilo = null
    }

    fun actualizar(nuevosDatos: DatosMapa) {
        val hayCambios = nuevosDatos.puntos != datos.puntos ||
            nuevosDatos.tipoRecorrido != datos.tipoRecorrido ||
            nuevosDatos.indiceSeleccionado != datos.indiceSeleccionado ||
            nuevosDatos.esEditable != datos.esEditable ||
            nuevosDatos.sonGestosHabilitados != datos.sonGestosHabilitados
        datos = nuevosDatos
        if (indiceArrastrado == null) puntosEnArrastre = null
        if (hayCambios && estilo != null) refrescar()
    }

    private fun alEstarListoElMapa(mapaListo: MapLibreMap) {
        mapa = mapaListo
        aplicarGestos()
        mapaListo.addOnMapClickListener(escuchaClicEnMapa)
        mapaListo.setStyle(Style.Builder().fromUri(ConfiguracionMapa.URL_ESTILO)) { estiloCargado ->
            estilo = estiloCargado
            haFalladoLaCarga = false
            agregarImagenesFuentesYCapas(estiloCargado)
            refrescar()
        }
    }

    private fun agregarImagenesFuentesYCapas(estiloBase: Style) {
        IconosMapa.crearTodos(paleta, densidad).forEach { (nombre, imagen) -> estiloBase.addImage(nombre, imagen) }

        estiloBase.addSource(GeoJsonSource(ID_FUENTE_LINEA))
        estiloBase.addSource(GeoJsonSource(ID_FUENTE_PUNTOS))

        estiloBase.addLayer(
            LineLayer(ID_CAPA_LINEA, ID_FUENTE_LINEA).withProperties(
                PropertyFactory.lineColor(paleta.primario),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
        estiloBase.addLayer(
            SymbolLayer(ID_CAPA_FLECHAS, ID_FUENTE_LINEA).withProperties(
                PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE),
                PropertyFactory.symbolSpacing(70f),
                PropertyFactory.iconImage(NombresImagenMapa.FLECHA),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )

        val estaSeleccionado = Expression.eq(Expression.get(PropiedadesPunto.SELECCIONADO), true)
        estiloBase.addLayer(
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
        estiloBase.addLayer(
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
        estiloBase.addLayer(
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
        estiloBase.addLayer(
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
    }

    private fun refrescar() {
        val estiloActual = estilo ?: return
        val puntosVisibles = puntosEnArrastre ?: datos.puntos
        estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_LINEA)
            ?.setGeoJson(ConstructorGeoJsonRecorrido.crearLinea(puntosVisibles, datos.tipoRecorrido))
        estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_PUNTOS)
            ?.setGeoJson(
                ConstructorGeoJsonRecorrido.crearPuntos(puntosVisibles, datos.tipoRecorrido, datos.indiceSeleccionado)
            )
        if (indiceArrastrado == null) aplicarGestos()
        val debeEncuadrar = !fueEncuadrado || (!datos.esEditable && puntosEncuadrados != datos.puntos)
        if (debeEncuadrar && indiceArrastrado == null) {
            encuadrar(datos.puntos)
            fueEncuadrado = true
            puntosEncuadrados = datos.puntos
        }
    }

    private fun aplicarGestos() {
        val ajustes = mapa?.uiSettings ?: return
        ajustes.setAllGesturesEnabled(datos.sonGestosHabilitados)
        ajustes.setRotateGesturesEnabled(false)
        ajustes.setTiltGesturesEnabled(false)
    }

    private fun encuadrar(todosLosPuntos: List<PuntoRecorrido>) {
        val mapaActual = mapa ?: return
        val puntos = todosLosPuntos.filter { punto -> punto.tieneCoordenadasValidas() }
        if (puntos.isEmpty()) {
            mapaActual.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(ConfiguracionMapa.LATITUD_INICIAL, ConfiguracionMapa.LONGITUD_INICIAL),
                    ConfiguracionMapa.ZOOM_INICIAL
                )
            )
            return
        }
        val primero = puntos.first()
        val sonCasiIguales = puntos.all { punto ->
            abs(punto.latitud - primero.latitud) < 1e-6 && abs(punto.longitud - primero.longitud) < 1e-6
        }
        if (sonCasiIguales) {
            mapaActual.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(primero.latitud, primero.longitud), ConfiguracionMapa.ZOOM_UN_PUNTO)
            )
            return
        }
        val limites = LatLngBounds.Builder()
            .includes(puntos.map { punto -> LatLng(punto.latitud, punto.longitud) })
            .build()
        val margen = (ConfiguracionMapa.MARGEN_ENCUADRE_DP * densidad).toInt()
        mapaActual.moveCamera(CameraUpdateFactory.newLatLngBounds(limites, margen))
    }

    private fun manejarToque(evento: MotionEvent): Boolean {
        if (evento.actionMasked == MotionEvent.ACTION_DOWN && datos.sonGestosHabilitados) {
            // Evitar que un contenedor con desplazamiento vertical robe los gestos del mapa
            vistaMapa.parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (!datos.esEditable || estilo == null) return false
        return when (evento.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val indice = buscarMarcadorCercano(evento.x, evento.y)
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
                arrastrar(indice, evento)
                true
            }
            MotionEvent.ACTION_UP -> terminarToque(fueCancelado = false)
            MotionEvent.ACTION_CANCEL -> terminarToque(fueCancelado = true)
            else -> indiceArrastrado != null
        }
    }

    private fun arrastrar(indice: Int, evento: MotionEvent) {
        val proyeccion = mapa?.projection ?: return
        if (!haySuperadoUmbral) {
            val umbral = ViewConfiguration.get(vistaMapa.context).scaledTouchSlop
            val desplazamiento = hypot(evento.x - posicionInicialToque.x, evento.y - posicionInicialToque.y)
            if (desplazamiento <= umbral) return
            haySuperadoUmbral = true
            mapa?.uiSettings?.setAllGesturesEnabled(false)
        }
        val coordenada = proyeccion.fromScreenLocation(PointF(evento.x, evento.y))
        puntosEnArrastre = datos.puntos.mapIndexed { posicion, punto ->
            if (posicion == indice) punto.copy(latitud = coordenada.latitude, longitud = coordenada.longitude) else punto
        }
        refrescar()
    }

    private fun terminarToque(fueCancelado: Boolean): Boolean {
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
        aplicarGestos()
        refrescar()
        return true
    }

    private fun buscarMarcadorCercano(x: Float, y: Float): Int? {
        val proyeccion = mapa?.projection ?: return null
        val radioMaximo = ConfiguracionMapa.RADIO_CAPTURA_MARCADOR_DP * densidad
        var mejorIndice: Int? = null
        var mejorDistancia = Float.MAX_VALUE
        datos.puntos.forEachIndexed { indice, punto ->
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
}
