package com.unaj.senderos.core.domain.port

fun interface GeneradorIdentificador {
    operator fun invoke(): String
}
