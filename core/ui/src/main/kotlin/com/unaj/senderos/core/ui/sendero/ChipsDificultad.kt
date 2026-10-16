package com.unaj.senderos.core.ui.componentes

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.unaj.senderos.core.domain.model.Dificultad

@Composable
fun ChipsDificultad(
    seleccionadas: Set<Dificultad>,
    alAlternar: (Dificultad) -> Unit
) {
    Dificultad.entries.forEach { dificultad ->
        val texto = obtenerTextoDificultad(dificultad)
        FilterChip(
            selected = dificultad in seleccionadas,
            onClick = { alAlternar(dificultad) },
            label = { Text(texto) },
            leadingIcon = { IndicadorDificultad(dificultad, mostrarTexto = false) },
            modifier = Modifier.semantics { contentDescription = "Dificultad ${texto.lowercase()}" }
        )
    }
}
