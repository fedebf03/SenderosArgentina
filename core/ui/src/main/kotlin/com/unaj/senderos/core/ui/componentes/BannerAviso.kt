package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unaj.senderos.presentation.tema.TemaSenderos

// Renderizar un aviso no bloqueante en una franja, con una acción opcional; el lector de pantalla lo anuncia al aparecer
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BannerAviso(
    texto: String,
    modifier: Modifier = Modifier,
    textoAccion: String? = null,
    alAccionar: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Outlined.CloudOff, contentDescription = null)
            // El texto y la acción comparten renglón si entran; si no, la acción pasa debajo del texto
            FlowRow(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = texto,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                if (textoAccion != null) {
                    TextButton(onClick = alAccionar) { Text(textoAccion) }
                }
            }
        }
    }
}

// Previsualizar el aviso de falta de conexión con y sin acción
@Preview(showBackground = true)
@Composable
private fun BannerAvisoPreview() {
    TemaSenderos {
        BannerAviso(texto = "Sin conexión: los cambios se sincronizarán después", textoAccion = "Entendido")
    }
}
