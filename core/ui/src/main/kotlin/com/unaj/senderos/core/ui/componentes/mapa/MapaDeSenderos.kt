package com.unaj.senderos.presentation.mapa

import com.unaj.senderos.presentation.componentes.BannerAviso
import androidx.compose.animation.AnimatedVisibility
import android.annotation.SuppressLint
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.presentation.tema.CelesteArgentino
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
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
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val TAG = "MAPA_SENDEROS"

private const val ID_FUENTE_LINEAS = "fuente-lineas-senderos"
private const val ID_FUENTE_INICIOS = "fuente-inicios-senderos"
private const val ID_CAPA_LINEAS = "capa-lineas-senderos"
private const val ID_CAPA_INICIOS = "capa-inicios-senderos"
private const val ID_CAPA_NOMBRES = "capa-nombres-senderos"

private const val PROPIEDAD_ID = "id"
private const val PROPIEDAD_NOMBRE = "nombre"
private const val PROPIEDAD_SELECCIONADO = "seleccionado"
private const val MARGEN_CONTROLES_DP = 40
private const val ALTO_TARJETA_DP = 120
private const val ZOOM_MI_UBICACION = 12.0

// Renderizar el mapa con los senderos, su selección y la ubicación del usuario
@Composable
fun MapaDeSenderos(
    senderos: List<Sendero>,
    idSeleccionado: String?,
    alSeleccionarSendero: (String?) -> Unit,
    mostrarUbicacion: Boolean,
    alPedirUbicacion: () -> Unit,
    modifier: Modifier = Modifier,
    estaSinConexion: Boolean = false,
    alMostrarAviso: (String) -> Unit = {}
) {
    val contexto = LocalContext.current
    val densidad = LocalDensity.current.density
    val esquemaDeColores = MaterialTheme.colorScheme
    val propietarioDelCiclo = LocalLifecycleOwner.current

    val vistaMapa = remember(propietarioDelCiclo) {
        MapLibre.getInstance(contexto.applicationContext)
        MapView(contexto, MapLibreMapOptions.createFromAttributes(contexto))
    }
    val controlador = remember(vistaMapa) {
        ControladorMapaDeSenderos(
            vistaMapa = vistaMapa,
            colores = ColoresMapaDeSenderos(
                linea = esquemaDeColores.primary.toArgb(),
                inicio = CelesteArgentino.toArgb(),
                borde = esquemaDeColores.surface.toArgb(),
                texto = esquemaDeColores.onSurface.toArgb()
            ),
            densidad = densidad
        )
    }

    SideEffect {
        controlador.actualizar(senderos, idSeleccionado, alSeleccionarSendero)
        controlador.mostrarUbicacion(mostrarUbicacion)
    }

    VincularCicloDeVidaMapa(vistaMapa, alIniciar = controlador::iniciar, alLiberar = controlador::liberar)

    Box(modifier = modifier) {
        AndroidView(factory = { vistaMapa }, modifier = Modifier.fillMaxSize())
        ControlesDeMapa(
            alAcercar = controlador::acercar,
            alAlejar = controlador::alejar,
            alIrAMiUbicacion = {
                when {
                    !mostrarUbicacion -> alPedirUbicacion()
                    !controlador.centrarEnUbicacion() -> {
                        alMostrarAviso("Todavía no se conoce tu ubicación")
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )
        AnimatedVisibility(
            visible = controlador.haFalladoLaCarga || estaSinConexion,
            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)
        ) {
            BannerAviso(
                texto = if (controlador.haFalladoLaCarga) {
                    "Mapa no disponible sin conexión"
                } else {
                    "Sin conexión: el mapa usa lo que ya se descargó"
                }
            )
        }
    }
}

// Renderizar los botones para acercar, alejar e ir a la ubicación del usuario
@Composable
private fun ControlesDeMapa(
    alAcercar: () -> Unit,
    alAlejar: () -> Unit,
    alIrAMiUbicacion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonDeMapa(Icons.Filled.Add, "Acercar", alAcercar)
        BotonDeMapa(Icons.Filled.Remove, "Alejar", alAlejar)
        BotonDeMapa(Icons.Filled.MyLocation, "Ir a mi ubicación", alIrAMiUbicacion)
    }
}

// Renderizar un botón flotante de los controles del mapa
@Composable
private fun BotonDeMapa(icono: ImageVector, descripcion: String, alTocar: () -> Unit) {
    SmallFloatingActionButton(
        onClick = alTocar,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(imageVector = icono, contentDescription = descripcion)
    }
}

// Colores ya convertidos a enteros ARGB, que es lo que esperan las capas de MapLibre
private data class ColoresMapaDeSenderos(val linea: Int, val inicio: Int, val borde: Int, val texto: Int)

private class ControladorMapaDeSenderos(
    private val vistaMapa: MapView,
    private val colores: ColoresMapaDeSenderos,
    private val densidad: Float
) {
    var haFalladoLaCarga by mutableStateOf(false)
        private set

    private var mapa: MapLibreMap? = null
    private var estilo: Style? = null
    private var senderos: List<Sendero> = emptyList()
    private var idSeleccionado: String? = null
    private var alSeleccionarSendero: (String?) -> Unit = {}
    private var idsEncuadrados: Set<String>? = null
    private var debeMostrarUbicacion = false

    private val escuchaFalloDeCarga = MapView.OnDidFailLoadingMapListener { descripcion ->
        Log.e(TAG, "Falló la carga del mapa: $descripcion")
        haFalladoLaCarga = true
    }

    private val escuchaClicEnMapa = MapLibreMap.OnMapClickListener { posicion ->
        alSeleccionarSendero(buscarSenderoTocado(posicion))
        true
    }

    // Iniciar el controlador: cargar el estilo del mapa y registrar sus escuchas
    fun iniciar() {
        vistaMapa.addOnDidFailLoadingMapListener(escuchaFalloDeCarga)
        vistaMapa.getMapAsync { mapaListo ->
            mapa = mapaListo
            mapaListo.uiSettings.setRotateGesturesEnabled(false)
            mapaListo.uiSettings.setTiltGesturesEnabled(false)
            mapaListo.addOnMapClickListener(escuchaClicEnMapa)
            mapaListo.setStyle(Style.Builder().fromUri(ConfiguracionMapa.URL_ESTILO)) { estiloCargado ->
                estilo = estiloCargado
                haFalladoLaCarga = false
                agregarFuentesYCapas(estiloCargado)
                refrescar()
                activarUbicacionSiCorresponde()
            }
        }
    }

    // Quitar los detectores para no retener el controlador desde la vista
    fun liberar() {
        vistaMapa.removeOnDidFailLoadingMapListener(escuchaFalloDeCarga)
        mapa?.removeOnMapClickListener(escuchaClicEnMapa)
        mapa = null
        estilo = null
    }

    // Actualizar los senderos, la selección y la acción de selección, y redibujar el mapa
    fun actualizar(nuevosSenderos: List<Sendero>, nuevoIdSeleccionado: String?, alSeleccionar: (String?) -> Unit) {
        val hayCambios = nuevosSenderos != senderos || nuevoIdSeleccionado != idSeleccionado
        val cambioLaSeleccion = nuevoIdSeleccionado != idSeleccionado
        senderos = nuevosSenderos
        idSeleccionado = nuevoIdSeleccionado
        alSeleccionarSendero = alSeleccionar
        if (hayCambios) refrescar()
        if (cambioLaSeleccion && nuevoIdSeleccionado != null) centrarEnSeleccionado(animar = true)
    }

    // Agregar al estilo las fuentes y capas que dibujan los senderos
    private fun agregarFuentesYCapas(estiloBase: Style) {
        estiloBase.addSource(GeoJsonSource(ID_FUENTE_LINEAS))
        estiloBase.addSource(GeoJsonSource(ID_FUENTE_INICIOS))

        val estaSeleccionado = Expression.eq(Expression.get(PROPIEDAD_SELECCIONADO), true)
        estiloBase.addLayer(
            LineLayer(ID_CAPA_LINEAS, ID_FUENTE_LINEAS).withProperties(
                PropertyFactory.lineColor(colores.linea),
                PropertyFactory.lineWidth(Expression.switchCase(estaSeleccionado, Expression.literal(7f), Expression.literal(4f))),
                PropertyFactory.lineOpacity(Expression.switchCase(estaSeleccionado, Expression.literal(1f), Expression.literal(0.75f))),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
        estiloBase.addLayer(
            CircleLayer(ID_CAPA_INICIOS, ID_FUENTE_INICIOS).withProperties(
                PropertyFactory.circleRadius(Expression.switchCase(estaSeleccionado, Expression.literal(12f), Expression.literal(9f))),
                PropertyFactory.circleColor(
                    Expression.switchCase(estaSeleccionado, Expression.color(colores.linea), Expression.color(colores.inicio))
                ),
                PropertyFactory.circleStrokeColor(colores.borde),
                PropertyFactory.circleStrokeWidth(3f)
            )
        )
        estiloBase.addLayer(
            SymbolLayer(ID_CAPA_NOMBRES, ID_FUENTE_INICIOS).withProperties(
                PropertyFactory.textField(Expression.get(PROPIEDAD_NOMBRE)),
                PropertyFactory.textFont(arrayOf(ConfiguracionMapa.FUENTE_TEXTO)),
                PropertyFactory.textSize(13f),
                PropertyFactory.textColor(colores.texto),
                PropertyFactory.textHaloColor(colores.borde),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                PropertyFactory.textOffset(arrayOf(0f, 1.1f))
            )
        )
    }

    // Refrescar los datos de las fuentes y reencuadrar la cámara cuando cambia el conjunto de senderos
    private fun refrescar() {
        val estiloActual = estilo ?: return
        // El seleccionado va último para que su línea se dibuje por encima de las demás
        val ordenados = senderos.sortedBy { sendero -> sendero.id == idSeleccionado }
        estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_LINEAS)?.setGeoJson(crearLineas(ordenados))
        estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_INICIOS)?.setGeoJson(crearInicios(ordenados))

        val idsActuales = senderos.map { sendero -> sendero.id }.toSet()
        if (idsActuales != idsEncuadrados) {
            // Al volver del detalle con un sendero elegido se retoma su encuadre en vez de mostrar todos
            if (idSeleccionado != null) centrarEnSeleccionado(animar = false) else encuadrar()
            idsEncuadrados = idsActuales
        }
    }

    // Crear la colección de líneas que dibuja el recorrido de cada sendero
    private fun crearLineas(senderosOrdenados: List<Sendero>): FeatureCollection {
        val lineas = senderosOrdenados.mapNotNull { sendero ->
            val coordenadas = sendero.recorrido
                .filter { punto -> punto.tieneCoordenadasValidas() }
                .map { punto -> Point.fromLngLat(punto.longitud, punto.latitud) }
            if (coordenadas.size < 2) return@mapNotNull null
            val coordenadasConCierre = if (sendero.tipoRecorrido == TipoRecorrido.CIRCULAR) {
                coordenadas + coordenadas.first()
            } else {
                coordenadas
            }
            Feature.fromGeometry(LineString.fromLngLats(coordenadasConCierre)).apply {
                addStringProperty(PROPIEDAD_ID, sendero.id)
                addBooleanProperty(PROPIEDAD_SELECCIONADO, sendero.id == idSeleccionado)
            }
        }
        return FeatureCollection.fromFeatures(lineas)
    }

    // Crear la colección de puntos que marca el inicio de cada sendero
    private fun crearInicios(senderosOrdenados: List<Sendero>): FeatureCollection {
        val inicios = senderosOrdenados.mapNotNull { sendero ->
            val inicio = sendero.recorrido.firstOrNull { punto -> punto.tieneCoordenadasValidas() } ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(inicio.longitud, inicio.latitud)).apply {
                addStringProperty(PROPIEDAD_ID, sendero.id)
                addStringProperty(PROPIEDAD_NOMBRE, sendero.nombre)
                addBooleanProperty(PROPIEDAD_SELECCIONADO, sendero.id == idSeleccionado)
            }
        }
        return FeatureCollection.fromFeatures(inicios)
    }

    // Mostrar u ocultar la ubicación del usuario en el mapa
    fun mostrarUbicacion(mostrar: Boolean) {
        debeMostrarUbicacion = mostrar
        activarUbicacionSiCorresponde()
    }

    // Solo se llama con el permiso ya otorgado: la pantalla decide cuándo pedirlo
    // Activar la ubicación del usuario en el mapa; solo se llama con el permiso ya otorgado: la pantalla decide cuándo pedirlo
    private fun activarUbicacionSiCorresponde() {
        val componente = mapa?.locationComponent ?: return
        val estiloActual = estilo ?: return
        if (!debeMostrarUbicacion || componente.isLocationComponentActivated) return
        componente.activateLocationComponent(
            LocationComponentActivationOptions.Builder(vistaMapa.context, estiloActual).build()
        )
        componente.isLocationComponentEnabled = true
        componente.renderMode = RenderMode.COMPASS
    }

    // Centrar el mapa en la ubicación del usuario y devolver si se pudo hacer
    fun centrarEnUbicacion(): Boolean {
        val mapaActual = mapa ?: return false
        if (!mapaActual.locationComponent.isLocationComponentActivated) return false
        val ubicacion = mapaActual.locationComponent.lastKnownLocation ?: return false
        mapaActual.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(ubicacion.latitude, ubicacion.longitude), ZOOM_MI_UBICACION),
            600
        )
        return true
    }

    // Acercar el mapa un nivel de zoom
    fun acercar() {
        mapa?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    // Alejar el mapa un nivel de zoom
    fun alejar() {
        mapa?.animateCamera(CameraUpdateFactory.zoomOut())
    }

    // Encuadrar todos los senderos una vez que la vista tiene su tamaño final
    private fun encuadrar() {
        // Encuadrar antes de que la vista tenga su tamaño final calcula la cámara para un lienzo vacío
        vistaMapa.doOnLayout { moverCamaraParaVerTodos() }
    }

    // Mover la cámara para que entren todos los senderos del mapa
    private fun moverCamaraParaVerTodos() {
        val mapaActual = mapa ?: return
        val coordenadas = senderos.flatMap { sendero -> sendero.recorrido }
            .filter { punto -> punto.tieneCoordenadasValidas() }
            .map { punto -> LatLng(punto.latitud, punto.longitud) }
        when {
            coordenadas.isEmpty() -> mapaActual.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(ConfiguracionMapa.LATITUD_INICIAL, ConfiguracionMapa.LONGITUD_INICIAL),
                    ConfiguracionMapa.ZOOM_INICIAL
                )
            )
            coordenadas.distinct().size == 1 -> mapaActual.moveCamera(
                CameraUpdateFactory.newLatLngZoom(coordenadas.first(), ConfiguracionMapa.ZOOM_UN_PUNTO)
            )
            else -> {
                val limites = LatLngBounds.Builder().includes(coordenadas).build()
                val margen = (ConfiguracionMapa.MARGEN_ENCUADRE_DP * densidad).toInt()
                // Margen extra a la derecha para que los botones de zoom no tapen los recorridos
                val margenConControles = margen + (MARGEN_CONTROLES_DP * densidad).toInt()
                mapaActual.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(limites, margen, margen, margenConControles, margen)
                )
            }
        }
    }

    // Centrar la cámara en el sendero seleccionado, con o sin animación
    private fun centrarEnSeleccionado(animar: Boolean) {
        vistaMapa.doOnLayout { moverCamaraAlSeleccionado(animar) }
    }

    // Mover la cámara para que entre el recorrido del sendero seleccionado
    private fun moverCamaraAlSeleccionado(animar: Boolean) {
        val mapaActual = mapa ?: return
        val coordenadas = senderos.firstOrNull { sendero -> sendero.id == idSeleccionado }
            ?.recorrido
            ?.filter { punto -> punto.tieneCoordenadasValidas() }
            ?.map { punto -> LatLng(punto.latitud, punto.longitud) }
            .orEmpty()
        if (coordenadas.isEmpty()) return
        val margen = (ConfiguracionMapa.MARGEN_ENCUADRE_DP * densidad).toInt()
        val margenConControles = margen + (MARGEN_CONTROLES_DP * densidad).toInt()
        val margenTarjeta = margen + (ALTO_TARJETA_DP * densidad).toInt()
        val actualizacion = if (coordenadas.distinct().size == 1) {
            CameraUpdateFactory.newLatLngZoom(coordenadas.first(), ConfiguracionMapa.ZOOM_UN_PUNTO)
        } else {
            val limites = LatLngBounds.Builder().includes(coordenadas).build()
            CameraUpdateFactory.newLatLngBounds(limites, margen, margen, margenConControles, margenTarjeta)
        }
        if (animar) mapaActual.animateCamera(actualizacion, 600) else mapaActual.moveCamera(actualizacion)
    }

    // Buscar el sendero tocado en una posición del mapa, o nulo si no hay ninguno
    private fun buscarSenderoTocado(posicion: LatLng): String? {
        val mapaActual = mapa ?: return null
        val pantalla = mapaActual.projection.toScreenLocation(posicion)
        val radio = ConfiguracionMapa.RADIO_CAPTURA_MARCADOR_DP * densidad
        val zona = RectF(pantalla.x - radio, pantalla.y - radio, pantalla.x + radio, pantalla.y + radio)
        val tocados = mapaActual.queryRenderedFeatures(zona, ID_CAPA_INICIOS, ID_CAPA_NOMBRES)
            .ifEmpty { mapaActual.queryRenderedFeatures(zona, ID_CAPA_LINEAS) }
        return tocados.firstNotNullOfOrNull { elemento -> elemento.getStringProperty(PROPIEDAD_ID) }
    }
}
