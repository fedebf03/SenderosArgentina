package com.unaj.senderos.core.domain.port

fun interface Reloj {
    operator fun invoke(): Long
}
