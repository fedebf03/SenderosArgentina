package com.unaj.senderos.core.data.repository

import com.unaj.senderos.core.database.PuntoRecorridoEntidad
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.data.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.core.data.falsos.FalsoPlanificadorSincronizacion
import com.unaj.senderos.core.data.falsos.FalsoSenderoDao
import com.unaj.senderos.core.data.falsos.crearEntidadDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderoRepositorioPerfilTest {

    private val planificador = FalsoPlanificadorSincronizacion()
    private val trazado = listOf(PuntoRecorridoEntidad(-40.0, -70.0, "PASO", ""), PuntoRecorridoEntidad(-40.01, -70.0, "PASO", ""))
    private val firma = CalculadoraPerfil.firmaTrazado(
        trazado.map { punto -> PuntoRecorrido(punto.latitud, punto.longitud) },
        TipoRecorrido.LINEAL
    )

    private fun perfilConFirma(firmaPerfil: String) = PerfilElevacion(
        puntos = listOf(PuntoPerfil(0.0, 1000.0), PuntoPerfil(1.0, 1100.0)),
        desnivelPositivoMetros = 100,
        desnivelNegativoMetros = 0,
        alturaMaximaMetros = 1100,
        alturaMinimaMetros = 1000,
        pendienteMaximaPorcentaje = 10.0,
        firmaTrazado = firmaPerfil
    )

    private fun crearRepositorio(dao: FalsoSenderoDao) =
        SenderoRepositorioOfflineFirst(dao, planificador, FalsoEjecutorTransacciones()) { 5_000L }

    private fun entidad() = crearEntidadDePrueba(
        fechaModificacion = 1_000L,
        estadoSincronizacion = "SINCRONIZADO",
        recorrido = trazado,
        tipoRecorrido = "LINEAL",
        desnivelMetros = 0,
        alturaMaximaMetros = 0,
        alturaMinimaMetros = 0
    )

    @Test
    fun `guardar el perfil conserva la fecha del usuario, marca pendiente y pide sincronizar`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidad()))

        val resultado = crearRepositorio(dao).guardarPerfilElevacion("usuario-1", "sendero-1", perfilConFirma(firma))

        assertTrue(resultado.isSuccess)
        val guardada = dao.entidades.value.single()
        assertEquals("PENDIENTE", guardada.estadoSincronizacion)
        assertEquals(1_000L, guardada.fechaModificacion)
        assertEquals(firma, guardada.perfilElevacion?.firmaTrazado)
        assertEquals(1100, guardada.alturaMaximaMetros)
        assertEquals(listOf("usuario-1"), planificador.solicitudes)
    }

    @Test
    fun `un sendero pendiente o con error conserva su estado y su fecha al guardar el perfil`() = runTest {
        listOf("PENDIENTE", "CON_ERROR").forEach { estado ->
            val dao = FalsoSenderoDao(listOf(entidad().copy(estadoSincronizacion = estado)))

            crearRepositorio(dao).guardarPerfilElevacion("usuario-1", "sendero-1", perfilConFirma(firma)).getOrThrow()

            assertEquals(estado, dao.entidades.value.single().estadoSincronizacion)
            assertEquals(1_000L, dao.entidades.value.single().fechaModificacion)
            assertEquals(firma, dao.entidades.value.single().perfilElevacion?.firmaTrazado)
        }
    }

    @Test
    fun `si el trazado cambio mientras se calculaba no guarda el perfil`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidad()))

        val resultado = crearRepositorio(dao).guardarPerfilElevacion("usuario-1", "sendero-1", perfilConFirma("otra"))

        assertTrue(resultado.isFailure)
        assertEquals("SINCRONIZADO", dao.entidades.value.single().estadoSincronizacion)
        assertEquals(null, dao.entidades.value.single().perfilElevacion)
        assertTrue(planificador.solicitudes.isEmpty())
    }

    @Test
    fun `un sendero eliminado o inexistente falla`() = runTest {
        val dao = FalsoSenderoDao(listOf(entidad().copy(estaEliminado = true)))

        val resultado = crearRepositorio(dao).guardarPerfilElevacion("usuario-1", "sendero-1", perfilConFirma(firma))

        assertTrue(resultado.isFailure)
    }
}
