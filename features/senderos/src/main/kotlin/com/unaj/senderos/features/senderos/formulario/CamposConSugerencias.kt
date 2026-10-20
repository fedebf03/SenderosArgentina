package com.unaj.senderos.features.senderos.formulario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

// El desplegable se cierra al salir del campo, para no tapar el campo siguiente
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampoConSugerencias(
    valor: String,
    etiqueta: String,
    opciones: List<String>,
    alCambiar: (String) -> Unit,
    tieneError: Boolean = false,
    mensajeError: String? = null,
    accionTeclado: ImeAction = ImeAction.Next
) {
    var estaAbierto by remember { mutableStateOf(false) }
    val opcionesVisibles = remember(opciones, valor) { filtrarSugerencias(opciones, valor).filterNot { opcion -> opcion == valor } }
    val estaExpandido = estaAbierto && opcionesVisibles.isNotEmpty()
    ExposedDropdownMenuBox(
        expanded = estaExpandido,
        onExpandedChange = { quiereAbrir -> estaAbierto = quiereAbrir }
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = { texto ->
                alCambiar(texto)
                estaAbierto = true
            },
            label = { Text(etiqueta) },
            singleLine = true,
            isError = tieneError,
            supportingText = if (tieneError && mensajeError != null) {
                { Text(text = mensajeError, style = MaterialTheme.typography.bodySmall) }
            } else {
                null
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = estaExpandido,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable)
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = accionTeclado),
            keyboardActions = KeyboardActions(onAny = { estaAbierto = false; defaultKeyboardAction(accionTeclado) }),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .onFocusChanged { estadoFoco -> if (!estadoFoco.isFocused) estaAbierto = false }
        )
        ExposedDropdownMenu(
            expanded = estaExpandido,
            onDismissRequest = { estaAbierto = false }
        ) {
            opcionesVisibles.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        alCambiar(opcion)
                        estaAbierto = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipsDeSugerencias(
    sugerencias: List<String>,
    yaAgregadas: List<String>,
    alAgregar: (String) -> Unit
) {
    val pendientes = sugerencias.filterNot { sugerencia ->
        yaAgregadas.any { agregada -> agregada.equals(sugerencia, ignoreCase = true) }
    }
    if (pendientes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Sugerencias",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pendientes.forEach { sugerencia ->
                SuggestionChip(
                    onClick = { alAgregar(sugerencia) },
                    modifier = Modifier.semantics { contentDescription = "Agregar $sugerencia" },
                    label = { Text(sugerencia) },
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}
