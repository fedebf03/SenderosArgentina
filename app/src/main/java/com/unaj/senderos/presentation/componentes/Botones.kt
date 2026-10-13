package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unaj.senderos.presentation.tema.TemaSenderos

private val ALTURA_MINIMA_BOTON = 50.dp
private val TAMANO_ICONO_BOTON = 20.dp
private val TAMANO_INDICADOR_BOTON = 22.dp
private val SEPARACION_ICONO_TEXTO = 12.dp

@Composable
fun IconoDeBoton(imagen: ImageVector) {
    Icon(imageVector = imagen, contentDescription = null, modifier = Modifier.size(TAMANO_ICONO_BOTON))
}

@Composable
private fun RowScope.ContenidoDeBoton(
    texto: String,
    icono: (@Composable () -> Unit)?,
    estaCargando: Boolean,
    colorIndicador: Color
) {
    if (estaCargando) {
        CircularProgressIndicator(
            modifier = Modifier.size(TAMANO_INDICADOR_BOTON),
            strokeWidth = 2.dp,
            color = colorIndicador
        )
    } else {
        if (icono != null) {
            icono()
            Spacer(modifier = Modifier.width(SEPARACION_ICONO_TEXTO))
        }
        Text(texto)
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    icono: (@Composable () -> Unit)? = null,
    estaCargando: Boolean = false,
    estaHabilitado: Boolean = true
) {
    Button(
        onClick = alTocar,
        enabled = estaHabilitado && !estaCargando,
        modifier = modifier.heightIn(min = ALTURA_MINIMA_BOTON).semantics(mergeDescendants = true) {
            if (estaCargando) {
                contentDescription = texto
                stateDescription = "Cargando"
            }
        }
    ) {
        ContenidoDeBoton(texto, icono, estaCargando, MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    icono: (@Composable () -> Unit)? = null,
    estaHabilitado: Boolean = true,
    esDestructivo: Boolean = false
) {
    val colores = if (esDestructivo) {
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    } else {
        ButtonDefaults.outlinedButtonColors()
    }
    OutlinedButton(
        onClick = alTocar,
        enabled = estaHabilitado,
        colors = colores,
        border = if (esDestructivo) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.outlinedButtonBorder(estaHabilitado)
        },
        modifier = modifier.heightIn(min = ALTURA_MINIMA_BOTON)
    ) {
        ContenidoDeBoton(texto, icono, false, MaterialTheme.colorScheme.primary)
    }
}

@Preview(showBackground = true)
@Composable
private fun BotonesPreview() {
    TemaSenderos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonPrincipal(texto = "Guardar", alTocar = {}, icono = { IconoDeBoton(Icons.Filled.Check) })
            BotonSecundario(texto = "Editar", alTocar = {})
        }
    }
}
