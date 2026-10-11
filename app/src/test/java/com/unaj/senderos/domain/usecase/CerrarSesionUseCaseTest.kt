package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CerrarSesionUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "a@b.com")

    @Test
    fun `con usuario detiene la sincronizacion antes de cerrar la sesion`() {
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio()
        var cierresAlDetener = -1
        falsoSendero.alDetenerSincronizacion = { cierresAlDetener = falsoAutenticacion.cantidadCierresSesion }

        CerrarSesionUseCase(falsoAutenticacion, falsoSendero).invoke()

        assertEquals(listOf("usuario-1"), falsoSendero.idsUsuarioDetenidos)
        assertEquals(0, cierresAlDetener)
        assertEquals(1, falsoAutenticacion.cantidadCierresSesion)
    }

    @Test
    fun `sin usuario no detiene la sincronizacion`() {
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = null)
        val falsoSendero = FalsoSenderoRepositorio()

        CerrarSesionUseCase(falsoAutenticacion, falsoSendero).invoke()

        assertTrue(falsoSendero.idsUsuarioDetenidos.isEmpty())
        assertEquals(1, falsoAutenticacion.cantidadCierresSesion)
    }
}
