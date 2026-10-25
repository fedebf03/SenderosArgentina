package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import com.unaj.senderos.core.testing.crearSenderoPublicoDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderosPublicosUseCasesTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana.perez@correo.com", nombre = "Ana")
    private val autenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    private val sinSesion = FalsoAutenticacionRepositorio(usuarioActual = null)
    private val repositorio = FalsoSenderoPublicoRepositorio()

    @Test
    fun `observar los publicos sin sesion falla`() {
        val resultado = ObservarSenderosPublicosUseCase(repositorio, sinSesion)()

        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `observar los publicos devuelve el catalogo`() = runTest {
        val publico = crearSenderoPublicoDePrueba()
        repositorio.publicos.value = listOf(publico)

        val lista = ObservarSenderosPublicosUseCase(repositorio, autenticacion)().getOrThrow().first()

        assertEquals(listOf(publico), lista)
    }

    @Test
    fun `observar un publico sin sesion falla`() {
        val resultado = ObservarSenderoPublicoUseCase(repositorio, sinSesion)("publico-1")

        assertTrue(resultado.isFailure)
    }

    @Test
    fun `un sendero propio publicado figura como publicado`() = runTest {
        repositorio.publicos.value = listOf(
            crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "sendero-1", idUsuario = "usuario-1"))
        )

        val estaPublicado = ObservarSenderoPublicadoUseCase(repositorio, autenticacion)("sendero-1").getOrThrow().first()

        assertTrue(estaPublicado)
    }

    @Test
    fun `un sendero publicado por otro autor no figura como publicado`() = runTest {
        repositorio.publicos.value = listOf(
            crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "sendero-1", idUsuario = "otro-usuario"))
        )

        val estaPublicado = ObservarSenderoPublicadoUseCase(repositorio, autenticacion)("sendero-1").getOrThrow().first()

        assertFalse(estaPublicado)
    }

    @Test
    fun `un sendero sin publicar no figura como publicado`() = runTest {
        val estaPublicado = ObservarSenderoPublicadoUseCase(repositorio, autenticacion)("sendero-1").getOrThrow().first()

        assertFalse(estaPublicado)
    }

    @Test
    fun `publicar sin sesion falla y no toca el repositorio`() = runTest {
        val resultado = PublicarSenderoUseCase(repositorio, sinSesion)(crearSenderoDePrueba())

        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(repositorio.llamadasPublicar.isEmpty())
    }

    @Test
    fun `no se puede publicar el sendero de otro usuario`() = runTest {
        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(crearSenderoDePrueba(idUsuario = "otro-usuario"))

        assertEquals("Solo podés publicar tus propios senderos", resultado.exceptionOrNull()?.message)
        assertTrue(repositorio.llamadasPublicar.isEmpty())
    }

    @Test
    fun `no se puede publicar un sendero guardado de otra persona`() = runTest {
        val copia = crearSenderoDePrueba(idUsuario = "usuario-1").copy(idOrigen = "publico-1")

        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(copia)

        assertEquals("Solo podés publicar tus propios senderos", resultado.exceptionOrNull()?.message)
        assertTrue(repositorio.llamadasPublicar.isEmpty())
    }

    @Test
    fun `no se publica un sendero con cambios pendientes`() = runTest {
        val pendiente = crearSenderoDePrueba(idUsuario = "usuario-1", estadoSincronizacion = EstadoSincronizacion.PENDIENTE)

        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(pendiente)

        assertTrue(resultado.isFailure)
        assertTrue(repositorio.llamadasPublicar.isEmpty())
    }

    @Test
    fun `no se publica un sendero con error de sincronizacion`() = runTest {
        val conError = crearSenderoDePrueba(idUsuario = "usuario-1", estadoSincronizacion = EstadoSincronizacion.CON_ERROR)

        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(conError)

        assertTrue(resultado.isFailure)
        assertTrue(repositorio.llamadasPublicar.isEmpty())
    }

    @Test
    fun `no se publica un sendero con la foto solo en este dispositivo`() = runTest {
        val conFotoLocal = crearSenderoDePrueba(idUsuario = "usuario-1").copy(foto = "/data/user/0/app/files/foto.jpg")

        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(conFotoLocal)

        assertTrue(resultado.isFailure)
        assertTrue(repositorio.llamadasPublicar.isEmpty())
    }

    @Test
    fun `no se publica un sendero con la foto de un punto solo en este dispositivo`() = runTest {
        val conPuntoConFotoLocal = crearSenderoDePrueba(
            idUsuario = "usuario-1",
            recorrido = listOf(PuntoRecorrido(-41.1, -71.3, foto = "/data/user/0/app/files/punto.jpg"))
        )

        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(conPuntoConFotoLocal)

        assertTrue(resultado.isFailure)
        assertTrue(repositorio.llamadasPublicar.isEmpty())
    }

    @Test
    fun `se publica un sendero con fotos remotas`() = runTest {
        val conFotosRemotas = crearSenderoDePrueba(
            idUsuario = "usuario-1",
            recorrido = listOf(PuntoRecorrido(-41.1, -71.3, foto = "https://res.cloudinary.com/demo/punto.jpg"))
        ).copy(foto = "https://res.cloudinary.com/demo/sendero.jpg")

        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(conFotosRemotas)

        assertTrue(resultado.isSuccess)
        assertEquals(1, repositorio.llamadasPublicar.size)
    }

    @Test
    fun `publicar quita la marca de favorito y firma con el nombre del autor`() = runTest {
        val favorito = crearSenderoDePrueba(idUsuario = "usuario-1", esFavorito = true)

        PublicarSenderoUseCase(repositorio, autenticacion)(favorito)

        val publicado = repositorio.llamadasPublicar.single()
        assertFalse(publicado.sendero.esFavorito)
        assertEquals("Ana", publicado.nombreAutor)
        assertEquals("usuario-1", publicado.idAutor)
    }

    @Test
    fun `sin nombre el autor se firma con la parte local del correo`() = runTest {
        val sinNombre = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana.perez@correo.com"))

        PublicarSenderoUseCase(repositorio, sinNombre)(crearSenderoDePrueba(idUsuario = "usuario-1"))

        assertEquals("ana.perez", repositorio.llamadasPublicar.single().nombreAutor)
    }

    @Test
    fun `un fallo del repositorio al publicar se propaga`() = runTest {
        repositorio.resultadoPublicar = Result.failure(IllegalStateException("sin permiso"))

        val resultado = PublicarSenderoUseCase(repositorio, autenticacion)(crearSenderoDePrueba(idUsuario = "usuario-1"))

        assertEquals("sin permiso", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `despublicar sin sesion falla y no toca el repositorio`() = runTest {
        val resultado = DespublicarSenderoUseCase(repositorio, sinSesion)("sendero-1")

        assertTrue(resultado.isFailure)
        assertTrue(repositorio.llamadasDespublicar.isEmpty())
    }

    @Test
    fun `despublicar delega en el repositorio`() = runTest {
        val resultado = DespublicarSenderoUseCase(repositorio, autenticacion)("sendero-1")

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("sendero-1"), repositorio.llamadasDespublicar)
    }

    @Test
    fun `guardar la copia crea un sendero nuevo del usuario sin favorito`() = runTest {
        val repositorioPropio = FalsoSenderoRepositorio()
        val guardar = GuardarSenderoUseCase(repositorioPropio, autenticacion, generarIdentificador = { "id-copia" })
        val publico = crearSenderoPublicoDePrueba(
            crearSenderoDePrueba(id = "publico-1", idUsuario = "autor-1", esFavorito = true)
        )

        val resultado = GuardarCopiaSenderoPublicoUseCase(guardar)(publico)

        val copia = resultado.getOrThrow()
        assertEquals("id-copia", copia.id)
        assertEquals("usuario-1", copia.idUsuario)
        assertFalse(copia.esFavorito)
        assertEquals("publico-1", copia.idOrigen)
        assertEquals(publico.sendero.nombre, copia.nombre)
        assertEquals(1, repositorioPropio.llamadasGuardarSendero.size)
        assertEquals("usuario-1", repositorioPropio.llamadasGuardarSendero.single().first)
    }

    @Test
    fun `guardar la copia sin sesion falla`() = runTest {
        val guardar = GuardarSenderoUseCase(FalsoSenderoRepositorio(), sinSesion)

        val resultado = GuardarCopiaSenderoPublicoUseCase(guardar)(crearSenderoPublicoDePrueba())

        assertTrue(resultado.isFailure)
        assertNull(resultado.getOrNull())
    }
}
