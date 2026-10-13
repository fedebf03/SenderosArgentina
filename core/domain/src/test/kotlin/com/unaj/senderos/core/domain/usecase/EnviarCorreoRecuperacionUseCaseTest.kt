package com.unaj.senderos.domain.usecase

import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnviarCorreoRecuperacionUseCaseTest {

    private val autenticacion = FalsoAutenticacionRepositorio()
    private val casoDeUso = EnviarCorreoRecuperacionUseCase(autenticacion)

    @Test
    fun `correo vacio falla con mensaje de requerido`() = runTest {
        val resultado = casoDeUso("   ")

        assertEquals("El correo es requerido", resultado.exceptionOrNull()?.message)
        assertTrue(autenticacion.correosDeRecuperacion.isEmpty())
    }

    @Test
    fun `correo con formato invalido falla`() = runTest {
        val resultado = casoDeUso("ana@correo")

        assertEquals("El correo no tiene un formato válido", resultado.exceptionOrNull()?.message)
        assertTrue(autenticacion.correosDeRecuperacion.isEmpty())
    }

    @Test
    fun `correo valido se envia recortado al repositorio`() = runTest {
        val resultado = casoDeUso("  ana@correo.com ")

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("ana@correo.com"), autenticacion.correosDeRecuperacion)
    }

    @Test
    fun `fallo del repositorio se propaga`() = runTest {
        autenticacion.resultadoRecuperacion = Result.failure(Exception("Sin conexión a internet"))

        val resultado = casoDeUso("ana@correo.com")

        assertEquals("Sin conexión a internet", resultado.exceptionOrNull()?.message)
    }
}
