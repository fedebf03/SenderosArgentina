package com.unaj.senderos.navegacion

import androidx.compose.runtime.saveable.Saver

sealed interface DestinoPantalla {
    data object Login : DestinoPantalla
    data object Registro : DestinoPantalla
    data object Explorar : DestinoPantalla
    data object Mapa : DestinoPantalla
    data object MisSenderos : DestinoPantalla
    data object Perfil : DestinoPantalla
    data class Detalle(val idSendero: String) : DestinoPantalla
    data class DetallePublico(val idSendero: String) : DestinoPantalla
    // idCaminata no nulo: sendero nuevo armado desde una caminata libre ya terminada
    data class Formulario(val idSendero: String?, val idCaminata: String? = null) : DestinoPantalla
    data class Recorrido(val idSendero: String) : DestinoPantalla
    data class HistorialDetalle(val idRecorrido: String) : DestinoPantalla
}

internal fun claveDeAlmacen(destino: DestinoPantalla, idUsuario: String): String? {
    return when (destino) {
        is DestinoPantalla.Login, is DestinoPantalla.Registro -> null
        is DestinoPantalla.Explorar -> "explorar-$idUsuario"
        is DestinoPantalla.Mapa -> "mapa-$idUsuario"
        is DestinoPantalla.MisSenderos -> "mis-senderos-$idUsuario"
        is DestinoPantalla.Perfil -> "perfil-$idUsuario"
        is DestinoPantalla.Detalle -> "detalle-$idUsuario-${destino.idSendero}"
        is DestinoPantalla.DetallePublico -> "detallepublico-$idUsuario-${destino.idSendero}"
        is DestinoPantalla.Formulario -> "formulario-$idUsuario-${destino.idSendero ?: destino.idCaminata ?: "nuevo"}"
        is DestinoPantalla.Recorrido -> "recorrido-$idUsuario-${destino.idSendero}"
        is DestinoPantalla.HistorialDetalle -> "historial-$idUsuario-${destino.idRecorrido}"
    }
}

// rememberSaveable solo sabe guardar tipos simples, así que el destino viaja como texto al girar la pantalla
internal val guardadorDeDestino = Saver<DestinoPantalla, String>(
    save = { destino ->
        when (destino) {
            is DestinoPantalla.Login -> "login"
            is DestinoPantalla.Registro -> "registro"
            is DestinoPantalla.Explorar -> "explorar"
            is DestinoPantalla.Mapa -> "mapa"
            is DestinoPantalla.MisSenderos -> "mis-senderos"
            is DestinoPantalla.Perfil -> "perfil"
            is DestinoPantalla.Detalle -> "detalle:${destino.idSendero}"
            is DestinoPantalla.DetallePublico -> "detallepublico:${destino.idSendero}"
            is DestinoPantalla.Formulario -> if (destino.idCaminata != null) {
                "formulariocaminata:${destino.idCaminata}"
            } else {
                "formulario:${destino.idSendero.orEmpty()}"
            }
            is DestinoPantalla.Recorrido -> "recorrido:${destino.idSendero}"
            is DestinoPantalla.HistorialDetalle -> "historial:${destino.idRecorrido}"
        }
    },
    restore = { texto ->
        when {
            texto == "login" -> DestinoPantalla.Login
            texto == "registro" -> DestinoPantalla.Registro
            texto == "explorar" -> DestinoPantalla.Explorar
            texto == "mapa" -> DestinoPantalla.Mapa
            texto == "mis-senderos" -> DestinoPantalla.MisSenderos
            texto == "perfil" -> DestinoPantalla.Perfil
            texto.startsWith("detalle:") -> DestinoPantalla.Detalle(texto.removePrefix("detalle:"))
            texto.startsWith("detallepublico:") -> DestinoPantalla.DetallePublico(texto.removePrefix("detallepublico:"))
            texto.startsWith("formulariocaminata:") -> DestinoPantalla.Formulario(null, texto.removePrefix("formulariocaminata:"))
            texto.startsWith("formulario:") -> DestinoPantalla.Formulario(texto.removePrefix("formulario:").ifEmpty { null })
            texto.startsWith("recorrido:") -> DestinoPantalla.Recorrido(texto.removePrefix("recorrido:"))
            texto.startsWith("historial:") -> DestinoPantalla.HistorialDetalle(texto.removePrefix("historial:"))
            else -> null
        }
    }
)
