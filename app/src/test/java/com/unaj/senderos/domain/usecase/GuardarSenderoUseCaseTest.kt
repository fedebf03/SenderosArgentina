package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.SenderoInvalidoException
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardarSenderoUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")
    private val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    private val falsoSendero = FalsoSenderoRepositorio()

    private fun crearCasoDeUso(
        autenticacion: FalsoAutenticacionRepositorio = falsoAutenticacion
    ): GuardarSenderoUseCase {
        return GuardarSenderoUseCase(
            senderoRepositorio = falsoSendero,
            autenticacionRepositorio = autenticacion,
            generarIdentificador = { "id-generado" }
        )
    }

    private fun camposInvalidosDe(resultado: Result<*>): Set<CampoSendero> {
        val excepcion = resultado.exceptionOrNull()
        assertTrue(excepcion is SenderoInvalidoException)
        return (excepcion as SenderoInvalidoException).camposInvalidos
    }

    @Test
    fun `sin sesion activa falla con mensaje y no toca el repositorio`() = runTest {
        val casoDeUso = crearCasoDeUso(FalsoAutenticacionRepositorio(usuarioActual = null))

        val resultado = casoDeUso(crearSenderoDePrueba())

        assertTrue(resultado.exceptionOrNull() is IllegalStateException)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    @Test
    fun `acumula todos los campos invalidos cuando hay varios errores a la vez`() = runTest {
        val senderoInvalido = crearSenderoDePrueba(
            nombre = "  ",
            region = "",
            descripcion = " ",
            distanciaKilometros = 0.0,
            desnivelMetros = -1,
            alturaMaximaMetros = -5,
            alturaMinimaMetros = -3
        )

        val resultado = crearCasoDeUso()(senderoInvalido)

        assertEquals(
            setOf(
                CampoSendero.NOMBRE,
                CampoSendero.REGION,
                CampoSendero.DESCRIPCION,
                CampoSendero.DISTANCIA,
                CampoSendero.DESNIVEL,
                CampoSendero.ALTURA_MAXIMA,
                CampoSendero.ALTURA_MINIMA
            ),
            camposInvalidosDe(resultado)
        )
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    @Test
    fun `nombre en blanco marca el campo NOMBRE`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(nombre = "   "))

        assertEquals(setOf(CampoSendero.NOMBRE), camposInvalidosDe(resultado))
    }

    @Test
    fun `region en blanco marca el campo REGION`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(region = ""))

        assertEquals(setOf(CampoSendero.REGION), camposInvalidosDe(resultado))
    }

    @Test
    fun `descripcion en blanco marca el campo DESCRIPCION`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(descripcion = " "))

        assertEquals(setOf(CampoSendero.DESCRIPCION), camposInvalidosDe(resultado))
    }

    @Test
    fun `distancia menor o igual a cero marca el campo DISTANCIA`() = runTest {
        val resultadoCero = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = 0.0))
        val resultadoNegativo = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = -2.5))

        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoCero))
        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoNegativo))
    }

    @Test
    fun `desnivel negativo marca el campo DESNIVEL y cero es valido`() = runTest {
        val resultadoNegativo = crearCasoDeUso()(crearSenderoDePrueba(desnivelMetros = -1))
        val resultadoCero = crearCasoDeUso()(crearSenderoDePrueba(desnivelMetros = 0))

        assertEquals(setOf(CampoSendero.DESNIVEL), camposInvalidosDe(resultadoNegativo))
        assertTrue(resultadoCero.isSuccess)
    }

    @Test
    fun `altura minima negativa marca el campo ALTURA_MINIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = -1, alturaMaximaMetros = 100)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MINIMA), camposInvalidosDe(resultado))
    }

    @Test
    fun `altura maxima negativa marca el campo ALTURA_MAXIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = 0, alturaMaximaMetros = -1)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MAXIMA), camposInvalidosDe(resultado))
    }

    @Test
    fun `altura maxima menor que la minima marca el campo ALTURA_MAXIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = 1500, alturaMaximaMetros = 1000)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MAXIMA), camposInvalidosDe(resultado))
    }

    @Test
    fun `normaliza textos descarta blancos y renumera las etapas`() = runTest {
        val sendero = crearSenderoDePrueba().copy(
            nombre = "  Cerro Tronador  ",
            region = " Río Negro ",
            descripcion = "  Glaciar  ",
            duracionEstimada = " 3 días ",
            puntoInicio = " Pampa Linda ",
            mejorEpoca = " Verano ",
            recomendaciones = listOf("  Llevar abrigo ", "   ", ""),
            servicios = listOf("", " Refugio  "),
            etapas = listOf(
                Etapa(numero = 7, nombre = "Primera", distanciaKilometros = 3.0),
                Etapa(numero = 2, nombre = "Segunda", distanciaKilometros = 4.0)
            )
        )

        val guardado = crearCasoDeUso()(sendero).getOrThrow()

        assertEquals("Cerro Tronador", guardado.nombre)
        assertEquals("Río Negro", guardado.region)
        assertEquals("Glaciar", guardado.descripcion)
        assertEquals("3 días", guardado.duracionEstimada)
        assertEquals("Pampa Linda", guardado.puntoInicio)
        assertEquals("Verano", guardado.mejorEpoca)
        assertEquals(listOf("Llevar abrigo"), guardado.recomendaciones)
        assertEquals(listOf("Refugio"), guardado.servicios)
        assertEquals(
            listOf(
                Etapa(numero = 1, nombre = "Primera", distanciaKilometros = 3.0),
                Etapa(numero = 2, nombre = "Segunda", distanciaKilometros = 4.0)
            ),
            guardado.etapas
        )
    }

    @Test
    fun `genera el identificador con el generador inyectado si esta en blanco`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(id = "")).getOrThrow()

        assertEquals("id-generado", guardado.id)
        assertEquals("id-generado", falsoSendero.llamadasGuardarSendero.single().second.id)
    }

    @Test
    fun `conserva el identificador existente`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(id = "existente")).getOrThrow()

        assertEquals("existente", guardado.id)
    }

    @Test
    fun `fuerza el idUsuario del usuario autenticado`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(idUsuario = "otro-usuario")).getOrThrow()

        assertEquals("usuario-1", guardado.idUsuario)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().first)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().second.idUsuario)
    }

    @Test
    fun `devuelve el sendero normalizado entregado al repositorio`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(nombre = " Nombre ")).getOrThrow()

        assertEquals(falsoSendero.llamadasGuardarSendero.single().second, guardado)
    }

    @Test
    fun `propaga el fallo del repositorio`() = runTest {
        falsoSendero.resultadoGuardarSendero = Result.failure(IllegalStateException("Disco lleno"))

        val resultado = crearCasoDeUso()(crearSenderoDePrueba())

        assertTrue(resultado.isFailure)
        assertEquals("Disco lleno", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `no llama al repositorio si el sendero es invalido`() = runTest {
        crearCasoDeUso()(crearSenderoDePrueba(nombre = ""))

        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    @Test
    fun `latitud fuera de rango marca el campo RECORRIDO`() = runTest {
        val resultadoNorte = crearCasoDeUso()(
            crearSenderoDePrueba(recorrido = listOf(PuntoRecorrido(latitud = 90.1, longitud = 0.0)))
        )
        val resultadoSur = crearCasoDeUso()(
            crearSenderoDePrueba(recorrido = listOf(PuntoRecorrido(latitud = -90.1, longitud = 0.0)))
        )

        assertEquals(setOf(CampoSendero.RECORRIDO), camposInvalidosDe(resultadoNorte))
        assertEquals(setOf(CampoSendero.RECORRIDO), camposInvalidosDe(resultadoSur))
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    @Test
    fun `longitud fuera de rango marca el campo RECORRIDO`() = runTest {
        val resultadoEste = crearCasoDeUso()(
            crearSenderoDePrueba(recorrido = listOf(PuntoRecorrido(latitud = 0.0, longitud = 180.1)))
        )
        val resultadoOeste = crearCasoDeUso()(
            crearSenderoDePrueba(recorrido = listOf(PuntoRecorrido(latitud = 0.0, longitud = -180.1)))
        )

        assertEquals(setOf(CampoSendero.RECORRIDO), camposInvalidosDe(resultadoEste))
        assertEquals(setOf(CampoSendero.RECORRIDO), camposInvalidosDe(resultadoOeste))
    }

    @Test
    fun `un punto invalido entre puntos validos marca el campo RECORRIDO`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(
                recorrido = listOf(
                    PuntoRecorrido(latitud = -40.5, longitud = -71.3),
                    PuntoRecorrido(latitud = 95.0, longitud = -71.3),
                    PuntoRecorrido(latitud = -40.6, longitud = -71.4)
                )
            )
        )

        assertEquals(setOf(CampoSendero.RECORRIDO), camposInvalidosDe(resultado))
    }

    @Test
    fun `coordenadas en los limites exactos son validas`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(
                recorrido = listOf(
                    PuntoRecorrido(latitud = 90.0, longitud = 180.0),
                    PuntoRecorrido(latitud = -90.0, longitud = -180.0)
                )
            )
        )

        assertTrue(resultado.isSuccess)
    }

    @Test
    fun `recorrido vacio es valido`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(recorrido = emptyList()))

        assertTrue(resultado.isSuccess)
        assertTrue(falsoSendero.llamadasGuardarSendero.single().second.recorrido.isEmpty())
    }

    @Test
    fun `recorta el nombre de cada punto del recorrido`() = runTest {
        val guardado = crearCasoDeUso()(
            crearSenderoDePrueba(
                recorrido = listOf(
                    PuntoRecorrido(latitud = -40.5, longitud = -71.3, tipo = TipoPunto.REFUGIO, nombre = "  Refugio Norte "),
                    PuntoRecorrido(latitud = -40.6, longitud = -71.4, tipo = TipoPunto.PASO, nombre = "   ")
                )
            )
        ).getOrThrow()

        assertEquals(
            listOf(
                PuntoRecorrido(latitud = -40.5, longitud = -71.3, tipo = TipoPunto.REFUGIO, nombre = "Refugio Norte"),
                PuntoRecorrido(latitud = -40.6, longitud = -71.4, tipo = TipoPunto.PASO, nombre = "")
            ),
            guardado.recorrido
        )
        assertEquals(guardado.recorrido, falsoSendero.llamadasGuardarSendero.single().second.recorrido)
    }
}
