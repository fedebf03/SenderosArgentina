package com.unaj.senderos.navegacion

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras

class AlmacenDestinosViewModel : ViewModel() {
    private val almacenesPorDestino = mutableMapOf<String, ViewModelStore>()

    fun obtenerPropietario(claveDestino: String, actividad: HasDefaultViewModelProviderFactory): ViewModelStoreOwner {
        val almacen = almacenesPorDestino.getOrPut(claveDestino) { ViewModelStore() }
        return PropietarioDeDestino(almacen, actividad)
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

// Hilt crea los ViewModels con la fábrica de la Activity, pero cada destino los guarda en su propio almacén
private class PropietarioDeDestino(
    override val viewModelStore: ViewModelStore,
    private val actividad: HasDefaultViewModelProviderFactory
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = actividad.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras(actividad.defaultViewModelCreationExtras).apply {
            set(VIEW_MODEL_STORE_OWNER_KEY, this@PropietarioDeDestino)
        }
}
