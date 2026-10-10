package com.unaj.senderos.presentation.detalle

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
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
class DetalleSenderoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
    private val senderoHuellaAndina = Sendero(
        id = "huella-andina",
        idUsuario = usuario.id,
        nombre = "Huella Andina",
        region = "Neuquén, Río Negro y Chubut",
        distanciaKilometros = 570.0,
        dificultad = Dificultad.MEDIA,
        desnivelMetros = 1200,
        tipoRecorrido = TipoRecorrido.LINEAL
    )

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio
    private lateinit var falsoSendero: FalsoSenderoRepositorio

    // Configurar el despachador principal de prueba y los falsos antes de cada caso
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        falsoSendero = FalsoSenderoRepositorio(listOf(senderoHuellaAndina))
    }

    // Restaurar el despachador principal original después de cada caso
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel con los casos de uso armados sobre los repositorios falsos
    private fun crearViewModel(idSendero: String = "huella-andina"): DetalleSenderoViewModel {
        return DetalleSenderoViewModel(
            idSendero = idSendero,
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(falsoSendero, falsoAutenticacion),
            alternarFavoritoUseCase = AlternarFavoritoUseCase(falsoSendero, falsoAutenticacion),
            eliminarSenderoUseCase = EliminarSenderoUseCase(falsoSendero, falsoAutenticacion)
        )
    }

    // Probar que el estado transiciona de cargando a éxito con el sendero cargado
    @Test
    fun `al inicializar transiciona de cargando a exito con el sendero`() = runTest {
        val viewModel = crearViewModel()

        viewModel.estadoVisual.test {
            val estadoInicial = awaitItem()
            assertTrue(estadoInicial.estaCargando)
            assertNull(estadoInicial.sendero)
            advanceUntilIdle()
            var estadoFinal = awaitItem()
            while (estadoFinal.estaCargando) {
                estadoFinal = awaitItem()
            }
            assertEquals(senderoHuellaAndina, estadoFinal.sendero)
            assertNull(estadoFinal.mensajeError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que un sendero inexistente expone el mensaje de error y termina la carga
    @Test
    fun `sendero inexistente expone mensajeError y deja de cargar`() = runTest {
        val viewModel = crearViewModel(idSendero = "no-existe")

        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertNull(estado.sendero)
        assertEquals("No se encontró el sendero", estado.mensajeError)
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
    }

    // Probar que un fallo del flujo expone su mensaje y termina la carga
    @Test
    fun `ante un fallo del flujo expone mensajeError y deja de cargar`() = runTest {
        falsoSendero.errorEnFlujo = IllegalStateException("Fallo de red")
        val viewModel = crearViewModel()

        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("Fallo de red", estado.mensajeError)
    }

    // Probar que un fallo del flujo sin mensaje expone el mensaje por defecto
    @Test
    fun `ante un fallo del flujo sin mensaje expone el mensaje por defecto`() = runTest {
        falsoSendero.errorEnFlujo = IllegalStateException()
        val viewModel = crearViewModel()

        advanceUntilIdle()

        assertEquals("Ocurrió un error inesperado", viewModel.estadoVisual.value.mensajeError)
    }

    // Probar que alternar favorito llama al caso de uso y el estado refleja el cambio emitido por el falso
    @Test
    fun `AlAlternarFavorito llama al caso de uso y el estado refleja el cambio`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.sendero?.esFavorito ?: true)

        viewModel.procesarEvento(DetalleSenderoEvento.AlAlternarFavorito)
        advanceUntilIdle()

        assertEquals(listOf(usuario.id to "huella-andina"), falsoSendero.llamadasAlternarFavorito)
        assertTrue(viewModel.estadoVisual.value.sendero?.esFavorito ?: false)
    }

    // Probar que un fallo al alternar favorito emite un aviso con el mensaje del error
    @Test
    fun `AlAlternarFavorito con fallo emite MostrarAviso con el mensaje`() = runTest {
        falsoSendero.resultadoAlternarFavorito = Result.failure(IllegalStateException("Fallo al guardar"))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlAlternarFavorito)
            advanceUntilIdle()

            assertEquals(DetalleSenderoEfecto.MostrarAviso("Fallo al guardar"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que alternar una sección la agrega al conjunto expandido y alternarla otra vez la quita
    @Test
    fun `AlAlternarSeccion agrega y quita la seccion expandida`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.ETAPAS))
        advanceUntilIdle()
        assertEquals(setOf(SeccionDetalle.ETAPAS), viewModel.estadoVisual.value.seccionesExpandidas)

        viewModel.procesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.SERVICIOS))
        advanceUntilIdle()
        assertEquals(
            setOf(SeccionDetalle.ETAPAS, SeccionDetalle.SERVICIOS),
            viewModel.estadoVisual.value.seccionesExpandidas
        )

        viewModel.procesarEvento(DetalleSenderoEvento.AlAlternarSeccion(SeccionDetalle.ETAPAS))
        advanceUntilIdle()
        assertEquals(setOf(SeccionDetalle.SERVICIOS), viewModel.estadoVisual.value.seccionesExpandidas)
    }

    // Probar que abrir el menú de opciones lo muestra y cerrarlo lo oculta
    @Test
    fun `AlAbrirMenuOpciones muestra el menu y AlCerrarMenuOpciones lo oculta`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlAbrirMenuOpciones)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.esMenuOpcionesVisible)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCerrarMenuOpciones)
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.esMenuOpcionesVisible)
    }

    // Probar que tocar editar cierra el menú y emite la navegación a la edición con el id del sendero
    @Test
    fun `AlTocarEditar cierra el menu y emite NavegarAEdicion con el id`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlAbrirMenuOpciones)
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEditar)
            advanceUntilIdle()

            assertEquals(DetalleSenderoEfecto.NavegarAEdicion("huella-andina"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.estadoVisual.value.esMenuOpcionesVisible)
    }

    // Probar que tocar iniciar recorrido emite el aviso de funcionalidad no disponible
    @Test
    fun `AlTocarIniciarRecorrido emite MostrarAviso no disponible`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
            advanceUntilIdle()

            assertEquals(DetalleSenderoEfecto.MostrarAviso("Todavía no disponible"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que tocar eliminar muestra el diálogo y cierra el menú de opciones
    @Test
    fun `AlTocarEliminar muestra el dialogo y cierra el menu`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlAbrirMenuOpciones)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminar)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.esDialogoEliminarVisible)
        assertFalse(estado.esMenuOpcionesVisible)
    }

    // Probar que cancelar la eliminación oculta el diálogo sin eliminar el sendero
    @Test
    fun `AlCancelarEliminar oculta el dialogo y no elimina`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminar)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarEliminar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarVisible)
        assertTrue(falsoSendero.llamadasEliminarSendero.isEmpty())
    }

    // Probar que confirmar la eliminación con éxito vuelve al listado y no setea mensajeError por el null posterior
    @Test
    fun `AlConfirmarEliminar con exito emite VolverAlListado y no setea mensajeError`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminar)
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
            advanceUntilIdle()

            assertEquals(DetalleSenderoEfecto.VolverAlListado, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(usuario.id to "huella-andina"), falsoSendero.llamadasEliminarSendero)
        val estado = viewModel.estadoVisual.value
        assertNull(estado.mensajeError)
        assertFalse(estado.esDialogoEliminarVisible)
    }

    // Probar que un fallo al eliminar emite un aviso con el mensaje y oculta el diálogo
    @Test
    fun `AlConfirmarEliminar con fallo emite MostrarAviso con el mensaje`() = runTest {
        falsoSendero.resultadoEliminarSendero = Result.failure(IllegalStateException("Fallo al eliminar"))
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminar)
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
            advanceUntilIdle()

            assertEquals(DetalleSenderoEfecto.MostrarAviso("Fallo al eliminar"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarVisible)
        assertEquals(senderoHuellaAndina, viewModel.estadoVisual.value.sendero)
    }

    // Probar que dos confirmaciones seguidas de eliminación llaman una sola vez al repositorio y no avisan
    @Test
    fun `AlConfirmarEliminar repetido durante la eliminacion llama una sola vez`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
            viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
            advanceUntilIdle()

            assertEquals(DetalleSenderoEfecto.VolverAlListado, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(usuario.id to "huella-andina"), falsoSendero.llamadasEliminarSendero)
    }

    // Probar que dos toques seguidos en favorito sin avanzar el despachador llaman una sola vez
    @Test
    fun `AlAlternarFavorito repetido con una alternancia en curso llama una sola vez`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlAlternarFavorito)
        viewModel.procesarEvento(DetalleSenderoEvento.AlAlternarFavorito)
        advanceUntilIdle()

        assertEquals(listOf(usuario.id to "huella-andina"), falsoSendero.llamadasAlternarFavorito)
    }

    // Probar que un sendero cargado que desaparece por un borrado externo expone el error de no encontrado
    @Test
    fun `sendero cargado que pasa a null por borrado externo expone mensajeError`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertEquals(senderoHuellaAndina, viewModel.estadoVisual.value.sendero)

        falsoSendero.senderos.value = emptyList()
        advanceUntilIdle()

        assertEquals("No se encontró el sendero", viewModel.estadoVisual.value.mensajeError)
        assertFalse(viewModel.estadoVisual.value.estaCargando)
    }

    // Probar que tras un fallo al eliminar, un null posterior sí muestra el error de no encontrado
    @Test
    fun `tras un fallo al eliminar un null posterior expone mensajeError`() = runTest {
        falsoSendero.resultadoEliminarSendero = Result.failure(IllegalStateException("Fallo al eliminar"))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
        advanceUntilIdle()
        assertNull(viewModel.estadoVisual.value.mensajeError)

        falsoSendero.senderos.value = emptyList()
        advanceUntilIdle()

        assertEquals("No se encontró el sendero", viewModel.estadoVisual.value.mensajeError)
    }

    // Probar que compartir emite el texto exacto definido en el contrato
    @Test
    fun `AlCompartir emite el texto exacto del contrato`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlCompartir)
            advanceUntilIdle()

            assertEquals(
                DetalleSenderoEfecto.CompartirSendero(
                    "Huella Andina (Neuquén, Río Negro y Chubut)\n" +
                        "Distancia: 570 km · Desnivel: 1200 m · Dificultad: Media\n" +
                        "Compartido desde SenderosArgentina"
                ),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que compartir redondea la distancia y usa el nombre de dificultad Alta
    @Test
    fun `AlCompartir redondea la distancia y nombra la dificultad Alta`() = runTest {
        val senderoCorto = senderoHuellaAndina.copy(
            id = "sendero-corto",
            nombre = "Cerro Corto",
            region = "Mendoza",
            distanciaKilometros = 12.6,
            dificultad = Dificultad.ALTA,
            desnivelMetros = 300
        )
        falsoSendero.senderos.value = listOf(senderoCorto)
        val viewModel = crearViewModel(idSendero = "sendero-corto")
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlCompartir)
            advanceUntilIdle()

            assertEquals(
                DetalleSenderoEfecto.CompartirSendero(
                    "Cerro Corto (Mendoza)\n" +
                        "Distancia: 13 km · Desnivel: 300 m · Dificultad: Alta\n" +
                        "Compartido desde SenderosArgentina"
                ),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que compartir sin sendero cargado no emite ningún efecto
    @Test
    fun `AlCompartir sin sendero cargado no emite efectos`() = runTest {
        val viewModel = crearViewModel(idSendero = "no-existe")
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlCompartir)
            advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que volver emite la navegación de regreso al listado
    @Test
    fun `AlVolver emite VolverAlListado`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(DetalleSenderoEvento.AlVolver)
            advanceUntilIdle()

            assertEquals(DetalleSenderoEfecto.VolverAlListado, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
