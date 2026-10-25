package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.model.ResultadoGuardadoPerfil
import com.unaj.senderos.core.domain.repository.PerfilRepositorio

class FalsoPerfilRepositorio : PerfilRepositorio {

    var perfilGuardado: PerfilUsuario? = null
    var resultadoGuardar: Result<ResultadoGuardadoPerfil> = Result.success(ResultadoGuardadoPerfil(estaEnLaNube = true))
    val perfilesGuardados: MutableList<Pair<String, PerfilUsuario>> = mutableListOf()
    var cantidadSincronizaciones: Int = 0
        private set

    override suspend fun obtener(idUsuario: String): PerfilUsuario? = perfilGuardado

    override suspend fun guardar(idUsuario: String, perfil: PerfilUsuario): Result<ResultadoGuardadoPerfil> {
        perfilesGuardados.add(idUsuario to perfil)
        if (resultadoGuardar.isSuccess) perfilGuardado = perfil
        return resultadoGuardar
    }

    override suspend fun sincronizarPendiente(idUsuario: String) {
        cantidadSincronizaciones++
    }
}
