package com.unaj.senderos.presentation.formulario

import app.cash.turbine.test
import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class FormularioSenderoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
    private val senderoOriginal = crearSenderoDePrueba(
        id = "sendero-1",
        idUsuario = usuario.id,
        nombre = "Cerro Catedral",
        distanciaKilometros = 12.0,
        desnivelMetros = 600,
        alturaMaximaMetros = 2000,
        alturaMinimaMetros = 1400,
        esFavorito = true
    )

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio
    private lateinit var falsoSendero: FalsoSenderoRepositorio

    // Configurar el despachador principal de prueba y los falsos antes de cada caso
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        falsoSendero = FalsoSenderoRepositorio(listOf(senderoOriginal))
    }

    // Restaurar el despachador principal original después de cada caso
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel con los casos de uso armados sobre los repositorios falsos
    private fun crearViewModel(idSendero: String? = null): FormularioSenderoViewModel {
        return FormularioSenderoViewModel(
            idSendero = idSendero,
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(falsoSendero, falsoAutenticacion),
            guardarSenderoUseCase = GuardarSenderoUseCase(
                senderoRepositorio = falsoSendero,
                autenticacionRepositorio = falsoAutenticacion,
                generarIdentificador = { "id-generado" }
            )
        )
    }

    // Avanzar las corrutinas pendientes hasta que el despachador quede inactivo
    private fun avanzar() {
        despachadorPrueba.scheduler.advanceUntilIdle()
    }

    // Procesar un evento y avanzar las corrutinas pendientes
    private fun enviar(viewModel: FormularioSenderoViewModel, evento: FormularioSenderoEvento) {
        viewModel.procesarEvento(evento)
        avanzar()
    }

    // Completar los campos obligatorios del formulario con valores válidos
    private fun completarFormularioValido(viewModel: FormularioSenderoViewModel) {
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "  Cerro Tronador "))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.REGION, "Río Negro"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESCRIPCION, "Glaciar"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "8,5"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "400"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MAXIMA, "3000"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MINIMA, "1500"))
    }

    // Probar que el modo creación arranca con el formulario vacío y sin cargar
    @Test
    fun `modo creacion arranca con el formulario vacio`() = runTest {
        val viewModel = crearViewModel()
        avanzar()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.esEdicion)
        assertFalse(estado.estaCargando)
        assertEquals("", estado.nombre)
        assertEquals("", estado.distancia)
        assertTrue(estado.recomendaciones.isEmpty())
        assertFalse(estado.hayCambiosSinGuardar)
        assertNull(estado.mensajeError)
    }

    // Probar que el modo edición carga los campos del sendero y no marca cambios
    @Test
    fun `modo edicion carga los campos y no marca cambios`() = runTest {
        val viewModel = crearViewModel(idSendero = "sendero-1")
        assertTrue(viewModel.estadoVisual.value.estaCargando)
        avanzar()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.esEdicion)
        assertFalse(estado.estaCargando)
        assertEquals("Cerro Catedral", estado.nombre)
        assertEquals("Bariloche", estado.region)
        assertEquals("Ascenso con vistas al lago", estado.descripcion)
        assertEquals("12", estado.distancia)
        assertEquals("600", estado.desnivel)
        assertEquals("2000", estado.alturaMaxima)
        assertEquals("1400", estado.alturaMinima)
        assertEquals("5 horas", estado.duracionEstimada)
        assertEquals("Base del cerro", estado.puntoInicio)
        assertEquals("Verano", estado.mejorEpoca)
        assertEquals(Dificultad.MEDIA, estado.dificultad)
        assertEquals(TipoRecorrido.CIRCULAR, estado.tipoRecorrido)
        assertEquals(listOf("Llevar agua"), estado.recomendaciones)
        assertEquals(listOf("Refugio"), estado.servicios)
        assertEquals(1, estado.etapas.size)
        assertFalse(estado.hayCambiosSinGuardar)
    }

    // Probar que una distancia con decimales se muestra sin ceros de más
    @Test
    fun `modo edicion muestra la distancia decimal con su fraccion`() = runTest {
        falsoSendero.senderos.value = listOf(senderoOriginal.copy(distanciaKilometros = 12.5))
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()

        assertEquals("12.5", viewModel.estadoVisual.value.distancia)
    }

    // Probar que editar un sendero inexistente informa el error y deja de cargar
    @Test
    fun `modo edicion con sendero inexistente informa mensajeError`() = runTest {
        val viewModel = crearViewModel(idSendero = "no-existe")
        avanzar()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("No se encontró el sendero", estado.mensajeError)
    }

    // Probar que editar sin sesión activa informa el error del caso de uso
    @Test
    fun `modo edicion sin sesion activa informa el mensaje del error`() = runTest {
        falsoAutenticacion.usuarioActual = null
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("No hay una sesión activa", estado.mensajeError)
    }

    // Probar que cambiar un texto actualiza el campo y marca cambios sin guardar
    @Test
    fun `cambiar texto actualiza el campo y marca cambios`() = runTest {
        val viewModel = crearViewModel()

        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Nuevo"))

        assertEquals("Nuevo", viewModel.estadoVisual.value.nombre)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    // Probar que cambiar un campo con error le quita la marca de error
    @Test
    fun `cambiar un campo con error le quita la marca de error`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlGuardar)
        assertTrue(CampoSendero.DISTANCIA in viewModel.estadoVisual.value.camposConError)
        assertTrue(CampoSendero.DESNIVEL in viewModel.estadoVisual.value.camposConError)

        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "5"))

        assertFalse(CampoSendero.DISTANCIA in viewModel.estadoVisual.value.camposConError)
        assertTrue(CampoSendero.DESNIVEL in viewModel.estadoVisual.value.camposConError)
    }

    // Probar que los campos de elementos nuevos no marcan cambios sin guardar
    @Test
    fun `campos de elementos nuevos no marcan cambios`() = runTest {
        val viewModel = crearViewModel()

        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVA_RECOMENDACION, "a"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVO_SERVICIO, "b"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE_NUEVA_ETAPA, "c"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, "1"))

        val estado = viewModel.estadoVisual.value
        assertEquals("a", estado.textoNuevaRecomendacion)
        assertEquals("b", estado.textoNuevoServicio)
        assertEquals("c", estado.nombreNuevaEtapa)
        assertEquals("1", estado.distanciaNuevaEtapa)
        assertFalse(estado.hayCambiosSinGuardar)
    }

    // Probar que seleccionar dificultad y tipo de recorrido actualiza el estado y marca cambios
    @Test
    fun `seleccionar dificultad y tipo de recorrido marca cambios`() = runTest {
        val viewModel = crearViewModel()

        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarDificultad(Dificultad.ALTA))
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarTipoRecorrido(TipoRecorrido.IDA_Y_VUELTA))

        val estado = viewModel.estadoVisual.value
        assertEquals(Dificultad.ALTA, estado.dificultad)
        assertEquals(TipoRecorrido.IDA_Y_VUELTA, estado.tipoRecorrido)
        assertTrue(estado.hayCambiosSinGuardar)
    }

    // Probar que agregar una recomendación la guarda recortada, limpia el campo y marca cambios
    @Test
    fun `agregar recomendacion la recorta limpia el campo y marca cambios`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVA_RECOMENDACION, "  Llevar agua  "))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarRecomendacion)

        val estado = viewModel.estadoVisual.value
        assertEquals(listOf("Llevar agua"), estado.recomendaciones)
        assertEquals("", estado.textoNuevaRecomendacion)
        assertTrue(estado.hayCambiosSinGuardar)
    }

    // Probar que agregar una recomendación en blanco no agrega nada ni marca cambios
    @Test
    fun `agregar recomendacion en blanco no agrega nada`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVA_RECOMENDACION, "   "))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarRecomendacion)

        assertTrue(viewModel.estadoVisual.value.recomendaciones.isEmpty())
        assertFalse(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    // Probar que quitar una recomendación elimina la del índice indicado
    @Test
    fun `quitar recomendacion elimina la del indice indicado`() = runTest {
        val viewModel = crearViewModel()
        for (texto in listOf("Uno", "Dos", "Tres")) {
            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVA_RECOMENDACION, texto))
            enviar(viewModel, FormularioSenderoEvento.AlAgregarRecomendacion)
        }

        enviar(viewModel, FormularioSenderoEvento.AlQuitarRecomendacion(1))

        assertEquals(listOf("Uno", "Tres"), viewModel.estadoVisual.value.recomendaciones)
    }

    // Probar que agregar un servicio lo guarda recortado y limpia el campo
    @Test
    fun `agregar servicio lo recorta y limpia el campo`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVO_SERVICIO, " Refugio "))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarServicio)

        assertEquals(listOf("Refugio"), viewModel.estadoVisual.value.servicios)
        assertEquals("", viewModel.estadoVisual.value.textoNuevoServicio)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    // Probar que agregar un servicio en blanco no agrega nada
    @Test
    fun `agregar servicio en blanco no agrega nada`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVO_SERVICIO, " "))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarServicio)

        assertTrue(viewModel.estadoVisual.value.servicios.isEmpty())
    }

    // Probar que quitar un servicio elimina el del índice indicado
    @Test
    fun `quitar servicio elimina el del indice indicado`() = runTest {
        val viewModel = crearViewModel()
        for (texto in listOf("Camping", "Refugio")) {
            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVO_SERVICIO, texto))
            enviar(viewModel, FormularioSenderoEvento.AlAgregarServicio)
        }

        enviar(viewModel, FormularioSenderoEvento.AlQuitarServicio(0))

        assertEquals(listOf("Refugio"), viewModel.estadoVisual.value.servicios)
    }

    // Probar que agregar una etapa válida acepta coma decimal, la numera y limpia los campos
    @Test
    fun `agregar etapa valida acepta coma decimal y limpia los campos`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE_NUEVA_ETAPA, "Base al refugio"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, "3,5"))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarEtapa)

        val estado = viewModel.estadoVisual.value
        assertEquals(listOf(Etapa(numero = 1, nombre = "Base al refugio", distanciaKilometros = 3.5)), estado.etapas)
        assertEquals("", estado.nombreNuevaEtapa)
        assertEquals("", estado.distanciaNuevaEtapa)
        assertTrue(estado.hayCambiosSinGuardar)
    }

    // Probar que agregar una etapa inválida emite el aviso y no agrega nada
    @Test
    fun `agregar etapa invalida emite aviso y no agrega nada`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE_NUEVA_ETAPA, "Sin distancia"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, "abc"))

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarEtapa)
            avanzar()

            assertEquals(FormularioSenderoEfecto.MostrarAviso("Completá el nombre y los km de la etapa"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.estadoVisual.value.etapas.isEmpty())
    }

    // Probar que una etapa con distancia cero o sin nombre también es inválida
    @Test
    fun `agregar etapa con distancia cero o sin nombre emite aviso`() = runTest {
        val viewModel = crearViewModel()

        viewModel.efectoVisual.test {
            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE_NUEVA_ETAPA, "Etapa"))
            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, "0"))
            enviar(viewModel, FormularioSenderoEvento.AlAgregarEtapa)
            assertEquals(FormularioSenderoEfecto.MostrarAviso("Completá el nombre y los km de la etapa"), awaitItem())

            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE_NUEVA_ETAPA, " "))
            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, "2"))
            enviar(viewModel, FormularioSenderoEvento.AlAgregarEtapa)
            assertEquals(FormularioSenderoEfecto.MostrarAviso("Completá el nombre y los km de la etapa"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.estadoVisual.value.etapas.isEmpty())
    }

    // Probar que quitar una etapa renumera las restantes de 1 a n
    @Test
    fun `quitar etapa renumera las restantes`() = runTest {
        val viewModel = crearViewModel()
        for (nombre in listOf("A", "B", "C")) {
            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE_NUEVA_ETAPA, nombre))
            enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, "2"))
            enviar(viewModel, FormularioSenderoEvento.AlAgregarEtapa)
        }

        enviar(viewModel, FormularioSenderoEvento.AlQuitarEtapa(0))

        assertEquals(
            listOf(
                Etapa(numero = 1, nombre = "B", distanciaKilometros = 2.0),
                Etapa(numero = 2, nombre = "C", distanciaKilometros = 2.0)
            ),
            viewModel.estadoVisual.value.etapas
        )
    }

    // Probar que guardar con números inválidos marca los campos y avisa sin llamar al caso de uso
    @Test
    fun `guardar con numero invalido marca campos y avisa sin llamar al caso de uso`() = runTest {
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "abc"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "12.5"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MAXIMA, ""))

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
            avanzar()

            assertEquals(FormularioSenderoEfecto.MostrarAviso("Revisá los campos marcados"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            setOf(CampoSendero.DISTANCIA, CampoSendero.DESNIVEL, CampoSendero.ALTURA_MAXIMA),
            viewModel.estadoVisual.value.camposConError
        )
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
        assertFalse(viewModel.estadoVisual.value.estaGuardando)
    }

    // Probar que guardar un formulario válido en creación emite SenderoGuardado con el id generado
    @Test
    fun `guardar valido en creacion emite SenderoGuardado con el id generado`() = runTest {
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
            avanzar()

            assertEquals(FormularioSenderoEfecto.SenderoGuardado("id-generado"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val (idUsuarioGuardado, guardado) = falsoSendero.llamadasGuardarSendero.single()
        assertEquals("usuario-1", idUsuarioGuardado)
        assertEquals("id-generado", guardado.id)
        assertEquals("Cerro Tronador", guardado.nombre)
        assertEquals(8.5, guardado.distanciaKilometros, 0.0)
        assertEquals(400, guardado.desnivelMetros)
        assertEquals(3000, guardado.alturaMaximaMetros)
        assertEquals(1500, guardado.alturaMinimaMetros)
        assertFalse(viewModel.estadoVisual.value.hayCambiosSinGuardar)
        assertFalse(viewModel.estadoVisual.value.estaGuardando)
    }

    // Probar que guardar en edición conserva el id y el estado de favorito del original
    @Test
    fun `guardar en edicion conserva el id y esFavorito`() = runTest {
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Cerro Catedral Norte"))

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
            avanzar()

            assertEquals(FormularioSenderoEfecto.SenderoGuardado("sendero-1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val guardado = falsoSendero.llamadasGuardarSendero.single().second
        assertEquals("sendero-1", guardado.id)
        assertTrue(guardado.esFavorito)
        assertEquals("Cerro Catedral Norte", guardado.nombre)
        assertEquals(12.0, guardado.distanciaKilometros, 0.0)
    }

    // Probar que una excepción de validación del caso de uso marca sus campos y avisa
    @Test
    fun `SenderoInvalidoException marca camposConError y avisa`() = runTest {
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "   "))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "0"))

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
            avanzar()

            assertEquals(FormularioSenderoEfecto.MostrarAviso("Revisá los campos marcados"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            setOf(CampoSendero.NOMBRE, CampoSendero.DISTANCIA),
            viewModel.estadoVisual.value.camposConError
        )
        assertFalse(viewModel.estadoVisual.value.estaGuardando)
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    // Probar que otro fallo del guardado avisa con el mensaje del error
    @Test
    fun `fallo del repositorio al guardar avisa con el mensaje del error`() = runTest {
        falsoSendero.resultadoGuardarSendero = Result.failure(IllegalStateException("Disco lleno"))
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
            avanzar()

            assertEquals(FormularioSenderoEfecto.MostrarAviso("Disco lleno"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.estadoVisual.value.estaGuardando)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    // Probar que un fallo sin mensaje avisa con el texto genérico
    @Test
    fun `fallo sin mensaje avisa con el texto generico`() = runTest {
        falsoSendero.resultadoGuardarSendero = Result.failure(IllegalStateException())
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
            avanzar()

            assertEquals(FormularioSenderoEfecto.MostrarAviso("Ocurrió un error inesperado"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que guardar repetido mientras se guarda produce una sola llamada
    @Test
    fun `AlGuardar repetido mientras guarda produce una sola llamada`() = runTest {
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        avanzar()

        assertEquals(1, falsoSendero.llamadasGuardarSendero.size)
    }

    // Probar que volver sin cambios emite VolverAtras directamente
    @Test
    fun `AlVolver sin cambios emite VolverAtras`() = runTest {
        val viewModel = crearViewModel()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlVolver)
            avanzar()

            assertEquals(FormularioSenderoEfecto.VolverAtras, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.estadoVisual.value.esDialogoDescartarVisible)
    }

    // Probar que volver con cambios muestra el diálogo de descartar y no navega
    @Test
    fun `AlVolver con cambios muestra el dialogo de descartar`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Algo"))

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlVolver)
            avanzar()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.estadoVisual.value.esDialogoDescartarVisible)
    }

    // Probar que confirmar el descarte oculta el diálogo y emite VolverAtras
    @Test
    fun `AlConfirmarDescartar oculta el dialogo y emite VolverAtras`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Algo"))
        enviar(viewModel, FormularioSenderoEvento.AlVolver)

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlConfirmarDescartar)
            avanzar()

            assertEquals(FormularioSenderoEfecto.VolverAtras, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.estadoVisual.value.esDialogoDescartarVisible)
    }

    // Probar que cancelar el descarte oculta el diálogo sin navegar y conserva los cambios
    @Test
    fun `AlCancelarDescartar oculta el dialogo sin navegar`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Algo"))
        enviar(viewModel, FormularioSenderoEvento.AlVolver)

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FormularioSenderoEvento.AlCancelarDescartar)
            avanzar()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.estadoVisual.value.esDialogoDescartarVisible)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
        assertEquals("Algo", viewModel.estadoVisual.value.nombre)
    }
}
