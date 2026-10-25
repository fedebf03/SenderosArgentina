package com.unaj.senderos.core.domain.model

// La antigüedad solo se informa con la señal débil, para que la pantalla avise "posición de hace X s"
data class ResultadoCaptura(
    val punto: PuntoCapturado,
    val antiguedadPosicionMilisegundos: Long?
)
