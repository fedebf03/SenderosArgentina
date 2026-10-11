package com.unaj.senderos.presentation.formulario

import app.cash.turbine.test
import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAlmacenFotos
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
    private lateinit var falsoAlmacenFotos: FalsoAlmacenFotos

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        falsoSendero = FalsoSenderoRepositorio(listOf(senderoOriginal))
        falsoAlmacenFotos = FalsoAlmacenFotos()
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(idSendero: String? = null): FormularioSenderoViewModel {
        return FormularioSenderoViewModel(
            idSendero = idSendero,
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(falsoSendero, falsoAutenticacion),
            guardarSenderoUseCase = GuardarSenderoUseCase(
                senderoRepositorio = falsoSendero,
                autenticacionRepositorio = falsoAutenticacion,
                generarIdentificador = { "id-generado" }
            ),
            almacenFotos = falsoAlmacenFotos
        )
    }

    private fun avanzar() {
        despachadorPrueba.scheduler.advanceUntilIdle()
    }

    private fun enviar(viewModel: FormularioSenderoViewModel, evento: FormularioSenderoEvento) {
        viewModel.procesarEvento(evento)
        avanzar()
    }

    private fun completarFormularioValido(viewModel: FormularioSenderoViewModel) {
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "  Cerro Tronador "))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.REGION, "Río Negro"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESCRIPCION, "Glaciar"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "8,5"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "400"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MAXIMA, "3000"))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MINIMA, "1500"))
    }

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

    @Test
    fun `modo edicion muestra la distancia decimal con su fraccion`() = runTest {
        falsoSendero.senderos.value = listOf(senderoOriginal.copy(distanciaKilometros = 12.5))
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()

        assertEquals("12.5", viewModel.estadoVisual.value.distancia)
    }

    @Test
    fun `modo edicion con sendero inexistente informa mensajeError`() = runTest {
        val viewModel = crearViewModel(idSendero = "no-existe")
        avanzar()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("No se encontró el sendero", estado.mensajeError)
    }

    @Test
    fun `modo edicion sin sesion activa informa el mensaje del error`() = runTest {
        falsoAutenticacion.usuarioActual = null
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("No hay una sesión activa", estado.mensajeError)
    }

    @Test
    fun `cambiar texto actualiza el campo y marca cambios`() = runTest {
        val viewModel = crearViewModel()

        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Nuevo"))

        assertEquals("Nuevo", viewModel.estadoVisual.value.nombre)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

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

    @Test
    fun `agregar recomendacion en blanco no agrega nada`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVA_RECOMENDACION, "   "))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarRecomendacion)

        assertTrue(viewModel.estadoVisual.value.recomendaciones.isEmpty())
        assertFalse(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

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

    @Test
    fun `agregar servicio lo recorta y limpia el campo`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVO_SERVICIO, " Refugio "))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarServicio)

        assertEquals(listOf("Refugio"), viewModel.estadoVisual.value.servicios)
        assertEquals("", viewModel.estadoVisual.value.textoNuevoServicio)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `agregar servicio en blanco no agrega nada`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVO_SERVICIO, " "))

        enviar(viewModel, FormularioSenderoEvento.AlAgregarServicio)

        assertTrue(viewModel.estadoVisual.value.servicios.isEmpty())
    }

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

    @Test
    fun `AlGuardar repetido mientras guarda produce una sola llamada`() = runTest {
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        avanzar()

        assertEquals(1, falsoSendero.llamadasGuardarSendero.size)
    }

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

    private fun crearViewModelConPuntos(vararg puntos: PuntoRecorrido): FormularioSenderoViewModel {
        val viewModel = crearViewModel()
        for (punto in puntos) {
            enviar(viewModel, FormularioSenderoEvento.AlAgregarPunto(punto.latitud, punto.longitud))
        }
        return viewModel
    }

    @Test
    fun `el recorrido y el editor del mapa arrancan vacios y ocultos`() = runTest {
        val viewModel = crearViewModel()
        avanzar()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.recorrido.isEmpty())
        assertFalse(estado.esEditorMapaVisible)
        assertNull(estado.indicePuntoSeleccionado)
        assertEquals(0.0, estado.distanciaCalculadaKilometros, 0.0)
    }

    @Test
    fun `abrir y cerrar el editor del mapa alterna la visibilidad sin marcar cambios`() = runTest {
        val viewModel = crearViewModel()

        enviar(viewModel, FormularioSenderoEvento.AlAbrirEditorMapa)
        assertTrue(viewModel.estadoVisual.value.esEditorMapaVisible)

        enviar(viewModel, FormularioSenderoEvento.AlCerrarEditorMapa)
        assertFalse(viewModel.estadoVisual.value.esEditorMapaVisible)
        assertFalse(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `agregar punto lo anade al final como PASO y marca cambios`() = runTest {
        val viewModel = crearViewModel()

        enviar(viewModel, FormularioSenderoEvento.AlAgregarPunto(-40.5, -71.3))
        enviar(viewModel, FormularioSenderoEvento.AlAgregarPunto(-40.6, -71.4))

        val estado = viewModel.estadoVisual.value
        assertEquals(
            listOf(
                PuntoRecorrido(-40.5, -71.3, TipoPunto.PASO, ""),
                PuntoRecorrido(-40.6, -71.4, TipoPunto.PASO, "")
            ),
            estado.recorrido
        )
        assertTrue(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `mover punto cambia solo las coordenadas y marca cambios`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(-40.5, -71.3), PuntoRecorrido(-40.6, -71.4))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTipoPunto(1, TipoPunto.AGUA))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarNombrePunto(1, "Arroyo"))

        enviar(viewModel, FormularioSenderoEvento.AlMoverPunto(1, -41.0, -72.0))

        assertEquals(
            listOf(
                PuntoRecorrido(-40.5, -71.3, TipoPunto.PASO, ""),
                PuntoRecorrido(-41.0, -72.0, TipoPunto.AGUA, "Arroyo")
            ),
            viewModel.estadoVisual.value.recorrido
        )
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `seleccionar punto guarda el indice y nulo limpia la seleccion`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(-40.5, -71.3), PuntoRecorrido(-40.6, -71.4))

        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarPunto(1))
        assertEquals(1, viewModel.estadoVisual.value.indicePuntoSeleccionado)

        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarPunto(null))
        assertNull(viewModel.estadoVisual.value.indicePuntoSeleccionado)
    }

    @Test
    fun `eliminar punto lo quita del recorrido y marca cambios`() = runTest {
        val viewModel = crearViewModelConPuntos(
            PuntoRecorrido(1.0, 1.0),
            PuntoRecorrido(2.0, 2.0),
            PuntoRecorrido(3.0, 3.0)
        )

        enviar(viewModel, FormularioSenderoEvento.AlEliminarPunto(1))

        assertEquals(
            listOf(PuntoRecorrido(1.0, 1.0), PuntoRecorrido(3.0, 3.0)),
            viewModel.estadoVisual.value.recorrido
        )
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `eliminar el punto seleccionado limpia la seleccion`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(1.0, 1.0), PuntoRecorrido(2.0, 2.0))
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarPunto(1))

        enviar(viewModel, FormularioSenderoEvento.AlEliminarPunto(1))

        assertNull(viewModel.estadoVisual.value.indicePuntoSeleccionado)
    }

    @Test
    fun `eliminar un punto anterior al seleccionado ajusta el indice`() = runTest {
        val viewModel = crearViewModelConPuntos(
            PuntoRecorrido(1.0, 1.0),
            PuntoRecorrido(2.0, 2.0),
            PuntoRecorrido(3.0, 3.0)
        )
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarPunto(2))

        enviar(viewModel, FormularioSenderoEvento.AlEliminarPunto(0))

        assertEquals(1, viewModel.estadoVisual.value.indicePuntoSeleccionado)
    }

    @Test
    fun `eliminar un punto posterior al seleccionado conserva el indice`() = runTest {
        val viewModel = crearViewModelConPuntos(
            PuntoRecorrido(1.0, 1.0),
            PuntoRecorrido(2.0, 2.0),
            PuntoRecorrido(3.0, 3.0)
        )
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarPunto(0))

        enviar(viewModel, FormularioSenderoEvento.AlEliminarPunto(2))

        assertEquals(0, viewModel.estadoVisual.value.indicePuntoSeleccionado)
    }

    @Test
    fun `insertar punto antes agrega el punto medio con el anterior`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(-40.0, -70.0), PuntoRecorrido(-42.0, -72.0))

        enviar(viewModel, FormularioSenderoEvento.AlInsertarPuntoAntes(1))

        val recorrido = viewModel.estadoVisual.value.recorrido
        assertEquals(3, recorrido.size)
        assertEquals(PuntoRecorrido(-40.0, -70.0), recorrido[0])
        assertEquals(-41.0, recorrido[1].latitud, 1e-9)
        assertEquals(-71.0, recorrido[1].longitud, 1e-9)
        assertEquals(TipoPunto.PASO, recorrido[1].tipo)
        assertEquals(PuntoRecorrido(-42.0, -72.0), recorrido[2])
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `insertar punto antes en el indice cero no hace nada`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlAgregarPunto(1.0, 1.0))
        enviar(viewModel, FormularioSenderoEvento.AlAgregarPunto(2.0, 2.0))
        val recorridoAntes = viewModel.estadoVisual.value.recorrido

        enviar(viewModel, FormularioSenderoEvento.AlInsertarPuntoAntes(0))

        assertEquals(recorridoAntes, viewModel.estadoVisual.value.recorrido)
    }

    @Test
    fun `cambiar tipo de punto lo actualiza y marca cambios`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(1.0, 1.0), PuntoRecorrido(2.0, 2.0))

        enviar(viewModel, FormularioSenderoEvento.AlCambiarTipoPunto(0, TipoPunto.MIRADOR))

        assertEquals(2, viewModel.estadoVisual.value.recorrido.size)
        assertEquals(TipoPunto.MIRADOR, viewModel.estadoVisual.value.recorrido[0].tipo)
        assertEquals(TipoPunto.PASO, viewModel.estadoVisual.value.recorrido[1].tipo)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `cambiar nombre de punto lo actualiza y marca cambios`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(1.0, 1.0), PuntoRecorrido(2.0, 2.0))

        enviar(viewModel, FormularioSenderoEvento.AlCambiarNombrePunto(1, "Refugio Sur"))

        assertEquals(2, viewModel.estadoVisual.value.recorrido.size)
        assertEquals("Refugio Sur", viewModel.estadoVisual.value.recorrido[1].nombre)
        assertEquals("", viewModel.estadoVisual.value.recorrido[0].nombre)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `invertir sentido invierte el orden de los puntos y marca cambios`() = runTest {
        val viewModel = crearViewModelConPuntos(
            PuntoRecorrido(1.0, 1.0),
            PuntoRecorrido(2.0, 2.0),
            PuntoRecorrido(3.0, 3.0)
        )

        enviar(viewModel, FormularioSenderoEvento.AlInvertirSentido)

        assertEquals(
            listOf(PuntoRecorrido(3.0, 3.0), PuntoRecorrido(2.0, 2.0), PuntoRecorrido(1.0, 1.0)),
            viewModel.estadoVisual.value.recorrido
        )
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `distancia calculada usa el recorrido y el tipo de recorrido actual`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(0.0, 0.0), PuntoRecorrido(0.0, 1.0))

        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarTipoRecorrido(TipoRecorrido.LINEAL))
        assertEquals(111.195, viewModel.estadoVisual.value.distanciaCalculadaKilometros, 0.05)

        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarTipoRecorrido(TipoRecorrido.IDA_Y_VUELTA))
        assertEquals(222.39, viewModel.estadoVisual.value.distanciaCalculadaKilometros, 0.1)
    }

    @Test
    fun `usar distancia del mapa copia la distancia redondeada a un decimal`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(0.0, 0.0), PuntoRecorrido(0.0, 1.0))
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarTipoRecorrido(TipoRecorrido.LINEAL))

        enviar(viewModel, FormularioSenderoEvento.AlUsarDistanciaDelMapa)

        assertEquals("111.2", viewModel.estadoVisual.value.distancia)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `modo edicion carga el recorrido del sendero sin marcar cambios`() = runTest {
        val recorrido = listOf(
            PuntoRecorrido(-40.5, -71.3, TipoPunto.REFUGIO, "Refugio"),
            PuntoRecorrido(-40.6, -71.4, TipoPunto.PASO, "")
        )
        falsoSendero.senderos.value = listOf(senderoOriginal.copy(recorrido = recorrido))
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()

        assertEquals(recorrido, viewModel.estadoVisual.value.recorrido)
        assertFalse(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `guardar en creacion envia el recorrido`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(-40.5, -71.3), PuntoRecorrido(-40.6, -71.4))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTipoPunto(0, TipoPunto.CAMPING))
        completarFormularioValido(viewModel)

        enviar(viewModel, FormularioSenderoEvento.AlGuardar)

        val guardado = falsoSendero.llamadasGuardarSendero.single().second
        assertEquals(
            listOf(
                PuntoRecorrido(-40.5, -71.3, TipoPunto.CAMPING, ""),
                PuntoRecorrido(-40.6, -71.4, TipoPunto.PASO, "")
            ),
            guardado.recorrido
        )
    }

    @Test
    fun `guardar en edicion envia el recorrido editado`() = runTest {
        falsoSendero.senderos.value = listOf(
            senderoOriginal.copy(recorrido = listOf(PuntoRecorrido(-40.5, -71.3), PuntoRecorrido(-40.6, -71.4)))
        )
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()
        enviar(viewModel, FormularioSenderoEvento.AlInvertirSentido)
        enviar(viewModel, FormularioSenderoEvento.AlAgregarPunto(-41.0, -72.0))

        enviar(viewModel, FormularioSenderoEvento.AlGuardar)

        assertEquals(
            listOf(PuntoRecorrido(-40.6, -71.4), PuntoRecorrido(-40.5, -71.3), PuntoRecorrido(-41.0, -72.0)),
            falsoSendero.llamadasGuardarSendero.single().second.recorrido
        )
    }

    @Test
    fun `indices fuera de rango se ignoran sin excepcion`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(1.0, 1.0), PuntoRecorrido(2.0, 2.0))
        val estadoAntes = viewModel.estadoVisual.value

        enviar(viewModel, FormularioSenderoEvento.AlMoverPunto(5, 9.0, 9.0))
        enviar(viewModel, FormularioSenderoEvento.AlMoverPunto(-1, 9.0, 9.0))
        enviar(viewModel, FormularioSenderoEvento.AlEliminarPunto(2))
        enviar(viewModel, FormularioSenderoEvento.AlEliminarPunto(-1))
        enviar(viewModel, FormularioSenderoEvento.AlInsertarPuntoAntes(7))
        enviar(viewModel, FormularioSenderoEvento.AlInsertarPuntoAntes(-3))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTipoPunto(2, TipoPunto.PELIGRO))
        enviar(viewModel, FormularioSenderoEvento.AlCambiarNombrePunto(-1, "Nada"))
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarPunto(9))

        val estadoDespues = viewModel.estadoVisual.value
        assertEquals(estadoAntes.recorrido, estadoDespues.recorrido)
        assertNull(estadoDespues.indicePuntoSeleccionado)
    }

    @Test
    fun `usar distancia del mapa quita el error del campo distancia`() = runTest {
        val viewModel = crearViewModelConPuntos(PuntoRecorrido(0.0, 0.0), PuntoRecorrido(0.0, 1.0))
        completarFormularioValido(viewModel)
        enviar(viewModel, FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "abc"))
        enviar(viewModel, FormularioSenderoEvento.AlGuardar)
        assertTrue(CampoSendero.DISTANCIA in viewModel.estadoVisual.value.camposConError)

        enviar(viewModel, FormularioSenderoEvento.AlUsarDistanciaDelMapa)

        assertFalse(CampoSendero.DISTANCIA in viewModel.estadoVisual.value.camposConError)
    }

    @Test
    fun `invertir sentido con menos de dos puntos no hace nada`() = runTest {
        val sinPuntos = crearViewModel()
        enviar(sinPuntos, FormularioSenderoEvento.AlInvertirSentido)
        assertFalse(sinPuntos.estadoVisual.value.hayCambiosSinGuardar)

        val conUnPunto = crearViewModelConPuntos(PuntoRecorrido(1.0, 1.0))
        enviar(conUnPunto, FormularioSenderoEvento.AlSeleccionarPunto(0))
        val estadoAntes = conUnPunto.estadoVisual.value

        enviar(conUnPunto, FormularioSenderoEvento.AlInvertirSentido)

        assertEquals(estadoAntes, conUnPunto.estadoVisual.value)
    }

    @Test
    fun `seleccionar foto guarda la ruta de la copia local y marca cambios`() = runTest {
        val viewModel = crearViewModel()

        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarFoto("content://media/picker/1"))

        val estado = viewModel.estadoVisual.value
        assertEquals(listOf("content://media/picker/1"), falsoAlmacenFotos.urisRecibidas)
        assertEquals("/data/fotos_senderos/copia-1.jpg", estado.foto)
        assertFalse(estado.estaProcesandoFoto)
        assertTrue(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `fallo al copiar la foto avisa y no cambia la foto`() = runTest {
        falsoAlmacenFotos.errorAlGuardar = IllegalStateException("Archivo corrupto")
        val viewModel = crearViewModel()

        viewModel.efectoVisual.test {
            enviar(viewModel, FormularioSenderoEvento.AlSeleccionarFoto("content://media/picker/1"))

            assertEquals(FormularioSenderoEfecto.MostrarAviso("No se pudo usar esa foto"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("", viewModel.estadoVisual.value.foto)
        assertFalse(viewModel.estadoVisual.value.estaProcesandoFoto)
    }

    @Test
    fun `quitar foto la deja vacia y marca cambios`() = runTest {
        val viewModel = crearViewModel()
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarFoto("content://media/picker/1"))

        enviar(viewModel, FormularioSenderoEvento.AlQuitarFoto)

        assertEquals("", viewModel.estadoVisual.value.foto)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `guardar en creacion envia la foto elegida`() = runTest {
        val viewModel = crearViewModel()
        completarFormularioValido(viewModel)
        enviar(viewModel, FormularioSenderoEvento.AlSeleccionarFoto("content://media/picker/1"))

        enviar(viewModel, FormularioSenderoEvento.AlGuardar)

        assertEquals("/data/fotos_senderos/copia-1.jpg", falsoSendero.llamadasGuardarSendero.single().second.foto)
    }

    @Test
    fun `modo edicion carga la foto del sendero y la conserva al guardar`() = runTest {
        falsoSendero.senderos.value = listOf(senderoOriginal.copy(foto = "https://res.cloudinary.com/prueba/cerro.jpg"))
        val viewModel = crearViewModel(idSendero = "sendero-1")
        avanzar()

        assertEquals("https://res.cloudinary.com/prueba/cerro.jpg", viewModel.estadoVisual.value.foto)
        enviar(viewModel, FormularioSenderoEvento.AlGuardar)
        assertEquals(
            "https://res.cloudinary.com/prueba/cerro.jpg",
            falsoSendero.llamadasGuardarSendero.single().second.foto
        )
    }
}
