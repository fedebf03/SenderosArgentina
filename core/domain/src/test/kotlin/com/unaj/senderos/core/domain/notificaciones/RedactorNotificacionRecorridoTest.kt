package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoAvisoVisual
import com.unaj.senderos.domain.model.TipoPunto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactorNotificacionRecorridoTest {

    private val trazado = listOf(
        PuntoRecorrido(-34.0, -58.0),
        PuntoRecorrido(-34.0, -57.99, TipoPunto.MIRADOR, "Mirador del cerro"),
        PuntoRecorrido(-34.0, -57.98)
    )

    private val largoTrazadoKilometros = CalculadoraRecorrido.distanciasAcumuladasKilometros(trazado).last()

    // Crear un aviso de cercanía a un punto del tipo indicado
    private fun crearAvisoCercania(tipo: TipoPunto): AvisoRecorrido {
        return AvisoRecorrido.CercaDePunto(0, PuntoRecorrido(0.0, 0.0, tipo))
    }

    // Construir un estado con señal buena y los datos indicados
    private fun crearEstado(
        nombre: String = "Cerro Uritorco",
        recorridoKilometros: Double = 1.0,
        restanteKilometros: Double = 3.0,
        senal: CalidadSenal = CalidadSenal.BUENA,
        tiempoMilisegundos: Long = 0L,
        velocidad: Float? = null,
        pasos: Int? = null,
        indiceProximoPunto: Int = 1,
        posicion: Coordenada? = null
    ): EstadoSeguimiento {
        return EstadoSeguimiento(
            idSendero = "s1",
            trazado = trazado,
            nombreSendero = nombre,
            posicionActual = posicion,
            distanciaRecorridaKilometros = recorridoKilometros,
            distanciaRestanteKilometros = restanteKilometros,
            indiceProximoPunto = indiceProximoPunto,
            velocidadMetrosPorSegundo = velocidad,
            tiempoTranscurridoMilisegundos = tiempoMilisegundos,
            pasos = pasos,
            calidadSenal = senal
        )
    }

    // Verificar que el progreso es 1 menos lo que resta sobre el largo del trazado, en porcentaje
    @Test
    fun `el progreso es el porcentaje avanzado del largo del trazado`() {
        val estado = crearEstado(restanteKilometros = largoTrazadoKilometros * 0.75)
        assertEquals(25, RedactorNotificacionRecorrido.calcularProgresoPorcentaje(estado))
    }

    // Verificar que mientras se busca señal el progreso es indeterminado
    @Test
    fun `buscando senal el progreso es indeterminado`() {
        assertNull(RedactorNotificacionRecorrido.calcularProgresoPorcentaje(crearEstado(senal = CalidadSenal.BUSCANDO)))
    }

    // Verificar que sin estado el progreso es indeterminado
    @Test
    fun `sin estado el progreso es indeterminado`() {
        assertNull(RedactorNotificacionRecorrido.calcularProgresoPorcentaje(null))
    }

    // Verificar que el progreso queda acotado a [0, 100]
    @Test
    fun `el progreso queda acotado entre cero y cien`() {
        assertEquals(0, RedactorNotificacionRecorrido.calcularProgresoPorcentaje(crearEstado(restanteKilometros = largoTrazadoKilometros * 3)))
        assertEquals(100, RedactorNotificacionRecorrido.calcularProgresoPorcentaje(crearEstado(restanteKilometros = 0.0)))
    }

    // Verificar que un trazado sin largo da progreso cero
    @Test
    fun `un trazado sin largo da progreso cero`() {
        val estado = crearEstado(restanteKilometros = 0.0).copy(trazado = listOf(PuntoRecorrido(-34.0, -58.0)))
        assertEquals(0, RedactorNotificacionRecorrido.calcularProgresoPorcentaje(estado))
    }

    // Verificar que tras un desvío (recorrido caminado mayor al avanzado) el progreso sigue la distancia restante
    @Test
    fun `el progreso con desvio depende de lo que resta del trazado`() {
        val estado = crearEstado(recorridoKilometros = 9.0, restanteKilometros = largoTrazadoKilometros / 2)
        assertEquals(50, RedactorNotificacionRecorrido.calcularProgresoPorcentaje(estado))
    }

    // Verificar que el contenido lleva el instante de inicio para el cronómetro y nulo si se desconoce
    @Test
    fun `el contenido lleva el instante de inicio para el cronometro`() {
        assertEquals(5_000L, RedactorNotificacionRecorrido.redactarPersistente(crearEstado().copy(instanteInicioMilisegundos = 5_000L), false).instanteInicioMilisegundos)
        assertNull(RedactorNotificacionRecorrido.redactarPersistente(crearEstado(), false).instanteInicioMilisegundos)
        assertNull(RedactorNotificacionRecorrido.redactarPersistente(null, false).instanteInicioMilisegundos)
    }

    // Verificar el título con el nombre del sendero
    @Test
    fun `el titulo lleva el nombre del sendero`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(crearEstado(), esSimulado = false)
        assertEquals("Recorriendo Cerro Uritorco", contenido.titulo)
    }

    // Verificar el prefijo de simulación en el título
    @Test
    fun `el titulo de una simulacion lleva el prefijo`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(crearEstado(), esSimulado = true)
        assertEquals("Simulación · Recorriendo Cerro Uritorco", contenido.titulo)
    }

    // Verificar el título cuando el nombre está vacío
    @Test
    fun `sin nombre el titulo habla del sendero`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(crearEstado(nombre = ""), esSimulado = false)
        assertEquals("Recorriendo el sendero", contenido.titulo)
    }

    // Verificar el texto corto con distancias restante y recorrida
    @Test
    fun `el texto corto muestra kilometros restantes y recorridos`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(crearEstado(), esSimulado = false)
        assertEquals("3,0 km restantes · 1,0 km recorridos", contenido.textoCorto)
    }

    // Verificar que la señal débil tiene prioridad en el texto corto y en el expandido
    @Test
    fun `con senal debil el aviso de senal tiene prioridad`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(crearEstado(senal = CalidadSenal.DEBIL), esSimulado = false)
        assertEquals("Sin señal GPS · salí al aire libre", contenido.textoCorto)
        assertTrue(contenido.textoExpandido.startsWith("Sin señal GPS · salí al aire libre"))
    }

    // Verificar el texto mientras se busca señal
    @Test
    fun `buscando senal el texto lo informa`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(crearEstado(senal = CalidadSenal.BUSCANDO), esSimulado = false)
        assertEquals("Buscando señal GPS…", contenido.textoCorto)
        assertNull(contenido.progresoPorcentaje)
    }

    // Verificar el contenido sin estado
    @Test
    fun `sin estado el texto dice que se prepara el recorrido`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(null, esSimulado = false)
        assertEquals("Preparando el recorrido", contenido.textoCorto)
        assertNull(contenido.progresoPorcentaje)
    }

    // Verificar que el texto expandido incluye velocidad entera, pasos y próximo punto a 10 m, sin el tiempo (lo muestra el cronómetro)
    @Test
    fun `el texto expandido incluye velocidad pasos y proximo punto redondeados`() {
        val estado = crearEstado(
            tiempoMilisegundos = 3_900_000L,
            velocidad = 1.0f,
            pasos = 1234,
            posicion = Coordenada(-34.0, -58.0)
        )
        val lineas = RedactorNotificacionRecorrido.redactarPersistente(estado, esSimulado = false).textoExpandido.lines()
        assertEquals("3,0 km restantes · 1,0 km recorridos", lineas[0])
        assertFalse(lineas.any { linea -> linea.startsWith("Tiempo") })
        assertTrue(lineas.contains("Velocidad: 4 km/h"))
        assertTrue(lineas.contains("Pasos: 1234"))
        assertTrue(lineas.contains("Próximo punto: Mirador del cerro (a 920 m)"))
    }

    // Verificar que un pequeño movimiento no cambia el texto expandido (evita republicar la notificación sin necesidad)
    @Test
    fun `un movimiento menor a diez metros no cambia el texto expandido`() {
        val antes = crearEstado(velocidad = 1.0f, posicion = Coordenada(-34.0, -58.0))
        val despues = crearEstado(velocidad = 1.05f, posicion = Coordenada(-34.0, -57.99995))
        assertEquals(
            RedactorNotificacionRecorrido.redactarPersistente(antes, false).textoExpandido,
            RedactorNotificacionRecorrido.redactarPersistente(despues, false).textoExpandido
        )
    }

    // Verificar que sin pasos ni punto próximo esas líneas no aparecen
    @Test
    fun `sin pasos ni proximo punto se omiten esas lineas`() {
        val estado = crearEstado(indiceProximoPunto = 99)
        val texto = RedactorNotificacionRecorrido.redactarPersistente(estado, esSimulado = false).textoExpandido
        assertFalse(texto.contains("Pasos"))
        assertFalse(texto.contains("Próximo punto"))
    }

    // Verificar que la distancia aproximada se redondea a 10 m y a 0,1 km desde el kilómetro
    @Test
    fun `la distancia aproximada se redondea a diez metros`() {
        assertEquals("920 m", RedactorNotificacionRecorrido.formatearDistanciaAproximada(922.0))
        assertEquals("930 m", RedactorNotificacionRecorrido.formatearDistanciaAproximada(925.0))
        assertEquals("1,5 km", RedactorNotificacionRecorrido.formatearDistanciaAproximada(1540.0))
    }

    // Verificar el formato de distancias en metros y kilómetros
    @Test
    fun `la distancia corta va en metros y la larga en kilometros`() {
        assertEquals("350 m", RedactorNotificacionRecorrido.formatearDistanciaMetros(350.4))
        assertEquals("1,5 km", RedactorNotificacionRecorrido.formatearDistanciaMetros(1500.0))
    }

    // Verificar el tipo visual elegido para cada aviso
    @Test
    fun `cada aviso elige su tipo visual`() {
        assertEquals(TipoAvisoVisual.AGUA, RedactorNotificacionRecorrido.elegirTipoVisual(crearAvisoCercania(TipoPunto.AGUA)))
        assertEquals(TipoAvisoVisual.PELIGRO, RedactorNotificacionRecorrido.elegirTipoVisual(crearAvisoCercania(TipoPunto.PELIGRO)))
        assertEquals(TipoAvisoVisual.MIRADOR, RedactorNotificacionRecorrido.elegirTipoVisual(crearAvisoCercania(TipoPunto.MIRADOR)))
        assertEquals(TipoAvisoVisual.REFUGIO, RedactorNotificacionRecorrido.elegirTipoVisual(crearAvisoCercania(TipoPunto.REFUGIO)))
        assertEquals(TipoAvisoVisual.CAMPING, RedactorNotificacionRecorrido.elegirTipoVisual(crearAvisoCercania(TipoPunto.CAMPING)))
        assertEquals(TipoAvisoVisual.PASO, RedactorNotificacionRecorrido.elegirTipoVisual(crearAvisoCercania(TipoPunto.PASO)))
        assertEquals(TipoAvisoVisual.DESVIO, RedactorNotificacionRecorrido.elegirTipoVisual(AvisoRecorrido.Desvio))
        assertEquals(TipoAvisoVisual.LLEGADA, RedactorNotificacionRecorrido.elegirTipoVisual(AvisoRecorrido.LlegadaAlFinal))
    }

    // Verificar el aviso de cercanía con nombre y distancia
    @Test
    fun `el aviso de cercania lleva el nombre y la distancia`() {
        val aviso = AvisoRecorrido.CercaDePunto(1, trazado[1])
        val estado = crearEstado(posicion = Coordenada(-34.0, -57.9904))
        val contenido = RedactorNotificacionRecorrido.redactarAviso(aviso, estado)
        assertEquals("Cerca de Mirador del cerro", contenido.titulo)
        assertTrue(contenido.texto.startsWith("Mirador del cerro · a "))
        assertEquals(TipoAvisoVisual.MIRADOR, contenido.tipoVisual)
    }

    // Verificar el aviso de cercanía sin nombre ni posición
    @Test
    fun `el aviso sin nombre describe el tipo y sin posicion usa el umbral`() {
        val aviso = AvisoRecorrido.CercaDePunto(2, PuntoRecorrido(0.0, 0.0, TipoPunto.AGUA))
        val contenido = RedactorNotificacionRecorrido.redactarAviso(aviso, null)
        assertEquals("Cerca de una fuente de agua", contenido.titulo)
        assertEquals("Estás a menos de 50 metros", contenido.texto)
    }

    // Verificar el título de advertencia del peligro
    @Test
    fun `el aviso de peligro advierte con cuidado`() {
        val aviso = AvisoRecorrido.CercaDePunto(2, PuntoRecorrido(0.0, 0.0, TipoPunto.PELIGRO, "Grieta"))
        val contenido = RedactorNotificacionRecorrido.redactarAviso(aviso, null)
        assertEquals("¡Cuidado! Grieta cerca", contenido.titulo)
        assertEquals(TipoAvisoVisual.PELIGRO, contenido.tipoVisual)
    }

    // Verificar el aviso de desvío con la distancia al trazado
    @Test
    fun `el aviso de desvio informa la distancia al trazado`() {
        val estado = crearEstado().copy(distanciaAlTrazadoMetros = 140.0)
        val contenido = RedactorNotificacionRecorrido.redactarAviso(AvisoRecorrido.Desvio, estado)
        assertEquals("Te alejaste del sendero", contenido.titulo)
        assertEquals("Estás a unos 140 m del trazado", contenido.texto)
        assertFalse(contenido.permiteFinalizar)
    }

    // Verificar el aviso de llegada con la acción de finalizar
    @Test
    fun `el aviso de llegada permite finalizar`() {
        val contenido = RedactorNotificacionRecorrido.redactarAviso(AvisoRecorrido.LlegadaAlFinal, crearEstado())
        assertEquals("¡Llegaste al final!", contenido.titulo)
        assertEquals("Terminaste Cerro Uritorco. Tocá Finalizar para guardar el recorrido", contenido.texto)
        assertTrue(contenido.permiteFinalizar)
    }
}
