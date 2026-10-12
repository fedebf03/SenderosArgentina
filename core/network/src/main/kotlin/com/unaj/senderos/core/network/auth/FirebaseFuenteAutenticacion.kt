package com.unaj.senderos.data.repository

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
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

private const val TAG = "AUTH"

class AutenticacionRepositorioImpl : AutenticacionRepositorio {

    // Iniciar sesión con correo y contraseña
    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> {
        return esperarResultado { FirebaseAuth.getInstance().signInWithEmailAndPassword(email, clave) }
    }

    // Registrar un usuario nuevo con correo y contraseña
    override suspend fun registrar(email: String, clave: String): Result<Usuario> {
        return esperarResultado { FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, clave) }
    }

    // Iniciar sesión con el token de identidad de Google
    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> {
        val credencialGoogle = GoogleAuthProvider.getCredential(idToken, null)
        return esperarResultado { FirebaseAuth.getInstance().signInWithCredential(credencialGoogle) }
    }

    // Enviar el correo de recuperación sin revelar si el correo pertenece a una cuenta existente
    override suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
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

    // Obtener el usuario con sesión activa, si existe
    override fun obtenerUsuarioActual(): Usuario? {
        return FirebaseAuth.getInstance().currentUser?.aUsuario()
    }

    // Cerrar la sesión del usuario actual
    override fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
    }

    // Esperar el resultado de la operación de autenticación y convertirlo en Result
    private suspend fun esperarResultado(operacion: () -> Task<AuthResult>): Result<Usuario> {
        return suspendCancellableCoroutine { continuacion ->
            operacion()
                .addOnSuccessListener { resultado ->
                    val usuario = resultado.user?.aUsuario()
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

    // Convertir el usuario de Firebase en el modelo de dominio
    private fun FirebaseUser.aUsuario() = Usuario(
        id = uid,
        email = email ?: "",
        nombre = displayName ?: "",
        fotoUrl = photoUrl?.toString() ?: ""
    )

    // Traducir el error de Firebase a un mensaje en español
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
