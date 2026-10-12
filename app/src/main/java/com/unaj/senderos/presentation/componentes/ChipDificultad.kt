package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.presentation.tema.obtenerColoresDificultad

// Renderizar la etiqueta de color que indica la dificultad de un sendero
@Composable
fun ChipDificultad(dificultad: Dificultad, modifier: Modifier = Modifier) {
    val colores = obtenerColoresDificultad(dificultad)

    Surface(shape = CircleShape, color = colores.fondo, modifier = modifier) {
        Text(
            text = obtenerTextoDificultad(dificultad),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = colores.texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}
