package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.domain.usecase.RetirarPuntosIncorporadosUseCase
import com.unaj.senderos.core.domain.usecase.ConvertirCaminataEnBorradorUseCase
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.CalcularPerfilDeTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ImportarTrazadoUseCase
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
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

// Editor de recorrido: modo para marcar puntos de interés con tipo, nombre y foto, e "Insertar punto antes"
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioEditorPuntosTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val almacenFotos = FalsoAlmacenFotos()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): FormularioSenderoViewModel {
        val repositorio = FalsoSenderoRepositorio()
        return FormularioSenderoViewModel(
            null,
            null,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            GuardarSenderoUseCase(repositorio, autenticacion),
            almacenFotos,
            CalcularPerfilDeTrazadoUseCase(FalsoFuenteElevacion()),
            ImportarTrazadoUseCase(FalsoLectorArchivos()),
            ConvertirCaminataEnBorradorUseCase(FalsoRecorridoRepositorio(), FalsoPuntosCapturadosRepositorio(), autenticacion),
            RetirarPuntosIncorporadosUseCase(FalsoPuntosCapturadosRepositorio(), autenticacion)
        )
    }

    // Trazar un camino recto de tres puntos hacia el este
    private fun trazarCamino(viewModel: FormularioSenderoViewModel) {
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.01))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.02))
    }

    @Test
    fun `el editor arranca en modo trazar camino`() {
        val viewModel = crearViewModel()

        assertEquals(ModoEditorRecorrido.TRAZAR_CAMINO, viewModel.estadoVisual.value.modoEditor)
    }

    @Test
    fun `en modo punto de interes tocar el mapa abre el menu sin agregar el punto todavia`() {
        val viewModel = crearViewModel()
        trazarCamino(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))

        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0005, 0.004))

        val estado = viewModel.estadoVisual.value
        assertEquals(3, estado.recorrido.size)
        assertEquals(PuntoDeInteresNuevo(latitud = 0.0005, longitud = 0.004), estado.puntoDeInteresNuevo)
    }

    @Test
    fun `guardar el punto de interes lo inserta en el tramo mas cercano con tipo nombre y foto`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazarCamino(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0005, 0.004))
        viewModel.procesarEvento(FormularioSenderoEvento.AlElegirTipoPuntoNuevo(TipoPunto.AGUA))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarNombrePuntoNuevo("Arroyo"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo("content://foto/1"))
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardarPuntoNuevo)

        val estado = viewModel.estadoVisual.value
        assertNull(estado.puntoDeInteresNuevo)
        assertEquals(4, estado.recorrido.size)
        assertEquals(
            PuntoRecorrido(latitud = 0.0005, longitud = 0.004, tipo = TipoPunto.AGUA, nombre = "Arroyo", foto = "/fotos/copia-1.jpg"),
            estado.recorrido[1]
        )
        assertTrue(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `cancelar el punto de interes no agrega nada y borra la foto copiada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazarCamino(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0005, 0.004))
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo("content://foto/1"))
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlCancelarPuntoNuevo)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.puntoDeInteresNuevo)
        assertEquals(3, viewModel.estadoVisual.value.recorrido.size)
        assertEquals(listOf("/fotos/copia-1.jpg"), almacenFotos.rutasBorradas)
    }

    // Una copia lenta que termina cuando ya se abrió otro punto no se le pega: se borra
    @Test
    fun `una foto que termina de copiarse despues de cancelar no se pega al punto siguiente`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo("content://foto/1"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCancelarPuntoNuevo)
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.01))

        advanceUntilIdle()

        assertEquals("", viewModel.estadoVisual.value.puntoDeInteresNuevo?.foto)
        assertEquals(listOf("/fotos/copia-1.jpg"), almacenFotos.rutasBorradas)
    }

    @Test
    fun `cerrar el editor con el menu abierto lo descarta`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)

        assertNull(viewModel.estadoVisual.value.puntoDeInteresNuevo)
    }

    @Test
    fun `quitar la foto del punto de interes la borra`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo("content://foto/1"))
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlQuitarFotoPuntoNuevo)
        advanceUntilIdle()

        assertEquals("", viewModel.estadoVisual.value.puntoDeInteresNuevo?.foto)
        assertEquals(listOf("/fotos/copia-1.jpg"), almacenFotos.rutasBorradas)
    }

    @Test
    fun `una foto que no se puede copiar deja el punto sin foto y avisa`() = runTest(despachadorPrueba) {
        almacenFotos.error = IllegalStateException("sin espacio")
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))

        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo("content://foto/1"))
        advanceUntilIdle()

        val punto = viewModel.estadoVisual.value.puntoDeInteresNuevo
        assertEquals("", punto?.foto)
        assertFalse(punto?.estaProcesandoFoto ?: true)
        assertEquals("No se pudo guardar la foto del punto", punto?.mensajeError)
    }

    @Test
    fun `un fallo de la camara se muestra en el menu del punto`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))

        viewModel.procesarEvento(FormularioSenderoEvento.AlFallarFotoPuntoNuevo("No hay una aplicación de cámara disponible"))

        assertEquals("No hay una aplicación de cámara disponible", viewModel.estadoVisual.value.puntoDeInteresNuevo?.mensajeError)
    }

    @Test
    fun `volver al modo trazar camino hace que tocar el mapa agregue al final`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.MARCAR_PUNTO_DE_INTERES))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarModoEditor(ModoEditorRecorrido.TRAZAR_CAMINO))

        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))

        assertEquals(1, viewModel.estadoVisual.value.recorrido.size)
        assertNull(viewModel.estadoVisual.value.puntoDeInteresNuevo)
    }

    @Test
    fun `elegir una foto para un punto existente la copia y se la asigna`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazarCamino(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarPunto(1))

        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoDePunto(1, "content://foto/1"))
        assertEquals(1, viewModel.estadoVisual.value.indicePuntoCopiandoFoto)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals("/fotos/copia-1.jpg", estado.recorrido[1].foto)
        assertNull(estado.indicePuntoCopiandoFoto)
        assertTrue(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `quitar la foto de un punto existente la deja vacia`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazarCamino(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoDePunto(1, "content://foto/1"))
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlQuitarFotoDePunto(1))

        assertEquals("", viewModel.estadoVisual.value.recorrido[1].foto)
    }

    // El punto pudo borrarse o moverse de lugar en la lista mientras se copiaba: la foto no se le pone a otro
    @Test
    fun `si el punto se elimina mientras se copia la foto la copia se descarta`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazarCamino(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoDePunto(1, "content://foto/1"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlEliminarPunto(1))

        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.recorrido.all { punto -> punto.foto.isEmpty() })
        assertNull(estado.indicePuntoCopiandoFoto)
        assertEquals(listOf("/fotos/copia-1.jpg"), almacenFotos.rutasBorradas)
    }

    @Test
    fun `una foto de punto que no se puede copiar deja el punto como estaba`() = runTest(despachadorPrueba) {
        almacenFotos.error = IllegalStateException("sin espacio")
        val viewModel = crearViewModel()
        trazarCamino(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFotoDePunto(1, "content://foto/1"))
        advanceUntilIdle()

        assertEquals("", viewModel.estadoVisual.value.recorrido[1].foto)
        assertNull(viewModel.estadoVisual.value.indicePuntoCopiandoFoto)
    }

    @Test
    fun `insertar punto antes agrega el punto medio y conserva la seleccion`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.02))
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarPunto(1))

        viewModel.procesarEvento(FormularioSenderoEvento.AlInsertarPuntoAntes(1))

        val estado = viewModel.estadoVisual.value
        assertEquals(3, estado.recorrido.size)
        assertEquals(0.01, estado.recorrido[1].longitud, 1e-9)
        assertEquals(2, estado.indicePuntoSeleccionado)
        assertTrue(estado.hayCambiosSinGuardar)
    }
}
