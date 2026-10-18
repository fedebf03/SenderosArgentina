package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoPerfilRepositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Casos de uso del perfil: validar lo que se guarda, completar con la cuenta lo que falta y exigir la contraseña actual para cambiarla
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilUseCasesTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana Cuenta", fotoUrl = "https://foto/ana.jpg")
    private val autenticacion = FalsoAutenticacionRepositorio(usuario)
    private val perfiles = FalsoPerfilRepositorio()

    @Test
    fun `sin perfil guardado se arma con el nombre y la foto de la cuenta`() = runTest {
        val perfil = ObtenerPerfilUseCase(perfiles, autenticacion)().getOrThrow()

        assertEquals("Ana Cuenta", perfil.nombre)
        assertEquals("https://foto/ana.jpg", perfil.foto)
    }

    @Test
    fun `con perfil guardado se usa ese`() = runTest {
        perfiles.perfilGuardado = PerfilUsuario(nombre = "Ana", pais = "Argentina")

        val perfil = ObtenerPerfilUseCase(perfiles, autenticacion)().getOrThrow()

        assertEquals("Argentina", perfil.pais)
    }

    @Test
    fun `sin sesion no se puede obtener el perfil`() = runTest {
        autenticacion.usuarioActual = null

        assertTrue(ObtenerPerfilUseCase(perfiles, autenticacion)().isFailure)
    }

    @Test
    fun `guardar recorta los espacios de los datos`() = runTest {
        GuardarPerfilUseCase(perfiles, autenticacion)(PerfilUsuario(nombre = "  Ana  ", pais = " Argentina ", provincia = " Río Negro", localidad = "Bariloche "))

        assertEquals(PerfilUsuario("Ana", "Argentina", "Río Negro", "Bariloche"), perfiles.perfilesGuardados.single().second)
    }

    @Test
    fun `guardar sin nombre marca el nombre como invalido y no guarda`() = runTest {
        val resultado = GuardarPerfilUseCase(perfiles, autenticacion)(PerfilUsuario(nombre = "   "))

        val error = resultado.exceptionOrNull() as PerfilInvalidoException
        assertEquals(setOf(CampoPerfil.NOMBRE), error.camposInvalidos)
        assertTrue(perfiles.perfilesGuardados.isEmpty())
    }

    @Test
    fun `un dato de mas de 60 caracteres es invalido`() = runTest {
        val largo = "a".repeat(LONGITUD_MAXIMA_DATO_PERFIL + 1)

        val resultado = GuardarPerfilUseCase(perfiles, autenticacion)(PerfilUsuario(nombre = "Ana", pais = largo, localidad = largo))

        val error = resultado.exceptionOrNull() as PerfilInvalidoException
        assertEquals(setOf(CampoPerfil.PAIS, CampoPerfil.LOCALIDAD), error.camposInvalidos)
    }

    @Test
    fun `sincronizar pendiente le pide al repositorio que sincronice`() = runTest {
        SincronizarPerfilPendienteUseCase(perfiles, autenticacion)()

        assertEquals(1, perfiles.cantidadSincronizaciones)
    }

    @Test
    fun `sin sesion no sincroniza el perfil`() = runTest {
        autenticacion.usuarioActual = null

        SincronizarPerfilPendienteUseCase(perfiles, autenticacion)()

        assertEquals(0, perfiles.cantidadSincronizaciones)
    }

    @Test
    fun `cambiar la contraseña pide la actual y la nueva al repositorio`() = runTest {
        val resultado = CambiarContrasenaUseCase(autenticacion)("clave-vieja", "clave-nueva")

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("clave-vieja" to "clave-nueva"), autenticacion.cambiosDeContrasena)
    }

    @Test
    fun `una contraseña nueva de menos de 6 caracteres se rechaza`() = runTest {
        val resultado = CambiarContrasenaUseCase(autenticacion)("clave-vieja", "corta")

        assertTrue(resultado.isFailure)
        assertTrue(autenticacion.cambiosDeContrasena.isEmpty())
    }

    @Test
    fun `una contraseña nueva igual a la actual se rechaza`() = runTest {
        val resultado = CambiarContrasenaUseCase(autenticacion)("misma-clave", "misma-clave")

        assertTrue(resultado.isFailure)
        assertTrue(autenticacion.cambiosDeContrasena.isEmpty())
    }

    @Test
    fun `sin la contraseña actual no se cambia`() = runTest {
        val resultado = CambiarContrasenaUseCase(autenticacion)("  ", "clave-nueva")

        assertTrue(resultado.isFailure)
    }

    @Test
    fun `una cuenta de Google no tiene contraseña para cambiar`() = runTest {
        autenticacion.tieneContrasenaConfigurada = false
        val casoDeUso = CambiarContrasenaUseCase(autenticacion)

        assertEquals(false, casoDeUso.estaDisponible())
        assertTrue(casoDeUso("clave-vieja", "clave-nueva").isFailure)
        assertTrue(autenticacion.cambiosDeContrasena.isEmpty())
    }

    @Test
    fun `el error del repositorio al cambiar la contraseña llega al llamador`() = runTest {
        autenticacion.resultadoCambioContrasena = Result.failure(Exception("La contraseña actual no es correcta"))

        val resultado = CambiarContrasenaUseCase(autenticacion)("clave-vieja", "clave-nueva")

        assertEquals("La contraseña actual no es correcta", resultado.exceptionOrNull()?.message)
    }
}
