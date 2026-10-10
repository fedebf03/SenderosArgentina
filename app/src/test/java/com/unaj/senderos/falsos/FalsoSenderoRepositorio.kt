package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.SenderoRepositorio

class FalsoSenderoRepositorio(
    var resultado: Result<List<Sendero>> = Result.success(emptyList())
) : SenderoRepositorio {

    val idsUsuarioConsultados: MutableList<String> = mutableListOf()

    // Registrar el identificador consultado y devolver el resultado configurado
    override suspend fun obtenerSenderosDelUsuario(idUsuario: String): Result<List<Sendero>> {
        idsUsuarioConsultados.add(idUsuario)
        return resultado
    }
}
