package com.unaj.senderos.features.senderos.formulario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun CampoTexto(
    valor: String,
    etiqueta: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    tieneError: Boolean = false,
    mensajeError: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionTeclado: ImeAction = ImeAction.Next,
    esUnaLinea: Boolean = true,
    alTerminar: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = esUnaLinea,
        minLines = if (esUnaLinea) 1 else 3,
        isError = tieneError,
        supportingText = if (tieneError && mensajeError != null) {
            { Text(text = mensajeError, style = MaterialTheme.typography.bodySmall) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = accionTeclado),
        keyboardActions = KeyboardActions(onDone = { alTerminar?.invoke() }),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun FichaSeleccionable(
    texto: String,
    estaSeleccionada: Boolean,
    alSeleccionar: () -> Unit,
    iconoInicial: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = estaSeleccionada,
        onClick = alSeleccionar,
        label = { Text(texto) },
        leadingIcon = iconoInicial,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
internal fun FilaElementoQuitable(
    texto: String,
    descripcionQuitar: String,
    alQuitar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = alQuitar) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = descripcionQuitar)
        }
    }
}

@Composable
internal fun CampoConBotonAgregar(
    valor: String,
    etiqueta: String,
    descripcionAgregar: String,
    alCambiar: (String) -> Unit,
    alAgregar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CampoTexto(
            valor = valor,
            etiqueta = etiqueta,
            alCambiar = alCambiar,
            modifier = Modifier.weight(1f),
            accionTeclado = ImeAction.Done,
            alTerminar = alAgregar
        )
        IconButton(onClick = alAgregar) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = descripcionAgregar)
        }
    }
}
