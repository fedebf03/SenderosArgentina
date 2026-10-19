package com.unaj.senderos.core.data.sincronizacion

import com.unaj.senderos.core.database.PuntoRecorridoEntidad
import com.unaj.senderos.core.data.repository.SenderoRepositorioOfflineFirst
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.network.PuntoRecorridoRed
import com.unaj.senderos.core.network.SenderoRed
import com.unaj.senderos.core.domain.port.ErrorElevacionException
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.domain.usecase.CalcularPerfilElevacionUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.data.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.data.falsos.FalsoPlanificadorSincronizacion
import com.unaj.senderos.core.data.falsos.FalsoSenderoDao
import com.unaj.senderos.core.data.falsos.FalsoSenderoFuenteRemota
import com.unaj.senderos.core.data.falsos.FalsoSubidorFotos
import com.unaj.senderos.core.data.falsos.crearEntidadDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizadorPerfilesTest {

    private val idUsuario = "usuario-1"
    private val ahora = 1_000_000L
    private val fuenteRemota = FalsoSenderoFuenteRemota()
    private val fuenteElevacion = FalsoFuenteElevacion()
    private val trazado = listOf(PuntoRecorridoEntidad(-40.0, -70.0, "PASO", ""), PuntoRecorridoEntidad(-40.01, -70.0, "PASO", ""))
    private val firmaVigente = CalculadoraPerfil.firmaTrazado(
        trazado.map { punto -> com.unaj.senderos.core.domain.model.PuntoRecorrido(punto.latitud, punto.longitud) },
        TipoRecorrido.LINEAL
    )

    private fun crearSincronizador(dao: FalsoSenderoDao): SincronizadorSenderos {
        val repositorio = SenderoRepositorioOfflineFirst(dao, FalsoPlanificadorSincronizacion(), FalsoEjecutorTransacciones()) { ahora }
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = idUsuario, email = "ana@correo.com"))
        val casoDeUso = CalcularPerfilElevacionUseCase(repositorio, fuenteElevacion, autenticacion)
        return SincronizadorSenderos(
            dao, fuenteRemota, FalsoEjecutorTransacciones(), FalsoSubidorFotos(),
            calculadorPerfiles = casoDeUso
        ) { ahora }
    }

    private fun entidadSinPerfil(id: String = "sendero-1", desnivel: Int = 0) = crearEntidadDePrueba(
        id = id,
        fechaModificacion = ahora - 1_000L,
        recorrido = trazado,
        tipoRecorrido = "LINEAL",
        desnivelMetros = desnivel,
        alturaMaximaMetros = 0,
        alturaMinimaMetros = 0
    )

    @Test
    fun `la sincronizacion calcula el perfil faltante y lo sube en la misma pasada`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil()))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        val local = dao.entidades.value.single()
        assertEquals("SINCRONIZADO", local.estadoSincronizacion)
        assertEquals(firmaVigente, local.perfilElevacion?.firmaTrazado)
        assertEquals(2, fuenteRemota.senderosGuardados.size)
        assertNull(fuenteRemota.senderosGuardados.first().second.perfilElevacion)
        val remoto = fuenteRemota.senderosGuardados.last().second
        assertEquals(firmaVigente, remoto.perfilElevacion?.firmaTrazado)
        assertEquals(1100, remoto.alturaMaximaMetros)
    }

    @Test
    fun `un sendero pendiente con fecha vieja sigue contando como guardado sin conexion aunque se le calcule el perfil`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil().copy(fechaModificacion = ahora - 5 * 60_000L)))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        assertEquals(1, resumen.senderosGuardadosSinConexion)
        assertEquals(ahora - 5 * 60_000L, dao.entidades.value.single().fechaModificacion)
        assertNotNull(dao.entidades.value.single().perfilElevacion)
    }

    @Test
    fun `un sendero ya sincronizado con perfil nuevo se sube sin contar como guardado sin conexion`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil().copy(fechaModificacion = ahora - 5 * 60_000L, estadoSincronizacion = "SINCRONIZADO")))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        assertEquals(0, resumen.senderosGuardadosSinConexion)
        assertEquals(firmaVigente, fuenteRemota.senderosGuardados.single().second.perfilElevacion?.firmaTrazado)
        assertEquals("SINCRONIZADO", dao.entidades.value.single().estadoSincronizacion)
    }

    @Test
    fun `una edicion remota mas nueva no es pisada por el perfil calculado localmente`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil().copy(estadoSincronizacion = "SINCRONIZADO", nombre = "Local")))
        fuenteRemota.precargar(
            idUsuario,
            SenderoRed(
                id = "sendero-1", nombre = "Editado en otro dispositivo", tipoRecorrido = "LINEAL",
                fechaModificacion = ahora - 500L,
                recorrido = listOf(PuntoRecorridoRed(-40.0, -70.0), PuntoRecorridoRed(-40.01, -70.0))
            )
        )

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        val local = dao.entidades.value.single()
        assertEquals("Editado en otro dispositivo", local.nombre)
        assertEquals(ahora - 500L, local.fechaModificacion)
        assertNotNull(local.perfilElevacion)
        assertEquals(ahora - 500L, fuenteRemota.senderosGuardados.last().second.fechaModificacion)
    }

    @Test
    fun `los cambios del usuario se suben antes de consultar las alturas`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil()))

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertNull(fuenteRemota.senderosGuardados.first().second.perfilElevacion)
    }

    @Test
    fun `cada sincronizacion calcula a lo sumo cinco perfiles`() = runTest {
        val dao = FalsoSenderoDao((1..7).map { numero -> entidadSinPerfil("s$numero") })

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(5, fuenteElevacion.llamadas.size)
        assertEquals(5, dao.entidades.value.count { entidad -> entidad.perfilElevacion != null })
    }

    @Test
    fun `una limitacion del servicio corta la pasada`() = runTest {
        fuenteElevacion.falloDevuelto = ErrorElevacionException("limite", esLimitacionDelServicio = true)
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil("a"), entidadSinPerfil("b"), entidadSinPerfil("c")))

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, fuenteElevacion.llamadas.size)
    }

    @Test
    fun `un rechazo definitivo no se reintenta en las sincronizaciones siguientes y no frena a los demas`() = runTest {
        fuenteElevacion.falloDevuelto = ErrorElevacionException("rechazado", esRechazoDefinitivo = true)
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil("a"), entidadSinPerfil("b")))
        val sincronizador = crearSincronizador(dao)

        sincronizador.sincronizar(idUsuario).getOrThrow()
        sincronizador.sincronizar(idUsuario).getOrThrow()

        assertEquals(2, fuenteElevacion.llamadas.size)
    }

    @Test
    fun `un perfil vigente no se vuelve a pedir`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil()))
        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()
        fuenteElevacion.llamadas.clear()

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertTrue(fuenteElevacion.llamadas.isEmpty())
    }

    @Test
    fun `un perfil desactualizado se recalcula`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil()))
        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()
        val movida = dao.entidades.value.single().copy(
            recorrido = listOf(PuntoRecorridoEntidad(-40.0, -70.0, "PASO", ""), PuntoRecorridoEntidad(-40.02, -70.0, "PASO", "")),
            estadoSincronizacion = "PENDIENTE"
        )
        dao.guardar(movida)
        fuenteElevacion.llamadas.clear()

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, fuenteElevacion.llamadas.size)
    }

    @Test
    fun `si no hay red el sendero se sube igual y el perfil queda sin calcular`() = runTest {
        fuenteElevacion.falloDevuelto = SinConexionElevacionException(null)
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil()))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        assertEquals(0, resumen.senderosConError)
        assertNull(dao.entidades.value.single().perfilElevacion)
        assertNull(fuenteRemota.senderosGuardados.single().second.perfilElevacion)
    }

    @Test
    fun `sin conexion no insiste con el resto de los senderos`() = runTest {
        fuenteElevacion.falloDevuelto = SinConexionElevacionException(null)
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil("a"), entidadSinPerfil("b"), entidadSinPerfil("c")))

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, fuenteElevacion.llamadas.size)
    }

    @Test
    fun `otro error del servicio no impide calcular los demas senderos`() = runTest {
        fuenteElevacion.falloDevuelto = IllegalStateException("servicio caido")
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil("a"), entidadSinPerfil("b")))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(2, fuenteElevacion.llamadas.size)
        assertEquals(2, resumen.senderosSubidos)
    }

    @Test
    fun `un sendero sin recorrido no pide alturas`() = runTest {
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)))

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertTrue(fuenteElevacion.llamadas.isEmpty())
    }

    @Test
    fun `el perfil reemplaza el desnivel que tenia el sendero`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidadSinPerfil(desnivel = 777)))

        crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        val local = dao.entidades.value.single()
        assertEquals(local.perfilElevacion?.desnivelPositivoMetros, local.desnivelMetros)
        assertNotNull(local.perfilElevacion)
        assertEquals(1100, local.alturaMaximaMetros)
    }
}
