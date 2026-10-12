package com.unaj.senderos.presentation.formulario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Renderizar el diálogo que ofrece sacar una foto con la cámara o elegirla de la galería
@Composable
internal fun DialogoOrigenFoto(
    alSacarFoto: () -> Unit,
    alElegirDeGaleria: () -> Unit,
    alCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Agregar foto") },
        text = {
            Column {
                OpcionOrigenFoto(Icons.Outlined.PhotoCamera, "Sacar foto", alSacarFoto)
                OpcionOrigenFoto(Icons.Outlined.AddPhotoAlternate, "Elegir de la galería", alElegirDeGaleria)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}

// Renderizar una fila tocable con ícono y texto para una opción de origen de la foto
@Composable
private fun OpcionOrigenFoto(icono: ImageVector, texto: String, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alTocar)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}
