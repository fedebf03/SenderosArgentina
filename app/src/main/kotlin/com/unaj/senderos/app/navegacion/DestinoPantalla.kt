package com.unaj.senderos.app.navegacion

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

// Cada destino es una ruta tipada de Navigation Compose. @Serializable permite que el back stack
// se guarde solo al girar la pantalla y también si Android cierra la app en segundo plano
sealed interface DestinoPantalla {
    @Serializable data object Login : DestinoPantalla
    @Serializable data object Registro : DestinoPantalla
    @Serializable data object Explorar : DestinoPantalla
    @Serializable data object Mapa : DestinoPantalla
    @Serializable data object MisSenderos : DestinoPantalla
    @Serializable data object Perfil : DestinoPantalla
    @Serializable data object EditarPerfil : DestinoPantalla
    @Serializable data class Detalle(val idSendero: String) : DestinoPantalla
    @Serializable data class DetallePublico(val idSendero: String) : DestinoPantalla
    // Con idCaminata, el sendero nuevo se arma desde una caminata libre ya terminada
    @Serializable data class Formulario(val idSendero: String?, val idCaminata: String? = null) : DestinoPantalla
    @Serializable data class Recorrido(val idSendero: String) : DestinoPantalla
    @Serializable data class HistorialDetalle(val idRecorrido: String) : DestinoPantalla
}

// Las secciones de la botonera inferior. Explorar es siempre la base del back stack y las demás van encima
internal fun NavDestination.esSeccionPrincipal(): Boolean {
    return hasRoute<DestinoPantalla.Explorar>() ||
        hasRoute<DestinoPantalla.Mapa>() ||
        hasRoute<DestinoPantalla.MisSenderos>() ||
        hasRoute<DestinoPantalla.Perfil>()
}
