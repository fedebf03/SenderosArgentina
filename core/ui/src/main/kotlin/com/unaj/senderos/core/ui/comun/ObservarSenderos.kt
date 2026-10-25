package com.unaj.senderos.core.ui.comun

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.model.SenderosDelUsuario
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val TAG = "SENDEROS"
private const val MENSAJE_POR_DEFECTO = "No se pudieron cargar los senderos"

// Explorar, Mis senderos, el mapa y el detalle público usan la misma lista y cada pantalla la presenta a su manera
fun ViewModel.observarSenderos(
    casoDeUso: ObservarSenderosDelUsuarioUseCase,
    alRecibir: (SenderosDelUsuario) -> Unit,
    alFallar: (String) -> Unit
) {
    val fallar = { error: Throwable ->
        Log.e(TAG, "Error al cargar senderos", error)
        alFallar(error.message?.takeIf { mensaje -> mensaje.isNotBlank() } ?: MENSAJE_POR_DEFECTO)
    }
    casoDeUso().fold(
        onSuccess = { flujo ->
            viewModelScope.launch {
                flujo.catch { error -> fallar(error) }.collect(alRecibir)
            }
        },
        onFailure = fallar
    )
}
