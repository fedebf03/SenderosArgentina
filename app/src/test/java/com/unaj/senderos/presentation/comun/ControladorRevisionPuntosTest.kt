package com.unaj.senderos.presentation.comun

import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearPuntoCapturadoDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.CoroutineScope
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControladorRevisionPuntosTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val avisos = mutableListOf<String>()
    private val trazado = listOf(
        PuntoRecorrido(latitud = -41.10, longitud = -71.30),
        PuntoRecorrido(latitud = -41.20, longitud = -71.30)
    )
    private val repositorioSenderos = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = trazado)))
    private val repositorioPuntos = FalsoPuntosCapturadosRepositorio(
        listOf(
            crearPuntoCapturadoDePrueba(id = "a", latitud = -41.12, longitud = -71.30, instanteMilisegundos = 1L),
            crearPuntoCapturadoDePrueba(id = "b", latitud = -41.15, longitud = -71.30, tipo = TipoPunto.AGUA, instanteMilisegundos = 2L),
            crearPuntoCapturadoDePrueba(id = "ajeno", idSendero = "otro", instanteMilisegundos = 3L)
        )
    ).also { falso -> falso.senderoRepositorio = repositorioSenderos }

    // Dar al falso de puntos el repositorio de senderos para que incorporar emule la inserción real
    init {
        repositorioPuntos.senderoRepositorio = repositorioSenderos
    }

    // Configurar el despachador principal de las corrutinas de prueba
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    // Restaurar el despachador principal original
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el controlador del sendero de prueba con los falsos compartidos
    private fun crearControlador(): ControladorRevisionPuntos {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        return ControladorRevisionPuntos(
            idSendero = "sendero-1",
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorioSenderos, repositorioPuntos, autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            alcance = CoroutineScope(despachadorPrueba),
            alEmitirAviso = { mensaje -> avisos.add(mensaje) }
        )
    }

    // Verificar que los puntos del sendero llegan marcados por defecto y los de otro sendero no
    @Test
    fun `los puntos pendientes del sendero llegan marcados por defecto`() = runTest(despachadorPrueba) {
        val controlador = crearControlador()
        advanceUntilIdle()

        val estado = controlador.estado.value
        assertEquals(listOf("a", "b"), estado.puntosPendientes.map { punto -> punto.punto.id })
        assertEquals(setOf("a", "b"), estado.idsMarcados)
        assertTrue(estado.hayPuntosPorRevisar)
    }

    // Verificar que alternar un punto lo desmarca y volver a alternarlo lo marca
    @Test
    fun `alternar un punto lo desmarca y lo vuelve a marcar`() = runTest(despachadorPrueba) {
        val controlador = crearControlador()
        advanceUntilIdle()

        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("a"))
        assertEquals(setOf("b"), controlador.estado.value.idsMarcados)

        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("a"))
        assertEquals(setOf("a", "b"), controlador.estado.value.idsMarcados)
    }

    // Verificar que agregar incorpora los marcados al recorrido y descarta los desmarcados
    @Test
    fun `agregar al sendero incorpora los marcados y descarta los desmarcados`() = runTest(despachadorPrueba) {
        val controlador = crearControlador()
        advanceUntilIdle()
        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("b"))

        controlador.procesarEvento(RevisionPuntosEvento.AlAgregarAlSendero)
        advanceUntilIdle()

        val recorridoGuardado = repositorioSenderos.llamadasGuardarSendero.single().second.recorrido
        assertEquals(3, recorridoGuardado.size)
        assertEquals(-41.12, recorridoGuardado[1].latitud, 0.0)
        assertTrue(repositorioPuntos.llamadasDescartar.any { llamada -> llamada.second.toSet() == setOf("b") })
        assertTrue(controlador.estado.value.puntosPendientes.isEmpty())
        assertFalse(controlador.estado.value.estaProcesando)
        assertEquals(listOf("Puntos agregados al sendero"), avisos)
    }

    // Verificar que con todos desmarcados no se toca el sendero y se descartan todos
    @Test
    fun `agregar con todos desmarcados solo descarta`() = runTest(despachadorPrueba) {
        val controlador = crearControlador()
        advanceUntilIdle()
        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("a"))
        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("b"))

        controlador.procesarEvento(RevisionPuntosEvento.AlAgregarAlSendero)
        advanceUntilIdle()

        assertTrue(repositorioSenderos.llamadasGuardarSendero.isEmpty())
        assertTrue(repositorioPuntos.llamadasDescartar.any { llamada -> llamada.second.toSet() == setOf("a", "b") })
        assertTrue(controlador.estado.value.puntosPendientes.isEmpty())
        assertEquals(listOf("Puntos descartados"), avisos)
        assertTrue(repositorioPuntos.llamadasIncorporar.isEmpty())
    }

    // Verificar que incorporar recibe solo los marcados y descartar solo los desmarcados
    @Test
    fun `incorporar recibe solo los marcados`() = runTest(despachadorPrueba) {
        val controlador = crearControlador()
        advanceUntilIdle()
        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("b"))

        controlador.procesarEvento(RevisionPuntosEvento.AlAgregarAlSendero)
        advanceUntilIdle()

        assertEquals(setOf("a"), repositorioPuntos.llamadasIncorporar.single().third)
        assertEquals(setOf("b"), repositorioPuntos.llamadasDescartar.single().second.toSet())
    }

    // Verificar que un punto a más de 100 m del trazado llega desmarcado y con su distancia
    @Test
    fun `un punto lejos del trazado llega desmarcado por defecto`() = runTest(despachadorPrueba) {
        repositorioPuntos.puntos.value = repositorioPuntos.puntos.value +
            crearPuntoCapturadoDePrueba(id = "lejos", latitud = -41.15, longitud = -71.28, instanteMilisegundos = 4L)
        val controlador = crearControlador()
        advanceUntilIdle()

        val estado = controlador.estado.value
        val lejos = estado.puntosPendientes.first { punto -> punto.punto.id == "lejos" }
        assertTrue(lejos.superaDistanciaRecomendada)
        assertTrue((lejos.distanciaAlTrazadoMetros ?: 0.0) > 1_000.0)
        assertEquals(setOf("a", "b"), estado.idsMarcados)
    }

    // Verificar que el usuario puede marcar a mano un punto lejano y que entonces se incorpora
    @Test
    fun `marcar a mano un punto lejano lo incorpora`() = runTest(despachadorPrueba) {
        repositorioPuntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "lejos", latitud = -41.15, longitud = -71.28, instanteMilisegundos = 4L)
        )
        val controlador = crearControlador()
        advanceUntilIdle()
        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("lejos"))
        assertEquals(setOf("lejos"), controlador.estado.value.idsMarcados)

        controlador.procesarEvento(RevisionPuntosEvento.AlAgregarAlSendero)
        advanceUntilIdle()

        assertEquals(setOf("lejos"), repositorioPuntos.llamadasIncorporar.single().third)
        assertEquals(listOf("Puntos agregados al sendero"), avisos)
    }

    // Verificar que sin recorrido no hay distancia ni aviso y el punto queda marcado
    @Test
    fun `sin recorrido el punto no tiene distancia y queda marcado`() = runTest(despachadorPrueba) {
        repositorioSenderos.senderos.value = listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = emptyList()))
        val controlador = crearControlador()
        advanceUntilIdle()

        val estado = controlador.estado.value
        assertTrue(estado.puntosPendientes.all { punto -> punto.distanciaAlTrazadoMetros == null && !punto.superaDistanciaRecomendada })
        assertEquals(setOf("a", "b"), estado.idsMarcados)
    }

    // Verificar que un error técnico desconocido no se muestra y se usa el texto de respaldo
    @Test
    fun `un error tecnico se reemplaza por el texto de respaldo`() = runTest(despachadorPrueba) {
        repositorioPuntos.resultadoDescartar = Result.failure(RuntimeException("SQLiteException: no such table"))
        val controlador = crearControlador()
        advanceUntilIdle()

        controlador.procesarEvento(RevisionPuntosEvento.AlTocarDescartarTodos)
        controlador.procesarEvento(RevisionPuntosEvento.AlConfirmarDescartarTodos)
        advanceUntilIdle()

        assertEquals(listOf("No se pudieron actualizar los puntos"), avisos)
    }

    // Verificar que si el sendero no se puede guardar se avisa el motivo y no se descarta nada
    @Test
    fun `un fallo al incorporar avisa y conserva los puntos`() = runTest(despachadorPrueba) {
        repositorioSenderos.resultadoGuardarSendero = Result.failure(IllegalStateException("No se pudo guardar el sendero"))
        val controlador = crearControlador()
        advanceUntilIdle()
        controlador.procesarEvento(RevisionPuntosEvento.AlAlternarPunto("b"))

        controlador.procesarEvento(RevisionPuntosEvento.AlAgregarAlSendero)
        advanceUntilIdle()

        assertEquals(listOf("No se pudo guardar el sendero"), avisos)
        assertTrue(repositorioPuntos.llamadasDescartar.isEmpty())
        assertEquals(2, controlador.estado.value.puntosPendientes.size)
        assertFalse(controlador.estado.value.estaProcesando)
    }

    // Verificar que descartar todos exige confirmación antes de borrar
    @Test
    fun `descartar todos pide confirmacion y no borra hasta confirmar`() = runTest(despachadorPrueba) {
        val controlador = crearControlador()
        advanceUntilIdle()

        controlador.procesarEvento(RevisionPuntosEvento.AlTocarDescartarTodos)
        advanceUntilIdle()
        assertTrue(controlador.estado.value.esConfirmarDescartarTodosVisible)
        assertTrue(repositorioPuntos.llamadasDescartar.isEmpty())

        controlador.procesarEvento(RevisionPuntosEvento.AlCancelarDescartarTodos)
        assertFalse(controlador.estado.value.esConfirmarDescartarTodosVisible)
        assertEquals(2, controlador.estado.value.puntosPendientes.size)

        controlador.procesarEvento(RevisionPuntosEvento.AlTocarDescartarTodos)
        controlador.procesarEvento(RevisionPuntosEvento.AlConfirmarDescartarTodos)
        advanceUntilIdle()

        assertEquals(setOf("a", "b"), repositorioPuntos.llamadasDescartar.single().second.toSet())
        assertTrue(controlador.estado.value.puntosPendientes.isEmpty())
        assertFalse(controlador.estado.value.esConfirmarDescartarTodosVisible)
        assertEquals(listOf("Puntos descartados"), avisos)
    }

    // Verificar que abrir y cerrar la revisión alterna su visibilidad y que se cierra sola al quedar sin puntos
    @Test
    fun `la revision se abre se cierra y se cierra sola sin puntos`() = runTest(despachadorPrueba) {
        val controlador = crearControlador()
        advanceUntilIdle()

        controlador.procesarEvento(RevisionPuntosEvento.AlAbrir)
        assertTrue(controlador.estado.value.esRevisionVisible)
        controlador.procesarEvento(RevisionPuntosEvento.AlCerrar)
        assertFalse(controlador.estado.value.esRevisionVisible)

        controlador.procesarEvento(RevisionPuntosEvento.AlAbrir)
        repositorioPuntos.puntos.value = emptyList()
        advanceUntilIdle()
        assertFalse(controlador.estado.value.esRevisionVisible)
    }

    // Verificar que un fallo al descartar todos avisa el motivo y deja los puntos
    @Test
    fun `un fallo al descartar todos avisa el motivo`() = runTest(despachadorPrueba) {
        repositorioPuntos.resultadoDescartar = Result.failure(IllegalStateException("No se pudo descartar"))
        val controlador = crearControlador()
        advanceUntilIdle()

        controlador.procesarEvento(RevisionPuntosEvento.AlTocarDescartarTodos)
        controlador.procesarEvento(RevisionPuntosEvento.AlConfirmarDescartarTodos)
        advanceUntilIdle()

        assertEquals(listOf("No se pudo descartar"), avisos)
        assertEquals(2, controlador.estado.value.puntosPendientes.size)
    }
}
