package com.unaj.senderos.features.perfil.editar

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.model.ResultadoGuardadoPerfil
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.CambiarContrasenaUseCase
import com.unaj.senderos.core.domain.usecase.CampoPerfil
import com.unaj.senderos.core.domain.usecase.GuardarPerfilUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerPerfilUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoPerfilRepositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Editar el perfil: carga lo guardado, valida, guarda con aviso según llegue o no a la nube, y cambia la contraseña con confirmación
@OptIn(ExperimentalCoroutinesApi::class)
class EditarPerfilViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana Cuenta"))
    private val perfiles = FalsoPerfilRepositorio()
    private val fotos = FalsoAlmacenFotos()
    private val efectosRecibidos = mutableListOf<EditarPerfilEfecto>()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): EditarPerfilViewModel {
        val viewModel = EditarPerfilViewModel(
            ObtenerPerfilUseCase(perfiles, autenticacion),
            GuardarPerfilUseCase(perfiles, autenticacion),
            CambiarContrasenaUseCase(autenticacion),
            fotos,
            ObtenerUsuarioActualUseCase(autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    @Test
    fun `carga el perfil guardado en los campos`() = runTest(despachadorPrueba) {
        perfiles.perfilGuardado = PerfilUsuario("Ana", "Argentina", "Neuquén", "Aluminé", "/foto.jpg")

        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("Ana", estado.nombre)
        assertEquals("Aluminé", estado.localidad)
        assertEquals("/foto.jpg", estado.foto)
        assertEquals("ana@correo.com", estado.email)
        assertFalse(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `sin perfil guardado arranca con el nombre de la cuenta`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals("Ana Cuenta", viewModel.estadoVisual.value.nombre)
    }

    @Test
    fun `escribir en un campo marca cambios sin guardar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.PAIS, "Chile"))

        assertEquals("Chile", viewModel.estadoVisual.value.pais)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `guardar manda el perfil escrito y avisa que quedo guardado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.NOMBRE, "Ana Pérez"))
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.LOCALIDAD, "Bariloche"))

        viewModel.procesarEvento(EditarPerfilEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals("Ana Pérez", perfiles.perfilesGuardados.single().second.nombre)
        assertEquals("Bariloche", perfiles.perfilesGuardados.single().second.localidad)
        assertTrue(efectosRecibidos.contains(EditarPerfilEfecto.PerfilGuardado("Perfil guardado")))
        assertFalse(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `si no llego a la nube el aviso dice que se sube cuando haya conexion`() = runTest(despachadorPrueba) {
        perfiles.resultadoGuardar = Result.success(ResultadoGuardadoPerfil(estaEnLaNube = false))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(EditarPerfilEvento.AlGuardar)
        advanceUntilIdle()

        val aviso = efectosRecibidos.filterIsInstance<EditarPerfilEfecto.PerfilGuardado>().single()
        assertTrue(aviso.mensaje.contains("cuando haya conexión"))
    }

    @Test
    fun `un nombre vacio marca el campo y no guarda`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.NOMBRE, ""))

        viewModel.procesarEvento(EditarPerfilEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals(setOf(CampoPerfil.NOMBRE), viewModel.estadoVisual.value.camposConError)
        assertTrue(perfiles.perfilesGuardados.isEmpty())
        assertFalse(viewModel.estadoVisual.value.estaGuardando)
    }

    @Test
    fun `volver con cambios pide confirmar y sin cambios vuelve directo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(EditarPerfilEvento.AlVolver)
        advanceUntilIdle()
        assertTrue(efectosRecibidos.contains(EditarPerfilEfecto.VolverAlPerfil))

        efectosRecibidos.clear()
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.PAIS, "Chile"))
        viewModel.procesarEvento(EditarPerfilEvento.AlVolver)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoDescartarVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `confirmar el descarte vuelve al perfil`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.PAIS, "Chile"))
        viewModel.procesarEvento(EditarPerfilEvento.AlVolver)

        viewModel.procesarEvento(EditarPerfilEvento.AlConfirmarDescartar)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.contains(EditarPerfilEfecto.VolverAlPerfil))
        assertFalse(viewModel.estadoVisual.value.esDialogoDescartarVisible)
    }

    @Test
    fun `elegir una foto la copia y la usa como foto del perfil`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(EditarPerfilEvento.AlSeleccionarFoto("content://galeria/1"))
        advanceUntilIdle()

        assertNotNull(viewModel.estadoVisual.value.foto.takeIf { ruta -> ruta.isNotEmpty() })
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
        assertFalse(viewModel.estadoVisual.value.estaProcesandoFoto)
    }

    @Test
    fun `quitar la foto la vacia`() = runTest(despachadorPrueba) {
        perfiles.perfilGuardado = PerfilUsuario("Ana", foto = "/foto.jpg")
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(EditarPerfilEvento.AlQuitarFoto)

        assertEquals("", viewModel.estadoVisual.value.foto)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `cambiar la contraseña la manda con la actual y vacia los campos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.ACTUAL, "vieja123"))
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.NUEVA, "nueva456"))
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.REPETIDA, "nueva456"))

        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarContrasena)
        advanceUntilIdle()

        assertEquals(listOf("vieja123" to "nueva456"), autenticacion.cambiosDeContrasena)
        assertEquals("", viewModel.estadoVisual.value.claveNueva)
        assertTrue(efectosRecibidos.contains(EditarPerfilEfecto.MostrarAviso("Contraseña cambiada")))
    }

    @Test
    fun `si las contraseñas nuevas no coinciden no se cambia nada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.ACTUAL, "vieja123"))
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.NUEVA, "nueva456"))
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.REPETIDA, "otra789"))

        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarContrasena)
        advanceUntilIdle()

        assertTrue(autenticacion.cambiosDeContrasena.isEmpty())
        assertNotNull(viewModel.estadoVisual.value.mensajeErrorContrasena)
    }

    @Test
    fun `el error de la contraseña actual incorrecta se muestra bajo el formulario`() = runTest(despachadorPrueba) {
        autenticacion.resultadoCambioContrasena = Result.failure(Exception("La contraseña actual no es correcta"))
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.ACTUAL, "mala"))
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.NUEVA, "nueva456"))
        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.REPETIDA, "nueva456"))

        viewModel.procesarEvento(EditarPerfilEvento.AlCambiarContrasena)
        advanceUntilIdle()

        assertEquals("La contraseña actual no es correcta", viewModel.estadoVisual.value.mensajeErrorContrasena)
        assertFalse(viewModel.estadoVisual.value.estaCambiandoContrasena)
    }

    @Test
    fun `una cuenta de Google no ofrece cambiar la contraseña`() = runTest(despachadorPrueba) {
        autenticacion.tieneContrasenaConfigurada = false

        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.puedeCambiarContrasena)
        assertNull(viewModel.estadoVisual.value.mensajeErrorContrasena)
    }
}
