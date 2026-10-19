package com.unaj.senderos.core.domain.model

enum class CampoSendero { NOMBRE, REGION, DESCRIPCION, DISTANCIA, RECORRIDO }

class SenderoInvalidoException(val camposInvalidos: Set<CampoSendero>) :
    IllegalArgumentException("Hay campos con errores")
