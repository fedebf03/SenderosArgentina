package com.unaj.senderos.core.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.ui.sendero.obtenerIconoTipoPunto
import com.unaj.senderos.core.ui.sendero.obtenerTextoTipoPunto

// Lo más urgente (agua y peligro) va primero en la grilla y "Otro" queda al final
private val TIPOS_DE_PUNTO_CAPTURABLES: List<TipoPunto> = listOf(
    TipoPunto.AGUA, TipoPunto.PELIGRO, TipoPunto.MIRADOR,
    TipoPunto.REFUGIO, TipoPunto.CAMPING, TipoPunto.OTRO
)
private const val COLUMNAS_GRILLA_TIPOS = 3

// Las baldosas son grandes para poder tocarlas caminando o con guantes
@Composable
fun GrillaTiposDePunto(tipoElegido: TipoPunto, alElegir: (TipoPunto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
        TIPOS_DE_PUNTO_CAPTURABLES.chunked(COLUMNAS_GRILLA_TIPOS).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                fila.forEach { tipo ->
                    BaldosaTipoDePunto(
                        tipo = tipo,
                        estaElegido = tipo == tipoElegido,
                        alTocar = { alElegir(tipo) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BaldosaTipoDePunto(tipo: TipoPunto, estaElegido: Boolean, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        selected = estaElegido,
        onClick = alTocar,
        shape = RoundedCornerShape(16.dp),
        color = if (estaElegido) esquema.primaryContainer else esquema.surfaceContainerHigh,
        contentColor = if (estaElegido) esquema.onPrimaryContainer else esquema.onSurface,
        border = if (estaElegido) BorderStroke(2.dp, esquema.primary) else null,
        modifier = modifier.heightIn(min = 88.dp).semantics { role = Role.RadioButton }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(imageVector = obtenerIconoTipoPunto(tipo), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(
                text = obtenerTextoTipoPunto(tipo),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (estaElegido) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
