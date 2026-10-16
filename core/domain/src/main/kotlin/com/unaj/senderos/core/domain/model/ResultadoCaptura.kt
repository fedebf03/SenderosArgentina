package com.unaj.senderos.core.domain.model

// Resultado de capturar un punto de interés durante el recorrido.
// La antigüedad de la posición usada se informa solo con la señal débil (null si la señal es buena), para que la UI avise "posición de hace X s"
data class ResultadoCaptura(
    val punto: PuntoCapturado,
    val antiguedadPosicionMilisegundos: Long?
)
