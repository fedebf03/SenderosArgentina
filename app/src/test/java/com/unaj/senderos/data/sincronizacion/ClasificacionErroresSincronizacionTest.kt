package com.unaj.senderos.data.sincronizacion

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClasificacionErroresSincronizacionTest {

    // Crear una excepción de Firestore con el código indicado
    private fun crearError(codigo: Code): FirebaseFirestoreException {
        return FirebaseFirestoreException("fallo", codigo)
    }

    // Probar que los códigos transitorios de Firestore se reintentan
    @Test
    fun `los codigos transitorios son reintentables`() {
        listOf(Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED, Code.ABORTED, Code.RESOURCE_EXHAUSTED).forEach { codigo ->
            assertTrue("$codigo", esErrorReintentable(crearError(codigo)))
        }
    }

    // Probar que los códigos permanentes de Firestore no se reintentan
    @Test
    fun `los codigos permanentes no son reintentables`() {
        listOf(Code.PERMISSION_DENIED, Code.UNAUTHENTICATED, Code.INVALID_ARGUMENT, Code.NOT_FOUND).forEach { codigo ->
            assertFalse("$codigo", esErrorReintentable(crearError(codigo)))
        }
    }

    // Probar que cualquier otro error se considera transitorio
    @Test
    fun `un error ajeno a Firestore es reintentable`() {
        assertTrue(esErrorReintentable(IllegalStateException("otro")))
        assertTrue(esErrorReintentable(null))
    }
}
