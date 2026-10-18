package com.unaj.senderos.features.perfil

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.port.ArchivoLeido
import com.unaj.senderos.core.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.core.domain.usecase.ExportarCopiaSeguridadUseCase
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ImportarCopiaSeguridadUseCase
import com.unaj.senderos.core.domain.usecase.ModoRestauracion
import com.unaj.senderos.core.domain.usecase.ObservarRecorridosUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.usecase.SincronizarPerfilPendienteUseCase
import com.unaj.senderos.core.testing.FalsoPerfilRepositorio
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoEscritorArchivos
import com.unaj.senderos.core.testing.FalsoFormatoCopiaSeguridad
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import com.unaj.senderos.core.testing.FalsoNotificador
import com.unaj.senderos.core.testing.FalsoPreferenciasAplicacion
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
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
import org.junit.Before
import org.junit.Test

// Copia de seguridad desde el Perfil: exportar todos los senderos y restaurar una copia
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilCopiaSeguridadTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val senderos = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(id = "s1"), crearSenderoDePrueba(id = "s2")))
    private val escritor = FalsoEscritorArchivos()
    private val lector = FalsoLectorArchivos()
    private val formato = FalsoFormatoCopiaSeguridad()
    private val efectosRecibidos = mutableListOf<PerfilEfecto>()

    // Usar el despachador de prueba como principal
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    // Devolver el despachador principal original
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del Perfil con los casos de uso de la copia y juntar sus efectos
    private fun crearViewModel(): PerfilViewModel {
        val viewModel = PerfilViewModel(
            obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacion),
            cerrarSesionUseCase = CerrarSesionUseCase(autenticacion, senderos, FalsoLanzadorSeguimiento(), FalsoNotificador()),
            preferenciasAplicacion = FalsoPreferenciasAplicacion(),
            observarRecorridosUseCase = ObservarRecorridosUseCase(FalsoRecorridoRepositorio(), autenticacion),
            controlSeguimiento = FalsoControlSeguimiento(),
            exportarCopiaSeguridadUseCase = ExportarCopiaSeguridadUseCase(senderos, autenticacion, escritor, formato, despachadorCalculo = despachadorPrueba),
            importarCopiaSeguridadUseCase = ImportarCopiaSeguridadUseCase(
                senderos, autenticacion, lector, formato,
                GuardarSenderoUseCase(senderos, autenticacion, generarIdentificador = { "nuevo-id" }),
                despachadorPrueba
            ),
            sincronizarPerfilPendienteUseCase = SincronizarPerfilPendienteUseCase(FalsoPerfilRepositorio(), autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    @Test
    fun `exportar la copia avisa cuantos senderos se guardaron`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()

        viewModel.procesarEvento(PerfilEvento.AlExportarCopiaSeguridad("content://copia"))
        advanceUntilIdle()

        assertEquals(listOf<PerfilEfecto>(PerfilEfecto.MostrarAviso("Copia guardada: 2 senderos")), efectosRecibidos)
    }

    @Test
    fun `una copia sin senderos repetidos se restaura despues de confirmar`() = runTest(despachadorPrueba) {
        formato.registrar("texto", listOf(crearSenderoDePrueba(id = "s9", nombre = "Nuevo")))
        lector.archivos["content://copia"] = ArchivoLeido("senderos.json", "texto")
        val viewModel = crearViewModel()

        viewModel.procesarEvento(PerfilEvento.AlImportarCopiaSeguridad("content://copia"))
        advanceUntilIdle()
        assertEquals(1, viewModel.estadoVisual.value.copiaPendiente?.nuevos?.size)

        viewModel.procesarEvento(PerfilEvento.AlElegirModoRestauracion(ModoRestauracion.SOLO_NUEVOS))
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.copiaPendiente)
        assertEquals(listOf<PerfilEfecto>(PerfilEfecto.MostrarAviso("Se restauraron 1 de 1 senderos")), efectosRecibidos)
    }

    @Test
    fun `con senderos repetidos pregunta que hacer y aplica lo elegido`() = runTest(despachadorPrueba) {
        formato.registrar("texto", listOf(crearSenderoDePrueba(id = "s1", nombre = "Restaurado"), crearSenderoDePrueba(id = "s9")))
        lector.archivos["content://copia"] = ArchivoLeido("senderos.json", "texto")
        val viewModel = crearViewModel()
        viewModel.procesarEvento(PerfilEvento.AlImportarCopiaSeguridad("content://copia"))
        advanceUntilIdle()
        assertEquals(1, viewModel.estadoVisual.value.copiaPendiente?.existentes?.size)

        viewModel.procesarEvento(PerfilEvento.AlElegirModoRestauracion(ModoRestauracion.REEMPLAZAR_EXISTENTES))
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.copiaPendiente)
        assertEquals("Restaurado", senderos.senderos.value.single { sendero -> sendero.id == "s1" }.nombre)
        assertEquals(listOf<PerfilEfecto>(PerfilEfecto.MostrarAviso("Se restauraron 2 de 2 senderos")), efectosRecibidos)
    }

    @Test
    fun `cancelar la restauracion no guarda nada`() = runTest(despachadorPrueba) {
        formato.registrar("texto", listOf(crearSenderoDePrueba(id = "s1", nombre = "Restaurado")))
        lector.archivos["content://copia"] = ArchivoLeido("senderos.json", "texto")
        val viewModel = crearViewModel()
        viewModel.procesarEvento(PerfilEvento.AlImportarCopiaSeguridad("content://copia"))
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlCancelarRestauracion)

        assertNull(viewModel.estadoVisual.value.copiaPendiente)
        assertEquals("Cerro Catedral", senderos.senderos.value.single { sendero -> sendero.id == "s1" }.nombre)
    }

    @Test
    fun `un archivo que no es una copia avisa el motivo`() = runTest(despachadorPrueba) {
        lector.archivos["content://otro"] = ArchivoLeido("foto.jpg", "otra cosa")
        val viewModel = crearViewModel()

        viewModel.procesarEvento(PerfilEvento.AlImportarCopiaSeguridad("content://otro"))
        advanceUntilIdle()

        assertEquals(
            listOf<PerfilEfecto>(PerfilEfecto.MostrarAviso("El archivo no es una copia de seguridad de Senderos")),
            efectosRecibidos
        )
    }
}
