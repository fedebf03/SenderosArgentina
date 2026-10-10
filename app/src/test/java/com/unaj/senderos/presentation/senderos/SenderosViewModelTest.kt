package com.unaj.senderos.presentation.senderos

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SenderosViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
    private val listaSenderos = listOf(
        Sendero("1", usuario.id, "Huella Andina", "Mendoza", 12.5, Dificultad.ALTA),
        Sendero("2", usuario.id, "Huella Neuquina", "Neuquén", 8.0, Dificultad.BAJA)
    )

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio
    private lateinit var falsoSendero: FalsoSenderoRepositorio

    // Configurar el despachador principal de prueba y los falsos antes de cada caso
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        falsoSendero = FalsoSenderoRepositorio(listaSenderos)
    }

    // Restaurar el despachador principal original después de cada caso
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel con los casos de uso armados sobre los repositorios falsos
    private fun crearViewModel(): SenderosViewModel {
        return SenderosViewModel(
            observarSenderosDelUsuarioUseCase = ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion),
            cerrarSesionUseCase = CerrarSesionUseCase(falsoAutenticacion, falsoSendero)
        )
    }

    // Probar que el estado transiciona de cargando a éxito con usuario y senderos
    @Test
    fun `al inicializar transiciona de cargando a exito con usuario y senderos`() = runTest {
        val viewModel = crearViewModel()

        viewModel.estadoVisual.test {
            assertTrue(awaitItem().estaCargando)
            advanceUntilIdle()
            var estadoFinal = awaitItem()
            while (estadoFinal.estaCargando) {
                estadoFinal = awaitItem()
            }
            assertEquals(usuario, estadoFinal.usuario)
            assertEquals(listaSenderos, estadoFinal.senderos)
            assertNull(estadoFinal.mensajeError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que ante un fallo del flujo se expone el mensaje de error y termina la carga
    @Test
    fun `ante un fallo del flujo expone mensajeError y deja de cargar`() = runTest {
        falsoSendero.errorEnFlujo = IllegalStateException("Fallo de red")
        val viewModel = crearViewModel()

        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("Fallo de red", estado.mensajeError)
        assertTrue(estado.senderos.isEmpty())
    }

    // Probar que un fallo sin mensaje expone el mensaje de error por defecto
    @Test
    fun `ante un fallo sin mensaje expone el mensaje por defecto`() = runTest {
        falsoSendero.errorEnFlujo = IllegalStateException()
        val viewModel = crearViewModel()

        advanceUntilIdle()

        assertEquals("No se pudieron cargar los senderos", viewModel.estadoVisual.value.mensajeError)
    }

    // Probar que sin sesión activa se expone el mensaje de error y termina la carga
    @Test
    fun `sin sesion activa expone mensajeError y deja de cargar`() = runTest {
        falsoAutenticacion.usuarioActual = null
        val viewModel = crearViewModel()

        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("No hay una sesión activa", estado.mensajeError)
        assertTrue(estado.senderos.isEmpty())
    }

    // Probar que la lista se actualiza sola cuando la fuente falsa emite una nueva lista
    @Test
    fun `la lista se actualiza sola cuando la fuente emite una nueva lista`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertEquals(listaSenderos, viewModel.estadoVisual.value.senderos)
        val senderoNuevo = Sendero("3", usuario.id, "Sendero Nuevo", "Salta", 5.0, Dificultad.MEDIA)

        viewModel.estadoVisual.test {
            assertEquals(listaSenderos, awaitItem().senderos)
            falsoSendero.senderos.value = listaSenderos + senderoNuevo
            advanceUntilIdle()
            assertEquals(listaSenderos + senderoNuevo, awaitItem().senderos)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que seleccionar un sendero emite la navegación al detalle con su identificador
    @Test
    fun `AlSeleccionarSendero emite NavegarADetalle con el id`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(SenderosEvento.AlSeleccionarSendero("2"))
            advanceUntilIdle()

            assertEquals(SenderosEfecto.NavegarADetalle("2"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que cambiar la búsqueda actualiza el texto de búsqueda
    @Test
    fun `AlCambiarBusqueda actualiza textoBusqueda`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlCambiarBusqueda("Andina"))
        advanceUntilIdle()

        assertEquals("Andina", viewModel.estadoVisual.value.textoBusqueda)
    }

    // Probar que seleccionar una sección actualiza la sección seleccionada
    @Test
    fun `AlSeleccionarSeccion cambia la seccion seleccionada`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlSeleccionarSeccion(SeccionNavegacion.MAPA))
        advanceUntilIdle()

        assertEquals(SeccionNavegacion.MAPA, viewModel.estadoVisual.value.seccionSeleccionada)
    }

    // Probar que tocar el avatar muestra el menú y cerrarlo lo oculta
    @Test
    fun `AlTocarAvatar muestra el menu y AlCerrarMenuPerfil lo oculta`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlTocarAvatar)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.esMenuPerfilVisible)

        viewModel.procesarEvento(SenderosEvento.AlCerrarMenuPerfil)
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.esMenuPerfilVisible)
    }

    // Probar que cerrar sesión invoca el caso de uso, oculta el menú y emite la navegación al login
    @Test
    fun `AlCerrarSesion cierra la sesion oculta el menu y emite NavegarALogin`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(SenderosEvento.AlTocarAvatar)
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(SenderosEvento.AlCerrarSesion)
            advanceUntilIdle()

            assertEquals(SenderosEfecto.NavegarALogin, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, falsoAutenticacion.cantidadCierresSesion)
        assertFalse(viewModel.estadoVisual.value.esMenuPerfilVisible)
    }

    // Probar que tocar notificaciones emite el aviso de funcionalidad no disponible
    @Test
    fun `AlTocarNotificaciones emite MostrarAviso no disponible`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(SenderosEvento.AlTocarNotificaciones)
            advanceUntilIdle()

            assertEquals(SenderosEfecto.MostrarAviso("Todavía no disponible"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que tocar crear sendero emite el efecto de navegación a la pantalla de creación
    @Test
    fun `AlTocarCrearSendero emite NavegarACreacion`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(SenderosEvento.AlTocarCrearSendero)
            advanceUntilIdle()

            assertEquals(SenderosEfecto.NavegarACreacion, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
