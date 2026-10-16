package com.unaj.senderos.core.ui.componentes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BotonCircular(
    icono: ImageVector,
    descripcion: String,
    alTocar: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        IconButton(onClick = alTocar) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
