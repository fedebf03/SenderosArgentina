package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoRecorridoRepositorio
import com.unaj.senderos.falsos.crearRecorridoDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservarRecorridosUseCaseTest {

    private val repositorio = FalsoRecorridoRepositorio()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))

    // Verificar que sin sesión activa la lista falla
    @Test
    fun `sin sesion la lista falla`() {
        autenticacion.usuarioActual = null

        assertTrue(ObservarRecorridosUseCase(repositorio, autenticacion)().isFailure)
    }

    // Verificar que con sesión activa emite solo los recorridos del usuario actual
    @Test
    fun `con sesion emite los recorridos del usuario actual`() = runTest {
        repositorio.recorridos.value = listOf(
            crearRecorridoDePrueba(id = "propio"),
            crearRecorridoDePrueba(id = "ajeno", idUsuario = "usuario-2")
        )

        val lista = ObservarRecorridosUseCase(repositorio, autenticacion)().getOrThrow().first()

        assertEquals(listOf("propio"), lista.map { recorrido -> recorrido.id })
    }

    // Verificar que sin sesión el detalle de un recorrido falla
    @Test
    fun `sin sesion el detalle falla`() {
        autenticacion.usuarioActual = null

        assertTrue(ObservarRecorridoUseCase(repositorio, autenticacion)("recorrido-1").isFailure)
    }

    // Verificar que el detalle emite el recorrido pedido o null si no existe
    @Test
    fun `el detalle emite el recorrido pedido o null`() = runTest {
        repositorio.recorridos.value = listOf(crearRecorridoDePrueba(id = "propio"))
        val casoDeUso = ObservarRecorridoUseCase(repositorio, autenticacion)

        assertEquals("propio", casoDeUso("propio").getOrThrow().first()?.id)
        assertNull(casoDeUso("inexistente").getOrThrow().first())
    }
}
