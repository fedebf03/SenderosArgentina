package com.unaj.senderos.features.mapa

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoPublico
import com.unaj.senderos.core.domain.model.Usuario

data class MapaSenderosEstadoVisual(
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val senderosPublicos: List<SenderoPublico> = emptyList(),
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val idSeleccionado: String? = null,
    val seSolicitoUbicacion: Boolean = false,
    val estaSinConexion: Boolean = false
) {
    private val publicosDeOtros: List<SenderoPublico>
        get() = senderosPublicos.filter { publico ->
            senderos.none { propio -> propio.id == publico.id || propio.idOrigen == publico.id }
        }

    val senderosEnMapa: List<Sendero>
        get() = (senderos + publicosDeOtros.map { publico -> publico.sendero })
            .filter { sendero -> sendero.recorrido.isNotEmpty() }

    val senderoSeleccionado: Sendero?
        get() = senderosEnMapa.firstOrNull { sendero -> sendero.id == idSeleccionado }

    fun esPublicoDeOtro(idSendero: String): Boolean {
        return publicosDeOtros.any { publico -> publico.id == idSendero }
    }

    fun nombreAutorDe(sendero: Sendero): String? {
        return publicosDeOtros.firstOrNull { publico -> publico.id == sendero.id }?.nombreAutor
    }
}

sealed interface MapaSenderosEvento {
    data class AlSeleccionarSendero(val idSendero: String?) : MapaSenderosEvento
    data class AlAbrirDetalle(val idSendero: String) : MapaSenderosEvento
    data object AlSolicitarUbicacion : MapaSenderosEvento
}

sealed interface MapaSenderosEfecto {
    data class NavegarADetalle(val idSendero: String) : MapaSenderosEfecto
    data class NavegarADetallePublico(val idSendero: String) : MapaSenderosEfecto
}
