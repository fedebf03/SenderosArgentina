package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoNotificador
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import org.junit.Assert.assertEquals
import org.junit.Test

class CerrarSesionUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")
    private val autenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    private val senderos = FalsoSenderoRepositorio()
    private val lanzador = FalsoLanzadorSeguimiento()
    private val notificador = FalsoNotificador()

    @Test
    fun `cerrar sesion detiene el seguimiento antes de cerrar la sesion`() {
        val eventos = mutableListOf<String>()
        senderos.alDetenerSincronizacion = { eventos.add("sincronizacion") }
        val casoDeUso = CerrarSesionUseCase(autenticacion, senderos, object : com.unaj.senderos.core.domain.port.LanzadorSeguimiento {
            override fun iniciar(idSendero: String, esSimulado: Boolean) = Unit

            override fun detener() {
                eventos.add("seguimiento")
                lanzador.detener()
            }

            override fun cancelar() = Unit
        }, notificador)

        casoDeUso()

        assertEquals(1, lanzador.cantidadDetenciones)
        assertEquals("seguimiento", eventos.first())
        assertEquals(1, autenticacion.cantidadCierresSesion)
    }

    @Test
    fun `cerrar sesion cancela el aviso de error de sincronizacion`() {
        val casoDeUso = CerrarSesionUseCase(autenticacion, senderos, lanzador, notificador)

        casoDeUso()

        assertEquals(1, notificador.cantidadCancelacionesDeAvisoDeError)
    }
}
