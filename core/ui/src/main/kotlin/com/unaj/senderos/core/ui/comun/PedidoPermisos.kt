package com.unaj.senderos.core.ui.comun

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

// Desde Android 13 (API 33) las notificaciones necesitan el permiso POST_NOTIFICATIONS. Antes se ignora
@SuppressLint("InlinedApi")
val PERMISOS_NOTIFICACIONES: Array<String> = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

// Si el usuario ya negó el permiso una vez, antes de volver a pedirlo se explica para qué se usa. Si eligió no volver
// a preguntar, Android lo rechaza sin mostrar nada y la respuesta lo indica para ofrecer la configuración de la app
@Composable
fun recordarPedidoDePermisos(
    permisos: Array<String>,
    explicacion: String,
    alResponder: (respuesta: Map<String, Boolean>, negadoParaSiempre: Boolean) -> Unit
): () -> Unit {
    val contexto = LocalContext.current
    val permisosActuales by rememberUpdatedState(permisos)
    val alResponderActual by rememberUpdatedState(alResponder)
    var esExplicacionVisible by rememberSaveable { mutableStateOf(false) }

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        val ningunoOtorgado = respuesta.values.none { otorgado -> otorgado }
        alResponderActual(respuesta, ningunoOtorgado && quedoNegadoParaSiempre(contexto, permisosActuales))
    }
    // Elegir "Ahora no" cuenta como una negativa, así la pantalla reacciona igual que si lo negara en el diálogo de Android
    val rechazarSinPedir = {
        esExplicacionVisible = false
        alResponderActual(permisosActuales.associateWith { false }, false)
    }

    if (esExplicacionVisible) {
        AlertDialog(
            onDismissRequest = rechazarSinPedir,
            title = { Text("Permiso necesario") },
            text = { Text(explicacion) },
            confirmButton = {
                TextButton(
                    onClick = {
                        esExplicacionVisible = false
                        lanzador.launch(permisosActuales)
                    }
                ) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = rechazarSinPedir) { Text("Ahora no") } }
        )
    }

    return remember(contexto, lanzador) {
        {
            if (debeExplicarAntesDePedir(contexto, permisosActuales)) {
                esExplicacionVisible = true
            } else {
                lanzador.launch(permisosActuales)
            }
        }
    }
}

// Con un permiso negado para siempre el aviso ofrece abrir la configuración de la app, el único lugar donde se puede dar
suspend fun SnackbarHostState.mostrarAvisoDePermiso(mensaje: String, ofrecerAjustes: Boolean, contexto: Context) {
    if (!ofrecerAjustes) {
        showSnackbar(mensaje)
        return
    }
    val resultado = showSnackbar(mensaje, actionLabel = "Ajustes", duration = SnackbarDuration.Long)
    if (resultado == SnackbarResult.ActionPerformed) abrirAjustesDeLaApp(contexto)
}

private fun debeExplicarAntesDePedir(contexto: Context, permisos: Array<String>): Boolean {
    val actividad = contexto.buscarActividad() ?: return false
    return permisos.any { permiso -> ActivityCompat.shouldShowRequestPermissionRationale(actividad, permiso) }
}

private fun quedoNegadoParaSiempre(contexto: Context, permisos: Array<String>): Boolean {
    val actividad = contexto.buscarActividad() ?: return false
    return permisos.none { permiso -> ActivityCompat.shouldShowRequestPermissionRationale(actividad, permiso) }
}

private fun abrirAjustesDeLaApp(contexto: Context) {
    val intencion = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
    if (contexto.buscarActividad() == null) intencion.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    contexto.startActivity(intencion)
}

private tailrec fun Context.buscarActividad(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.buscarActividad()
        else -> null
    }
}
