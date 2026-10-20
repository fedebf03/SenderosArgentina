package com.unaj.senderos.core.ui.sendero

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.esFotoLocal
import java.io.File
import com.unaj.senderos.core.ui.componentes.IlustracionMontanias

@Composable
fun FotoSendero(
    foto: String,
    modifier: Modifier = Modifier,
    espejada: Boolean = false,
    bioma: Bioma = Bioma.SIN_DEFINIR
) {
    if (foto.isEmpty()) {
        IlustracionMontanias(modifier = modifier, espejada = espejada, bioma = bioma)
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
