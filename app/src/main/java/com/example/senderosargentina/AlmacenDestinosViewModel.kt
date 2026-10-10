package com.example.senderosargentina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

// Guardar un almacén de ViewModels por destino para poder liberarlos al salir de cada uno
class AlmacenDestinosViewModel : ViewModel() {
    private val almacenesPorDestino = mutableMapOf<String, ViewModelStore>()

    // Obtener el propietario de ViewModels asociado a la clave del destino, creándolo si no existe
    fun obtenerPropietario(claveDestino: String): ViewModelStoreOwner {
        val almacen = almacenesPorDestino.getOrPut(claveDestino) { ViewModelStore() }
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = almacen
        }
    }

    // Liberar los ViewModels del destino indicado y olvidar su almacén
    fun liberarDestino(claveDestino: String) {
        almacenesPorDestino.remove(claveDestino)?.clear()
    }

    // Liberar todos los destinos cuando se destruye la actividad definitivamente
    override fun onCleared() {
        super.onCleared()
        almacenesPorDestino.values.forEach { almacen -> almacen.clear() }
        almacenesPorDestino.clear()
    }
}
