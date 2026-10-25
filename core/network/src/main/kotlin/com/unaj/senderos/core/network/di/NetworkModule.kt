package com.unaj.senderos.core.network.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.senderos.core.network.perfil.FirestorePerfilFuenteRemota
import com.unaj.senderos.core.network.perfil.PerfilFuenteRemota
import com.unaj.senderos.core.network.recorrido.FirestoreRecorridoFuenteRemota
import com.unaj.senderos.core.network.sendero.FirestoreSenderoFuenteRemota
import com.unaj.senderos.core.network.senderopublico.FirestoreSenderoPublicoFuenteRemota
import com.unaj.senderos.core.network.recorrido.RecorridoFuenteRemota
import com.unaj.senderos.core.network.sendero.SenderoFuenteRemota
import com.unaj.senderos.core.network.senderopublico.SenderoPublicoFuenteRemota
import com.unaj.senderos.core.network.fotos.SubidorFotos
import com.unaj.senderos.core.network.fotos.SubidorFotosCloudinary
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.unaj.senderos.core.network.auth.FirebaseFuenteAutenticacion
import com.unaj.senderos.core.network.auth.FuenteAutenticacion
import com.unaj.senderos.core.network.openmeteo.ApiElevacionOpenMeteo
import com.unaj.senderos.core.network.openmeteo.ApiPronosticoOpenMeteo
import com.unaj.senderos.core.network.openmeteo.crearApiElevacionOpenMeteo
import com.unaj.senderos.core.network.openmeteo.crearApiPronosticoOpenMeteo
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindSenderoFuenteRemota(impl: FirestoreSenderoFuenteRemota): SenderoFuenteRemota

    @Binds
    abstract fun bindRecorridoFuenteRemota(impl: FirestoreRecorridoFuenteRemota): RecorridoFuenteRemota

    @Binds
    abstract fun bindSenderoPublicoFuenteRemota(impl: FirestoreSenderoPublicoFuenteRemota): SenderoPublicoFuenteRemota

    @Binds
    abstract fun bindPerfilFuenteRemota(impl: FirestorePerfilFuenteRemota): PerfilFuenteRemota

    @Binds
    abstract fun bindFuenteAutenticacion(impl: FirebaseFuenteAutenticacion): FuenteAutenticacion

    @Binds
    abstract fun bindSubidorFotos(impl: SubidorFotosCloudinary): SubidorFotos

    companion object {

        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideApiElevacion(): ApiElevacionOpenMeteo = crearApiElevacionOpenMeteo()

        @Provides
        @Singleton
        fun provideApiPronostico(): ApiPronosticoOpenMeteo = crearApiPronosticoOpenMeteo()

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
