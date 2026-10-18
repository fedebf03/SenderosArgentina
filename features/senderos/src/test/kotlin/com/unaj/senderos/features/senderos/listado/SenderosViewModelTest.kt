package com.unaj.senderos.features.senderos.listado

import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoAlmacenBorradorRecorrido
import com.unaj.senderos.core.domain.usecase.RecuperarRecorridoInterrumpidoUseCase
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosPublicosUseCase
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import com.unaj.senderos.core.testing.crearSenderoPublicoDePrueba
import kotlinx.coroutines.CoroutineScope
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsaSolicitudSincronizacion
import com.unaj.senderos.core.testing.FalsoMonitorConectividad
import com.unaj.senderos.core.testing.FalsoProveedorUbicacion
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private val proveedorUbicacion = FalsoProveedorUbicacion()
    private val monitorConectividad = FalsoMonitorConectividad()
    private val repositorioPublicos = FalsoSenderoPublicoRepositorio()
    private val efectosRecibidos = mutableListOf<SenderosEfecto>()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private val solicitudSincronizacion = FalsaSolicitudSincronizacion()

    private fun crearViewModel(
        propios: List<Sendero> = emptyList(),
        modo: ModoListado = ModoListado.MIS_SENDEROS,
        recolectarEfectos: Boolean = false
    ): SenderosViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val propiosCasoDeUso = ObservarSenderosDelUsuarioUseCase(FalsoSenderoRepositorio(propios), autenticacion)
        val publicosCasoDeUso = ObservarSenderosPublicosUseCase(repositorioPublicos, autenticacion)
        val viewModel = SenderosViewModel(
            modo,
            propiosCasoDeUso,
            publicosCasoDeUso,
            proveedorUbicacion,
            monitorConectividad,
            FalsoControlSeguimiento(),
            FalsoLanzadorSeguimiento(),
            ObtenerUsuarioActualUseCase(autenticacion),
            RecuperarRecorridoInterrumpidoUseCase(FalsoAlmacenBorradorRecorrido(), FalsoRecorridoRepositorio(), FalsoPuntosCapturadosRepositorio(), autenticacion),
            solicitudSincronizacion
        ) { 0L }
        if (recolectarEfectos) {
            CoroutineScope(despachadorPrueba).launch {
                viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
            }
        }
        return viewModel
    }

    private val propio = crearSenderoDePrueba(id = "propio-1", idUsuario = "usuario-1", nombre = "Mi sendero")
    private val favorito = crearSenderoDePrueba(id = "favorito-1", idUsuario = "usuario-1", nombre = "Mi favorito", esFavorito = true)
    private val copia = crearSenderoDePrueba(id = "copia-1", idUsuario = "usuario-1", nombre = "Sendero de otro").copy(idOrigen = "publico-1")
    private val copiaFavorita = crearSenderoDePrueba(id = "copia-2", idUsuario = "usuario-1", nombre = "Otra copia", esFavorito = true).copy(idOrigen = "publico-2")
    private val propioPublicado = crearSenderoDePrueba(id = "propio-2", idUsuario = "usuario-1", nombre = "Mi sendero publicado")
    private val deOtro = crearSenderoPublicoDePrueba(
        crearSenderoDePrueba(id = "publico-1", idUsuario = "autor-1", nombre = "Sendero de otro"),
        nombreAutor = "Luis"
    )
    private val miPublicacion = crearSenderoPublicoDePrueba(
        propioPublicado.copy(descripcion = "version publicada"),
        nombreAutor = "Ana"
    )

    private fun idsVisibles(viewModel: SenderosViewModel) = viewModel.estadoVisual.value.senderosFiltrados.map { it.id }

    @Test
    fun `Explorar muestra solo los senderos publicados`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(propio), ModoListado.EXPLORAR)
        advanceUntilIdle()

        assertEquals(listOf("publico-1"), idsVisibles(viewModel))
        assertFalse(viewModel.estadoVisual.value.estaCargando)
    }

    @Test
    fun `Explorar muestra mi sendero publicado en su version propia`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro, miPublicacion)
        val viewModel = crearViewModel(listOf(propio, propioPublicado), ModoListado.EXPLORAR)
        advanceUntilIdle()

        val visibles = viewModel.estadoVisual.value.senderosFiltrados
        assertEquals(listOf("publico-1", "propio-2"), visibles.map { it.id })
        assertEquals(propioPublicado.descripcion, visibles.last().descripcion)
    }

    @Test
    fun `Explorar sigue mostrando el original aunque ya se haya guardado una copia`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(copia), ModoListado.EXPLORAR)
        advanceUntilIdle()

        assertEquals(listOf("publico-1"), idsVisibles(viewModel))
    }

    @Test
    fun `Explorar sin publicaciones termina de cargar y queda vacio`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(listOf(propio), ModoListado.EXPLORAR)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.estaCargando)
        assertTrue(viewModel.estadoVisual.value.estaVacio)
    }

    @Test
    fun `Explorar sigue cargando hasta recibir los publicos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(listOf(propio), ModoListado.EXPLORAR)

        assertTrue(viewModel.estadoVisual.value.estaCargando)
    }

    @Test
    fun `un fallo al leer los publicos marca el error y deja de cargar`() = runTest(despachadorPrueba) {
        repositorioPublicos.errorEnFlujo = IllegalStateException("PERMISSION_DENIED")
        val viewModel = crearViewModel(listOf(propio), ModoListado.EXPLORAR)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.hayErrorEnPublicos)
        assertFalse(estado.estaCargando)
        assertNull(estado.mensajeError)
    }

    @Test
    fun `los publicos aparecen en vivo sin recargar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(emptyList(), ModoListado.EXPLORAR)
        advanceUntilIdle()
        assertTrue(idsVisibles(viewModel).isEmpty())

        repositorioPublicos.publicos.value = listOf(deOtro)
        advanceUntilIdle()

        assertEquals(listOf("publico-1"), idsVisibles(viewModel))
    }

    @Test
    fun `Mis senderos muestra solo los propios aunque haya publicos`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(propio, copia))
        advanceUntilIdle()

        assertEquals(listOf("propio-1", "copia-1"), idsVisibles(viewModel))
        assertFalse(viewModel.estadoVisual.value.estaCargando)
    }

    @Test
    fun `el chip Favoritos deja solo los favoritos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(listOf(propio, favorito, copiaFavorita))
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlAlternarFavoritos)

        assertEquals(listOf("favorito-1", "copia-2"), idsVisibles(viewModel))
    }

    @Test
    fun `el chip Guardados deja solo las copias de senderos publicos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(listOf(propio, favorito, copia, copiaFavorita))
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlAlternarGuardados)

        assertEquals(listOf("copia-1", "copia-2"), idsVisibles(viewModel))
    }

    @Test
    fun `Favoritos y Guardados juntos dejan los guardados que ademas son favoritos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(listOf(propio, favorito, copia, copiaFavorita))
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlAlternarFavoritos)
        viewModel.procesarEvento(SenderosEvento.AlAlternarGuardados)

        assertEquals(listOf("copia-2"), idsVisibles(viewModel))
    }

    @Test
    fun `tocar un chip dos veces lo desmarca`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(listOf(propio, favorito))
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlAlternarFavoritos)
        viewModel.procesarEvento(SenderosEvento.AlAlternarFavoritos)

        assertFalse(viewModel.estadoVisual.value.soloFavoritos)
        assertEquals(listOf("propio-1", "favorito-1"), idsVisibles(viewModel))
    }

    @Test
    fun `solo los publicos de otros muestran autor`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro, miPublicacion)
        val viewModel = crearViewModel(listOf(propio, propioPublicado), ModoListado.EXPLORAR)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals("Luis", estado.nombreAutorDe(deOtro.sendero))
        assertNull(estado.nombreAutorDe(propioPublicado))
        assertNull(estado.nombreAutorDe(propio))
    }

    @Test
    fun `tocar un sendero publico de otro abre el detalle publico`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(propio), ModoListado.EXPLORAR, recolectarEfectos = true)
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlSeleccionarSendero("publico-1"))
        advanceUntilIdle()

        assertEquals(listOf<SenderosEfecto>(SenderosEfecto.NavegarADetallePublico("publico-1")), efectosRecibidos)
    }

    @Test
    fun `tocar un sendero propio ya publicado abre el detalle propio`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(miPublicacion)
        val viewModel = crearViewModel(listOf(propioPublicado), ModoListado.EXPLORAR, recolectarEfectos = true)
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlSeleccionarSendero("propio-2"))
        advanceUntilIdle()

        assertEquals(listOf<SenderosEfecto>(SenderosEfecto.NavegarADetalle("propio-2")), efectosRecibidos)
    }

    @Test
    fun `al obtener la ubicacion con exito la guarda en el estado`() = runTest(despachadorPrueba) {
        val ubicacion = Coordenada(latitud = -34.6, longitud = -58.4)
        proveedorUbicacion.resultado = Result.success(ubicacion)
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(ubicacion, viewModel.estadoVisual.value.ubicacionActual)
        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `al obtener una ubicacion nula el estado queda sin ubicacion`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(null)
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.ubicacionActual)
        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `al fallar la ubicacion no se rompe y el estado queda sin ubicacion`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.failure(IllegalStateException("GPS apagado"))
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.ubicacionActual)
        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `no consulta la ubicacion antes de recibir el evento`() = runTest(despachadorPrueba) {
        crearViewModel()
        advanceUntilIdle()

        assertEquals(0, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `al cambiar el orden por cercania se actualiza el estado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        assertFalse(viewModel.estadoVisual.value.ordenarPorCercania)

        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = true))
        assertTrue(viewModel.estadoVisual.value.ordenarPorCercania)

        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = false))
        assertFalse(viewModel.estadoVisual.value.ordenarPorCercania)
    }

    @Test
    fun `alternar una dificultad la agrega y al repetirla la quita`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        assertTrue(viewModel.estadoVisual.value.dificultadesSeleccionadas.isEmpty())

        viewModel.procesarEvento(SenderosEvento.AlAlternarDificultad(Dificultad.MEDIA))
        viewModel.procesarEvento(SenderosEvento.AlAlternarDificultad(Dificultad.ALTA))
        assertEquals(setOf(Dificultad.MEDIA, Dificultad.ALTA), viewModel.estadoVisual.value.dificultadesSeleccionadas)

        viewModel.procesarEvento(SenderosEvento.AlAlternarDificultad(Dificultad.MEDIA))
        assertEquals(setOf(Dificultad.ALTA), viewModel.estadoVisual.value.dificultadesSeleccionadas)
    }

    private fun TestScope.recolectarEfectos(viewModel: SenderosViewModel): List<SenderosEfecto> {
        val efectos = mutableListOf<SenderosEfecto>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.efectoVisual.collect { efecto -> efectos.add(efecto) } }
        return efectos
    }

    @Test
    fun `una ubicacion nula no reemplaza una ubicacion valida`() = runTest(despachadorPrueba) {
        val ubicacion = Coordenada(latitud = -34.6, longitud = -58.4)
        proveedorUbicacion.resultado = Result.success(ubicacion)
        val viewModel = crearViewModel()
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        proveedorUbicacion.resultado = Result.success(null)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(ubicacion, viewModel.estadoVisual.value.ubicacionActual)
    }

    @Test
    fun `ignora el pedido de ubicacion si ya hay uno en curso`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(Coordenada(latitud = 1.0, longitud = 1.0))
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `sin ubicacion y con orden por cercania activo emite un aviso`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(null)
        val viewModel = crearViewModel()
        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = true))

        val efectos = recolectarEfectos(viewModel)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(listOf<SenderosEfecto>(SenderosEfecto.MostrarAviso("No pudimos obtener tu ubicación")), efectos)
    }

    @Test
    fun `un fallo de ubicacion con orden por cercania activo emite un aviso`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.failure(IllegalStateException("GPS apagado"))
        val viewModel = crearViewModel()
        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = true))

        val efectos = recolectarEfectos(viewModel)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(listOf<SenderosEfecto>(SenderosEfecto.MostrarAviso("No pudimos obtener tu ubicación")), efectos)
    }

    @Test
    fun `sin orden por cercania no emite aviso si falta la ubicacion`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(null)
        val viewModel = crearViewModel()

        val efectos = recolectarEfectos(viewModel)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(emptyList<SenderosEfecto>(), efectos)
    }

    @Test
    fun `al solicitar el permiso de notificaciones queda registrado en el estado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        assertFalse(viewModel.estadoVisual.value.seSolicitoPermisoNotificaciones)

        viewModel.procesarEvento(SenderosEvento.AlSolicitarPermisoNotificaciones)

        assertTrue(viewModel.estadoVisual.value.seSolicitoPermisoNotificaciones)
    }

    @Test
    fun `el estado refleja la falta de conexion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.estaSinConexion)

        monitorConectividad.hayConexionActual = false
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.estaSinConexion)

        monitorConectividad.hayConexionActual = true
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.estaSinConexion)
    }

    @Test
    fun `tocar sincronizar pide la sincronizacion del usuario`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(propios = listOf(propio))
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlTocarSincronizar)

        assertEquals(listOf("usuario-1"), solicitudSincronizacion.usuariosPedidos)
    }

    @Test
    fun `el estado del trabajo de sincronizacion llega al estado visual`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(propios = listOf(propio))
        advanceUntilIdle()

        solicitudSincronizacion.estado.value = com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion.EN_CURSO
        advanceUntilIdle()

        assertEquals(
            com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion.EN_CURSO,
            viewModel.estadoVisual.value.estadoTrabajoSincronizacion
        )
    }

    @Test
    fun `cuenta los senderos sin subir y los que fallaron`() = runTest(despachadorPrueba) {
        val pendiente = crearSenderoDePrueba(id = "p", estadoSincronizacion = com.unaj.senderos.core.domain.model.EstadoSincronizacion.PENDIENTE)
        val conError = crearSenderoDePrueba(id = "e", estadoSincronizacion = com.unaj.senderos.core.domain.model.EstadoSincronizacion.CON_ERROR)
        val viewModel = crearViewModel(propios = listOf(propio, pendiente, conError))
        advanceUntilIdle()

        assertEquals(1, viewModel.estadoVisual.value.cantidadSinSubir)
        assertEquals(1, viewModel.estadoVisual.value.cantidadConErrorDeSubida)
    }
}
