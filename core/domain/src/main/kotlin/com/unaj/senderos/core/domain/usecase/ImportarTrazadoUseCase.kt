package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.port.LectorArchivos
import com.unaj.senderos.core.domain.recorrido.ImportadorTrazado
import com.unaj.senderos.core.domain.recorrido.LectorTrazados
import com.unaj.senderos.core.domain.recorrido.TrazadoImportado
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Importa el trazado de un archivo GPX o CSV de otra aplicación para cargarlo en el formulario del sendero
class ImportarTrazadoUseCase(
    private val lectorArchivos: LectorArchivos,
    // Interpretar y simplificar un archivo grande lleva tiempo de procesador: nunca en el hilo principal
    private val despachadorCalculo: CoroutineDispatcher = Dispatchers.Default
) {
    // Construir el caso de uso para Hilt, que solo conoce el lector de archivos
    @Inject
    constructor(lectorArchivos: LectorArchivos) : this(lectorArchivos, Dispatchers.Default)


    // Leer el archivo elegido, interpretarlo y convertirlo en un recorrido editable; los fallos traen un mensaje para el usuario
    suspend operator fun invoke(uri: String): Result<TrazadoImportado> {
        val archivo = lectorArchivos.leerTexto(uri).getOrElse { fallo -> return Result.failure(fallo) }
        return withContext(despachadorCalculo) {
            LectorTrazados.leer(archivo.texto, archivo.nombre).fold(
                onSuccess = { leido -> ImportadorTrazado.importar(leido, archivo.nombre) },
                onFailure = { fallo -> Result.failure(fallo) }
            )
        }
    }
}
