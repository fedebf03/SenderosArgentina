package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.port.AlmacenBorradorRecorrido

class FalsoAlmacenBorradorRecorrido : AlmacenBorradorRecorrido {

    var borrador: RecorridoRealizado? = null
    val guardados: MutableList<RecorridoRealizado> = mutableListOf()
    var cantidadBorrados: Int = 0
        private set
    var resultadoGuardar: Result<Unit> = Result.success(Unit)

    override suspend fun guardar(borrador: RecorridoRealizado): Result<Unit> {
        guardados.add(borrador)
        if (resultadoGuardar.isSuccess) this.borrador = borrador
        return resultadoGuardar
    }

    override suspend fun leer(): RecorridoRealizado? = borrador

    override suspend fun borrar() {
        cantidadBorrados++
        borrador = null
    }
}
