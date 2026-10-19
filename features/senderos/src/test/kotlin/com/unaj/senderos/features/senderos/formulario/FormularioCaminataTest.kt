package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.CalcularPerfilDeTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ConvertirCaminataEnBorradorUseCase
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ImportarTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.RetirarPuntosIncorporadosUseCase
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearPuntoCapturadoDePrueba
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import com.unaj.senderos.core.testing.crearSenderoDePrueba
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val ID_CAMINATA = "libre-1000"

// Formulario de un sendero nuevo abierto desde una caminata libre: llega con el trazado y los puntos cargados y, al guardar, retira los puntos pendientes
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioCaminataTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val senderos = FalsoSenderoRepositorio()
    private val recorridos = FalsoRecorridoRepositorio()
    private val puntos = FalsoPuntosCapturadosRepositorio()
    private val efectosRecibidos = mutableListOf<FormularioSenderoEfecto>()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        val trazado = List(40) { indice -> Coordenada(-41.13, -71.30 + indice * 0.0002) }
        recorridos.recorridos.value = listOf(
            crearRecorridoDePrueba(idSendero = ID_CAMINATA, inicioMilisegundos = 1_000L).copy(trazadoCaminado = trazado)
        )
        puntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "p1", idSendero = ID_CAMINATA, latitud = -41.13, longitud = -71.2955, tipo = TipoPunto.AGUA, nota = "Arroyo")
        )
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del formulario con o sin caminata de origen y juntar sus efectos
    private fun crearViewModel(idCaminata: String?): FormularioSenderoViewModel {
        val viewModel = FormularioSenderoViewModel(
            null,
            idCaminata,
            ObservarDetalleSenderoUseCase(senderos, autenticacion),
            GuardarSenderoUseCase(senderos, autenticacion),
            FalsoAlmacenFotos(),
            CalcularPerfilDeTrazadoUseCase(FalsoFuenteElevacion()),
            ImportarTrazadoUseCase(FalsoLectorArchivos(), despachadorPrueba),
            ConvertirCaminataEnBorradorUseCase(recorridos, puntos, autenticacion),
            RetirarPuntosIncorporadosUseCase(puntos, autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    // Completar los campos obligatorios que la caminata no puede traer
    private fun completarCamposObligatorios(viewModel: FormularioSenderoViewModel) {
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.REGION, "Rio Negro"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESCRIPCION, "Caminata por la costa"))
    }

    @Test
    fun `un formulario abierto desde una caminata llega con el trazado, el nombre y la distancia cargados`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.recorrido.size >= 2)
        assertEquals(TipoRecorrido.LINEAL, estado.tipoRecorrido)
        assertTrue(estado.nombre.startsWith("Caminata del "))
        assertTrue(estado.distanciaCalculadaKilometros > 0.0)
        assertTrue(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `los puntos capturados de la caminata aparecen en el recorrido del formulario`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()

        val interes = viewModel.estadoVisual.value.recorrido.filter { punto -> punto.tipo != TipoPunto.PASO }
        assertEquals(listOf("Arroyo"), interes.map { punto -> punto.nombre })
    }

    @Test
    fun `al guardar el sendero nuevo se retiran los puntos capturados de la caminata`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()
        completarCamposObligatorios(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals(1, senderos.llamadasGuardarSendero.size)
        assertEquals(listOf("p1"), puntos.llamadasRetirar.single().second.toList())
    }

    @Test
    fun `si el guardado falla los puntos de la caminata se conservan`() = runTest(despachadorPrueba) {
        senderos.resultadoGuardarSendero = Result.failure(IllegalStateException("Sin espacio"))
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()
        completarCamposObligatorios(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertTrue(puntos.llamadasRetirar.isEmpty())
        assertEquals(1, puntos.puntos.value.size)
    }

    @Test
    fun `si la caminata no se puede convertir el formulario queda vacio y avisa`() = runTest(despachadorPrueba) {
        recorridos.recorridos.value = emptyList()
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.recorrido.isEmpty())
        assertTrue(efectosRecibidos.any { efecto -> efecto is FormularioSenderoEfecto.MostrarAviso })
    }

    @Test
    fun `un formulario nuevo sin caminata no toca los puntos capturados`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(null)
        advanceUntilIdle()
        completarCamposObligatorios(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Cerro Nuevo"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.05))

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertTrue(puntos.llamadasRetirar.isEmpty())
    }

    @Test
    fun `el sendero que nace de una caminata tiene siempre el mismo identificador`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()
        completarCamposObligatorios(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals("desde-$ID_CAMINATA", senderos.llamadasGuardarSendero.single().second.id)
    }

    @Test
    fun `una caminata que todavia no se convirtio no pregunta nada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.nombreSenderoYaConvertido)
    }

    @Test
    fun `una caminata ya convertida pregunta con el nombre del sendero existente`() = runTest(despachadorPrueba) {
        senderos.senderos.value = listOf(crearSenderoDePrueba(id = "desde-$ID_CAMINATA", nombre = "Costanera"))
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()

        assertEquals("Costanera", viewModel.estadoVisual.value.nombreSenderoYaConvertido)
    }

    @Test
    fun `reemplazar cierra la pregunta y deja seguir con el formulario`() = runTest(despachadorPrueba) {
        senderos.senderos.value = listOf(crearSenderoDePrueba(id = "desde-$ID_CAMINATA", nombre = "Costanera"))
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlReemplazarSenderoDeCaminata)

        assertNull(viewModel.estadoVisual.value.nombreSenderoYaConvertido)
        assertTrue(viewModel.estadoVisual.value.recorrido.size >= 2)
    }

    @Test
    fun `editar el existente pide abrir ese sendero`() = runTest(despachadorPrueba) {
        senderos.senderos.value = listOf(crearSenderoDePrueba(id = "desde-$ID_CAMINATA", nombre = "Costanera"))
        val viewModel = crearViewModel(ID_CAMINATA)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlEditarSenderoDeCaminata)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.contains(FormularioSenderoEfecto.EditarSenderoExistente("desde-$ID_CAMINATA")))
    }
}
