package com.unaj.senderos.presentation.componentes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.unaj.senderos.domain.model.esFotoLocal
import java.io.File

@Composable
fun FotoSendero(foto: String, modifier: Modifier = Modifier, espejada: Boolean = false) {
    if (foto.isEmpty()) {
        IlustracionMontanias(modifier = modifier, espejada = espejada)
        return
    }
    val relleno = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
    AsyncImage(
        model = if (esFotoLocal(foto)) File(foto) else foto,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = relleno,
        error = relleno,
        modifier = modifier
    )
}
