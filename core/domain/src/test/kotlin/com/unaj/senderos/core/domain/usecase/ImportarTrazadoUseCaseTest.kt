package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.port.ArchivoLeido
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportarTrazadoUseCaseTest {

    private val lector = FalsoLectorArchivos()
    private val casoDeUso = ImportarTrazadoUseCase(lector)

    @Test
    fun `importa el trazado del archivo elegido`() = runTest {
        lector.archivos["content://ruta"] = ArchivoLeido("Cerro_Otto.csv", "lat,lon\n-41.10,-71.30\n-41.12,-71.33")

        val importado = casoDeUso("content://ruta").getOrThrow()

        assertEquals("Cerro Otto", importado.nombre)
        assertEquals(TipoRecorrido.LINEAL, importado.tipoRecorrido)
        assertEquals(2, importado.recorrido.size)
    }

    @Test
    fun `un archivo que no se puede leer devuelve el fallo del lector`() = runTest {
        assertEquals("No se pudo leer el archivo", casoDeUso("content://inexistente").exceptionOrNull()?.message)
    }

    @Test
    fun `un archivo sin puntos devuelve un mensaje claro`() = runTest {
        lector.archivos["content://foto"] = ArchivoLeido("foto.jpg", "texto cualquiera")

        assertEquals("El archivo no tiene puntos de recorrido válidos", casoDeUso("content://foto").exceptionOrNull()?.message)
    }
}
