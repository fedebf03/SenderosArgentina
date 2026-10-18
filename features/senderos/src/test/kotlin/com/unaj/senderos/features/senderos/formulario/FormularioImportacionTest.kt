package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.port.ArchivoLeido
import com.unaj.senderos.core.domain.usecase.CalcularPerfilDeTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ImportarTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Importar un trazado GPX o CSV desde el formulario del sendero
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioImportacionTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val fuente = FalsoFuenteElevacion()
    private val lector = FalsoLectorArchivos()
    private val efectosRecibidos = mutableListOf<FormularioSenderoEfecto>()

    // Una ida y vuelta por el mismo camino, con altura, como el archivo de la costanera de El Calafate
    private val csvIdaYVuelta: String = run {
        val ida = List(50) { indice -> "-50.335,${-72.27 - indice * 0.0002},${180 + indice * 0.2}" }
        (listOf("Latitude,Longitude,Elevation") + ida + ida.reversed().drop(1)).joinToString("\n")
    }

    // Usar el despachador de prueba como principal
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        lector.archivos["content://costanera"] = ArchivoLeido("Costanera_de_El_Calafate.csv", csvIdaYVuelta)
    }

    // Devolver el despachador principal original
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del formulario y juntar sus efectos
    private fun crearViewModel(): FormularioSenderoViewModel {
        val repositorio = FalsoSenderoRepositorio()
        val viewModel = FormularioSenderoViewModel(
            null,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            GuardarSenderoUseCase(repositorio, autenticacion),
            FalsoAlmacenFotos(),
            CalcularPerfilDeTrazadoUseCase(fuente),
            ImportarTrazadoUseCase(lector, despachadorPrueba)
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    @Test
    fun `importar en un sendero vacio completa recorrido tipo nombre distancia y perfil del archivo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()

        viewModel.procesarEvento(FormularioSenderoEvento.AlImportarTrazado("content://costanera"))
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals(TipoRecorrido.IDA_Y_VUELTA, estado.tipoRecorrido)
        assertEquals("Costanera de El Calafate", estado.nombre)
        assertTrue(estado.recorrido.size >= 2)
        assertFalse(estado.distanciaEsManual)
        assertEquals(formatearDistanciaEsperada(estado), estado.distancia)
        assertEquals("190", estado.alturaMaxima)
        assertEquals("180", estado.alturaMinima)
        assertTrue("el perfil sale del archivo, sin pedirlo a la red", fuente.llamadas.isEmpty())
        assertTrue(estado.hayCambiosSinGuardar)
        assertFalse(estado.estaImportandoTrazado)
    }

    @Test
    fun `importar no pisa un nombre ya escrito`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Costanera del Lago Argentino"))

        viewModel.procesarEvento(FormularioSenderoEvento.AlImportarTrazado("content://costanera"))
        advanceUntilIdle()

        assertEquals("Costanera del Lago Argentino", viewModel.estadoVisual.value.nombre)
    }

    @Test
    fun `con un recorrido ya marcado pide confirmar antes de reemplazarlo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-41.0, -71.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-41.01, -71.0))

        viewModel.procesarEvento(FormularioSenderoEvento.AlImportarTrazado("content://costanera"))
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.trazadoImportadoPendiente != null)
        assertEquals(-41.0, estado.recorrido.first().latitud, 0.0)
    }

    @Test
    fun `confirmar el reemplazo aplica el trazado importado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-41.0, -71.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlImportarTrazado("content://costanera"))
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlConfirmarReemplazoTrazado)

        val estado = viewModel.estadoVisual.value
        assertNull(estado.trazadoImportadoPendiente)
        assertEquals(-50.335, estado.recorrido.first().latitud, 1e-9)
    }

    @Test
    fun `cancelar el reemplazo deja el recorrido como estaba`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-41.0, -71.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlImportarTrazado("content://costanera"))
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlCancelarReemplazoTrazado)

        assertNull(viewModel.estadoVisual.value.trazadoImportadoPendiente)
        assertEquals(1, viewModel.estadoVisual.value.recorrido.size)
    }

    @Test
    fun `un archivo que no se puede importar avisa el motivo`() = runTest(despachadorPrueba) {
        lector.archivos["content://foto"] = ArchivoLeido("foto.jpg", "no es un trazado")
        val viewModel = crearViewModel()

        viewModel.procesarEvento(FormularioSenderoEvento.AlImportarTrazado("content://foto"))
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.recorrido.isEmpty())
        assertFalse(viewModel.estadoVisual.value.estaImportandoTrazado)
        assertEquals(
            listOf<FormularioSenderoEfecto>(FormularioSenderoEfecto.MostrarAviso("El archivo no tiene puntos de recorrido válidos")),
            efectosRecibidos
        )
    }

    // Escribir la distancia del mapa como la escribe el formulario
    private fun formatearDistanciaEsperada(estado: FormularioSenderoEstadoVisual): String =
        com.unaj.senderos.core.ui.comun.formatearDecimalEditable(Math.round(estado.distanciaCalculadaKilometros * 10) / 10.0)
}
