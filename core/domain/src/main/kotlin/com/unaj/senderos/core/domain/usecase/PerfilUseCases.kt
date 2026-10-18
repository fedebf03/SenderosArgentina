package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.model.ResultadoGuardadoPerfil
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.PerfilRepositorio
import javax.inject.Inject

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"
const val LONGITUD_MAXIMA_DATO_PERFIL = 60
const val LONGITUD_MINIMA_CONTRASENA = 6

// Datos del perfil que no pasaron la validación
enum class CampoPerfil { NOMBRE, PAIS, PROVINCIA, LOCALIDAD }

class PerfilInvalidoException(val camposInvalidos: Set<CampoPerfil>) : Exception("Revisá los datos del perfil")

class ObtenerPerfilUseCase @Inject constructor(
    private val perfilRepositorio: PerfilRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Obtener el perfil guardado; si no hay, el que sale de la cuenta (nombre y foto del inicio de sesión)
    suspend operator fun invoke(): Result<PerfilUsuario> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val guardado = perfilRepositorio.obtener(usuario.id)
        return Result.success(
            guardado ?: PerfilUsuario(
                nombre = usuario.nombre,
                pais = usuario.pais,
                provincia = usuario.provincia,
                localidad = usuario.localidad,
                foto = usuario.fotoUrl
            )
        )
    }
}

class GuardarPerfilUseCase @Inject constructor(
    private val perfilRepositorio: PerfilRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Validar y guardar el perfil: el nombre es obligatorio y ningún dato supera los 60 caracteres
    suspend operator fun invoke(perfil: PerfilUsuario): Result<ResultadoGuardadoPerfil> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val normalizado = perfil.copy(
            nombre = perfil.nombre.trim(),
            pais = perfil.pais.trim(),
            provincia = perfil.provincia.trim(),
            localidad = perfil.localidad.trim()
        )
        val invalidos = buildSet {
            if (normalizado.nombre.isBlank() || normalizado.nombre.length > LONGITUD_MAXIMA_DATO_PERFIL) add(CampoPerfil.NOMBRE)
            if (normalizado.pais.length > LONGITUD_MAXIMA_DATO_PERFIL) add(CampoPerfil.PAIS)
            if (normalizado.provincia.length > LONGITUD_MAXIMA_DATO_PERFIL) add(CampoPerfil.PROVINCIA)
            if (normalizado.localidad.length > LONGITUD_MAXIMA_DATO_PERFIL) add(CampoPerfil.LOCALIDAD)
        }
        if (invalidos.isNotEmpty()) return Result.failure(PerfilInvalidoException(invalidos))
        return perfilRepositorio.guardar(usuario.id, normalizado)
    }
}

class SincronizarPerfilPendienteUseCase @Inject constructor(
    private val perfilRepositorio: PerfilRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Reintentar subir un perfil que se guardó sin conexión; sin sesión no hace nada
    suspend operator fun invoke() {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual() ?: return
        perfilRepositorio.sincronizarPendiente(usuario.id)
    }
}

class CambiarContrasenaUseCase @Inject constructor(
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Indicar si la cuenta tiene una contraseña propia que se pueda cambiar (las cuentas de Google no)
    fun estaDisponible(): Boolean = autenticacionRepositorio.tieneContrasena()

    // Validar y cambiar la contraseña: la nueva tiene al menos 6 caracteres y es distinta de la actual
    suspend operator fun invoke(claveActual: String, claveNueva: String): Result<Unit> {
        if (claveActual.isBlank()) return Result.failure(IllegalArgumentException("Ingresá tu contraseña actual"))
        if (claveNueva.length < LONGITUD_MINIMA_CONTRASENA) {
            return Result.failure(IllegalArgumentException("La contraseña nueva debe tener al menos $LONGITUD_MINIMA_CONTRASENA caracteres"))
        }
        if (claveNueva == claveActual) {
            return Result.failure(IllegalArgumentException("La contraseña nueva tiene que ser distinta de la actual"))
        }
        if (!autenticacionRepositorio.tieneContrasena()) {
            return Result.failure(IllegalStateException("Tu cuenta entra con Google y no tiene contraseña para cambiar"))
        }
        return autenticacionRepositorio.cambiarContrasena(claveActual, claveNueva)
    }
}
