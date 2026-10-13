package com.unaj.senderos.presentation.navegacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class AlmacenDestinosViewModel : ViewModel() {
    private val almacenesPorDestino = mutableMapOf<String, ViewModelStore>()

    fun obtenerPropietario(claveDestino: String): ViewModelStoreOwner {
        val almacen = almacenesPorDestino.getOrPut(claveDestino) { ViewModelStore() }
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = almacen
        }
    }

    fun liberarDestino(claveDestino: String) {
        almacenesPorDestino.remove(claveDestino)?.clear()
    }

    override fun onCleared() {
        super.onCleared()
        almacenesPorDestino.values.forEach { almacen -> almacen.clear() }
        almacenesPorDestino.clear()
    }
}
