package com.unaj.senderos.core.domain.model

enum class CampoSendero { NOMBRE, REGION, DESCRIPCION, DISTANCIA, DESNIVEL, ALTURA_MAXIMA, ALTURA_MINIMA, RECORRIDO }

class SenderoInvalidoException(val camposInvalidos: Set<CampoSendero>) :
    IllegalArgumentException("Hay campos con errores")
