package com.unaj.senderos.data.seguimiento

import com.unaj.senderos.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.port.ProveedorUbicacion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.cos
import kotlin.math.sin

private const val VELOCIDAD_CAMINATA_METROS_POR_SEGUNDO = 1.4
private const val INTERVALO_LECTURAS_MILISEGUNDOS = 1_000L
private const val PRECISION_SIMULADA_METROS = 5f
private const val LECTURAS_DE_DESVIO = 10
private const val DISTANCIA_DE_DESVIO_METROS = 150.0
private const val METROS_POR_GRADO = 111_195.0
private const val METROS_POR_KILOMETRO = 1000.0
private val MULTIPLICADORES_VALIDOS = setOf(1, 5, 20)

// Simular un caminante que recorre el trazado; es Kotlin puro para probarse en JVM con tiempo virtual
class ProveedorUbicacionSimulada(private val trazado: List<PuntoRecorrido>) : ProveedorUbicacion {

    @Volatile
    private var multiplicador = 1

    @Volatile
    private var lecturasDeDesvioPendientes = 0

    // Cambiar la velocidad de avance; solo se admiten los multiplicadores 1, 5 y 20
    fun cambiarMultiplicador(valor: Int) {
        require(valor in MULTIPLICADORES_VALIDOS) { "Multiplicador de simulación no válido: $valor" }
        multiplicador = valor
    }

    // Desplazar unas lecturas la posición 150 m fuera del trazado y luego volver
    fun simularDesvio() {
        lecturasDeDesvioPendientes = LECTURAS_DE_DESVIO
    }

    // Obtener el punto de partida del trazado como ubicación actual
    override suspend fun obtenerUbicacionActual(): Result<Coordenada?> {
        return Result.success(trazado.firstOrNull()?.let { punto -> Coordenada(punto.latitud, punto.longitud) })
    }

    // Emitir una lectura por segundo avanzando por el trazado hasta llegar a su final
    override fun observarUbicaciones(): Flow<LecturaUbicacion> = flow {
        val largosMetros = (0 until (trazado.size - 1).coerceAtLeast(0)).map { indice -> largoDelTramoEnMetros(indice) }
        val totalMetros = largosMetros.sum()
        if (totalMetros <= 0.0) {
            trazado.firstOrNull()?.let { primero ->
                emit(LecturaUbicacion(Coordenada(primero.latitud, primero.longitud), null, 0f, PRECISION_SIMULADA_METROS))
            }
            return@flow
        }
        var recorridoMetros = 0.0
        while (true) {
            emit(construirLectura(recorridoMetros, largosMetros, esFinal = recorridoMetros >= totalMetros))
            if (recorridoMetros >= totalMetros) break
            delay(INTERVALO_LECTURAS_MILISEGUNDOS)
            recorridoMetros = (recorridoMetros + VELOCIDAD_CAMINATA_METROS_POR_SEGUNDO * multiplicador).coerceAtMost(totalMetros)
        }
    }

    // Construir la lectura de la distancia recorrida; el desvío pendiente no se aplica en la última lectura
    private fun construirLectura(recorridoMetros: Double, largosMetros: List<Double>, esFinal: Boolean): LecturaUbicacion {
        var inicioTramo = 0.0
        var indiceTramo = largosMetros.indexOfLast { largo -> largo > 0.0 }.coerceAtLeast(0)
        for (indice in largosMetros.indices) {
            if (largosMetros[indice] > 0.0 && recorridoMetros <= inicioTramo + largosMetros[indice]) {
                indiceTramo = indice
                break
            }
            inicioTramo += largosMetros[indice]
        }
        val origen = trazado[indiceTramo]
        val destino = trazado[indiceTramo + 1]
        val inicioReal = largosMetros.take(indiceTramo).sum()
        val fraccion = ((recorridoMetros - inicioReal) / largosMetros[indiceTramo]).coerceIn(0.0, 1.0)
        var latitud = origen.latitud + (destino.latitud - origen.latitud) * fraccion
        var longitud = origen.longitud + (destino.longitud - origen.longitud) * fraccion
        val rumbo = CalculadoraRecorrido.rumboInicialGrados(
            Coordenada(origen.latitud, origen.longitud),
            Coordenada(destino.latitud, destino.longitud)
        )
        if (!esFinal && lecturasDeDesvioPendientes > 0) {
            lecturasDeDesvioPendientes--
            val rumboPerpendicular = Math.toRadians(rumbo + 90.0)
            latitud += DISTANCIA_DE_DESVIO_METROS * cos(rumboPerpendicular) / METROS_POR_GRADO
            longitud += DISTANCIA_DE_DESVIO_METROS * sin(rumboPerpendicular) / (METROS_POR_GRADO * cos(Math.toRadians(latitud)))
        }
        return LecturaUbicacion(
            coordenada = Coordenada(latitud, longitud),
            rumboGrados = rumbo.toFloat(),
            velocidadMetrosPorSegundo = (VELOCIDAD_CAMINATA_METROS_POR_SEGUNDO * multiplicador).toFloat(),
            precisionMetros = PRECISION_SIMULADA_METROS
        )
    }

    // Calcular el largo en metros de un tramo del trazado
    private fun largoDelTramoEnMetros(indiceTramo: Int): Double {
        val origen = trazado[indiceTramo]
        val destino = trazado[indiceTramo + 1]
        return CalculadoraRecorrido.distanciaEntre(Coordenada(origen.latitud, origen.longitud), destino) * METROS_POR_KILOMETRO
    }
}
