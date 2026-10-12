package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.CalculadoraCorredor
import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoGestorMapasSinConexion
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapaSinConexionUseCaseTest {

    private val gestor = FalsoGestorMapasSinConexion()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com"))
    private val sendero = crearSenderoDePrueba(
        recorrido = listOf(PuntoRecorrido(-40.0, -71.0), PuntoRecorrido(-40.1, -71.1))
    )

    // Verificar que sin sesión observar, descargar y eliminar fallan
    @Test
    fun `sin sesion los casos de uso fallan`() = runTest {
        autenticacion.usuarioActual = null

        assertTrue(ObservarMapaSinConexionUseCase(gestor, autenticacion)(sendero).isFailure)
        assertTrue(DescargarMapaSinConexionUseCase(gestor, autenticacion)(sendero).isFailure)
        assertTrue(EliminarMapaSinConexionUseCase(gestor, autenticacion)("sendero-1").isFailure)
    }

    // Verificar que observar pasa al gestor el usuario actual y la firma del trazado vigente
    @Test
    fun `observar usa el usuario actual y la firma del trazado`() = runTest {
        gestor.estado.value = EstadoMapaSinConexion.Disponible(10)

        val estado = ObservarMapaSinConexionUseCase(gestor, autenticacion)(sendero).getOrThrow().first()

        assertEquals(EstadoMapaSinConexion.Disponible(10), estado)
        assertEquals(
            Triple("usuario-1", "sendero-1", CalculadoraCorredor.firmaTrazado(sendero.recorrido)),
            gestor.ultimaObservacion
        )
    }

    // Verificar que descargar y eliminar delegan en el gestor con el usuario actual
    @Test
    fun `descargar y eliminar delegan en el gestor`() = runTest {
        assertTrue(DescargarMapaSinConexionUseCase(gestor, autenticacion)(sendero).isSuccess)
        assertTrue(EliminarMapaSinConexionUseCase(gestor, autenticacion)("sendero-1").isSuccess)

        assertEquals(listOf("usuario-1/sendero-1"), gestor.descargas)
        assertEquals(listOf("usuario-1/sendero-1"), gestor.eliminaciones)
    }
}
