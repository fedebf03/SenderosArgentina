package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.senderosargentina.R
import com.unaj.senderos.domain.model.Usuario

// Renderizar la cabecera de las secciones con el logo, el avatar y un contenido inferior opcional
@Composable
fun CabeceraPrincipal(
    usuario: Usuario?,
    alTocarAvatar: (() -> Unit)?,
    contenidoInferior: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo_senderos),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "SenderosArgentina",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            )
            if (alTocarAvatar != null) {
                Box(
                    modifier = Modifier.size(48.dp).accionable("Abrir perfil", alTocar = alTocarAvatar),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarUsuario(usuario = usuario, tamano = 36.dp)
                }
            }
        }
        contenidoInferior?.invoke()
    }
}

// Renderizar el campo de búsqueda de senderos con botón para borrar el texto
@Composable
fun BuscadorSenderos(texto: String, alCambiarTexto: (String) -> Unit) {
    TextField(
        value = texto,
        onValueChange = alCambiarTexto,
        placeholder = { Text("Buscar senderos") },
        leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { alCambiarTexto("") }) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth()
    )
}

// Renderizar el avatar circular del usuario con su foto o con su inicial
@Composable
fun AvatarUsuario(usuario: Usuario?, tamano: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        if (usuario != null && usuario.fotoUrl.isNotEmpty()) {
            AsyncImage(
                model = usuario.fotoUrl,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(tamano)
            )
        } else {
            Text(
                text = obtenerInicial(usuario),
                style = if (tamano > 48.dp) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// Obtener la inicial en mayúscula del nombre del usuario o, si falta, de su correo
private fun obtenerInicial(usuario: Usuario?): String {
    val texto = usuario?.nombre?.ifBlank { usuario.email } ?: ""
    return texto.trim().take(1).uppercase()
}
