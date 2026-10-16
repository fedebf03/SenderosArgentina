package com.unaj.senderos.core.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.ui.tema.obtenerColoresDelBioma

/** Dibujar un punto con el color principal del bioma; es decorativo y siempre va junto al nombre. */
@Composable
fun PuntoBioma(bioma: Bioma, modifier: Modifier = Modifier, tamano: Dp = 10.dp) {
    val colores = obtenerColoresDelBioma(bioma)
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(colores.principal)
    )
}

/** Mostrar una etiqueta con un punto de color y el nombre del bioma. */
@Composable
fun EtiquetaBioma(bioma: Bioma, modifier: Modifier = Modifier, conFondo: Boolean = true) {
    val colores = obtenerColoresDelBioma(bioma)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(if (conFondo) Modifier.background(colores.contenedor) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = obtenerDescripcionBioma(bioma) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PuntoBioma(bioma = bioma, tamano = 8.dp)
        Text(
            text = obtenerTextoBioma(bioma),
            style = MaterialTheme.typography.labelMedium,
            color = colores.sobreContenedor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
