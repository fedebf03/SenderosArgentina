package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoLanzadorSeguimiento
import com.unaj.senderos.falsos.FalsoNotificador
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import org.junit.Assert.assertEquals
import org.junit.Test

class CerrarSesionUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")
    private val autenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    private val senderos = FalsoSenderoRepositorio()
    private val lanzador = FalsoLanzadorSeguimiento()
    private val notificador = FalsoNotificador()

    // Verificar que cerrar sesión detiene el seguimiento antes de cerrar la sesión
    @Test
    fun `cerrar sesion detiene el seguimiento antes de cerrar la sesion`() {
        val eventos = mutableListOf<String>()
        senderos.alDetenerSincronizacion = { eventos.add("sincronizacion") }
        val casoDeUso = CerrarSesionUseCase(autenticacion, senderos, object : com.unaj.senderos.domain.repository.LanzadorSeguimiento {
            // Registrar el orden en que se pidió detener el seguimiento
            override fun iniciar(idSendero: String, esSimulado: Boolean) = Unit

            // Registrar la detención del seguimiento
            override fun detener() {
                eventos.add("seguimiento")
                lanzador.detener()
            }

            // Ignorar la cancelación: esta prueba no la usa
            override fun cancelar() = Unit
        }, notificador)

        casoDeUso()

        assertEquals(1, lanzador.cantidadDetenciones)
        assertEquals("seguimiento", eventos.first())
        assertEquals(1, autenticacion.cantidadCierresSesion)
    }

    // Verificar que cerrar sesión cancela el aviso de error de sincronización del usuario saliente
    @Test
    fun `cerrar sesion cancela el aviso de error de sincronizacion`() {
        val casoDeUso = CerrarSesionUseCase(autenticacion, senderos, lanzador, notificador)

        casoDeUso()

        assertEquals(1, notificador.cantidadCancelacionesDeAvisoDeError)
    }
}
