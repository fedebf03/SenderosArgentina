package com.unaj.senderos.features.perfil.editar

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.usecase.CampoPerfil

// Campos de texto del formulario de perfil
enum class CampoEdicionPerfil { NOMBRE, PAIS, PROVINCIA, LOCALIDAD }

enum class CampoContrasena { ACTUAL, NUEVA, REPETIDA }

data class EditarPerfilEstadoVisual(
    val estaCargando: Boolean = true,
    val estaGuardando: Boolean = false,
    val email: String = "",
    val nombre: String = "",
    val pais: String = "",
    val provincia: String = "",
    val localidad: String = "",
    // Ruta local o URL de la foto actual; vacía si no tiene
    val foto: String = "",
    val estaProcesandoFoto: Boolean = false,
    val camposConError: Set<CampoPerfil> = emptySet(),
    val hayCambiosSinGuardar: Boolean = false,
    val esDialogoDescartarVisible: Boolean = false,
    // Las cuentas de Google no tienen contraseña propia que cambiar
    val puedeCambiarContrasena: Boolean = false,
    val claveActual: String = "",
    val claveNueva: String = "",
    val claveRepetida: String = "",
    val estaCambiandoContrasena: Boolean = false,
    val mensajeErrorContrasena: String? = null
) {
    // Armar el perfil con lo que hay escrito en el formulario
    fun aPerfil() = PerfilUsuario(nombre, pais, provincia, localidad, foto)

    val puedeGuardar: Boolean
        get() = !estaCargando && !estaGuardando && !estaProcesandoFoto
}

sealed interface EditarPerfilEvento {
    data class AlCambiarTexto(val campo: CampoEdicionPerfil, val texto: String) : EditarPerfilEvento
    data class AlSeleccionarFoto(val uri: String) : EditarPerfilEvento
    data object AlQuitarFoto : EditarPerfilEvento
    data object AlGuardar : EditarPerfilEvento
    data object AlVolver : EditarPerfilEvento
    data object AlConfirmarDescartar : EditarPerfilEvento
    data object AlCancelarDescartar : EditarPerfilEvento
    data class AlCambiarClave(val campo: CampoContrasena, val texto: String) : EditarPerfilEvento
    data object AlCambiarContrasena : EditarPerfilEvento
}

sealed interface EditarPerfilEfecto {
    data object VolverAlPerfil : EditarPerfilEfecto
    data class PerfilGuardado(val mensaje: String) : EditarPerfilEfecto
    data class MostrarAviso(val mensaje: String) : EditarPerfilEfecto
}
