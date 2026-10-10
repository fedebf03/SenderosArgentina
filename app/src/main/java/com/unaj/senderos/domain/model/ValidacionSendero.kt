package com.unaj.senderos.domain.model

// Campos de un sendero que pueden resultar inválidos al validarlo
enum class CampoSendero { NOMBRE, REGION, DESCRIPCION, DISTANCIA, DESNIVEL, ALTURA_MAXIMA, ALTURA_MINIMA, RECORRIDO }

// Error de validación que reúne todos los campos inválidos de un sendero
class SenderoInvalidoException(val camposInvalidos: Set<CampoSendero>) :
    IllegalArgumentException("Hay campos con errores")
