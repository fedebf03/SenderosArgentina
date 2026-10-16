package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.network.UsuarioRed

fun UsuarioRed.aDominio(): Usuario = Usuario(
    id = id,
    email = email,
    nombre = nombre,
    fotoUrl = fotoUrl
)
