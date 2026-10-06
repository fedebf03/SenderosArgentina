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
import kotlin.coroutines.resume

class AutenticacionRepositorioImpl : AutenticacionRepositorio {

    // Autenticar al usuario mediante Firebase Auth con correo y contraseña
    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> {
        return esperarResultado { FirebaseAuth.getInstance().signInWithEmailAndPassword(email, clave) }
    }

    // Crear la cuenta en Firebase Auth; si sale bien, el usuario queda con la sesión iniciada
    override suspend fun registrar(email: String, clave: String): Result<Usuario> {
        return esperarResultado { FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, clave) }
    }

    // Autenticar al usuario mediante Firebase Auth utilizando el credencial de Google
    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> {
        val credencialGoogle = GoogleAuthProvider.getCredential(idToken, null)
        return esperarResultado { FirebaseAuth.getInstance().signInWithCredential(credencialGoogle) }
    }

    // Obtener la información del usuario con sesión activa en Firebase
    override fun obtenerUsuarioActual(): Usuario? {
        return FirebaseAuth.getInstance().currentUser?.aUsuario()
    }

    // Finalizar la sesión actual del usuario autenticado
    override fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
    }

    // Convertir la tarea de Firebase en un Result, traduciendo los errores al español
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

    // Pasar los datos del usuario de Firebase al modelo de dominio
    private fun FirebaseUser.aUsuario() = Usuario(
        id = uid,
        email = email ?: "",
        nombre = displayName ?: "",
        fotoUrl = photoUrl?.toString() ?: ""
    )

    // Reemplazar los mensajes en inglés de Firebase por mensajes para el usuario
    private fun traducirError(error: Exception): Exception {
        Log.e("AUTH", "Error de Firebase Auth", error)
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
