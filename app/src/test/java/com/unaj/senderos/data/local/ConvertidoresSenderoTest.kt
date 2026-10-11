package com.unaj.senderos.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertidoresSenderoTest {

    private val convertidores = ConvertidoresSendero()

    @Test
    fun `lista de textos ida y vuelta conserva el contenido`() {
        val original = listOf("Llevar agua", "Abrigo, impermeable", "Usar \"bastones\"")

        val texto = convertidores.listaTextosATexto(original)
        val recuperada = convertidores.textoAListaTextos(texto)

        assertEquals(original, recuperada)
    }

    @Test
    fun `lista de textos vacia ida y vuelta devuelve lista vacia`() {
        val texto = convertidores.listaTextosATexto(emptyList())

        assertEquals(emptyList<String>(), convertidores.textoAListaTextos(texto))
    }

    @Test
    fun `lista de etapas ida y vuelta conserva el contenido`() {
        val original = listOf(
            EtapaEntidad(numero = 1, nombre = "Lago Aluminé - Ruca Choroi", distanciaKilometros = 21.5),
            EtapaEntidad(numero = 2, nombre = "Ruca Choroi - Quillén", distanciaKilometros = 25.0)
        )

        val texto = convertidores.listaEtapasATexto(original)
        val recuperada = convertidores.textoAListaEtapas(texto)

        assertEquals(original, recuperada)
    }

    @Test
    fun `lista de etapas vacia ida y vuelta devuelve lista vacia`() {
        val texto = convertidores.listaEtapasATexto(emptyList())

        assertEquals(emptyList<EtapaEntidad>(), convertidores.textoAListaEtapas(texto))
    }

    @Test
    fun `lista de puntos ida y vuelta conserva el contenido y el orden`() {
        val original = listOf(
            PuntoRecorridoEntidad(latitud = -39.2, longitud = -71.1, tipo = "REFUGIO", nombre = "Refugio \"Norte\", cima"),
            PuntoRecorridoEntidad(latitud = -39.3, longitud = -71.2, tipo = "PASO", nombre = ""),
            PuntoRecorridoEntidad(latitud = -39.4, longitud = -71.3, tipo = "MIRADOR", nombre = "Mirador")
        )

        val texto = convertidores.listaPuntosATexto(original)
        val recuperada = convertidores.textoAListaPuntos(texto)

        assertEquals(original, recuperada)
    }

    @Test
    fun `lista de puntos vacia se serializa como arreglo vacio`() {
        val texto = convertidores.listaPuntosATexto(emptyList())

        assertEquals("[]", texto)
        assertEquals(emptyList<PuntoRecorridoEntidad>(), convertidores.textoAListaPuntos(texto))
    }

    @Test
    fun `valor por defecto de la columna se lee como lista vacia de puntos`() {
        assertEquals(emptyList<PuntoRecorridoEntidad>(), convertidores.textoAListaPuntos("[]"))
    }
}
