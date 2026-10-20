package com.unaj.senderos.features.senderos.listado

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoPublico
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.model.filtrarSenderosPorDificultad
import com.unaj.senderos.core.domain.model.filtrarSenderosPorTexto
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido

enum class ModoListado { EXPLORAR, MIS_SENDEROS }

data class SenderosEstadoVisual(
    val modo: ModoListado = ModoListado.MIS_SENDEROS,
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val senderosPublicos: List<SenderoPublico> = emptyList(),
    val textoBusqueda: String = "",
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val ubicacionActual: Coordenada? = null,
    val ordenarPorCercania: Boolean = false,
    val dificultadesSeleccionadas: Set<Dificultad> = emptySet(),
    val soloFavoritos: Boolean = false,
    val soloGuardados: Boolean = false,
    val hayErrorEnPublicos: Boolean = false,
    val seSolicitoPermisoNotificaciones: Boolean = false,
    val estaSinConexion: Boolean = false,
    // Recorrido que quedó a medias porque Android cerró la app. Se ofrece recuperarlo al volver a abrir Explorar
    val recorridoInterrumpido: RecorridoRealizado? = null,
    val estadoTrabajoSincronizacion: EstadoTrabajoSincronizacion = EstadoTrabajoSincronizacion.INACTIVO
) {
    val cantidadSinSubir: Int = senderos.count { sendero -> sendero.estadoSincronizacion == EstadoSincronizacion.PENDIENTE }
    val cantidadConErrorDeSubida: Int = senderos.count { sendero -> sendero.estadoSincronizacion == EstadoSincronizacion.CON_ERROR }

    fun distanciaKilometrosA(sendero: Sendero): Double? {
        val ubicacion = ubicacionActual ?: return null
        val primerPunto = sendero.recorrido.firstOrNull() ?: return null
        return CalculadoraRecorrido.distanciaEntre(ubicacion, primerPunto)
    }

    fun esPublicoDeOtro(idSendero: String): Boolean {
        return senderosPublicos.any { publico -> publico.id == idSendero } &&
            senderos.none { propio -> propio.id == idSendero }
    }

    fun nombreAutorDe(sendero: Sendero): String? {
        if (!esPublicoDeOtro(sendero.id)) return null
        return senderosPublicos.first { publico -> publico.id == sendero.id }.nombreAutor
    }

    val estaVacio: Boolean
        get() = if (modo == ModoListado.EXPLORAR) senderosPublicos.isEmpty() else senderos.isEmpty()

    private val senderosVisibles: List<Sendero> = elegirSegunModo()

    // Se calcula una sola vez por estado y no en cada recomposición
    val senderosFiltrados: List<Sendero> = calcularSenderosFiltrados()

    private fun elegirSegunModo(): List<Sendero> {
        return when (modo) {
            ModoListado.EXPLORAR -> senderosPublicos.map { publico ->
                senderos.firstOrNull { propio -> propio.id == publico.id } ?: publico.sendero
            }
            ModoListado.MIS_SENDEROS -> senderos.filter { sendero ->
                (!soloFavoritos || sendero.esFavorito) && (!soloGuardados || sendero.idOrigen.isNotEmpty())
            }
        }
    }

    // RF5 - Cercanía: los senderos sin recorrido o sin ubicación conocida quedan al final de la lista
    private fun calcularSenderosFiltrados(): List<Sendero> {
        val filtrados = filtrarSenderosPorDificultad(
            filtrarSenderosPorTexto(senderosVisibles, textoBusqueda),
            dificultadesSeleccionadas
        )
        if (!ordenarPorCercania) return filtrados
        return filtrados
            .map { sendero -> sendero to (distanciaKilometrosA(sendero) ?: Double.MAX_VALUE) }
            .sortedBy { (_, distancia) -> distancia }
            .map { (sendero, _) -> sendero }
    }
}

sealed interface SenderosEvento {
    data class AlCambiarBusqueda(val texto: String) : SenderosEvento
    data class AlCambiarOrdenCercania(val activado: Boolean) : SenderosEvento
    data class AlAlternarDificultad(val dificultad: Dificultad) : SenderosEvento
    data object AlAlternarFavoritos : SenderosEvento
    data object AlAlternarGuardados : SenderosEvento
    data object AlObtenerUbicacion : SenderosEvento
    data object AlSolicitarPermisoNotificaciones : SenderosEvento
    data object AlTocarSincronizar : SenderosEvento
    data object AlTocarCrearSendero : SenderosEvento
    // El botón de Explorar pide primero el permiso de ubicación. Con el permiso concedido llega AlCaminarAhora
    data object AlTocarCaminarAhora : SenderosEvento
    data object AlCaminarAhora : SenderosEvento
    data object AlDenegarPermisoParaCaminar : SenderosEvento
    data object AlRecuperarRecorrido : SenderosEvento
    data object AlDescartarRecorridoInterrumpido : SenderosEvento
    data class AlSeleccionarSendero(val idSendero: String) : SenderosEvento
}

sealed interface SenderosEfecto {
    data class NavegarADetalle(val idSendero: String) : SenderosEfecto
    data class NavegarADetallePublico(val idSendero: String) : SenderosEfecto
    data object NavegarACreacion : SenderosEfecto
    data object SolicitarPermisoUbicacionParaCaminar : SenderosEfecto
    data class NavegarACaminata(val idCaminata: String) : SenderosEfecto
    data class NavegarAHistorialDetalle(val idRecorrido: String) : SenderosEfecto
    data class MostrarAviso(val mensaje: String) : SenderosEfecto
}
