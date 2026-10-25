package com.unaj.senderos.core.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.unaj.senderos.core.ui.tema.TemaSenderos

// Si confirmar interrumpe o destruye algo, el botón de descarte lleva un verbo de continuidad ("Seguir…").
// En cambio "Cancelar" queda para cuando confirmar es una acción neutra
@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    alConfirmar: () -> Unit,
    textoDescartar: String,
    alDescartar: () -> Unit,
    alCerrar: () -> Unit = alDescartar,
    esDestructivo: Boolean = false
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = alConfirmar) {
                if (esDestructivo) {
                    Text(textoConfirmar, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(textoConfirmar)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = alDescartar) { Text(textoDescartar) }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DialogoConfirmacionPreview() {
    TemaSenderos {
        DialogoConfirmacion(
            titulo = "Eliminar sendero",
            texto = "¿Querés eliminar Cerro Catedral? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            alConfirmar = {},
            textoDescartar = "Cancelar",
            alDescartar = {},
            esDestructivo = true
        )
    }
}
