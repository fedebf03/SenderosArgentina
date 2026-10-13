package com.unaj.senderos.presentation.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.senderosargentina.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

private const val TAG = "AUTH"

suspend fun pedirTokenDeGoogle(
    contexto: Context,
    alObtenerToken: (String) -> Unit,
    alMostrarMensaje: suspend (String) -> Unit
) {
    val opcionGoogle = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(contexto.getString(R.string.default_web_client_id))
        .build()

    val peticion = GetCredentialRequest.Builder()
        .addCredentialOption(opcionGoogle)
        .build()

    try {
        val respuesta = CredentialManager.create(contexto).getCredential(contexto, peticion)
        val credencial = respuesta.credential

        if (credencial is CustomCredential &&
            credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            alObtenerToken(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
        } else {
            alMostrarMensaje("No se pudo iniciar con Google")
        }
    } catch (_: GetCredentialCancellationException) {
        // El usuario cerró el selector de cuentas, no hay nada que avisar
    } catch (e: NoCredentialException) {
        // Puede faltar la cuenta en el dispositivo o la huella SHA-1 en Firebase
        Log.e(TAG, "Sin cuentas de Google disponibles", e)
        alMostrarMensaje("No se encontró una cuenta de Google")
    } catch (e: GetCredentialException) {
        Log.e(TAG, "Falló el inicio de sesión con Google", e)
        alMostrarMensaje("No se pudo iniciar con Google")
    }
}
