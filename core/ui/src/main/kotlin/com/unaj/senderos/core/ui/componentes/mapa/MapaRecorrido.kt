package com.unaj.senderos.core.ui.componentes.mapa

import com.unaj.senderos.core.ui.componentes.BannerAviso
import androidx.compose.animation.AnimatedVisibility
import android.annotation.SuppressLint
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.tieneCoordenadasValidas
import com.unaj.senderos.core.ui.tema.CelesteArgentino
import com.unaj.senderos.core.ui.componentes.mapa.CapasMapaRecorrido.ID_CAPA_LINEA
import com.unaj.senderos.core.ui.componentes.mapa.CapasMapaRecorrido.ID_FUENTE_LINEA
import com.unaj.senderos.core.ui.componentes.mapa.CapasMapaRecorrido.ID_FUENTE_POSICION
import com.unaj.senderos.core.ui.componentes.mapa.CapasMapaRecorrido.ID_FUENTE_PUNTOS
import com.unaj.senderos.core.ui.componentes.mapa.CapasMapaRecorrido.ID_FUENTE_PUNTOS_MARCADOS
import com.unaj.senderos.core.ui.componentes.mapa.CapasMapaRecorrido.ID_FUENTE_TRAZADO_CAMINADO
import kotlin.math.abs
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val TAG = "MAPA"

data class ModoNavegacion(
    val rumboGrados: Float?,
    val estaSiguiendo: Boolean,
    val alMoverConGesto: () -> Unit
)

private const val MARGEN_BRUJULA_DP = 12f
private const val MARGEN_INFERIOR_BRUJULA_DP = 44f

@Composable
fun MapaRecorrido(
    puntos: List<PuntoRecorrido>,
    tipoRecorrido: TipoRecorrido,
    modifier: Modifier = Modifier,
    esEditable: Boolean = false,
    indiceSeleccionado: Int? = null,
    sonGestosHabilitados: Boolean = true,
    usarModoTextura: Boolean = false,
    mostrarControlesDeZoom: Boolean = false,
    posicionUsuario: Coordenada? = null,
    modoNavegacion: ModoNavegacion? = null,
    puntoEnfocado: Coordenada? = null,
    claveEnfoque: Int = 0,
    trazadoCaminado: List<Coordenada> = emptyList(),
    puntosMarcados: List<PuntoRecorrido> = emptyList(),
    colorLinea: Color? = null,
    alAgregarPunto: (Double, Double) -> Unit = { _, _ -> },
    alMoverPunto: (Int, Double, Double) -> Unit = { _, _, _ -> },
    alSeleccionarPunto: (Int?) -> Unit = {},
    alDescargarMapa: (() -> Unit)? = null
) {
    val contexto = LocalContext.current
    val densidad = LocalDensity.current.density
    val esquemaDeColores = MaterialTheme.colorScheme
    val propietarioDelCiclo = LocalLifecycleOwner.current

    // MapLibre se inicializa recién cuando se muestra un mapa (la llamada es idempotente). La vista se recrea
    // si cambia el propietario del ciclo, para no reutilizar un MapView ya destruido
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
            sobreError = esquemaDeColores.onError.toArgb(),
            caminado = esquemaDeColores.tertiary.toArgb()
        )
        ControladorMapaRecorrido(vistaMapa, paleta, densidad, claveEnfoqueInicial = claveEnfoque)
    }

    SideEffect {
        controlador.actualizar(
            DatosMapa(
                puntos = puntos,
                tipoRecorrido = tipoRecorrido,
                indiceSeleccionado = indiceSeleccionado,
                esEditable = esEditable,
                sonGestosHabilitados = sonGestosHabilitados,
                posicionUsuario = posicionUsuario,
                modoNavegacion = modoNavegacion,
                puntoEnfocado = puntoEnfocado,
                claveEnfoque = claveEnfoque,
                trazadoCaminado = trazadoCaminado,
                puntosMarcados = puntosMarcados,
                colorLinea = colorLinea?.toArgb(),
                alAgregarPunto = alAgregarPunto,
                alMoverPunto = alMoverPunto,
                alSeleccionarPunto = alSeleccionarPunto
            )
        )
    }

    VincularCicloDeVidaMapa(vistaMapa, alIniciar = controlador::iniciar, alLiberar = controlador::liberar)

    Box(modifier = modifier) {
        AndroidView(factory = { vistaMapa }, modifier = Modifier.fillMaxSize())
        if (mostrarControlesDeZoom) {
            ControlesDeZoom(
                alAcercar = controlador::acercar,
                alAlejar = controlador::alejar,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )
        }
        AnimatedVisibility(
            visible = controlador.haFalladoLaCarga,
            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)
        ) {
            BannerAviso(
                texto = "Mapa no disponible sin conexión",
                textoAccion = if (alDescargarMapa != null) "Descargar el mapa para usar sin señal" else null,
                alAccionar = { alDescargarMapa?.invoke() }
            )
        }
    }
}

internal data class DatosMapa(
    val puntos: List<PuntoRecorrido> = emptyList(),
    val tipoRecorrido: TipoRecorrido = TipoRecorrido.LINEAL,
    val indiceSeleccionado: Int? = null,
    val esEditable: Boolean = false,
    val sonGestosHabilitados: Boolean = true,
    val posicionUsuario: Coordenada? = null,
    val modoNavegacion: ModoNavegacion? = null,
    val puntoEnfocado: Coordenada? = null,
    val claveEnfoque: Int = 0,
    val trazadoCaminado: List<Coordenada> = emptyList(),
    val puntosMarcados: List<PuntoRecorrido> = emptyList(),
    val colorLinea: Int? = null,
    val alAgregarPunto: (Double, Double) -> Unit = { _, _ -> },
    val alMoverPunto: (Int, Double, Double) -> Unit = { _, _, _ -> },
    val alSeleccionarPunto: (Int?) -> Unit = {}
)

private class ControladorMapaRecorrido(
    private val vistaMapa: MapView,
    private val paleta: PaletaMapa,
    private val densidad: Float,
    claveEnfoqueInicial: Int
) {
    var haFalladoLaCarga by mutableStateOf(false)
        private set

    private var mapa: MapLibreMap? = null
    private var estilo: Style? = null
    private var datos = DatosMapa()
    private var colorLineaAplicado: Int? = null
    private var fueEncuadrado = false
    private var seSuspendioSeguimiento = false
    private var puntosEncuadrados: List<PuntoRecorrido>? = null
    // En navegación el estado cambia con cada lectura del GPS y cada giro. Por lo tanto recordamos lo ya dibujado,
    // porque rehacer el GeoJSON de miles de puntos en el hilo principal gasta batería sin cambiar la pantalla
    private var puntosDibujados: List<PuntoRecorrido>? = null
    private var tipoRecorridoDibujado: TipoRecorrido? = null
    private var indiceSeleccionadoDibujado: Int? = null
    private var trazadoCaminadoDibujado: List<Coordenada>? = null
    private var puntosMarcadosDibujados: List<PuntoRecorrido>? = null
    private var claveEnfoqueAplicada = claveEnfoqueInicial

    private val arrastre = ArrastreMarcadores(vistaMapa, densidad, alRedibujar = ::refrescar)

    private val escuchaFalloDeCarga = MapView.OnDidFailLoadingMapListener { descripcion ->
        Log.e(TAG, "Falló la carga del mapa: $descripcion")
        haFalladoLaCarga = true
    }

    private val escuchaInicioDeMovimiento = MapLibreMap.OnCameraMoveStartedListener { razon ->
        val modo = datos.modoNavegacion
        if (razon == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE && modo != null && modo.estaSiguiendo) {
            Log.d(TAG, "El usuario movió el mapa: se sale del modo seguimiento")
            seSuspendioSeguimiento = true
            modo.alMoverConGesto()
        }
    }

    // RF4 - Editor de recorrido: en modo edición cada toque en el mapa agrega un punto al final del trazado
    private val escuchaClicEnMapa = MapLibreMap.OnMapClickListener { posicion ->
        if (datos.esEditable) {
            datos.alAgregarPunto(posicion.latitude, posicion.longitude)
            true
        } else {
            val indice = mapa?.projection?.let { proyeccion ->
                val enPantalla = proyeccion.toScreenLocation(posicion)
                arrastre.marcadorCercano(proyeccion, datos.puntos, enPantalla.x, enPantalla.y)
            }
            datos.alSeleccionarPunto(indice)
            indice != null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun iniciar() {
        vistaMapa.setOnTouchListener { _, evento -> manejarToque(evento) }
        vistaMapa.addOnDidFailLoadingMapListener(escuchaFalloDeCarga)
        vistaMapa.getMapAsync { mapaListo -> alEstarListoElMapa(mapaListo) }
    }

    // Se quitan los detectores para que la vista no siga reteniendo el controlador
    fun liberar() {
        vistaMapa.setOnTouchListener(null)
        vistaMapa.removeOnDidFailLoadingMapListener(escuchaFalloDeCarga)
        mapa?.removeOnMapClickListener(escuchaClicEnMapa)
        mapa?.removeOnCameraMoveStartedListener(escuchaInicioDeMovimiento)
        mapa = null
        estilo = null
    }

    fun actualizar(nuevosDatos: DatosMapa) {
        val hayCambios = nuevosDatos.puntos != datos.puntos ||
            nuevosDatos.trazadoCaminado != datos.trazadoCaminado ||
            nuevosDatos.puntosMarcados != datos.puntosMarcados ||
            nuevosDatos.colorLinea != datos.colorLinea ||
            nuevosDatos.tipoRecorrido != datos.tipoRecorrido ||
            nuevosDatos.indiceSeleccionado != datos.indiceSeleccionado ||
            nuevosDatos.esEditable != datos.esEditable ||
            nuevosDatos.sonGestosHabilitados != datos.sonGestosHabilitados ||
            nuevosDatos.posicionUsuario != datos.posicionUsuario ||
            nuevosDatos.modoNavegacion?.rumboGrados != datos.modoNavegacion?.rumboGrados ||
            nuevosDatos.modoNavegacion?.estaSiguiendo != datos.modoNavegacion?.estaSiguiendo ||
            (nuevosDatos.modoNavegacion == null) != (datos.modoNavegacion == null)
        datos = nuevosDatos
        // Si el seguimiento ya se apagó en el estado, la suspensión local por el gesto deja de hacer falta
        if (nuevosDatos.modoNavegacion?.estaSiguiendo != true) seSuspendioSeguimiento = false
        arrastre.olvidarSiNoArrastra()
        if (hayCambios && estilo != null) refrescar()
        aplicarEnfoque()
    }

    // Se aplica una sola vez por cada clave nueva. De lo contrario cada recomposición movería la cámara
    private fun aplicarEnfoque() {
        val coordenada = datos.puntoEnfocado ?: return
        val mapaActual = mapa ?: return
        if (estilo == null || datos.claveEnfoque == claveEnfoqueAplicada) return
        if (!coordenada.tieneCoordenadasValidas()) return
        claveEnfoqueAplicada = datos.claveEnfoque
        mapaActual.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(coordenada.latitud, coordenada.longitud),
                ConfiguracionMapa.ZOOM_PUNTO_DE_INTERES
            )
        )
    }

    fun acercar() {
        mapa?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    fun alejar() {
        mapa?.animateCamera(CameraUpdateFactory.zoomOut())
    }

    private fun alEstarListoElMapa(mapaListo: MapLibreMap) {
        mapa = mapaListo
        aplicarGestos()
        mapaListo.addOnMapClickListener(escuchaClicEnMapa)
        mapaListo.addOnCameraMoveStartedListener(escuchaInicioDeMovimiento)
        mapaListo.setStyle(Style.Builder().fromUri(ConfiguracionMapa.URL_ESTILO)) { estiloCargado ->
            estilo = estiloCargado
            olvidarFuentesDibujadas()
            haFalladoLaCarga = false
            val colorLinea = datos.colorLinea ?: paleta.primario
            colorLineaAplicado = colorLinea
            CapasMapaRecorrido.agregarA(estiloCargado, paleta, densidad, colorLinea)
            refrescar()
        }
    }

    private fun refrescar() {
        val estiloActual = estilo ?: return
        val puntosVisibles = arrastre.puntosEnArrastre ?: datos.puntos
        val colorLineaDeseado = datos.colorLinea ?: paleta.primario
        if (colorLineaDeseado != colorLineaAplicado) {
            estiloActual.getLayerAs<LineLayer>(ID_CAPA_LINEA)?.setProperties(PropertyFactory.lineColor(colorLineaDeseado))
            colorLineaAplicado = colorLineaDeseado
        }
        val cambioElRecorrido = puntosVisibles !== puntosDibujados || datos.tipoRecorrido != tipoRecorridoDibujado
        if (cambioElRecorrido) {
            estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_LINEA)
                ?.setGeoJson(ConstructorGeoJsonRecorrido.crearLinea(puntosVisibles, datos.tipoRecorrido))
        }
        if (cambioElRecorrido || datos.indiceSeleccionado != indiceSeleccionadoDibujado) {
            estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_PUNTOS)
                ?.setGeoJson(
                    ConstructorGeoJsonRecorrido.crearPuntos(puntosVisibles, datos.tipoRecorrido, datos.indiceSeleccionado)
                )
        }
        puntosDibujados = puntosVisibles
        tipoRecorridoDibujado = datos.tipoRecorrido
        indiceSeleccionadoDibujado = datos.indiceSeleccionado
        if (datos.puntosMarcados != puntosMarcadosDibujados) {
            estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_PUNTOS_MARCADOS)
                ?.setGeoJson(ConstructorGeoJsonRecorrido.crearPuntosMarcados(datos.puntosMarcados))
            puntosMarcadosDibujados = datos.puntosMarcados
        }
        if (datos.trazadoCaminado !== trazadoCaminadoDibujado) {
            estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_TRAZADO_CAMINADO)
                ?.setGeoJson(ConstructorGeoJsonRecorrido.crearTrazadoCaminado(datos.trazadoCaminado))
            trazadoCaminadoDibujado = datos.trazadoCaminado
        }
        estiloActual.getSourceAs<GeoJsonSource>(ID_FUENTE_POSICION)
            ?.setGeoJson(ConstructorGeoJsonRecorrido.crearPosicion(datos.posicionUsuario, datos.modoNavegacion?.rumboGrados))
        if (!arrastre.estaArrastrando) aplicarGestos()
        val estaSiguiendoAlUsuario = seguirAlUsuario()
        // En navegación el trazado caminado crece con cada lectura y reencuadrar pisaría lo que el usuario movió.
        // Por lo tanto la lista a encuadrar solo se arma cuando puede usarse
        val puedeEncuadrar = !fueEncuadrado || (!datos.esEditable && datos.modoNavegacion == null)
        val puntosAEncuadrar = if (puedeEncuadrar) {
            datos.puntos + datos.trazadoCaminado.map { coordenada ->
                PuntoRecorrido(latitud = coordenada.latitud, longitud = coordenada.longitud)
            }
        } else {
            emptyList()
        }
        val debeEncuadrar = puedeEncuadrar && (!fueEncuadrado || puntosEncuadrados != puntosAEncuadrar)
        if (debeEncuadrar && !arrastre.estaArrastrando && !estaSiguiendoAlUsuario) {
            encuadrar(puntosAEncuadrar)
            fueEncuadrado = true
            puntosEncuadrados = puntosAEncuadrar
        }
        aplicarEnfoque()
    }

    // Cuando el estilo se carga de nuevo las fuentes empiezan vacías, por eso se olvida lo dibujado
    private fun olvidarFuentesDibujadas() {
        puntosDibujados = null
        tipoRecorridoDibujado = null
        indiceSeleccionadoDibujado = null
        trazadoCaminadoDibujado = null
        puntosMarcadosDibujados = null
    }

    // RF5 - Seguimiento: la cámara acompaña al usuario con su rumbo hasta que mueva el mapa con el dedo
    private fun seguirAlUsuario(): Boolean {
        val modo = datos.modoNavegacion ?: return false
        val posicion = datos.posicionUsuario ?: return false
        val mapaActual = mapa ?: return false
        if (!modo.estaSiguiendo || seSuspendioSeguimiento || !posicion.latitud.isFinite() || !posicion.longitud.isFinite()) return false
        val rumbo = modo.rumboGrados?.toDouble() ?: mapaActual.cameraPosition.bearing
        val camara = CameraPosition.Builder()
            .target(LatLng(posicion.latitud, posicion.longitud))
            .zoom(ConfiguracionMapa.ZOOM_NAVEGACION)
            .bearing(rumbo)
            .tilt(ConfiguracionMapa.INCLINACION_NAVEGACION_GRADOS)
            .padding(0.0, vistaMapa.height * ConfiguracionMapa.FRACCION_MARGEN_SUPERIOR_NAVEGACION, 0.0, 0.0)
            .build()
        mapaActual.easeCamera(
            CameraUpdateFactory.newCameraPosition(camara),
            ConfiguracionMapa.DURACION_ANIMACION_NAVEGACION_MILISEGUNDOS
        )
        fueEncuadrado = true
        return true
    }

    private fun aplicarGestos() {
        val ajustes = mapa?.uiSettings ?: return
        ajustes.setAllGesturesEnabled(datos.sonGestosHabilitados)
        // La brújula va abajo a la izquierda (encima del logo de MapLibre) para no chocar con los datos de arriba
        ajustes.setCompassGravity(Gravity.BOTTOM or Gravity.START)
        ajustes.setCompassMargins(
            (MARGEN_BRUJULA_DP * densidad).toInt(), 0, 0, (MARGEN_INFERIOR_BRUJULA_DP * densidad).toInt()
        )
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
            // Sin esto un contenedor con desplazamiento vertical se queda con los gestos del mapa
            vistaMapa.parent?.requestDisallowInterceptTouchEvent(true)
        }
        val mapaActual = mapa
        if (!datos.esEditable || estilo == null || mapaActual == null) return false
        return arrastre.manejar(evento, mapaActual, datos)
    }
}
