package com.example.senderosargentina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class AlmacenDestinosViewModel : ViewModel() {
    private val almacenesPorDestino = mutableMapOf<String, ViewModelStore>()

    // Obtener el propietario del almacén de ViewModels de un destino, creándolo si no existe
    fun obtenerPropietario(claveDestino: String): ViewModelStoreOwner {
        val almacen = almacenesPorDestino.getOrPut(claveDestino) { ViewModelStore() }
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = almacen
        }
    }

    // Liberar los ViewModels de un destino que ya no está en pantalla
    fun liberarDestino(claveDestino: String) {
        almacenesPorDestino.remove(claveDestino)?.clear()
    }

    // Liberar todos los destinos restantes cuando se destruye la Activity
    override fun onCleared() {
        super.onCleared()
        almacenesPorDestino.values.forEach { almacen -> almacen.clear() }
        almacenesPorDestino.clear()
    }
}
