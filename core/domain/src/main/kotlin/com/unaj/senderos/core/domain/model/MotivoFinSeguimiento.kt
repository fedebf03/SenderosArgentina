package com.unaj.senderos.domain.model

// Causa por la que el seguimiento terminó: fallos del sistema o cancelación pedida por el usuario (que no guarda el recorrido)
enum class MotivoFinSeguimiento { PERMISO_DE_UBICACION_REVOCADO, ERROR_DE_UBICACION, CANCELADO_POR_EL_USUARIO }
