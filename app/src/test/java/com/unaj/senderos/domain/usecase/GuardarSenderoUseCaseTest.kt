package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Etapa
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

    // Crear el caso de uso con un generador de identificadores determinista
    private fun crearCasoDeUso(
        autenticacion: FalsoAutenticacionRepositorio = falsoAutenticacion
    ): GuardarSenderoUseCase {
        return GuardarSenderoUseCase(
            senderoRepositorio = falsoSendero,
            autenticacionRepositorio = autenticacion,
            generarIdentificador = { "id-generado" }
        )
    }

    // Obtener el conjunto de campos inválidos de un resultado fallido por validación
    private fun camposInvalidosDe(resultado: Result<*>): Set<CampoSendero> {
        val excepcion = resultado.exceptionOrNull()
        assertTrue(excepcion is SenderoInvalidoException)
        return (excepcion as SenderoInvalidoException).camposInvalidos
    }

    // Probar que sin sesión activa falla con mensaje y no toca el repositorio
    @Test
    fun `sin sesion activa falla con mensaje y no toca el repositorio`() = runTest {
        val casoDeUso = crearCasoDeUso(FalsoAutenticacionRepositorio(usuarioActual = null))

        val resultado = casoDeUso(crearSenderoDePrueba())

        assertTrue(resultado.exceptionOrNull() is IllegalStateException)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    // Probar que se acumulan todos los campos inválidos cuando hay varios errores a la vez
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

    // Probar que un nombre en blanco marca solo el campo NOMBRE
    @Test
    fun `nombre en blanco marca el campo NOMBRE`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(nombre = "   "))

        assertEquals(setOf(CampoSendero.NOMBRE), camposInvalidosDe(resultado))
    }

    // Probar que una región en blanco marca solo el campo REGION
    @Test
    fun `region en blanco marca el campo REGION`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(region = ""))

        assertEquals(setOf(CampoSendero.REGION), camposInvalidosDe(resultado))
    }

    // Probar que una descripción en blanco marca solo el campo DESCRIPCION
    @Test
    fun `descripcion en blanco marca el campo DESCRIPCION`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(descripcion = " "))

        assertEquals(setOf(CampoSendero.DESCRIPCION), camposInvalidosDe(resultado))
    }

    // Probar que una distancia menor o igual a cero marca el campo DISTANCIA
    @Test
    fun `distancia menor o igual a cero marca el campo DISTANCIA`() = runTest {
        val resultadoCero = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = 0.0))
        val resultadoNegativo = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = -2.5))

        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoCero))
        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoNegativo))
    }

    // Probar que un desnivel negativo marca el campo DESNIVEL pero cero es válido
    @Test
    fun `desnivel negativo marca el campo DESNIVEL y cero es valido`() = runTest {
        val resultadoNegativo = crearCasoDeUso()(crearSenderoDePrueba(desnivelMetros = -1))
        val resultadoCero = crearCasoDeUso()(crearSenderoDePrueba(desnivelMetros = 0))

        assertEquals(setOf(CampoSendero.DESNIVEL), camposInvalidosDe(resultadoNegativo))
        assertTrue(resultadoCero.isSuccess)
    }

    // Probar que una altura mínima negativa marca el campo ALTURA_MINIMA
    @Test
    fun `altura minima negativa marca el campo ALTURA_MINIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = -1, alturaMaximaMetros = 100)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MINIMA), camposInvalidosDe(resultado))
    }

    // Probar que una altura máxima negativa marca el campo ALTURA_MAXIMA
    @Test
    fun `altura maxima negativa marca el campo ALTURA_MAXIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = 0, alturaMaximaMetros = -1)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MAXIMA), camposInvalidosDe(resultado))
    }

    // Probar que una altura máxima menor que la mínima marca el campo ALTURA_MAXIMA
    @Test
    fun `altura maxima menor que la minima marca el campo ALTURA_MAXIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = 1500, alturaMaximaMetros = 1000)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MAXIMA), camposInvalidosDe(resultado))
    }

    // Probar que se normalizan textos, se descartan blancos y se renumeran las etapas
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

    // Probar que se genera el identificador con el generador inyectado si está en blanco
    @Test
    fun `genera el identificador con el generador inyectado si esta en blanco`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(id = "")).getOrThrow()

        assertEquals("id-generado", guardado.id)
        assertEquals("id-generado", falsoSendero.llamadasGuardarSendero.single().second.id)
    }

    // Probar que se conserva el identificador existente
    @Test
    fun `conserva el identificador existente`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(id = "existente")).getOrThrow()

        assertEquals("existente", guardado.id)
    }

    // Probar que se fuerza el idUsuario del usuario autenticado ignorando el recibido
    @Test
    fun `fuerza el idUsuario del usuario autenticado`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(idUsuario = "otro-usuario")).getOrThrow()

        assertEquals("usuario-1", guardado.idUsuario)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().first)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().second.idUsuario)
    }

    // Probar que devuelve el sendero normalizado igual al entregado al repositorio
    @Test
    fun `devuelve el sendero normalizado entregado al repositorio`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(nombre = " Nombre ")).getOrThrow()

        assertEquals(falsoSendero.llamadasGuardarSendero.single().second, guardado)
    }

    // Probar que propaga el fallo del repositorio sin modificarlo
    @Test
    fun `propaga el fallo del repositorio`() = runTest {
        falsoSendero.resultadoGuardarSendero = Result.failure(IllegalStateException("Disco lleno"))

        val resultado = crearCasoDeUso()(crearSenderoDePrueba())

        assertTrue(resultado.isFailure)
        assertEquals("Disco lleno", resultado.exceptionOrNull()?.message)
    }

    // Probar que un sendero inválido no llama al repositorio
    @Test
    fun `no llama al repositorio si el sendero es invalido`() = runTest {
        crearCasoDeUso()(crearSenderoDePrueba(nombre = ""))

        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }
}
