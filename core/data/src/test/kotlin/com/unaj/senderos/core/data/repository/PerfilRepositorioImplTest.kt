package com.unaj.senderos.core.data.repository

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.network.auth.FuenteAutenticacion
import com.unaj.senderos.core.network.perfil.PerfilFuenteRemota
import com.unaj.senderos.core.network.perfil.PerfilRed
import com.unaj.senderos.core.network.fotos.SubidorFotos
import com.unaj.senderos.core.network.auth.UsuarioRed
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import com.unaj.senderos.core.data.perfil.AlmacenPerfilLocal
import com.unaj.senderos.core.data.perfil.PerfilGuardado

private const val ID_USUARIO = "usuario-1"

private class FalsaFuentePerfil : PerfilFuenteRemota {
    var remoto: PerfilRed? = null
    var falla = false
    val guardados = mutableListOf<PerfilRed>()

    override suspend fun obtenerPerfil(idUsuario: String): PerfilRed? {
        if (falla) throw IllegalStateException("Sin conexión")
        return remoto
    }

    override suspend fun guardarPerfil(idUsuario: String, perfil: PerfilRed) {
        if (falla) throw IllegalStateException("Sin conexión")
        guardados.add(perfil)
        remoto = perfil
    }
}

private class FalsaAutenticacionRed : FuenteAutenticacion {
    val nombres = mutableListOf<String>()
    override suspend fun iniciarSesion(email: String, clave: String): Result<UsuarioRed> = Result.failure(UnsupportedOperationException())
    override suspend fun registrar(email: String, clave: String): Result<UsuarioRed> = Result.failure(UnsupportedOperationException())
    override suspend fun iniciarSesionConGoogle(idToken: String): Result<UsuarioRed> = Result.failure(UnsupportedOperationException())
    override suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun actualizarNombre(nombre: String): Result<Unit> {
        nombres.add(nombre)
        return Result.success(Unit)
    }
    override suspend fun cambiarContrasena(claveActual: String, claveNueva: String): Result<Unit> = Result.success(Unit)
    override fun tieneContrasena(): Boolean = true
    override fun obtenerUsuarioActual(): UsuarioRed? = null
    override fun cerrarSesion() = Unit
}

private class FalsoSubidorDeFotos : SubidorFotos {
    var falla = false
    val subidas = mutableListOf<String>()

    override suspend fun subir(rutaLocal: String): String {
        if (falla) throw IllegalStateException("Cloudinary no está configurado")
        subidas.add(rutaLocal)
        return "https://nube/perfil.jpg"
    }
}

// Offline-first del perfil: se guarda siempre en el dispositivo y se sube cuando se puede, con la foto aparte
class PerfilRepositorioImplTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val fuente = FalsaFuentePerfil()
    private val autenticacion = FalsaAutenticacionRed()
    private val subidor = FalsoSubidorDeFotos()

    private fun crearAlmacen() = AlmacenPerfilLocal(File(carpeta.root, "perfiles"))

    private fun crearRepositorio(almacen: AlmacenPerfilLocal = crearAlmacen()) =
        PerfilRepositorioImpl(almacen, fuente, autenticacion, subidor)

    private fun crearFotoLocal(): String = carpeta.newFile("foto.jpg").also { archivo -> archivo.writeText("jpg") }.absolutePath

    @Test
    fun `guardar sin foto lo deja en el dispositivo y en la nube`() = runTest {
        val almacen = crearAlmacen()

        val resultado = crearRepositorio(almacen).guardar(ID_USUARIO, PerfilUsuario("Ana", "Argentina", "Neuquén", "Villa La Angostura")).getOrThrow()

        assertTrue(resultado.estaEnLaNube)
        assertEquals("Villa La Angostura", fuente.guardados.single().localidad)
        assertEquals(listOf("Ana"), autenticacion.nombres)
        assertFalse(almacen.leer(ID_USUARIO)!!.pendiente)
    }

    @Test
    fun `sin conexion queda pendiente en el dispositivo y no falla`() = runTest {
        fuente.falla = true
        val almacen = crearAlmacen()

        val resultado = crearRepositorio(almacen).guardar(ID_USUARIO, PerfilUsuario("Ana")).getOrThrow()

        assertFalse(resultado.estaEnLaNube)
        assertEquals("Ana", almacen.leer(ID_USUARIO)!!.nombre)
        assertTrue(almacen.leer(ID_USUARIO)!!.pendiente)
    }

    @Test
    fun `sincronizar pendiente sube lo que quedo sin subir`() = runTest {
        fuente.falla = true
        val almacen = crearAlmacen()
        val repositorio = crearRepositorio(almacen)
        repositorio.guardar(ID_USUARIO, PerfilUsuario("Ana"))
        fuente.falla = false

        repositorio.sincronizarPendiente(ID_USUARIO)

        assertEquals("Ana", fuente.guardados.single().nombre)
        assertFalse(almacen.leer(ID_USUARIO)!!.pendiente)
    }

    @Test
    fun `sincronizar sin nada pendiente no toca la nube`() = runTest {
        val repositorio = crearRepositorio()
        repositorio.guardar(ID_USUARIO, PerfilUsuario("Ana"))
        val subidasAntes = fuente.guardados.size

        repositorio.sincronizarPendiente(ID_USUARIO)

        assertEquals(subidasAntes, fuente.guardados.size)
    }

    @Test
    fun `una foto local se sube y la nube recibe su URL`() = runTest {
        val foto = crearFotoLocal()
        val almacen = crearAlmacen()

        val resultado = crearRepositorio(almacen).guardar(ID_USUARIO, PerfilUsuario("Ana", foto = foto)).getOrThrow()

        assertTrue(resultado.estaEnLaNube)
        assertEquals("https://nube/perfil.jpg", fuente.guardados.single().foto)
        assertEquals(foto, almacen.leer(ID_USUARIO)!!.fotoLocal)
    }

    @Test
    fun `si la foto no se puede subir el perfil se sube igual sin foto y queda pendiente`() = runTest {
        subidor.falla = true
        val foto = crearFotoLocal()
        val almacen = crearAlmacen()

        val resultado = crearRepositorio(almacen).guardar(ID_USUARIO, PerfilUsuario("Ana", foto = foto)).getOrThrow()

        assertFalse(resultado.estaEnLaNube)
        assertEquals("", fuente.guardados.single().foto)
        assertTrue(almacen.leer(ID_USUARIO)!!.pendiente)
        assertEquals(foto, almacen.leer(ID_USUARIO)!!.fotoEfectiva)
    }

    @Test
    fun `la foto pendiente se sube en la proxima sincronizacion`() = runTest {
        subidor.falla = true
        val foto = crearFotoLocal()
        val almacen = crearAlmacen()
        val repositorio = crearRepositorio(almacen)
        repositorio.guardar(ID_USUARIO, PerfilUsuario("Ana", foto = foto))
        subidor.falla = false

        repositorio.sincronizarPendiente(ID_USUARIO)

        assertEquals("https://nube/perfil.jpg", fuente.guardados.last().foto)
        assertFalse(almacen.leer(ID_USUARIO)!!.pendiente)
    }

    @Test
    fun `guardar de nuevo con la misma foto no la vuelve a subir`() = runTest {
        val foto = crearFotoLocal()
        val repositorio = crearRepositorio()
        repositorio.guardar(ID_USUARIO, PerfilUsuario("Ana", foto = foto))

        repositorio.guardar(ID_USUARIO, PerfilUsuario("Ana María", foto = foto))

        assertEquals(1, subidor.subidas.size)
        assertEquals("Ana María", fuente.guardados.last().nombre)
    }

    @Test
    fun `quitar la foto la borra tambien de la nube`() = runTest {
        val foto = crearFotoLocal()
        val almacen = crearAlmacen()
        val repositorio = crearRepositorio(almacen)
        repositorio.guardar(ID_USUARIO, PerfilUsuario("Ana", foto = foto))

        repositorio.guardar(ID_USUARIO, PerfilUsuario("Ana", foto = ""))

        assertEquals("", fuente.guardados.last().foto)
        assertEquals("", almacen.leer(ID_USUARIO)!!.fotoEfectiva)
    }

    @Test
    fun `sin perfil local baja el de la nube y lo guarda`() = runTest {
        fuente.remoto = PerfilRed("Ana", "Argentina", "Neuquén", "Aluminé", "https://nube/a.jpg")
        val almacen = crearAlmacen()

        val perfil = crearRepositorio(almacen).obtener(ID_USUARIO)

        assertEquals(PerfilUsuario("Ana", "Argentina", "Neuquén", "Aluminé", "https://nube/a.jpg"), perfil)
        assertEquals("Ana", almacen.leer(ID_USUARIO)!!.nombre)
    }

    @Test
    fun `sin perfil local ni conexion devuelve nulo`() = runTest {
        fuente.falla = true

        assertNull(crearRepositorio().obtener(ID_USUARIO))
    }

    @Test
    fun `con perfil local no consulta la nube`() = runTest {
        val almacen = crearAlmacen()
        almacen.guardar(ID_USUARIO, PerfilGuardado(nombre = "Ana"))
        fuente.falla = true

        assertEquals("Ana", crearRepositorio(almacen).obtener(ID_USUARIO)?.nombre)
    }

    @Test
    fun `el perfil guardado sobrevive a un almacen nuevo`() = runTest {
        crearAlmacen().guardar(ID_USUARIO, PerfilGuardado(nombre = "Ana", provincia = "Chubut", fotoLocal = "/f.jpg", pendiente = true))

        val leido = crearAlmacen().leer(ID_USUARIO)

        assertEquals(PerfilGuardado(nombre = "Ana", provincia = "Chubut", fotoLocal = "/f.jpg", pendiente = true), leido)
    }

    @Test
    fun `cada usuario tiene su propio perfil`() = runTest {
        val almacen = crearAlmacen()
        almacen.guardar("usuario-1", PerfilGuardado(nombre = "Ana"))
        almacen.guardar("usuario-2", PerfilGuardado(nombre = "Luis"))

        assertEquals("Ana", almacen.leer("usuario-1")!!.nombre)
        assertEquals("Luis", almacen.leer("usuario-2")!!.nombre)
    }
}
