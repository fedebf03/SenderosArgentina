package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.ResultadoSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Una diferencia de 0,001 grados de latitud equivale a unos 111,2 m; de longitud, a unos 85,2 m en -39,95
private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07
private const val TOLERANCIA_KILOMETROS = 0.004
private const val INSTANTE_INICIAL = 1_000L

class CalculadoraSeguimientoTest {

    // Construir un punto del trazado al sur del origen según los grados de latitud indicados
    private fun puntoAlSur(grados: Double, tipo: TipoPunto = TipoPunto.PASO): PuntoRecorrido {
        return PuntoRecorrido(latitud = LATITUD_BASE - grados, longitud = LONGITUD_BASE, tipo = tipo)
    }

    // Construir una coordenada al sur del origen con un desplazamiento opcional hacia el este
    private fun posicionAlSur(grados: Double, gradosAlEste: Double = 0.0): Coordenada {
        return Coordenada(latitud = LATITUD_BASE - grados, longitud = LONGITUD_BASE + gradosAlEste)
    }

    // Construir un sendero de prueba con el recorrido y el tipo indicados
    private fun construirSendero(puntos: List<PuntoRecorrido>, tipo: TipoRecorrido = TipoRecorrido.LINEAL): Sendero {
        return crearSenderoDePrueba(recorrido = puntos).copy(tipoRecorrido = tipo)
    }

    // Construir un sendero lineal de tres puntos de paso separados 0,001 grados de latitud
    private fun senderoLinealCorto(): Sendero {
        return construirSendero(listOf(puntoAlSur(0.0), puntoAlSur(0.001), puntoAlSur(0.002)))
    }

    // Construir un sendero lineal largo de dos puntos para las pruebas de desvío
    private fun senderoParaDesvio(): Sendero {
        return construirSendero(listOf(puntoAlSur(0.0), puntoAlSur(0.004)))
    }

    // Actualizar el seguimiento con una posición sin rumbo ni velocidad informados
    private fun actualizarConPosicion(estado: EstadoSeguimiento, posicion: Coordenada, ahora: Long): ResultadoSeguimiento {
        return CalculadoraSeguimiento.actualizar(estado, LecturaUbicacion(posicion, null, null, null), ahora)
    }

    // Aplicar varias posiciones en orden y devolver el último estado
    private fun recorrer(estadoInicial: EstadoSeguimiento, posiciones: List<Coordenada>): EstadoSeguimiento {
        var estado = estadoInicial
        posiciones.forEachIndexed { indice, posicion ->
            estado = actualizarConPosicion(estado, posicion, INSTANTE_INICIAL + (indice + 1) * 5_000L).estado
        }
        return estado
    }

    // Verificar que la distancia restante al iniciar es el largo total del trazado lineal
    @Test
    fun `al iniciar un sendero lineal la distancia restante es el largo total del trazado`() {
        val estado = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        assertEquals(0.2224, estado.distanciaRestanteKilometros, TOLERANCIA_KILOMETROS)
        assertEquals(0.0, estado.distanciaRecorridaKilometros, 0.0001)
    }

    // Verificar que el recorrido circular suma el tramo de cierre al largo total
    @Test
    fun `al iniciar un sendero circular el largo total incluye el tramo de cierre`() {
        val triangulo = listOf(
            PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
            PuntoRecorrido(LATITUD_BASE - 0.001, LONGITUD_BASE),
            PuntoRecorrido(LATITUD_BASE - 0.001, LONGITUD_BASE + 0.001)
        )
        val esperado = CalculadoraRecorrido.distanciaKilometros(triangulo, TipoRecorrido.CIRCULAR)

        val estado = CalculadoraSeguimiento.iniciar(construirSendero(triangulo, TipoRecorrido.CIRCULAR), INSTANTE_INICIAL)

        assertEquals(esperado, estado.distanciaRestanteKilometros, 0.0001)
        assertTrue(estado.distanciaRestanteKilometros > 0.3)
    }

    // Verificar que el recorrido de ida y vuelta duplica el largo total
    @Test
    fun `al iniciar un sendero de ida y vuelta el largo total se duplica`() {
        val estado = CalculadoraSeguimiento.iniciar(
            construirSendero(senderoLinealCorto().recorrido, TipoRecorrido.IDA_Y_VUELTA),
            INSTANTE_INICIAL
        )

        assertEquals(0.4448, estado.distanciaRestanteKilometros, TOLERANCIA_KILOMETROS)
    }

    // Verificar que avanzar sobre el trazado reduce la distancia restante y aumenta la recorrida
    @Test
    fun `avanzar sobre el trazado reduce la distancia restante y aumenta la recorrida`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.001)))

        assertEquals(0.1112, estado.distanciaRecorridaKilometros, TOLERANCIA_KILOMETROS)
        assertEquals(0.1112, estado.distanciaRestanteKilometros, TOLERANCIA_KILOMETROS)
    }

    // Verificar que un movimiento menor a 5 metros no suma distancia recorrida
    @Test
    fun `un movimiento menor a 5 metros no suma distancia recorrida`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.00002)))

        assertEquals(0.0, estado.distanciaRecorridaKilometros, 0.0001)
    }

    // Verificar que un movimiento de unos 11 metros sí suma distancia recorrida
    @Test
    fun `un movimiento de once metros suma distancia recorrida`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.0001)))

        assertEquals(0.0111, estado.distanciaRecorridaKilometros, 0.002)
    }

    // Verificar que el índice de tramo no retrocede cuando la vuelta se superpone con la ida
    @Test
    fun `el indice de tramo nunca retrocede en un recorrido de ida y vuelta`() {
        val sendero = construirSendero(senderoLinealCorto().recorrido, TipoRecorrido.IDA_Y_VUELTA)
        val posiciones = listOf(
            posicionAlSur(0.0), posicionAlSur(0.001), posicionAlSur(0.002), posicionAlSur(0.0015), posicionAlSur(0.0)
        )
        var estado = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)
        var indiceAnterior = estado.indiceTramoActual

        posiciones.forEachIndexed { indice, posicion ->
            estado = actualizarConPosicion(estado, posicion, INSTANTE_INICIAL + (indice + 1) * 5_000L).estado
            assertTrue(estado.indiceTramoActual >= indiceAnterior)
            indiceAnterior = estado.indiceTramoActual
        }

        assertEquals(3, estado.indiceTramoActual)
        assertTrue(estado.distanciaRestanteKilometros < 0.005)
    }

    // Verificar que la distancia al trazado es la perpendicular al tramo más cercano
    @Test
    fun `la distancia al trazado es la perpendicular aproximada al tramo`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.001, gradosAlEste = 0.001)))

        assertEquals(85.2, estado.distanciaAlTrazadoMetros, 3.0)
    }

    // Verificar que se avisa una sola vez al acercarse a menos de 50 metros de un punto que no es de paso
    @Test
    fun `avisa una sola vez al acercarse a un punto que no es de paso`() {
        val mirador = puntoAlSur(0.002, TipoPunto.MIRADOR)
        val sendero = construirSendero(listOf(puntoAlSur(0.0), mirador, puntoAlSur(0.004)))
        var estado = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val avisosPorPosicion = listOf(posicionAlSur(0.0), posicionAlSur(0.0015), posicionAlSur(0.0017), posicionAlSur(0.0019))
            .mapIndexed { indice, posicion ->
                val resultado = actualizarConPosicion(estado, posicion, INSTANTE_INICIAL + (indice + 1) * 5_000L)
                estado = resultado.estado
                resultado.avisos
            }

        assertTrue(avisosPorPosicion[0].isEmpty())
        assertTrue(avisosPorPosicion[1].isEmpty())
        assertEquals(listOf<AvisoRecorrido>(AvisoRecorrido.CercaDePunto(indice = 1, punto = mirador)), avisosPorPosicion[2])
        assertTrue(avisosPorPosicion[3].isEmpty())
    }

    // Verificar que los puntos de paso nunca generan aviso de proximidad
    @Test
    fun `no avisa al acercarse a puntos de paso`() {
        val sendero = construirSendero(listOf(puntoAlSur(0.0), puntoAlSur(0.002), puntoAlSur(0.004)))
        var estado = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val avisos = listOf(0.0, 0.0017, 0.002, 0.0023, 0.004).flatMapIndexed { indice, grados ->
            val resultado = actualizarConPosicion(estado, posicionAlSur(grados), INSTANTE_INICIAL + (indice + 1) * 5_000L)
            estado = resultado.estado
            resultado.avisos
        }

        assertTrue(avisos.isEmpty())
    }

    // Verificar que el aviso de desvío se emite al superar 100 metros y no se repite mientras siga desviado
    @Test
    fun `avisa el desvio al superar cien metros y no lo repite mientras siga desviado`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoParaDesvio(), INSTANTE_INICIAL)
        val sobreElTrazado = actualizarConPosicion(inicial, posicionAlSur(0.0), 6_000L)

        val primerDesvio = actualizarConPosicion(sobreElTrazado.estado, posicionAlSur(0.001, 0.0015), 11_000L)
        val segundoDesvio = actualizarConPosicion(primerDesvio.estado, posicionAlSur(0.002, 0.0016), 16_000L)

        assertTrue(sobreElTrazado.avisos.isEmpty())
        assertEquals(listOf<AvisoRecorrido>(AvisoRecorrido.Desvio), primerDesvio.avisos)
        assertTrue(primerDesvio.estado.estaDesviado)
        assertTrue(segundoDesvio.avisos.isEmpty())
        assertTrue(segundoDesvio.estado.estaDesviado)
    }

    // Verificar que el desvío se rearma solo tras volver a menos de 50 metros del trazado
    @Test
    fun `el desvio se rearma al volver a menos de cincuenta metros del trazado`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoParaDesvio(), INSTANTE_INICIAL)
        val desviado = actualizarConPosicion(inicial, posicionAlSur(0.001, 0.0015), 6_000L)

        val intermedio = actualizarConPosicion(desviado.estado, posicionAlSur(0.0012, 0.0008), 11_000L)
        val desvioSinRearmar = actualizarConPosicion(intermedio.estado, posicionAlSur(0.0014, 0.0015), 16_000L)
        val regreso = actualizarConPosicion(desvioSinRearmar.estado, posicionAlSur(0.0016, 0.0004), 21_000L)
        val nuevoDesvio = actualizarConPosicion(regreso.estado, posicionAlSur(0.0018, 0.0015), 26_000L)

        assertTrue(desviado.estado.estaDesviado)
        assertTrue(intermedio.estado.estaDesviado)
        assertTrue(desvioSinRearmar.avisos.isEmpty())
        assertFalse(regreso.estado.estaDesviado)
        assertEquals(listOf<AvisoRecorrido>(AvisoRecorrido.Desvio), nuevoDesvio.avisos)
    }

    // Verificar que el próximo punto avanza al pasar por un punto del trazado
    @Test
    fun `el proximo punto avanza al pasar un punto`() {
        val sendero = construirSendero(listOf(puntoAlSur(0.0), puntoAlSur(0.002), puntoAlSur(0.004)))
        val inicial = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val antesDelPunto = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.0015)))
        val despuesDelPunto = recorrer(antesDelPunto, listOf(posicionAlSur(0.0025)))

        assertEquals(1, antesDelPunto.indiceProximoPunto)
        assertEquals(2, despuesDelPunto.indiceProximoPunto)
    }

    // Verificar que el tiempo transcurrido es la diferencia entre el instante actual y el de inicio
    @Test
    fun `el tiempo transcurrido es la diferencia entre ahora y el inicio`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val resultado = actualizarConPosicion(inicial, posicionAlSur(0.0), INSTANTE_INICIAL + 60_000L)

        assertEquals(60_000L, resultado.estado.tiempoTranscurridoMilisegundos)
    }

    // Verificar que cerca del origen de un circular el usuario sigue en el primer tramo y no salta al cierre
    @Test
    fun `en un circular cerca del origen sigue en el primer tramo y no salta al cierre`() {
        val triangulo = listOf(
            PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
            PuntoRecorrido(LATITUD_BASE - 0.001, LONGITUD_BASE),
            PuntoRecorrido(LATITUD_BASE - 0.0005, LONGITUD_BASE + 0.001)
        )
        val inicial = CalculadoraSeguimiento.iniciar(construirSendero(triangulo, TipoRecorrido.CIRCULAR), INSTANTE_INICIAL)
        val cercaDelOrigen = Coordenada(LATITUD_BASE + 0.00001, LONGITUD_BASE + 0.000035)

        val estado = actualizarConPosicion(inicial, cercaDelOrigen, INSTANTE_INICIAL + 5_000L).estado

        assertEquals(0, estado.indiceTramoActual)
        assertEquals(inicial.distanciaRestanteKilometros, estado.distanciaRestanteKilometros, 0.01)
    }

    // Verificar que cortar la esquina en un vértice con giro no salta al tramo de la vuelta
    @Test
    fun `en ida y vuelta cortar la esquina de un giro no salta a la vuelta`() {
        val sendero = construirSendero(
            listOf(
                PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
                PuntoRecorrido(LATITUD_BASE - 0.002, LONGITUD_BASE),
                PuntoRecorrido(LATITUD_BASE - 0.002, LONGITUD_BASE + 0.002)
            ),
            TipoRecorrido.IDA_Y_VUELTA
        )
        val inicial = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val estado = recorrer(
            inicial,
            listOf(
                posicionAlSur(0.0),
                posicionAlSur(0.002),
                posicionAlSur(0.0018, gradosAlEste = 0.0001)
            )
        )

        assertEquals(1, estado.indiceTramoActual)
    }

    // Verificar que un punto de giro no de paso genera un solo aviso
    @Test
    fun `un punto de giro que no es de paso genera un solo aviso en ida y vuelta`() {
        val giro = PuntoRecorrido(LATITUD_BASE - 0.002, LONGITUD_BASE + 0.002, TipoPunto.MIRADOR)
        val sendero = construirSendero(
            listOf(PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE), PuntoRecorrido(LATITUD_BASE - 0.002, LONGITUD_BASE), giro),
            TipoRecorrido.IDA_Y_VUELTA
        )
        var estado = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)
        val posiciones = listOf(
            posicionAlSur(0.0),
            posicionAlSur(0.002),
            posicionAlSur(0.002, gradosAlEste = 0.0017),
            posicionAlSur(0.002, gradosAlEste = 0.002)
        )

        val avisos = posiciones.flatMapIndexed { indice, posicion ->
            val resultado = actualizarConPosicion(estado, posicion, INSTANTE_INICIAL + (indice + 1) * 5_000L)
            estado = resultado.estado
            resultado.avisos
        }

        assertEquals(listOf<AvisoRecorrido>(AvisoRecorrido.CercaDePunto(indice = 2, punto = giro)), avisos)
    }

    // Verificar que un punto del recorrido no se avisa de nuevo al pasar por él en la vuelta
    @Test
    fun `un punto no se vuelve a avisar en la vuelta de un recorrido de ida y vuelta`() {
        val mirador = puntoAlSur(0.002, TipoPunto.MIRADOR)
        val sendero = construirSendero(listOf(puntoAlSur(0.0), mirador, puntoAlSur(0.004)), TipoRecorrido.IDA_Y_VUELTA)
        var estado = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val avisos = listOf(0.0, 0.0017, 0.0023, 0.004, 0.0023, 0.0017, 0.0).flatMapIndexed { indice, grados ->
            val resultado = actualizarConPosicion(estado, posicionAlSur(grados), INSTANTE_INICIAL + (indice + 1) * 5_000L)
            estado = resultado.estado
            resultado.avisos
        }

        assertEquals(listOf<AvisoRecorrido>(AvisoRecorrido.CercaDePunto(indice = 1, punto = mirador)), avisos)
    }

    // Verificar que un camino lento con pasos de 3 metros suma distancia acumulada
    @Test
    fun `un camino lento con pasos de tres metros suma distancia`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        val posiciones = (0..10).map { paso -> posicionAlSur(paso * 0.000027) }

        val estado = recorrer(inicial, posiciones)

        assertTrue(estado.distanciaRecorridaKilometros >= 0.02)
    }

    // Verificar que oscilar sin moverse no suma distancia
    @Test
    fun `oscilar sin moverse no suma distancia`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        val posiciones = (0..12).map { paso -> posicionAlSur(if (paso % 2 == 0) 0.0 else 0.00002) }

        val estado = recorrer(inicial, posiciones)

        assertEquals(0.0, estado.distanciaRecorridaKilometros, 0.0001)
    }

    // Verificar que cada posición aceptada se agrega al trazado caminado en orden
    @Test
    fun `cada posicion aceptada se agrega al trazado caminado`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        val posiciones = listOf(posicionAlSur(0.0), posicionAlSur(0.0005), posicionAlSur(0.001))

        val estado = recorrer(inicial, posiciones)

        assertEquals(posiciones, estado.trazadoCaminado)
    }

    // Verificar que un movimiento menor a 5 metros no se agrega al trazado caminado
    @Test
    fun `un movimiento menor a cinco metros no se agrega al trazado caminado`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        val posiciones = listOf(posicionAlSur(0.0), posicionAlSur(0.00002), posicionAlSur(0.00003))

        val estado = recorrer(inicial, posiciones)

        assertEquals(listOf(posicionAlSur(0.0)), estado.trazadoCaminado)
    }

    // Verificar que el estado inicial no tiene trazado caminado ni velocidad máxima
    @Test
    fun `el estado inicial no tiene trazado caminado ni velocidad maxima`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        assertTrue(inicial.trazadoCaminado.isEmpty())
        assertEquals(0f, inicial.velocidadMaximaMetrosPorSegundo, 0f)
    }

    // Verificar que la velocidad máxima conserva el mayor valor reportado por las lecturas
    @Test
    fun `la velocidad maxima conserva el mayor valor reportado`() {
        var estado = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        listOf(1.0f, 3.5f, 2.0f).forEachIndexed { indice, velocidad ->
            estado = CalculadoraSeguimiento.actualizar(
                estado,
                LecturaUbicacion(posicionAlSur(indice * 0.0005), null, velocidad, null),
                INSTANTE_INICIAL + (indice + 1) * 5_000L
            ).estado
        }

        assertEquals(3.5f, estado.velocidadMaximaMetrosPorSegundo, 0f)
    }

    // Verificar que una lectura sin velocidad no modifica la velocidad máxima
    @Test
    fun `una lectura sin velocidad no modifica la velocidad maxima`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        val conVelocidad = CalculadoraSeguimiento.actualizar(
            inicial, LecturaUbicacion(posicionAlSur(0.0), null, 2.5f, null), INSTANTE_INICIAL + 1_000L
        ).estado

        val sinVelocidad = actualizarConPosicion(conVelocidad, posicionAlSur(0.0005), INSTANTE_INICIAL + 2_000L).estado

        assertEquals(2.5f, sinVelocidad.velocidadMaximaMetrosPorSegundo, 0f)
    }

    // Construir una lectura con rumbo y velocidad opcionales
    private fun lectura(coordenada: Coordenada, rumbo: Float? = null, velocidad: Float? = null): LecturaUbicacion {
        return LecturaUbicacion(coordenada, rumbo, velocidad, precisionMetros = null)
    }

    // Verificar que el rumbo del GPS se toma cuando la velocidad es de al menos 0,5 m/s
    @Test
    fun `el rumbo del GPS se actualiza con velocidad suficiente`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = CalculadoraSeguimiento.actualizar(inicial, lectura(posicionAlSur(0.0), 90f, 1.5f), 6_000L).estado

        assertEquals(90f, estado.rumboGrados)
        assertEquals(1.5f, estado.velocidadMetrosPorSegundo)
    }

    // Verificar que detenido se conserva el último rumbo conocido
    @Test
    fun `detenido se conserva el ultimo rumbo conocido`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        val enMovimiento = CalculadoraSeguimiento.actualizar(inicial, lectura(posicionAlSur(0.0), 90f, 1.5f), 6_000L).estado

        val detenido = CalculadoraSeguimiento.actualizar(enMovimiento, lectura(posicionAlSur(0.0), 180f, 0.1f), 11_000L).estado

        assertEquals(90f, detenido.rumboGrados)
    }

    // Verificar que el rumbo queda sin definir hasta que haya movimiento o rumbo informado
    @Test
    fun `sin rumbo ni desplazamiento el rumbo queda sin definir`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.00002)))

        assertNull(estado.rumboGrados)
    }

    // Verificar que sin rumbo del GPS se calcula el rumbo geodésico del desplazamiento hacia el norte
    @Test
    fun `sin rumbo del GPS se calcula el rumbo hacia el norte`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.001), posicionAlSur(0.0)))

        assertEquals(0.0, estado.rumboGrados?.toDouble() ?: -999.0, 1.0)
    }

    // Verificar que sin rumbo del GPS se calcula el rumbo geodésico del desplazamiento hacia el este
    @Test
    fun `sin rumbo del GPS se calcula el rumbo hacia el este`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.0, gradosAlEste = 0.0002)))

        assertEquals(90.0, estado.rumboGrados?.toDouble() ?: -999.0, 1.0)
    }

    // Verificar que el rumbo del GPS se normaliza a [0, 360)
    @Test
    fun `el rumbo del GPS se normaliza al intervalo de 0 a 360`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val mayor = CalculadoraSeguimiento.actualizar(inicial, lectura(posicionAlSur(0.0), 370f, 2f), 6_000L).estado
        val negativo = CalculadoraSeguimiento.actualizar(inicial, lectura(posicionAlSur(0.0), -90f, 2f), 6_000L).estado

        assertEquals(10f, mayor.rumboGrados ?: -1f, 0.001f)
        assertEquals(270f, negativo.rumboGrados ?: -1f, 0.001f)
    }

    // Verificar que en una simulación desviada no se suma distancia, ni en el salto de ida ni en el de regreso
    @Test
    fun `en una simulacion desviada no se suma distancia`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoParaDesvio(), INSTANTE_INICIAL).copy(esSimulado = true)

        val estado = recorrer(
            inicial,
            listOf(posicionAlSur(0.0), posicionAlSur(0.001, 0.0018), posicionAlSur(0.002, 0.0018), posicionAlSur(0.003))
        )

        assertEquals(0.0, estado.distanciaRecorridaKilometros, 0.0001)
    }

    // Verificar que en un recorrido real desviado la distancia sí se suma
    @Test
    fun `en un recorrido real desviado la distancia se suma`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoParaDesvio(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.001, 0.0018), posicionAlSur(0.002, 0.0018)))

        assertTrue(estado.distanciaRecorridaKilometros > 0.1)
    }

    // Verificar que un desplazamiento de 7 metros sin rumbo no alcanza para calcular el rumbo
    @Test
    fun `un desplazamiento menor a diez metros no calcula el rumbo`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = recorrer(inicial, listOf(posicionAlSur(0.0), posicionAlSur(0.00006)))

        assertNull(estado.rumboGrados)
    }

    // Verificar que la imprecisión de la lectura eleva el umbral para calcular el rumbo
    @Test
    fun `un desplazamiento menor a la precision de la lectura no calcula el rumbo`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)
        val primera = CalculadoraSeguimiento.actualizar(inicial, lectura(posicionAlSur(0.0)), 6_000L).estado

        val impreciso = CalculadoraSeguimiento.actualizar(
            primera,
            LecturaUbicacion(posicionAlSur(0.00014), null, null, precisionMetros = 20f),
            11_000L
        ).estado
        val preciso = CalculadoraSeguimiento.actualizar(
            primera,
            LecturaUbicacion(posicionAlSur(0.00014), null, null, precisionMetros = 5f),
            11_000L
        ).estado

        assertNull(impreciso.rumboGrados)
        assertEquals(180.0, preciso.rumboGrados?.toDouble() ?: -999.0, 1.0)
    }

    // Verificar que con rumbo del GPS y velocidad desconocida se acepta el rumbo
    @Test
    fun `con rumbo del GPS y velocidad desconocida se acepta el rumbo`() {
        val inicial = CalculadoraSeguimiento.iniciar(senderoLinealCorto(), INSTANTE_INICIAL)

        val estado = CalculadoraSeguimiento.actualizar(inicial, lectura(posicionAlSur(0.0), 45f, null), 6_000L).estado

        assertEquals(45f, estado.rumboGrados)
    }
}
