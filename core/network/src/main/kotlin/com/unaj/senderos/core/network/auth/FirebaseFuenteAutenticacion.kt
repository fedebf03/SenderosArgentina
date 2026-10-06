package com.unaj.senderos.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AutenticacionRepositorioImpl : AutenticacionRepositorio {

    // Obtener la instancia de FirebaseAuth de forma segura
    private fun obtenerFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // Autenticar al usuario mediante Firebase Auth con correo y contraseña
    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> {
        val instanciaAuth = try {
            obtenerFirebaseAuth()
        } catch (excepcionInicio: Exception) {
            return Result.failure(
                Exception("Firebase no está inicializado. Verifique el archivo google-services.json.", excepcionInicio)
            )
        }

        return suspendCancellableCoroutine { continuacion ->
            instanciaAuth.signInWithEmailAndPassword(email, clave)
                .addOnSuccessListener { resultadoAutenticacion ->
                    val usuarioFirebase = resultadoAutenticacion.user
                    if (usuarioFirebase != null) {
                        val usuarioDomain = Usuario(
                            id = usuarioFirebase.uid,
                            email = usuarioFirebase.email ?: email,
                            nombre = usuarioFirebase.displayName ?: "",
                            fotoUrl = usuarioFirebase.photoUrl?.toString() ?: ""
                        )
                        if (continuacion.isActive) {
                            continuacion.resume(Result.success(usuarioDomain))
                        }
                    } else {
                        if (continuacion.isActive) {
                            continuacion.resume(Result.failure(Exception("No se obtuvo la información del usuario")))
                        }
                    }
                }
                .addOnFailureListener { excepcion ->
                    if (continuacion.isActive) {
                        continuacion.resume(Result.failure(excepcion))
                    }
                }
        }
    }

    // Autenticar al usuario mediante Firebase Auth utilizando el credencial de Google
    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> {
        val instanciaAuth = try {
            obtenerFirebaseAuth()
        } catch (excepcionInicio: Exception) {
            return Result.failure(
                Exception("Firebase no está inicializado. Verifique el archivo google-services.json.", excepcionInicio)
            )
        }

        val credencialGoogle = GoogleAuthProvider.getCredential(idToken, null)

        return suspendCancellableCoroutine { continuacion ->
            instanciaAuth.signInWithCredential(credencialGoogle)
                .addOnSuccessListener { resultadoAutenticacion ->
                    val usuarioFirebase = resultadoAutenticacion.user
                    if (usuarioFirebase != null) {
                        val usuarioDomain = Usuario(
                            id = usuarioFirebase.uid,
                            email = usuarioFirebase.email ?: "",
                            nombre = usuarioFirebase.displayName ?: "",
                            fotoUrl = usuarioFirebase.photoUrl?.toString() ?: ""
                        )
                        if (continuacion.isActive) {
                            continuacion.resume(Result.success(usuarioDomain))
                        }
                    } else {
                        if (continuacion.isActive) {
                            continuacion.resume(Result.failure(Exception("No se obtuvo la información del usuario desde Google")))
                        }
                    }
                }
                .addOnFailureListener { excepcion ->
                    if (continuacion.isActive) {
                        continuacion.resume(Result.failure(excepcion))
                    }
                }
        }
    }

    // Obtener la información del usuario con sesión activa en Firebase
    override fun obtenerUsuarioActual(): Usuario? {
        val instanciaAuth = try {
            obtenerFirebaseAuth()
        } catch (e: Exception) {
            return null
        }
        val usuarioFirebase = instanciaAuth.currentUser ?: return null
        return Usuario(
            id = usuarioFirebase.uid,
            email = usuarioFirebase.email ?: "",
            nombre = usuarioFirebase.displayName ?: "",
            fotoUrl = usuarioFirebase.photoUrl?.toString() ?: ""
        )
    }

    // Finalizar la sesión actual del usuario autenticado
    override fun cerrarSesion() {
        try {
            obtenerFirebaseAuth().signOut()
        } catch (_: Exception) {
        }
    }
}
