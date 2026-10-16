package com.unaj.senderos.core.data.mapas

import android.content.Context
import android.util.Log
import com.unaj.senderos.core.domain.recorrido.CalculadoraCorredor
import com.unaj.senderos.core.domain.configuracion.ConfiguracionEstiloMapa
import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.domain.model.Rectangulo
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.port.GestorMapasSinConexion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.offline.OfflineGeometryRegionDefinition
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionDefinition
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "MAPAS_SIN_CONEXION"

// La firma es nula si no hay región
private data class InstantaneaRegion(
    val firma: String?,
    val progreso: ProgresoRegion?,
    val mensajeError: String?,
    val esperandoConexion: Boolean = false,
    val erroresSeguidos: Int = 0
)

private class RegionCargada(
    val region: OfflineRegion,
    val metadatos: MetadatosRegion,
    val progreso: ProgresoRegion
)

/**
 * Gestiona los mapas sin conexión con el OfflineManager de MapLibre.
 *
 * Todas las llamadas a MapLibre y todos sus callbacks ocurren en el hilo principal. El límite de teselas
 * (setOfflineMapboxTileCountLimit y mapboxTileCountLimitExceeded) solo aplica al servidor de Mapbox; con
 * OpenFreeMap no hay tope de teselas en el cliente y el tamaño se acota con el corredor de 1 km y el zoom 10 a 16.
 *
 * Reintentos: el AAR de MapLibre 13.6.1 no trae documentación (no hay KDoc ni jar de fuentes en la caché de Gradle),
 * así que el reintento automático de MapLibre ante REASON_CONNECTION o REASON_SERVER no está verificado aquí. Por eso
 * esos errores mantienen la región activa y se cuentan: tras 5 seguidos sin avance se detiene la descarga.
 *
 * Las operaciones de descarga y borrado se serializan con un Mutex general y corren sin cancelación (NonCancellable)
 * para que cancelar el ámbito de quien las pide no deje regiones huérfanas ni registros obsoletos.
 */
class GestorMapasSinConexionMapLibre @Inject constructor(@ApplicationContext contexto: Context) : GestorMapasSinConexion {
    private val contextoAplicacion = contexto.applicationContext
    private val densidadPantalla = contextoAplicacion.resources.displayMetrics.density
    private val alcanceProceso = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val instantaneas = ConcurrentHashMap<String, MutableStateFlow<InstantaneaRegion>>()
    private val regiones = ConcurrentHashMap<String, OfflineRegion>()
    private val regionesAnteriores = ConcurrentHashMap<String, OfflineRegion>()
    private val exclusionCarga = Mutex()
    private val exclusionOperaciones = Mutex()
    private var regionesCargadas = false

    override fun observar(idUsuario: String, idSendero: String, firmaActual: String): Flow<EstadoMapaSinConexion> {
        val clave = crearClaveRegion(idUsuario, idSendero)
        return flow {
            try {
                withContext(Dispatchers.Main) { asegurarRegionesCargadas() }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e(TAG, "No se pudieron leer las regiones guardadas", error)
            }
            emitAll(
                obtenerInstantanea(clave).map { instantanea ->
                    determinarEstado(
                        instantanea.progreso,
                        instantanea.firma,
                        firmaActual,
                        instantanea.mensajeError,
                        instantanea.esperandoConexion
                    )
                }
            )
        }.distinctUntilChanged()
    }

    override suspend fun descargar(idUsuario: String, sendero: Sendero): Result<Unit> {
        return ejecutarProtegido("descargar ${sendero.id}") {
            asegurarRegionesCargadas()
            val rectangulos = CalculadoraCorredor.rectangulos(sendero.recorrido)
            require(rectangulos.isNotEmpty()) { "El sendero no tiene un recorrido válido para descargar" }
            val clave = crearClaveRegion(idUsuario, sendero.id)
            val firma = CalculadoraCorredor.firmaTrazado(sendero.recorrido)
            val instantaneaActual = obtenerInstantanea(clave).value
            val regionActual = regiones[clave]
            val accion = if (regionActual == null) {
                AccionDescarga.CREAR
            } else {
                decidirAccionDescarga(instantaneaActual.firma, instantaneaActual.progreso, firma)
            }
            when (accion) {
                AccionDescarga.YA_DISPONIBLE -> Log.d(TAG, "Mapa ya disponible: $clave")
                AccionDescarga.REACTIVAR -> reactivarRegion(clave, regionActual)
                else -> crearRegionNueva(clave, idUsuario, sendero.id, firma, rectangulos, accion, regionActual)
            }
        }
    }

    override suspend fun eliminar(idUsuario: String, idSendero: String): Result<Unit> {
        return ejecutarProtegido("eliminar $idSendero") {
            asegurarRegionesCargadas()
            val clave = crearClaveRegion(idUsuario, idSendero)
            regiones[clave]?.let { region -> borrarRegion(clave, region) }
            regionesAnteriores[clave]?.let { region ->
                borrarRegionNativa(region)
                regionesAnteriores.remove(clave)
            }
        }
    }

    private fun reactivarRegion(clave: String, region: OfflineRegion?) {
        if (region == null) return
        actualizarInstantanea(clave) { instantanea ->
            instantanea.copy(
                progreso = instantanea.progreso?.copy(estaDescargando = true),
                mensajeError = null,
                esperandoConexion = false,
                erroresSeguidos = 0
            )
        }
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        Log.d(TAG, "Descarga reactivada: $clave")
    }

    // La región vieja se conserva hasta que la nueva termine o se descarta según la acción
    private suspend fun crearRegionNueva(
        clave: String,
        idUsuario: String,
        idSendero: String,
        firma: String,
        rectangulos: List<Rectangulo>,
        accion: AccionDescarga,
        regionActual: OfflineRegion?
    ) {
        val definicion = OfflineGeometryRegionDefinition(
            ConfiguracionEstiloMapa.URL_ESTILO,
            crearGeometria(rectangulos),
            ConfiguracionEstiloMapa.ZOOM_MINIMO_DESCARGA,
            ConfiguracionEstiloMapa.ZOOM_MAXIMO_DESCARGA,
            densidadPantalla
        )
        val metadatos = MetadatosRegion(idUsuario = idUsuario, idSendero = idSendero, firma = firma)
        val regionNueva = crearRegion(definicion, metadatos.aBytes())

        var regionADescartar: OfflineRegion? = null
        if (regionActual != null) {
            if (accion == AccionDescarga.CREAR_CONSERVANDO_ANTERIOR) {
                regionADescartar = regionesAnteriores.put(clave, regionActual)
            } else {
                regionADescartar = regionActual
            }
        }
        regiones[clave] = regionNueva
        actualizarInstantanea(clave) { InstantaneaRegion(firma, ProgresoRegion(0, 0, 0, false, true), null) }
        regionNueva.setObserver(crearObservador(clave, regionNueva))
        regionNueva.setDownloadState(OfflineRegion.STATE_ACTIVE)
        Log.d(TAG, "Descarga iniciada: $clave con ${rectangulos.size} rectángulos")
        regionADescartar?.let { region -> borrarMejorEsfuerzo(region) }
    }

    private suspend fun ejecutarProtegido(descripcion: String, bloque: suspend () -> Unit): Result<Unit> {
        return exclusionOperaciones.withLock {
            withContext(Dispatchers.Main + NonCancellable) {
                try {
                    bloque()
                    Result.success(Unit)
                } catch (cancelacion: CancellationException) {
                    throw cancelacion
                } catch (error: Exception) {
                    Log.e(TAG, "Falló la operación: $descripcion", error)
                    Result.failure(error)
                }
            }
        }
    }

    private fun obtenerAdministrador(): OfflineManager {
        MapLibre.getInstance(contextoAplicacion)
        return OfflineManager.getInstance(contextoAplicacion)
    }

    private fun obtenerInstantanea(clave: String): MutableStateFlow<InstantaneaRegion> {
        return instantaneas.getOrPut(clave) { MutableStateFlow(InstantaneaRegion(null, null, null)) }
    }

    // Se lee y escribe solo desde el hilo principal
    private fun actualizarInstantanea(clave: String, transformar: (InstantaneaRegion) -> InstantaneaRegion) {
        val flujo = obtenerInstantanea(clave)
        flujo.value = transformar(flujo.value)
    }

    // Cargar una sola vez las regiones guardadas; publicar el resultado únicamente si toda la carga terminó bien
    private suspend fun asegurarRegionesCargadas() {
        exclusionCarga.withLock {
            if (regionesCargadas) return
            val porClave = mutableMapOf<String, MutableList<RegionCargada>>()
            listarRegiones().forEach { region ->
                val metadatos = leerMetadatosRegion(region.metadata) ?: return@forEach
                val clave = crearClaveRegion(metadatos.idUsuario, metadatos.idSendero)
                val lista = porClave.getOrPut(clave) { mutableListOf() }
                if (lista.any { cargada -> cargada.region.id == region.id }) return@forEach
                lista.add(RegionCargada(region, metadatos, obtenerProgreso(region)))
            }

            val vigentes = mutableMapOf<String, RegionCargada>()
            val anteriores = mutableMapOf<String, OfflineRegion>()
            val sobrantes = mutableListOf<OfflineRegion>()
            porClave.forEach { (clave, lista) ->
                val plan = planificarRegiones(lista.map { cargada -> CandidataRegion(cargada.region.id, cargada.progreso.estaCompleta) })
                    ?: return@forEach
                vigentes[clave] = lista.first { cargada -> cargada.region.id == plan.principal }
                plan.anterior?.let { idAnterior -> anteriores[clave] = lista.first { cargada -> cargada.region.id == idAnterior }.region }
                plan.sobrantes.forEach { idSobrante -> sobrantes.add(lista.first { cargada -> cargada.region.id == idSobrante }.region) }
            }

            vigentes.forEach { (clave, cargada) ->
                if (regiones.putIfAbsent(clave, cargada.region) == null) {
                    actualizarInstantanea(clave) { InstantaneaRegion(cargada.metadatos.firma, cargada.progreso, null) }
                    cargada.region.setObserver(crearObservador(clave, cargada.region))
                }
            }
            anteriores.forEach { (clave, region) -> regionesAnteriores.putIfAbsent(clave, region) }
            regionesCargadas = true
            Log.d(TAG, "Regiones cargadas: ${regiones.size}, anteriores: ${anteriores.size}, sobrantes: ${sobrantes.size}")
            sobrantes.forEach { region -> borrarMejorEsfuerzo(region) }
        }
    }

    private fun crearObservador(clave: String, region: OfflineRegion): OfflineRegion.OfflineRegionObserver {
        return object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                if (regiones[clave] !== region) return
                val progreso = aProgreso(status)
                actualizarInstantanea(clave) { instantanea ->
                    val huboAvance = progreso.recursosCompletados > (instantanea.progreso?.recursosCompletados ?: 0L)
                    instantanea.copy(
                        progreso = progreso,
                        esperandoConexion = instantanea.esperandoConexion && !huboAvance && !progreso.estaCompleta,
                        erroresSeguidos = if (huboAvance) 0 else instantanea.erroresSeguidos
                    )
                }
                if (status.isComplete) {
                    region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                    Log.d(TAG, "Descarga completa: $clave (${status.completedResourceSize} bytes)")
                    descartarRegionAnterior(clave)
                }
            }

            override fun onError(error: OfflineRegionError) {
                if (regiones[clave] !== region) return
                Log.e(TAG, "Error de descarga en $clave: ${error.reason} ${error.message}")
                val categoria = categorizarMotivo(error.reason)
                val erroresSeguidos = obtenerInstantanea(clave).value.erroresSeguidos + 1
                when (decidirAnteError(categoria, erroresSeguidos)) {
                    DecisionError.ESPERAR_CONEXION -> actualizarInstantanea(clave) { instantanea ->
                        instantanea.copy(esperandoConexion = true, erroresSeguidos = erroresSeguidos)
                    }
                    DecisionError.DETENER -> {
                        actualizarInstantanea(clave) { instantanea ->
                            instantanea.copy(
                                esperandoConexion = false,
                                erroresSeguidos = erroresSeguidos,
                                mensajeError = mensajeErrorParaUsuario(categoria)
                            )
                        }
                        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                    }
                }
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Log.d(TAG, "Límite de teselas de Mapbox excedido ($limit); no aplica a OpenFreeMap")
            }
        }
    }

    private fun categorizarMotivo(motivo: String): CategoriaErrorRegion {
        return when (motivo) {
            OfflineRegionError.REASON_CONNECTION -> CategoriaErrorRegion.CONEXION
            OfflineRegionError.REASON_SERVER -> CategoriaErrorRegion.SERVIDOR
            OfflineRegionError.REASON_NOT_FOUND -> CategoriaErrorRegion.NO_ENCONTRADO
            else -> CategoriaErrorRegion.OTRO
        }
    }

    private fun descartarRegionAnterior(clave: String) {
        alcanceProceso.launch {
            exclusionOperaciones.withLock {
                withContext(NonCancellable) {
                    regionesAnteriores.remove(clave)?.let { region -> borrarMejorEsfuerzo(region) }
                }
            }
        }
    }

    private fun aProgreso(estado: OfflineRegionStatus): ProgresoRegion {
        return ProgresoRegion(
            recursosCompletados = estado.completedResourceCount,
            recursosRequeridos = estado.requiredResourceCount,
            bytesCompletados = estado.completedResourceSize,
            estaCompleta = estado.isComplete,
            estaDescargando = estado.downloadState == OfflineRegion.STATE_ACTIVE
        )
    }

    private fun crearGeometria(rectangulos: List<Rectangulo>): MultiPolygon {
        val poligonos = rectangulos.map { rectangulo ->
            val anillo = listOf(
                Point.fromLngLat(rectangulo.oeste, rectangulo.sur),
                Point.fromLngLat(rectangulo.este, rectangulo.sur),
                Point.fromLngLat(rectangulo.este, rectangulo.norte),
                Point.fromLngLat(rectangulo.oeste, rectangulo.norte),
                Point.fromLngLat(rectangulo.oeste, rectangulo.sur)
            )
            Polygon.fromLngLats(listOf(anillo))
        }
        return MultiPolygon.fromPolygons(poligonos)
    }

    private suspend fun listarRegiones(): Array<OfflineRegion> {
        return suspendCancellableCoroutine { continuacion ->
            obtenerAdministrador().listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    continuacion.resume(offlineRegions ?: emptyArray())
                }

                override fun onError(error: String) {
                    Log.e(TAG, "listOfflineRegions falló: $error")
                    continuacion.resumeWithException(IllegalStateException("No se pudieron leer los mapas guardados"))
                }
            })
        }
    }

    private suspend fun crearRegion(definicion: OfflineRegionDefinition, metadatos: ByteArray): OfflineRegion {
        return suspendCancellableCoroutine { continuacion ->
            obtenerAdministrador().createOfflineRegion(definicion, metadatos, object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    continuacion.resume(offlineRegion)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "createOfflineRegion falló: $error")
                    continuacion.resumeWithException(IllegalStateException("No se pudo crear la descarga del mapa"))
                }
            })
        }
    }

    private suspend fun obtenerProgreso(region: OfflineRegion): ProgresoRegion {
        return suspendCancellableCoroutine { continuacion ->
            region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                override fun onStatus(status: OfflineRegionStatus?) {
                    if (status == null) {
                        continuacion.resumeWithException(IllegalStateException("No se pudo leer el avance del mapa"))
                    } else {
                        continuacion.resume(aProgreso(status))
                    }
                }

                override fun onError(error: String?) {
                    Log.e(TAG, "getStatus falló: $error")
                    continuacion.resumeWithException(IllegalStateException("No se pudo leer el avance del mapa"))
                }
            })
        }
    }

    private suspend fun borrarRegion(clave: String, region: OfflineRegion) {
        borrarRegionNativa(region)
        regiones.remove(clave, region)
        actualizarInstantanea(clave) { InstantaneaRegion(null, null, null) }
        Log.d(TAG, "Región eliminada: $clave")
    }

    private suspend fun borrarRegionNativa(region: OfflineRegion) {
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        suspendCancellableCoroutine { continuacion ->
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() {
                    continuacion.resume(Unit)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "delete falló: $error")
                    continuacion.resumeWithException(IllegalStateException("No se pudo eliminar el mapa descargado"))
                }
            })
        }
    }

    // Borrar una región sobrante sin propagar el fallo; la próxima carga volverá a detectarla
    private suspend fun borrarMejorEsfuerzo(region: OfflineRegion) {
        try {
            borrarRegionNativa(region)
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo borrar una región sobrante", error)
        }
    }
}
