package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.unaj.senderos.presentation.comun.TEXTO_SIN_DATO

// Texto que lee el lector de pantalla en lugar del guion que se muestra cuando falta un dato
private const val DESCRIPCION_SIN_DATO = "sin dato"

fun Modifier.accionable(etiquetaAccion: String?, rol: Role = Role.Button, alTocar: () -> Unit): Modifier {
    return this.clickable(onClickLabel = etiquetaAccion, role = rol, onClick = alTocar)
}

fun Modifier.descripcionSiSinDato(texto: String): Modifier {
    return if (texto == TEXTO_SIN_DATO) this.semantics { contentDescription = DESCRIPCION_SIN_DATO } else this
}
