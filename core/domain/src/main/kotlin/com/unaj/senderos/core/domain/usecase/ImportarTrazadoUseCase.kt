package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.port.LectorArchivos
import com.unaj.senderos.core.domain.recorrido.ImportadorTrazado
import com.unaj.senderos.core.domain.recorrido.LectorTrazados
import com.unaj.senderos.core.domain.recorrido.TrazadoImportado
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportarTrazadoUseCase(
    private val lectorArchivos: LectorArchivos,
    // Interpretar y simplificar un archivo grande lleva tiempo de procesador, por eso nunca va en el hilo principal
    private val despachadorCalculo: CoroutineDispatcher = Dispatchers.Default
) {
    @Inject
    constructor(lectorArchivos: LectorArchivos) : this(lectorArchivos, Dispatchers.Default)


    // RF4 - Importar GPX: leemos el archivo elegido y lo convertimos en un recorrido editable para el formulario
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
