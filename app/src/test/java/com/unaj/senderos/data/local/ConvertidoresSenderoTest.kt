package com.unaj.senderos.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertidoresSenderoTest {

    private val convertidores = ConvertidoresSendero()

    // Probar que una lista de textos con comas y comillas sobrevive a la ida y vuelta
    @Test
    fun `lista de textos ida y vuelta conserva el contenido`() {
        val original = listOf("Llevar agua", "Abrigo, impermeable", "Usar \"bastones\"")

        val texto = convertidores.listaTextosATexto(original)
        val recuperada = convertidores.textoAListaTextos(texto)

        assertEquals(original, recuperada)
    }

    // Probar que una lista de textos vacía sobrevive a la ida y vuelta
    @Test
    fun `lista de textos vacia ida y vuelta devuelve lista vacia`() {
        val texto = convertidores.listaTextosATexto(emptyList())

        assertEquals(emptyList<String>(), convertidores.textoAListaTextos(texto))
    }

    // Probar que una lista de etapas sobrevive a la ida y vuelta
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

    // Probar que una lista de etapas vacía sobrevive a la ida y vuelta
    @Test
    fun `lista de etapas vacia ida y vuelta devuelve lista vacia`() {
        val texto = convertidores.listaEtapasATexto(emptyList())

        assertEquals(emptyList<EtapaEntidad>(), convertidores.textoAListaEtapas(texto))
    }
}
