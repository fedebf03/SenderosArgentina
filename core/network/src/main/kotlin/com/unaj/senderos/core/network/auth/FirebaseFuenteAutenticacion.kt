package com.unaj.senderos.core.network

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import javax.inject.Inject

private const val TAG = "AUTH"

class FirebaseFuenteAutenticacion @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : FuenteAutenticacion {

    override suspend fun iniciarSesion(email: String, clave: String): Result<UsuarioRed> {
        return esperarResultado { firebaseAuth.signInWithEmailAndPassword(email, clave) }
    }

    override suspend fun registrar(email: String, clave: String): Result<UsuarioRed> {
        return esperarResultado { firebaseAuth.createUserWithEmailAndPassword(email, clave) }
    }

    override suspend fun iniciarSesionConGoogle(idToken: String): Result<UsuarioRed> {
        val credencialGoogle = GoogleAuthProvider.getCredential(idToken, null)
        return esperarResultado { firebaseAuth.signInWithCredential(credencialGoogle) }
    }

    // Enviar el correo de recuperación sin revelar si el correo pertenece a una cuenta existente
    override suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (excepcion: FirebaseAuthInvalidUserException) {
            Log.d(TAG, "Solicitud de recuperación procesada")
            Result.success(Unit)
        } catch (excepcion: Exception) {
            Log.e(TAG, "Error al enviar el correo de recuperación", excepcion)
            val mensaje = when (excepcion) {
                is FirebaseAuthInvalidCredentialsException -> "El correo no tiene un formato válido"
                is FirebaseNetworkException -> "Sin conexión a internet"
                else -> "No se pudo enviar el correo de recuperación"
            }
            Result.failure(Exception(mensaje, excepcion))
        }
    }

    override fun obtenerUsuarioActual(): UsuarioRed? {
        return firebaseAuth.currentUser?.aUsuarioRed()
    }

    override fun cerrarSesion() {
        firebaseAuth.signOut()
    }

    private suspend fun esperarResultado(operacion: () -> Task<AuthResult>): Result<UsuarioRed> {
        return suspendCancellableCoroutine { continuacion ->
            operacion()
                .addOnSuccessListener { resultado ->
                    val usuario = resultado.user?.aUsuarioRed()
                    continuacion.resume(
                        if (usuario != null) {
                            Result.success(usuario)
                        } else {
                            Result.failure(Exception("No se pudo iniciar sesión"))
                        }
                    )
                }
                .addOnFailureListener { error ->
                    continuacion.resume(Result.failure(traducirError(error)))
                }
        }
    }

    private fun FirebaseUser.aUsuarioRed() = UsuarioRed(
        id = uid,
        email = email ?: "",
        nombre = displayName ?: "",
        fotoUrl = photoUrl?.toString() ?: ""
    )

    private fun traducirError(error: Exception): Exception {
        Log.e(TAG, "Error de Firebase Auth", error)
        val mensaje = when (error) {
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil"
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
            is FirebaseNetworkException -> "Sin conexión a internet"
            else -> "Ocurrió un error inesperado"
        }
        return Exception(mensaje, error)
    }
}
